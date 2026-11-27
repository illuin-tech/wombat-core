package tech.illuin.wombat.module.kubernetes_api.profile;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.profile.BareServerProfile;
import tech.illuin.wombat.core.asset.profile.InferredProfile;
import tech.illuin.wombat.core.asset.profile.Lifetime;
import tech.illuin.wombat.core.asset.profile.ServerProfile;
import tech.illuin.wombat.core.asset.profile.ServerProvider;
import tech.illuin.wombat.module.kubernetes_api.profile.ClusterInventory.Container;
import tech.illuin.wombat.module.kubernetes_api.profile.ClusterInventory.Node;
import tech.illuin.wombat.module.kubernetes_api.profile.ClusterInventory.Pod;
import tech.illuin.wombat.module.kubernetes_api.profile.ClusterInventory.Quota;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KubernetesProfileInferenceTest
{
    private static final double GB = 1024.0 * 1024.0 * 1024.0;
    private static final Hints NO_HINTS = new Hints(null, null);

    private final KubernetesProfileInference inference = new KubernetesProfileInference();
    private final FakeInventory inventory = new FakeInventory();

    @Test
    void nodesOfAKnownProviderAndInstanceTypeMakeACloudProfile() throws ProfileInferenceException
    {
        this.inventory.nodes = List.of(
            awsNode("a", "m5.xlarge"),
            awsNode("b", "m5.xlarge"),
            awsNode("c", "c5.large")
        );

        var profile = assertInstanceOf(InferredCloudServerProfile.class, this.inference.infer(this.inventory, Set.of("payments"), NO_HINTS));

        assertEquals(ServerProvider.aws, profile.provider());
        assertEquals("m5.xlarge", profile.instanceType());
        assertSame(Country.WORLD, profile.location());
        assertEquals(Lifetime.ofYears(5), profile.lifespan());
        assertEquals(2, profile.nodeCount());
        assertEquals(KubernetesProfileOrigin.NODES, profile.origin());
        assertEquals(List.of(
            "No location set, the world average is assumed",
            "No lifespan set, 5y is assumed",
            "Nodes are of several kinds, only the most common one is profiled (2 out of 3 nodes)"
        ), profile.warnings());
        assertEquals(List.of("nodes"), this.inventory.reads);
    }

    @Test
    void userFieldsWinOverInferredOnes() throws ProfileInferenceException
    {
        this.inventory.nodes = List.of(awsNode("a", "m5.xlarge"));

        ServerProfile profile = this.inference.infer(this.inventory, Set.of(), new Hints(Country.GERMANY, Lifetime.ofYears(6)));

        assertSame(Country.GERMANY, profile.location());
        assertEquals(Lifetime.ofYears(6), profile.lifespan());
        assertEquals(List.of(), ((InferredProfile) profile).warnings());
    }

    @Test
    void nodesOfAnUnknownProviderMakeABareServerOfTheirCapacity() throws ProfileInferenceException
    {
        this.inventory.nodes = List.of(
            new Node("a", "kind://docker/kind/a", Map.of("node.kubernetes.io/instance-type", "k3s"), new ResourceAmount(8, 32 * GB)),
            new Node("b", null, Map.of(), new ResourceAmount(8, 32 * GB))
        );

        var profile = assertInstanceOf(InferredBareServerProfile.class, this.inference.infer(this.inventory, Set.of(), NO_HINTS));

        assertEquals(new BareServerProfile.CPU(1, 4, 2, null), profile.cpu());
        assertEquals(new BareServerProfile.RAM(1, 32), profile.ram());
        assertSame(Country.WORLD, profile.location());
        assertEquals(KubernetesProfileOrigin.NODES, profile.origin());
    }

    @Test
    void theManagedApiServerTellsTheProviderNodesDoNot() throws ProfileInferenceException
    {
        this.inventory.host = "abc.c1.gra9.k8s.ovh.net";
        this.inventory.nodes = List.of(new Node("a", "openstack:///1234", Map.of("node.kubernetes.io/instance-type", "b3-8"), new ResourceAmount(2, 8 * GB)));

        var profile = assertInstanceOf(InferredCloudServerProfile.class, this.inference.infer(this.inventory, Set.of(), NO_HINTS));

        assertEquals(ServerProvider.ovhcloud, profile.provider());
        assertEquals("b3-8", profile.instanceType());
        assertSame(Country.WORLD, profile.location());
    }

    @Test
    void forbiddenNodesFallBackToTheNamespacesQuotas() throws ProfileInferenceException
    {
        this.inventory.host = "abc.gr7.eu-west-3.eks.amazonaws.com";
        this.inventory.nodesForbidden = true;
        this.inventory.quotas.put("payments", List.of(
            new Quota("payments", "compute", Map.of("requests.cpu", 4.0, "requests.memory", 16 * GB, "limits.cpu", 8.0)),
            new Quota("payments", "tighter", Map.of("cpu", 3.0))
        ));
        this.inventory.quotas.put("analytics", List.of(new Quota("analytics", "compute", Map.of("limits.cpu", 2.0, "limits.memory", 4 * GB))));

        var profile = assertInstanceOf(InferredBareServerProfile.class, this.inference.infer(this.inventory, Set.of("payments", "analytics"), NO_HINTS));

        assertEquals(5, profile.cpu().vcpu());
        assertEquals(new BareServerProfile.RAM(1, 20), profile.ram());
        assertSame(Country.WORLD, profile.location());
        assertEquals(KubernetesProfileOrigin.RESOURCE_QUOTAS, profile.origin());
        assertTrue(this.inventory.reads.stream().noneMatch("pods"::equals));
    }

    @Test
    void aMissingMemoryQuotaIsEstimatedFromTheCpuOne() throws ProfileInferenceException
    {
        this.inventory.nodesForbidden = true;
        this.inventory.quotas.put("payments", List.of(new Quota("payments", "compute", Map.of("requests.cpu", 2.0))));

        var profile = assertInstanceOf(InferredBareServerProfile.class, this.inference.infer(this.inventory, Set.of("payments"), NO_HINTS));

        assertEquals(new BareServerProfile.RAM(1, 8), profile.ram());
        assertTrue(profile.warnings().contains("No memory quota in namespaces payments, 4 GiB per core is assumed"), profile.warnings().toString());
        assertTrue(profile.warnings().contains("Sized after the resource quotas of namespaces payments: a slice of the cluster, not its nodes"), profile.warnings().toString());
    }

    @Test
    void aNamespaceWithoutCpuQuotaFallsBackToTheContainers() throws ProfileInferenceException
    {
        this.inventory.nodesForbidden = true;
        this.inventory.quotas.put("payments", List.of(new Quota("payments", "compute", Map.of("requests.cpu", 2.0))));
        this.inventory.quotas.put("analytics", List.of(new Quota("analytics", "objects", Map.of("pods", 10.0))));
        this.inventory.pods = List.of(pod("Running", new Container(new ResourceAmount(1.5, 2 * GB), ResourceAmount.ZERO, false)));

        var profile = assertInstanceOf(InferredBareServerProfile.class, this.inference.infer(this.inventory, Set.of("payments", "analytics"), NO_HINTS));

        assertEquals(KubernetesProfileOrigin.CONTAINER_REQUESTS, profile.origin());
        assertEquals(2, profile.cpu().vcpu());
        assertEquals(new BareServerProfile.RAM(1, 2), profile.ram());
    }

    @Test
    void quotasAreSkippedWhenEveryNamespaceIsPolled() throws ProfileInferenceException
    {
        this.inventory.nodesForbidden = true;
        this.inventory.pods = List.of(pod("Running", new Container(new ResourceAmount(3, 4 * GB), ResourceAmount.ZERO, false)));

        var profile = assertInstanceOf(InferredBareServerProfile.class, this.inference.infer(this.inventory, Set.of(), NO_HINTS));

        assertEquals(KubernetesProfileOrigin.CONTAINER_REQUESTS, profile.origin());
        assertTrue(this.inventory.reads.stream().noneMatch(read -> read.startsWith("quotas")));
    }

    @Test
    void terminatedPodsAreNotCounted() throws ProfileInferenceException
    {
        this.inventory.nodesForbidden = true;
        this.inventory.pods = List.of(
            pod("Running", new Container(new ResourceAmount(2, 4 * GB), ResourceAmount.ZERO, false)),
            pod("Succeeded", new Container(new ResourceAmount(16, 64 * GB), ResourceAmount.ZERO, false)),
            pod("Failed", new Container(new ResourceAmount(16, 64 * GB), ResourceAmount.ZERO, false))
        );

        var profile = assertInstanceOf(InferredBareServerProfile.class, this.inference.infer(this.inventory, Set.of(), NO_HINTS));

        assertEquals(2, profile.cpu().vcpu());
    }

    @Test
    void containersWithoutRequestsAreSizedByTheirLimits() throws ProfileInferenceException
    {
        this.inventory.nodesForbidden = true;
        this.inventory.pods = List.of(pod("Running", new Container(ResourceAmount.ZERO, new ResourceAmount(1, GB), false)));

        var profile = assertInstanceOf(InferredBareServerProfile.class, this.inference.infer(this.inventory, Set.of(), NO_HINTS));

        assertEquals(KubernetesProfileOrigin.CONTAINER_LIMITS, profile.origin());
        assertEquals(new BareServerProfile.CPU(1, 1, 1, null), profile.cpu());
    }

    @Test
    void aPodReservesItsLargestPhasePlusItsOverhead()
    {
        Pod pod = new Pod("ns", "pod", "Running",
            List.of(container(1, 1), container(1, 1)),
            List.of(container(0.5, 4), sidecar(0.5, 1), container(3, 1)),
            new ResourceAmount(0.25, 0)
        );

        ResourceAmount reserved = KubernetesProfileInference.effective(pod, Container::requests);

        // CPU: the last init container with the sidecar (3.5) beats the containers with it (2.5); memory: the first init container (4)
        assertEquals(new ResourceAmount(3.75, 4), reserved);
    }

    @Test
    void failsWithTheReasonOfEachSourceWhenNoneTells()
    {
        this.inventory.nodesForbidden = true;
        this.inventory.podsForbidden = true;

        var e = assertThrows(ProfileInferenceException.class, () -> this.inference.infer(this.inventory, Set.of("payments"), NO_HINTS));

        assertTrue(e.getMessage().contains("nodes unreadable (forbidden)"), e.getMessage());
        assertTrue(e.getMessage().contains("no CPU quota in namespace payments"), e.getMessage());
        assertTrue(e.getMessage().contains("pods unreadable (forbidden)"), e.getMessage());
    }

    private static Node awsNode(String name, String instanceType)
    {
        return new Node(name, "aws:///eu-west-3a/i-" + name, Map.of(
            "node.kubernetes.io/instance-type", instanceType
        ), new ResourceAmount(4, 16 * GB));
    }

    private static Pod pod(String phase, Container container)
    {
        return new Pod("payments", "pod", phase, List.of(container), List.of(), null);
    }

    private static Container container(double cpu, double memory)
    {
        return new Container(new ResourceAmount(cpu, memory), ResourceAmount.ZERO, false);
    }

    private static Container sidecar(double cpu, double memory)
    {
        return new Container(new ResourceAmount(cpu, memory), ResourceAmount.ZERO, true);
    }

    private record Hints(Country location, Lifetime lifespan) implements InferableServerProfile {}

    private static final class FakeInventory implements ClusterInventory
    {
        private List<Node> nodes = List.of();
        private boolean nodesForbidden;
        private final Map<String, List<Quota>> quotas = new HashMap<>();
        private List<Pod> pods = List.of();
        private boolean podsForbidden;
        private String host;
        private final List<String> reads = new ArrayList<>();

        @Override
        public List<Node> nodes() throws InventoryUnavailableException
        {
            this.reads.add("nodes");
            if (this.nodesForbidden)
                throw new InventoryUnavailableException("forbidden");
            return this.nodes;
        }

        @Override
        public List<Quota> quotas(String namespace)
        {
            this.reads.add("quotas/" + namespace);
            return this.quotas.getOrDefault(namespace, List.of());
        }

        @Override
        public List<Pod> pods(Set<String> namespaces) throws InventoryUnavailableException
        {
            this.reads.add("pods");
            if (this.podsForbidden)
                throw new InventoryUnavailableException("forbidden");
            return this.pods;
        }

        @Override
        public Optional<String> apiServerHost()
        {
            return Optional.ofNullable(this.host);
        }

        @Override
        public void close() {}
    }
}
