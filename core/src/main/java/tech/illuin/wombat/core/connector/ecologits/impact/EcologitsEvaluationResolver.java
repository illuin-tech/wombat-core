package tech.illuin.wombat.core.connector.ecologits.impact;

import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.activity.llm.LLMActivityData;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.ServiceFamily;
import tech.illuin.wombat.core.connector.ecologits.connector.EcologitsClient;
import tech.illuin.wombat.core.connector.ecologits.connector.model.EcologitsEstimationRequest;
import tech.illuin.wombat.core.connector.ecologits.connector.model.EcologitsEstimationResponse;
import tech.illuin.wombat.core.evaluation.AssetEvaluation;
import tech.illuin.wombat.core.evaluation.WombatEvaluationException;
import tech.illuin.wombat.core.evaluation.WombatEvaluationResolver;
import tech.illuin.wombat.core.evaluation.impact.commons.AssetImpact;
import tech.illuin.wombat.core.asset.profile.LLMProfile;
import tech.illuin.wombat.core.evaluation.impact.commons.Footprint;
import tech.illuin.wombat.core.evaluation.impact.commons.ImpactProvider;
import tech.illuin.wombat.core.evaluation.impact.commons.ServiceImpact;
import tech.illuin.wombat.core.evaluation.impact.llm.LLMImpact;

import java.util.List;

public class EcologitsEvaluationResolver implements WombatEvaluationResolver
{
    private final EcologitsClient client;

    public EcologitsEvaluationResolver(EcologitsClient client)
    {
        this.client = client;
    }

    @Override
    public boolean accept(Asset asset)
    {
        return asset.type().family() == ServiceFamily.LLM;
    }

    @Override
    public AssetEvaluation resolve(Asset asset, ActivityData activity) throws WombatEvaluationException
    {
        LLMProfile profile = (LLMProfile) asset.profile();
        LLMActivityData llmActivityData = (LLMActivityData) activity;
        EcologitsEstimationResponse estimation = this.estimate(profile, llmActivityData.outputTokenCount());
        Footprint estimationFootprint = this.convert(estimation);

        ServiceImpact llmImpact = new LLMImpact(
            profile.model(),
            asset.type(),
            profile,
            1.0,
            estimationFootprint,
            estimation,
            llmActivityData.outputTokenCount(),
            llmActivityData.requestCount()
        );

        return new AssetImpact(
            asset.environmentId(),
            asset.id(),
            estimationFootprint,
            List.of(llmImpact),
            ImpactProvider.ECOLOGITS
        );


    }

    private EcologitsEstimationResponse estimate(LLMProfile profile, long outputTokenCount)
    {
        EcologitsEstimationRequest.Provider ecologitsProvider = EcologitsEstimationRequest.Provider.forName(profile.provider())
            .orElseThrow(() -> new IllegalArgumentException("Unsupported LLM provider: " + profile.provider()));

        EcologitsEstimationRequest request = new EcologitsEstimationRequest(
            ecologitsProvider,
            profile.model(),
            outputTokenCount,
            profile.location()
        );
        return this.client.estimate(request);
    }

    private Footprint convert(EcologitsEstimationResponse response)
    {
        EcologitsEstimationResponse.Impacts impacts = response.impacts();
        EcologitsEstimationResponse.Usage usage = impacts.usage();
        EcologitsEstimationResponse.Embodied embodied = impacts.embodied();
        return new Footprint(
            convert(impacts.gwp(), usage == null ? null : usage.gwp(), embodied == null ? null : embodied.gwp()),
            convert(impacts.pe(), usage == null ? null : usage.pe(), embodied == null ? null : embodied.pe()),
            convert(impacts.adpe(), usage == null ? null : usage.adpe(), embodied == null ? null : embodied.adpe())
        );
    }

    private static Footprint.FootprintImpact convert(
        EcologitsEstimationResponse.Metric total,
        EcologitsEstimationResponse.Metric usage,
        EcologitsEstimationResponse.Metric embodied
    ) {
        float use = usage != null ? convert(usage) : convert(total);
        float emb = embodied != null ? convert(embodied) : 0f;
        return new Footprint.FootprintImpact(
            total.unit(),
            total.name(),
            new Footprint.FootprintImpact.FootprintImpactItem(emb, List.of()),
            new Footprint.FootprintImpact.FootprintImpactItem(use, List.of())
        );
    }

    private static float convert(EcologitsEstimationResponse.Metric metric)
    {
        return (float) ((metric.value().min() + metric.value().max()) / 2.0);
    }
}
