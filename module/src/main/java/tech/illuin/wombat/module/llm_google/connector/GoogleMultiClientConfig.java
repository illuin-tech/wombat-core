package tech.illuin.wombat.module.llm_google.connector;

import feign.Feign;
import feign.jackson.JacksonDecoder;
import tech.illuin.wombat.core.context.WombatContext;
import tech.illuin.wombat.core.secret.SecretResolver;
import tech.illuin.wombat.module.llm_google.LLMGoogleAsset;

public class GoogleMultiClientConfig
{
    public static GoogleMultiClient create(WombatContext context)
    {
        SecretResolver secrets = context.secrets();
        GoogleMultiClient multi = new GoogleMultiClient();
        context.assets().stream()
            .filter(LLMGoogleAsset.class::isInstance)
            .map(LLMGoogleAsset.class::cast)
            .forEach(asset -> multi.register(asset.identity().id(), createClient(asset, secrets)));
        return multi;
    }

    private static GoogleClient createClient(LLMGoogleAsset properties, SecretResolver secrets)
    {
        String serviceAccountJson = properties.usesServiceAccountKey()
            ? secrets.require(properties.credentialsKey())
            : null;

        return Feign.builder()
            .decoder(new JacksonDecoder())
            .requestInterceptor(new GoogleAuthInterceptor(serviceAccountJson))
            .target(GoogleClient.class, properties.googleUrl());
    }
}
