package tech.illuin.wombat.asset;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import tech.illuin.wombat.asset.impl.CompositeAssetHandler;
import tech.illuin.wombat.asset.impl.KubernetesAPIAssetHandler;
import tech.illuin.wombat.asset.impl.LLMPrometheusAssetHandler;
import tech.illuin.wombat.asset.impl.LLMStaticAssetHandler;
import tech.illuin.wombat.boavizta.BoaviztaClient;
import tech.illuin.wombat.handler.KubernetesImpactService;
import tech.illuin.wombat.handler.LLMImpactService;

import java.util.List;

@ApplicationScoped
public class AssetConfig
{
    @Singleton
    public KubernetesAPIAssetHandler provideKubernetesAPIAssetHandler(KubernetesImpactService kubernetesImpactService, @RestClient BoaviztaClient boaviztaClient)
    {
        return new KubernetesAPIAssetHandler(kubernetesImpactService, boaviztaClient);
    }

    @Singleton
    public LLMStaticAssetHandler provideLLMStaticAssetHandler(LLMImpactService llmImpactService)
    {
        return new LLMStaticAssetHandler(llmImpactService);
    }

    @Singleton
    public LLMPrometheusAssetHandler provideLLMPrometheusAssetHandler(LLMImpactService llmImpactService)
    {
        return new LLMPrometheusAssetHandler(llmImpactService);
    }

    @Singleton
    public CompositeAssetHandler provideCompositeAssetHandler(
        KubernetesAPIAssetHandler kubernetesAssetHandler,
        LLMStaticAssetHandler llmStaticAssetHandler,
        LLMPrometheusAssetHandler llmPrometheusAssetHandler
    )
    {
        return new CompositeAssetHandler(List.of(kubernetesAssetHandler, llmStaticAssetHandler, llmPrometheusAssetHandler));
    }

    @Singleton
    public AssetImpactService provideAssetImpactService(AssetService assetService, CompositeAssetHandler compositeAssetHandler)
    {
        return new AssetImpactService(assetService, compositeAssetHandler);
    }
}
