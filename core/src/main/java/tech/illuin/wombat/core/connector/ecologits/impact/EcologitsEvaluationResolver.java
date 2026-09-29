package tech.illuin.wombat.core.connector.ecologits.impact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.activity.llm.LLMActivityData;
import tech.illuin.wombat.core.activity.llm.LLMServiceActivity;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.profile.LLMProfile;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.core.connector.ecologits.connector.EcologitsClient;
import tech.illuin.wombat.core.connector.ecologits.connector.model.EcologitsEstimationRequest;
import tech.illuin.wombat.core.connector.ecologits.connector.model.EcologitsEstimationResponse;
import tech.illuin.wombat.core.evaluation.AssetEvaluation;
import tech.illuin.wombat.core.evaluation.WombatEvaluationException;
import tech.illuin.wombat.core.evaluation.WombatEvaluationResolver;
import tech.illuin.wombat.core.evaluation.impact.commons.AssetImpact;
import tech.illuin.wombat.core.evaluation.impact.commons.Footprint;
import tech.illuin.wombat.core.evaluation.impact.commons.ImpactProvider;
import tech.illuin.wombat.core.evaluation.impact.commons.ServiceImpact;
import tech.illuin.wombat.core.evaluation.impact.llm.LLMImpact;

import java.util.*;

public class EcologitsEvaluationResolver implements WombatEvaluationResolver
{
    private final EcologitsClient client;

    private static final Logger logger = LoggerFactory.getLogger(EcologitsEvaluationResolver.class);

    public EcologitsEvaluationResolver(EcologitsClient client)
    {
        this.client = client;
    }

    @Override
    public boolean accept(Asset asset)
    {
        return asset.profile() instanceof LLMProfile;
    }

    @Override
    public AssetEvaluation resolve(Asset asset, ActivityData activity) throws WombatEvaluationException
    {
        LLMProfile profile = (LLMProfile) asset.profile();
        LLMActivityData llmActivityData = (LLMActivityData) activity;

        Map<String, LLMServiceActivity> targetServices = new LinkedHashMap<>();
        Set<String> serviceFilter = llmActivityData.serviceIds();
        llmActivityData.services().forEach((serviceId, serviceActivity) -> {
            if (serviceFilter == null || serviceFilter.isEmpty() || serviceFilter.contains(serviceId) || (serviceActivity.model() != null && serviceFilter.contains(serviceActivity.model())))
                targetServices.put(serviceId, serviceActivity);
        });

        long totalTokens = targetServices.values().stream().mapToLong(LLMServiceActivity::outputTokenCount).sum();

        List<ServiceImpact> serviceImpacts = new ArrayList<>();
        for (Map.Entry<String, LLMServiceActivity> entry : targetServices.entrySet())
        {
            String serviceId = entry.getKey();
            LLMServiceActivity serviceActivity = entry.getValue();

            LLMProvider provider = serviceActivity.provider();
            String model = serviceActivity.model() != null ? serviceActivity.model() : serviceId;
            String location = serviceActivity.location();

            if (provider == null)
                throw new WombatEvaluationException("No provider specified for LLM service " + serviceId + " (asset " + asset.identity().id() + ")");

            EcologitsEstimationResponse estimation = this.estimate(provider, model, location, serviceActivity.outputTokenCount());
            if (!validate(estimation))
            {
                logger.error("Ecologits returned no impact estimation for model {} of provider {} (asset {}), the model may not be registered in Ecologits", model, provider, asset.identity().id());
                continue;
            }

            Footprint serviceFootprint = this.convert(estimation);
            double share = totalTokens > 0
                ? (double) serviceActivity.outputTokenCount() / totalTokens
                : (targetServices.isEmpty() ? 0.0 : 1.0 / targetServices.size());

            ServiceImpact serviceImpact = new LLMImpact(
                serviceId,
                asset.type(),
                profile,
                provider,
                model,
                location,
                share,
                serviceFootprint,
                estimation,
                serviceActivity.outputTokenCount(),
                serviceActivity.requestCount()
            );
            serviceImpacts.add(serviceImpact);
        }

        Comparator<ServiceImpact> comparator = Comparator.comparingDouble(
            si -> si.footprint().gwp().totalValue()
        );
        serviceImpacts.sort(comparator.reversed());

        List<Footprint> footprints = serviceImpacts.stream().map(ServiceImpact::footprint).toList();
        Footprint globalFootprint = Footprint.sum(footprints);

        return new AssetImpact(
            asset.identity().environmentId(),
            asset.identity().id(),
            globalFootprint,
            serviceImpacts,
            ImpactProvider.ECOLOGITS
        );
    }

    private EcologitsEstimationResponse estimate(LLMProvider provider, String model, String location, long outputTokenCount)
    {
        EcologitsEstimationRequest.Provider ecologitsProvider = EcologitsEstimationRequest.Provider.forName(provider)
            .orElseThrow(() -> new IllegalArgumentException("Unsupported LLM provider: " + provider));

        EcologitsEstimationRequest request = new EcologitsEstimationRequest(
            ecologitsProvider,
            model,
            outputTokenCount,
            location
        );
        return this.client.estimate(request);
    }

    private static boolean validate(EcologitsEstimationResponse response)
    {
        if (response == null || response.impacts() == null)
            return false;
        EcologitsEstimationResponse.Impacts impacts = response.impacts();
        return impacts.gwp() != null && impacts.pe() != null && impacts.adpe() != null;
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
