package tech.illuin.wombat.module.kubernetes_api.profile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.profile.Lifetime;
import tech.illuin.wombat.core.asset.profile.ServerProfile;
import tech.illuin.wombat.core.asset.profile.ServerProvider;
import tech.illuin.wombat.module.kubernetes_api.profile.ClusterInventory.Container;
import tech.illuin.wombat.module.kubernetes_api.profile.ClusterInventory.Node;
import tech.illuin.wombat.module.kubernetes_api.profile.ClusterInventory.Pod;
import tech.illuin.wombat.module.kubernetes_api.profile.ClusterInventory.Quota;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Infers the server profile of a Kubernetes cluster from the first of these sources its credentials can read:
 * <ol>
 *     <li>its nodes: their instance type when their provider is known, their capacity otherwise;</li>
 *     <li>the resource quotas of the polled namespaces;</li>
 *     <li>the resources requested by the containers of the polled namespaces, or else their limits.</li>
 * </ol>
 * Only nodes of a known provider and instance type make a cloud profile: every other source makes a bare server one,
 * quotas and containers sizing a slice of the cluster rather than its nodes.
 * <p>
 * The location and lifespan are the ones set by the user, or else the world and {@link #DEFAULT_LIFESPAN}.
 */
public class KubernetesProfileInference
{
    public static final Country DEFAULT_LOCATION = Country.WORLD;
    public static final Lifetime DEFAULT_LIFESPAN = Lifetime.ofYears(5);
    /** Memory assumed for a slice whose quota or containers only set CPU, the ratio of general-purpose instances. */
    public static final double DEFAULT_MEMORY_BYTES_PER_CORE = 4.0 * 1024 * 1024 * 1024;

    private static final String DEFAULT_MEMORY_PER_CORE_TEXT = "4 GiB";
    private static final String UNKNOWN_DISKS = "Disks are unknown, the impact model's default ones are assumed";

    private static final List<String> CPU_QUOTA_KEYS = List.of("requests.cpu", "cpu", "limits.cpu");
    private static final List<String> MEMORY_QUOTA_KEYS = List.of("requests.memory", "memory", "limits.memory");
    private static final Set<String> TERMINATED_PHASES = Set.of("Succeeded", "Failed");

    private static final Logger logger = LoggerFactory.getLogger(KubernetesProfileInference.class);

    /**
     * @param namespaces the polled namespaces, all of them when empty
     * @param hints the fields set by the user, which win over inferred ones
     */
    public ServerProfile infer(ClusterInventory inventory, Set<String> namespaces, InferableServerProfile hints) throws ProfileInferenceException
    {
        Country location = hints.location() != null ? hints.location() : DEFAULT_LOCATION;
        Lifetime lifespan = hints.lifespan() != null ? hints.lifespan() : DEFAULT_LIFESPAN;
        List<String> failures = new ArrayList<>();
        List<String> defaults = new ArrayList<>();
        if (hints.location() == null)
            defaults.add("No location set, the world average is assumed");
        if (hints.lifespan() == null)
            defaults.add("No lifespan set, " + DEFAULT_LIFESPAN.asText() + " is assumed");

        Optional<ServerProfile> profile = this.fromNodes(inventory, location, lifespan, defaults, failures);
        if (profile.isEmpty())
            profile = this.fromQuotas(inventory, namespaces, location, lifespan, defaults, failures);
        if (profile.isEmpty())
            profile = this.fromContainers(inventory, namespaces, location, lifespan, defaults, failures);

        return profile.orElseThrow(() -> new ProfileInferenceException("Cannot infer the server profile: " + String.join("; ", failures)));
    }

    private Optional<ServerProfile> fromNodes(ClusterInventory inventory, Country location, Lifetime lifespan, List<String> defaults, List<String> failures)
    {
        try {
            List<Node> nodes = inventory.nodes();
            if (nodes.isEmpty())
            {
                failures.add("no node listed");
                return Optional.empty();
            }

            Optional<ServerProvider> clusterProvider = inventory.apiServerHost().flatMap(CloudPlacement::apiServerProvider);
            List<NodeKind> kinds = nodes.stream()
                .sorted(Comparator.comparing(Node::name, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(node -> NodeKind.of(node, clusterProvider))
                .toList();
            List<NodeKind> dominant = dominant(kinds, NodeKind::key);
            NodeKind kind = dominant.getFirst();
            List<String> warnings = new ArrayList<>(defaults);
            if (dominant.size() < kinds.size())
            {
                logger.info("Nodes are of several kinds, inferring the profile of the most common one ({} out of {} nodes)", dominant.size(), kinds.size());
                warnings.add("Nodes are of several kinds, only the most common one is profiled (" + dominant.size() + " out of " + kinds.size() + " nodes)");
            }

            if (kind.isCloud())
            {
                logger.info("Inferred a {} {} profile in {} from {} node(s)", kind.provider(), kind.instanceType(), location.alpha3(), dominant.size());
                return Optional.of(new InferredCloudServerProfile(kind.provider(), kind.instanceType(), location, lifespan, dominant.size(), KubernetesProfileOrigin.NODES, warnings));
            }

            if (kind.capacity().cpuCores() <= 0)
            {
                failures.add("nodes tell neither a known provider and instance type nor their capacity");
                return Optional.empty();
            }
            logger.info("Nodes are of no known provider and instance type, inferring a bare server of their capacity {} from {} node(s)", kind.capacity(), dominant.size());
            warnings.add("Nodes are of no known provider and instance type, a server of their capacity is assumed");
            warnings.add(UNKNOWN_DISKS);
            return Optional.of(InferredBareServerProfile.of(kind.capacity(), location, lifespan, dominant.size(), KubernetesProfileOrigin.NODES, warnings));
        }
        catch (InventoryUnavailableException e) {
            failures.add("nodes unreadable (" + e.getMessage() + ")");
            return Optional.empty();
        }
    }

    private Optional<ServerProfile> fromQuotas(ClusterInventory inventory, Set<String> namespaces, Country location, Lifetime lifespan, List<String> defaults, List<String> failures)
    {
        if (namespaces.isEmpty())
        {
            failures.add("quotas only bound the polled namespaces, and all are polled");
            return Optional.empty();
        }

        ResourceAmount total = ResourceAmount.ZERO;
        List<String> memoryUnbounded = new ArrayList<>();
        for (String namespace : new TreeSet<>(namespaces))
        {
            Optional<List<Quota>> read = readQuotas(inventory, namespace, failures);
            if (read.isEmpty())
                return Optional.empty();
            List<Quota> quotas = read.get();

            Optional<Double> cpu = tightest(quotas, CPU_QUOTA_KEYS);
            if (cpu.isEmpty())
            {
                failures.add("no CPU quota in namespace " + namespace);
                return Optional.empty();
            }
            Optional<Double> memory = tightest(quotas, MEMORY_QUOTA_KEYS);
            if (memory.isEmpty())
                memoryUnbounded.add(namespace);
            total = total.plus(new ResourceAmount(cpu.get(), memory.orElse(cpu.get() * DEFAULT_MEMORY_BYTES_PER_CORE)));
        }

        List<String> warnings = new ArrayList<>(defaults);
        warnings.add("Sized after the resource quotas of namespaces " + String.join(", ", new TreeSet<>(namespaces)) + ": a slice of the cluster, not its nodes");
        if (!memoryUnbounded.isEmpty())
        {
            logger.info("Some namespaces have no memory quota, assuming {} bytes per core for them", DEFAULT_MEMORY_BYTES_PER_CORE);
            warnings.add("No memory quota in namespaces " + String.join(", ", memoryUnbounded) + ", " + DEFAULT_MEMORY_PER_CORE_TEXT + " per core is assumed");
        }
        warnings.add(UNKNOWN_DISKS);
        logger.info("Inferred a bare server profile of {} from the quotas of namespaces {}", total, namespaces);
        return Optional.of(InferredBareServerProfile.of(total, location, lifespan, 1, KubernetesProfileOrigin.RESOURCE_QUOTAS, warnings));
    }

    /**
     * @return the quotas of {@code namespace}, empty when they cannot be read, which is added to {@code failures}
     */
    private static Optional<List<Quota>> readQuotas(ClusterInventory inventory, String namespace, List<String> failures)
    {
        try {
            return Optional.of(inventory.quotas(namespace));
        }
        catch (InventoryUnavailableException e) {
            failures.add("quotas of namespace " + namespace + " unreadable (" + e.getMessage() + ")");
            return Optional.empty();
        }
    }

    /**
     * Several quotas of a namespace all apply, so the lowest bounds it; each quota bounds a resource with the first of
     * {@code keys} it sets, requests coming before limits.
     */
    private static Optional<Double> tightest(List<Quota> quotas, List<String> keys)
    {
        return quotas.stream()
            .map(quota -> keys.stream().map(quota.hard()::get).filter(Objects::nonNull).findFirst())
            .flatMap(Optional::stream)
            .min(Double::compare);
    }

    private Optional<ServerProfile> fromContainers(ClusterInventory inventory, Set<String> namespaces, Country location, Lifetime lifespan, List<String> defaults, List<String> failures)
    {
        try {
            List<Pod> pods = inventory.pods(namespaces).stream()
                .filter(pod -> !TERMINATED_PHASES.contains(pod.phase()))
                .toList();

            ResourceAmount requests = sum(pods, Container::requests);
            if (requests.cpuCores() > 0)
                return Optional.of(this.slice(requests, location, lifespan, KubernetesProfileOrigin.CONTAINER_REQUESTS, pods.size(), defaults));

            ResourceAmount limits = sum(pods, Container::limits);
            if (limits.cpuCores() > 0)
                return Optional.of(this.slice(limits, location, lifespan, KubernetesProfileOrigin.CONTAINER_LIMITS, pods.size(), defaults));

            failures.add(pods.isEmpty() ? "no running pod" : "no container requests nor limits CPU");
            return Optional.empty();
        }
        catch (InventoryUnavailableException e) {
            failures.add("pods unreadable (" + e.getMessage() + ")");
            return Optional.empty();
        }
    }

    private ServerProfile slice(ResourceAmount resources, Country location, Lifetime lifespan, KubernetesProfileOrigin origin, int podCount, List<String> defaults)
    {
        List<String> warnings = new ArrayList<>(defaults);
        String sizedAfter = origin == KubernetesProfileOrigin.CONTAINER_REQUESTS ? "resource requests" : "resource limits, none requesting any";
        warnings.add("Sized after the " + sizedAfter + " of " + podCount + " pod(s): a slice of the cluster, not its nodes");
        ResourceAmount sized = resources;
        if (resources.memoryBytes() <= 0)
        {
            sized = new ResourceAmount(resources.cpuCores(), resources.cpuCores() * DEFAULT_MEMORY_BYTES_PER_CORE);
            warnings.add("No container sets its memory, " + DEFAULT_MEMORY_PER_CORE_TEXT + " per core is assumed");
        }
        warnings.add(UNKNOWN_DISKS);
        logger.info("Inferred a bare server profile of {} from the {} of {} pod(s)", sized, origin, podCount);
        return InferredBareServerProfile.of(sized, location, lifespan, 1, origin, warnings);
    }

    private static ResourceAmount sum(List<Pod> pods, Function<Container, ResourceAmount> resources)
    {
        return pods.stream()
            .map(pod -> effective(pod, resources))
            .reduce(ResourceAmount.ZERO, ResourceAmount::plus);
    }

    /**
     * The resources Kubernetes reserves for a pod: the largest of its running containers, sidecars included, and of
     * each init container alongside the sidecars started before it, plus the pod's overhead.
     */
    static ResourceAmount effective(Pod pod, Function<Container, ResourceAmount> resources)
    {
        ResourceAmount sidecars = ResourceAmount.ZERO;
        ResourceAmount initPeak = ResourceAmount.ZERO;
        for (Container init : pod.initContainers())
        {
            if (init.sidecar())
                sidecars = sidecars.plus(resources.apply(init));
            else
                initPeak = initPeak.max(sidecars.plus(resources.apply(init)));
        }

        ResourceAmount running = pod.containers().stream()
            .map(resources)
            .reduce(sidecars, ResourceAmount::plus);
        return running.max(initPeak).plus(pod.overhead());
    }

    /**
     * @return the items sharing the most common key, in their order; ties go to the key met first
     */
    private static <T, K> List<T> dominant(List<T> items, Function<T, K> key)
    {
        Map<K, List<T>> groups = items.stream().collect(Collectors.groupingBy(key, LinkedHashMap::new, Collectors.toList()));
        return groups.values().stream()
            .reduce((best, group) -> group.size() > best.size() ? group : best)
            .orElse(List.of());
    }

    /**
     * @param provider null when unknown
     * @param instanceType null when unknown
     */
    private record NodeKind(
        ServerProvider provider,
        String instanceType,
        ResourceAmount capacity
    ) {
        /**
         * @param clusterProvider the provider of the managed API server, for nodes that do not tell theirs
         */
        static NodeKind of(Node node, Optional<ServerProvider> clusterProvider)
        {
            return new NodeKind(
                CloudPlacement.provider(node.providerId(), node.labels()).or(() -> clusterProvider).orElse(null),
                CloudPlacement.instanceType(node.labels()).orElse(null),
                node.capacity() != null ? node.capacity() : ResourceAmount.ZERO
            );
        }

        boolean isCloud()
        {
            return this.provider != null && this.instanceType != null;
        }

        /**
         * Cloud nodes are alike when of the same instance type, the others when of the same capacity.
         */
        Object key()
        {
            return this.isCloud() ? List.of(this.provider, this.instanceType) : this.capacity;
        }
    }
}
