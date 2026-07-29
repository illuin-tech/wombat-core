package tech.illuin.wombat.ui;

import tech.illuin.wombat.ecologits.model.EcologitsEstimationResponse;
import tech.illuin.wombat.model.Footprint;
import tech.illuin.wombat.model.Footprint.FootprintImpact;
import tech.illuin.wombat.model.Footprint.FootprintImpact.FootprintImpactItem;
import tech.illuin.wombat.asset.model.profile.LLMProfile;
import tech.illuin.wombat.model.ImpactProvider;

import java.util.List;

public sealed interface LLMAssetImpact extends AssetImpact permits LLMStaticAssetImpact, LLMPrometheusAssetImpact
{

    LLMProfile profile();

    EcologitsEstimationResponse estimation();

    int outputTokenCount();

    double requestCount();

    boolean serviceIncluded();

    default Footprint toFootprint()
    {
        EcologitsEstimationResponse.Impacts impacts = this.estimation().impacts();
        EcologitsEstimationResponse.Usage usage = impacts.usage();
        EcologitsEstimationResponse.Embodied embodied = impacts.embodied();
        return new Footprint(
            this.impact(impacts.gwp(), usage == null ? null : usage.gwp(), embodied == null ? null : embodied.gwp()),
            this.impact(impacts.pe(), usage == null ? null : usage.pe(), embodied == null ? null : embodied.pe()),
            this.impact(impacts.adpe(), usage == null ? null : usage.adpe(), embodied == null ? null : embodied.adpe()),
            this.profile().model(),
            ImpactProvider.ECOLOGITS,
            this.type()
        );
    }

    private FootprintImpact impact(
        EcologitsEstimationResponse.Metric total,
        EcologitsEstimationResponse.Metric usage,
        EcologitsEstimationResponse.Metric embodied
    )
    {
        float use = usage != null ? this.amount(usage) : this.amount(total);
        float emb = embodied != null ? this.amount(embodied) : 0f;
        return new FootprintImpact(
            total.unit(),
            total.name(),
            new FootprintImpactItem(emb, List.of()),
            new FootprintImpactItem(use, List.of())
        );
    }

    private float amount(EcologitsEstimationResponse.Metric metric)
    {
        return (float) ((metric.value().min() + metric.value().max()) / 2.0 * this.requestCount());
    }
}
