package tech.illuin.wombat.handler.impact_provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.boavizta.BoaviztaClient;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceConfigResponse;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactRequest;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactResponse;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactResponse.Impact;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactResponse.Impact.ImpactItem;
import tech.illuin.wombat.handler.model.BoaviztaKubernetesConfig;
import tech.illuin.wombat.handler.model.ProviderConfig;
import tech.illuin.wombat.persistence.model.TimeRange;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BoaviztaImpactProvider implements ImpactProvider<BoaviztaInstanceImpactResponse>
{
    private static final Logger logger = LoggerFactory.getLogger(BoaviztaImpactProvider.class);

    private static final double NANOS_PER_CORE = 1_000_000_000.0;
    private static final double FULL_LOAD = 100.0;

    private final BoaviztaClient boaviztaClient;

    public BoaviztaImpactProvider(BoaviztaClient boaviztaClient)
    {
        this.boaviztaClient = boaviztaClient;
    }

    @Override
    public BoaviztaInstanceImpactResponse resolveImpact(ProviderConfig config, TimeRange timeRange, double load)
    {
        BoaviztaKubernetesConfig k8sConfig = (BoaviztaKubernetesConfig) config;
        BoaviztaInstanceConfigResponse instanceConfig = this.getInstanceConfig(k8sConfig.provider(), k8sConfig.instanceType());
        int vcpu = instanceConfig.vcpu() != null && instanceConfig.vcpu().def() != null ? instanceConfig.vcpu().def() : 0;
        double cores = load / NANOS_PER_CORE;
        int nodeCount = instanceConfig.nodesRequired(cores);
        logger.info("Boavizta instance has {} vCPU; {} cores used spread over {} node(s)", vcpu, cores, nodeCount);

        BoaviztaInstanceImpactResponse impact = vcpu > 0
            ? this.computeImpact(k8sConfig, cores / vcpu)
            : this.requestImpact(k8sConfig, 0.0);

        return new BoaviztaInstanceImpactResponse(impact.impacts(), nodeCount);
    }

    /**
     * When the load fits within a single instance ({@code loadRatio <= 1}), the impact is a single
     * request at that load. Otherwise it is the sum of {@code floor(loadRatio)} fully-loaded
     * instances plus, if there is a remainder, one instance running the leftover fraction. For
     * example a load of 3.2 vCPU yields 3 × impact(100%) + impact(20%).
     */
    private BoaviztaInstanceImpactResponse computeImpact(BoaviztaKubernetesConfig config, double loadRatio)
    {
        if (loadRatio <= 1.0)
            return this.requestImpact(config, loadRatio * FULL_LOAD);

        int fullNodes = (int) Math.floor(loadRatio);
        double remainder = loadRatio - fullNodes;
        logger.info("Load ratio {} exceeds one node: {} node(s) at 100% + remainder {}", loadRatio, fullNodes, remainder);

        BoaviztaInstanceImpactResponse total = scale(this.requestImpact(config, FULL_LOAD), fullNodes);
        if (remainder > 0)
            total = add(total, this.requestImpact(config, remainder * FULL_LOAD));
        return total;
    }

    private BoaviztaInstanceImpactResponse requestImpact(BoaviztaKubernetesConfig config, double loadPercentage)
    {
        return this.getInstanceImpact(
            new BoaviztaInstanceImpactRequest(
                config.provider(),
                config.instanceType(),
                new BoaviztaInstanceImpactRequest.Usage(config.location(), List.of(new BoaviztaInstanceImpactRequest.Usage.LoadSegment(100, loadPercentage)))
            ),
            config.lifespan()
        );
    }

    private BoaviztaInstanceImpactResponse getInstanceImpact(BoaviztaInstanceImpactRequest request, int duration)
    {
        return this.boaviztaClient.getInstanceImpact(true, duration, Set.of("gwp", "adp", "pe"), request);
    }

    private BoaviztaInstanceConfigResponse getInstanceConfig(BoaviztaInstanceImpactRequest.Provider provider, String instanceType)
    {
        return this.boaviztaClient.getInstanceConfig(provider, instanceType);
    }

    private static BoaviztaInstanceImpactResponse scale(BoaviztaInstanceImpactResponse response, double factor)
    {
        Map<String, Impact> scaled = new LinkedHashMap<>();
        response.impacts().forEach((key, impact) -> scaled.put(key, scaleImpact(impact, factor)));
        return new BoaviztaInstanceImpactResponse(scaled, 0);
    }

    private static Impact scaleImpact(Impact impact, double factor)
    {
        return new Impact(impact.unit(), impact.description(), scaleItem(impact.embedded(), factor), scaleItem(impact.use(), factor));
    }

    private static ImpactItem scaleItem(ImpactItem item, double factor)
    {
        if (item == null) return null;
        return new ImpactItem((float) (item.value() * factor), (float) (item.min() * factor), (float) (item.max() * factor), item.warnings());
    }

    private static BoaviztaInstanceImpactResponse add(BoaviztaInstanceImpactResponse left, BoaviztaInstanceImpactResponse right)
    {
        Map<String, Impact> sum = new LinkedHashMap<>(left.impacts());
        right.impacts().forEach((key, impact) -> sum.merge(key, impact, BoaviztaImpactProvider::addImpact));
        return new BoaviztaInstanceImpactResponse(sum, 0);
    }

    private static Impact addImpact(Impact left, Impact right)
    {
        return new Impact(left.unit(), left.description(), addItem(left.embedded(), right.embedded()), addItem(left.use(), right.use()));
    }

    private static ImpactItem addItem(ImpactItem left, ImpactItem right)
    {
        if (left == null) return right;
        if (right == null) return left;
        List<String> warnings = new ArrayList<>();
        if (left.warnings() != null) warnings.addAll(left.warnings());
        if (right.warnings() != null) warnings.addAll(right.warnings());
        return new ImpactItem(left.value() + right.value(), left.min() + right.min(), left.max() + right.max(), warnings);
    }
}
