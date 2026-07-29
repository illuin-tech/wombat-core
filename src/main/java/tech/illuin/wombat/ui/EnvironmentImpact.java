package tech.illuin.wombat.ui;

import tech.illuin.wombat.model.Footprint;
import tech.illuin.wombat.model.Footprint.FootprintImpact;
import tech.illuin.wombat.model.Footprint.FootprintImpact.FootprintImpactItem;
import tech.illuin.wombat.model.Footprints;
import tech.illuin.wombat.persistence.model.TimeRange;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public record EnvironmentImpact(
    Footprint globalImpact,
    Footprint dockerImpact,
    Footprint llmImpact,
    List<ServiceImpact> serviceImpacts,
    Map<String, Double> impactShares,
    List<String> allServices,
    List<String> includedServices,
    List<AssetBreakdown> assets,
    List<LLMAssetBreakdown> llmAssets,
    TimeRange timeRange
)
{
    public static EnvironmentImpact from(List<AssetImpact> assetImpacts, TimeRange timeRange)
    {
        List<KubernetesAPIAssetImpact> kubernetesImpacts = assetImpacts.stream()
            .filter(KubernetesAPIAssetImpact.class::isInstance)
            .map(KubernetesAPIAssetImpact.class::cast)
            .toList();

        List<LLMAssetImpact> llmImpacts = assetImpacts.stream()
            .filter(LLMAssetImpact.class::isInstance)
            .map(LLMAssetImpact.class::cast)
            .toList();
        List<LLMAssetBreakdown> llmAssets = llmImpacts.stream()
            .map(LLMAssetBreakdown::from)
            .toList();

        List<ServiceImpact> services = new ArrayList<>();
        kubernetesImpacts.forEach(asset ->
            asset.response().serviceImpacts().forEach(footprint ->
                services.add(ServiceImpact.of(asset.name(), footprint))
            )
        );
        List<Footprint> llmFootprints = new ArrayList<>();
        for (LLMAssetImpact impact : llmImpacts)
        {
            if (!impact.serviceIncluded()) continue;
            Footprint footprint = impact.toFootprint();
            llmFootprints.add(footprint);
            services.add(ServiceImpact.of(impact.name(), footprint));
        }
        services.sort(Comparator.comparingDouble((ServiceImpact s) -> s.footprint().gwp().totalValue()).reversed());

        double totalGwp = services.stream().mapToDouble(s -> s.footprint().gwp().totalValue()).sum();
        Map<String, Double> shares = new LinkedHashMap<>();
        services.forEach(s ->
            shares.put(s.label(), totalGwp == 0.0 ? 0.0 : s.footprint().gwp().totalValue() / totalGwp)
        );

        List<String> allServices = Stream.concat(
                kubernetesImpacts.stream().flatMap(r -> r.response().services().stream()),
                llmImpacts.stream().map(i -> i.profile().model()))
            .distinct().sorted().toList();
        List<String> includedServices = Stream.concat(
                kubernetesImpacts.stream().flatMap(r -> r.response().impactShares().keySet().stream()),
                llmImpacts.stream().filter(LLMAssetImpact::serviceIncluded).map(i -> i.profile().model()))
            .distinct().sorted().toList();

        List<AssetBreakdown> breakdowns = kubernetesImpacts.stream()
            .map(AssetBreakdown::from)
            .toList();

        List<Footprint> dockerFootprints = kubernetesImpacts.stream().map(r -> r.response().globalImpact()).toList();
        Footprint dockerImpact = aggregate(dockerFootprints);
        Footprint llmImpact = aggregate(llmFootprints);

        List<Footprint> globalFootprints = new ArrayList<>(dockerFootprints);
        globalFootprints.addAll(llmFootprints);
        Footprint globalImpact = aggregate(globalFootprints);

        return new EnvironmentImpact(globalImpact, dockerImpact, llmImpact, services, shares, allServices, includedServices, breakdowns, llmAssets, timeRange);
    }

    public int assetCount()
    {
        return this.assets.size() + this.llmAssets.size();
    }

    public int serviceCount()
    {
        return this.serviceImpacts.size();
    }

    public long dockerServiceCount()
    {
        return this.serviceImpacts.stream().filter(ServiceImpact::container).count();
    }

    public long llmServiceCount()
    {
        return this.serviceImpacts.stream().filter(ServiceImpact::llm).count();
    }

    private static Footprint aggregate(List<Footprint> footprints)
    {
        if (footprints.isEmpty())
            return new Footprint(zero("kgCO2eq"), zero("MJ"), zero("kgSbeq"), "environment", null, null);

        Footprint base = footprints.getFirst();
        return new Footprint(
            Footprints.sum(footprints.stream().map(Footprint::gwp).toList()),
            Footprints.sum(footprints.stream().map(Footprint::pe).toList()),
            Footprints.sum(footprints.stream().map(Footprint::adp).toList()),
            "environment",
            base.impactProvider(),
            base.type()
        );
    }

    private static FootprintImpact zero(String unit)
    {
        return new FootprintImpact(unit, "", new FootprintImpactItem(0f, List.of()), new FootprintImpactItem(0f, List.of()));
    }
}
