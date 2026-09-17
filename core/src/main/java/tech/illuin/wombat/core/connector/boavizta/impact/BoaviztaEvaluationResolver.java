package tech.illuin.wombat.core.connector.boavizta.impact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.ServiceFamily;
import tech.illuin.wombat.core.asset.profile.ServerProvider;
import tech.illuin.wombat.core.connector.boavizta.connector.BoaviztaClient;
import tech.illuin.wombat.core.connector.boavizta.connector.model.BoaviztaImpactResponse;
import tech.illuin.wombat.core.connector.boavizta.connector.model.BoaviztaInstanceConfigResponse;
import tech.illuin.wombat.core.connector.boavizta.connector.model.BoaviztaInstanceImpactRequest;
import tech.illuin.wombat.core.connector.boavizta.connector.model.BoaviztaServerProvider;
import tech.illuin.wombat.core.evaluation.AssetEvaluation;
import tech.illuin.wombat.core.evaluation.WombatEvaluationException;
import tech.illuin.wombat.core.evaluation.WombatEvaluationResolver;
import tech.illuin.wombat.core.evaluation.impact.commons.*;
import tech.illuin.wombat.core.evaluation.impact.kubernetes.KubernetesImpact;
import tech.illuin.wombat.core.activity.kubernetes.KubernetesActivityData;
import tech.illuin.wombat.core.asset.profile.ServerProfile;
import tech.illuin.wombat.core.evaluation.impact.kubernetes.ServerSpecification;

import java.time.Duration;
import java.util.*;

public class BoaviztaEvaluationResolver implements WombatEvaluationResolver
{
    private final BoaviztaClient client;

    private static final double NANOCORES_PER_CORE = 1_000_000_000.0;
    private static final double FULL_LOAD = 100.0;
    private static final String rebalanceWarning = "This usage has been rebalanced by the Wombat service, it does not come from the Boavizta API";
    private static final String timeProrationWarning = "This impact has been prorated to the requested time range by the Wombat service";

    private static final Logger logger = LoggerFactory.getLogger(BoaviztaEvaluationResolver.class);

    public BoaviztaEvaluationResolver(BoaviztaClient client)
    {
        this.client = client;
    }

    @Override
    public boolean accept(Asset asset)
    {
        return asset.profile().serviceFamily() == ServiceFamily.KUBERNETES_CONTAINER;
    }

    @Override
    public AssetEvaluation resolve(Asset asset, ActivityData activity) throws WombatEvaluationException
    {
        ServerProfile profile = (ServerProfile) asset.profile();
        KubernetesActivityData kubernetesActivityData = (KubernetesActivityData) activity;
        logger.info("Used CPU load {}", kubernetesActivityData.cpuUsage() / NANOCORES_PER_CORE);

        BoaviztaImpactResponse impactResponse = this.computeImpact(profile, kubernetesActivityData.cpuUsage());
        ServerSpecification specification = this.computeSpecification(profile);

        List<Share> includedShares = includedShares(kubernetesActivityData.containerShares(), kubernetesActivityData.serviceIds());

        int vcpu = specification.vcpu() != null ? specification.vcpu() : 0;
        double cpuUsageCore = kubernetesActivityData.cpuUsage() / NANOCORES_PER_CORE;
        double loadPercent = vcpu > 0 ? cpuUsageCore / vcpu * 100.0 : 0.0;
        int nodeCount = specification.nodesRequired(cpuUsageCore);

        Comparator<KubernetesImpact> comparator = Comparator.comparingDouble(
            ki -> ki.footprint().gwp().totalValue()
        );
        List<ServiceImpact> serviceImpacts = includedShares.stream()
            .map(share -> new KubernetesImpact(
                share.serviceId(),
                asset.type(),
                profile,
                share.share(),
                this.convert(impactResponse, share.share(), kubernetesActivityData.range(), profile.lifespan()),
                kubernetesActivityData.containerLocations().get(share.serviceId()),
                specification,
                loadPercent,
                nodeCount
            ))
            .sorted(comparator.reversed())
            .map(ki -> (ServiceImpact) ki)
            .toList()
        ;

        Footprint globalFootprint = this.resolveGlobalFootprint(impactResponse, includedShares, kubernetesActivityData.range(), profile.lifespan());

        return new AssetImpact(
            asset.environmentId(),
            asset.id(),
            globalFootprint,
            serviceImpacts,
            ImpactProvider.BOAVIZTA
        );
    }

    private Footprint resolveGlobalFootprint(
        BoaviztaImpactResponse providerResponse,
        List<Share> shares,
        TimeRange timeRange,
        int lifespanHours
    ) {
        double globalShare = shares.stream().mapToDouble(Share::share).sum();
        return this.convert(providerResponse, globalShare, timeRange, lifespanHours);
    }

    private static List<Share> includedShares(Map<String, Double> containerShares, Collection<String> serviceIds)
    {
        Set<String> serviceFilter = (serviceIds == null || serviceIds.isEmpty())
            ? containerShares.keySet()
            : new HashSet<>(serviceIds);

        return containerShares.entrySet().stream()
            .filter(share -> serviceFilter.contains(share.getKey()))
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .map(share -> new Share(share.getKey(), share.getValue()))
            .toList();
    }

    private BoaviztaImpactResponse computeImpact(ServerProfile profile, double load)
    {
        BoaviztaInstanceConfigResponse instanceConfig = this.getInstanceConfig(profile.provider(), profile.instanceType());

        ServerSpecification specs = convert(instanceConfig);
        int vcpu = specs.vcpu() != null  ? specs.vcpu() : 0;
        double cores = load / NANOCORES_PER_CORE;
        int nodeCount = specs.nodesRequired(cores);

        logger.info("Boavizta instance has {} vCPU; {} cores used spread over {} node(s)", vcpu, cores, nodeCount);

        BoaviztaImpactResponse impact = vcpu > 0
            ? this.computeImpactFromRatio(profile, cores / vcpu)
            : this.requestImpactFromPercentage(profile, 0.0);

        return new BoaviztaImpactResponse(impact.impacts(), nodeCount);
    }

    private BoaviztaImpactResponse computeImpactFromRatio(ServerProfile profile, double loadRatio)
    {
        if (loadRatio <= 1.0)
            return this.requestImpactFromPercentage(profile, loadRatio * FULL_LOAD);

        int fullNodes = (int) Math.floor(loadRatio);
        double remainder = loadRatio - fullNodes;

        logger.info("Load ratio {} exceeds one node: {} node(s) at 100% + remainder {}", loadRatio, fullNodes, remainder);

        BoaviztaImpactResponse total = scale(this.requestImpactFromPercentage(profile, FULL_LOAD), fullNodes);
        if (remainder > 0)
            total = add(total, this.requestImpactFromPercentage(profile, remainder * FULL_LOAD));
        return total;
    }

    private BoaviztaImpactResponse requestImpactFromPercentage(ServerProfile profile, double loadPercentage)
    {
        return this.getInstanceImpact(
            new BoaviztaInstanceImpactRequest(
                getProvider(profile.provider()),
                profile.instanceType(),
                new BoaviztaInstanceImpactRequest.Usage(profile.location(), List.of(new BoaviztaInstanceImpactRequest.Usage.LoadSegment(100, loadPercentage)))
            ),
            profile.lifespan()
        );
    }

    private BoaviztaImpactResponse getInstanceImpact(BoaviztaInstanceImpactRequest request, int duration)
    {
        return this.client.getInstanceImpact(true, duration, Set.of("gwp", "adp", "pe"), request);
    }

    private BoaviztaInstanceConfigResponse getInstanceConfig(ServerProvider provider, String instanceType)
    {
        return this.client.getInstanceConfig(getProvider(provider), instanceType);
    }

    private static BoaviztaImpactResponse scale(BoaviztaImpactResponse response, double factor)
    {
        Map<String, BoaviztaImpactResponse.Impact> scaled = new LinkedHashMap<>();
        response.impacts().forEach((key, impact) -> scaled.put(key, scaleImpact(impact, factor)));
        return new BoaviztaImpactResponse(scaled, 0);
    }

    private static BoaviztaImpactResponse.Impact scaleImpact(BoaviztaImpactResponse.Impact impact, double factor)
    {
        return new BoaviztaImpactResponse.Impact(impact.unit(), impact.description(), scaleItem(impact.embedded(), factor), scaleItem(impact.use(), factor));
    }

    private static BoaviztaImpactResponse.Impact.ImpactItem scaleItem(BoaviztaImpactResponse.Impact.ImpactItem item, double factor)
    {
        if (item == null) return null;
        return new BoaviztaImpactResponse.Impact.ImpactItem((float) (item.value() * factor), (float) (item.min() * factor), (float) (item.max() * factor), item.warnings());
    }

    private static BoaviztaImpactResponse add(BoaviztaImpactResponse left, BoaviztaImpactResponse right)
    {
        Map<String, BoaviztaImpactResponse.Impact> sum = new LinkedHashMap<>(left.impacts());
        right.impacts().forEach((key, impact) -> sum.merge(key, impact, BoaviztaEvaluationResolver::addImpact));
        return new BoaviztaImpactResponse(sum, 0);
    }

    private static BoaviztaImpactResponse.Impact addImpact(BoaviztaImpactResponse.Impact left, BoaviztaImpactResponse.Impact right)
    {
        return new BoaviztaImpactResponse.Impact(left.unit(), left.description(), addItem(left.embedded(), right.embedded()), addItem(left.use(), right.use()));
    }

    private static BoaviztaImpactResponse.Impact.ImpactItem addItem(BoaviztaImpactResponse.Impact.ImpactItem left, BoaviztaImpactResponse.Impact.ImpactItem right)
    {
        if (left == null)
            return right;
        if (right == null)
            return left;
        List<String> warnings = new ArrayList<>();
        if (left.warnings() != null)
            warnings.addAll(left.warnings());
        if (right.warnings() != null)
            warnings.addAll(right.warnings());
        return new BoaviztaImpactResponse.Impact.ImpactItem(left.value() + right.value(), left.min() + right.min(), left.max() + right.max(), warnings);
    }

    public Footprint convert(BoaviztaImpactResponse impact, Double share, TimeRange range, int lifespanHours)
    {
        double prorationFactor = Duration.between(range.start(), range.end()).toSeconds() / (double) (lifespanHours * 3600L);
        return new Footprint(
            convert(impact.impacts().get("gwp"), share, prorationFactor),
            convert(impact.impacts().get("pe"), share, prorationFactor),
            convert(impact.impacts().get("adp"), share, prorationFactor)
        );
    }

    private static Footprint.FootprintImpact convert(BoaviztaImpactResponse.Impact input, Double share, double prorationFactor)
    {
        List<String> embeddedWarnings = new ArrayList<>(input.embedded().warnings() == null ? Collections.emptyList() : input.embedded().warnings());
        List<String> useWarnings = new ArrayList<>(input.use().warnings() == null ? Collections.emptyList() : input.use().warnings());
        if (share != 1.0d)
        {
            embeddedWarnings.add(rebalanceWarning);
            useWarnings.add(rebalanceWarning);
        }
        embeddedWarnings.add(timeProrationWarning);
        useWarnings.add(timeProrationWarning);

        return new Footprint.FootprintImpact(
            input.unit(),
            input.description(),
            new Footprint.FootprintImpact.FootprintImpactItem(
                (float) (input.embedded().value() * share * prorationFactor),
                embeddedWarnings
            ),
            new Footprint.FootprintImpact.FootprintImpactItem(
                (float) (input.use().value() * share * prorationFactor),
                useWarnings
            )
        );
    }

    public ServerSpecification computeSpecification(ServerProfile profile)
    {
        BoaviztaServerProvider provider = getProvider(profile.provider());
        BoaviztaInstanceConfigResponse config = this.client.getInstanceConfig(provider, profile.instanceType());
        return convert(config);
    }

    private static ServerSpecification convert(BoaviztaInstanceConfigResponse config)
    {
        return new ServerSpecification(
            config.vcpu() == null || config.vcpu().def() == null ? 0 : config.vcpu().def(),
            config.memory() == null || config.memory().def() == null ? 0 : config.memory().def(),
            config.ssdStorage() == null || config.ssdStorage().def() == null ? 0 : config.ssdStorage().def(),
            config.hddStorage() == null || config.hddStorage().def() == null ? 0 : config.hddStorage().def(),
            config.gpuUnits() == null || config.gpuUnits().def() == null ? 0 : config.gpuUnits().def(),
            config.platform() == null || config.platform().def() == null ? null : config.platform().def()
        );
    }

    private static BoaviztaServerProvider getProvider(ServerProvider provider)
    {
        return BoaviztaServerProvider.forName(provider)
            .orElseThrow(() -> new IllegalArgumentException("Unsupported Server provider: " + provider));
    }

    private record Share(String serviceId, double share) {}
}
