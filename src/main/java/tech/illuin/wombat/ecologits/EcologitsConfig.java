package tech.illuin.wombat.ecologits;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import tech.illuin.wombat.handler.LLMImpactService;
import tech.illuin.wombat.persistence.ModelMetricRepository;

@ApplicationScoped
public class EcologitsConfig
{

    @Singleton
    public LLMImpactService provideLLMImpactService(@RestClient EcologitsClient ecologitsClient, ModelMetricRepository modelMetricRepository)
    {
        return new LLMImpactService(ecologitsClient, modelMetricRepository);
    }
}
