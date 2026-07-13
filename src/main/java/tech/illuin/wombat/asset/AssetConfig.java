package tech.illuin.wombat.asset;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import tech.illuin.wombat.asset.impl.CompositeAssetHandler;
import tech.illuin.wombat.asset.impl.KubernetesAssetHandler;
import tech.illuin.wombat.asset.impl.LLMAssetHandler;
import tech.illuin.wombat.boavizta.BoaviztaClient;
import tech.illuin.wombat.handler.KubernetesImpactService;
import tech.illuin.wombat.handler.LLMImpactService;

import java.util.List;

@ApplicationScoped
public class AssetConfig
{
    @Singleton
    public KubernetesAssetHandler provideKubernetesAssetHandler(KubernetesImpactService kubernetesImpactService, @RestClient BoaviztaClient boaviztaClient)
    {
        return new KubernetesAssetHandler(kubernetesImpactService, boaviztaClient);
    }

    @Singleton
    public LLMAssetHandler provideLLMAssetHandler(LLMImpactService llmImpactService)
    {
        return new LLMAssetHandler(llmImpactService);
    }

    @Singleton
    public CompositeAssetHandler provideCompositeAssetHandler(KubernetesAssetHandler kubernetesAssetHandler, LLMAssetHandler llmAssetHandler)
    {
        return new CompositeAssetHandler(List.of(kubernetesAssetHandler, llmAssetHandler));
    }

    @Singleton
    public AssetImpactService provideAssetImpactService(AssetService assetService, CompositeAssetHandler compositeAssetHandler)
    {
        return new AssetImpactService(assetService, compositeAssetHandler);
    }
}
