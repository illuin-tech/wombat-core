package tech.illuin.wombat.ecologits;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import tech.illuin.wombat.handler.LLMImpactService;

@ApplicationScoped
public class EcologitsConfig
{

    @Singleton
    public LLMImpactService provideLLMImpactService(@RestClient EcologitsClient ecologitsClient)
    {
        return new LLMImpactService(ecologitsClient);
    }
}
