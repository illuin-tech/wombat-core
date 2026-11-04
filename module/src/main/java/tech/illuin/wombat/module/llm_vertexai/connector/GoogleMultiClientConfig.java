package tech.illuin.wombat.module.llm_vertexai.connector;

import feign.Feign;
import feign.Client;
import feign.jackson.JacksonDecoder;
import tech.illuin.wombat.core.context.WombatContext;
import tech.illuin.wombat.core.secret.SecretResolver;
import tech.illuin.wombat.module.llm_vertexai.LLMVertexAIAsset;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;

public class GoogleMultiClientConfig
{
    public static GoogleMultiClient create(WombatContext context)
    {
        return create(context, null);
    }

    public static GoogleMultiClient create(WombatContext context, Client client)
    {
        SecretResolver secrets = context.secrets();
        GoogleMultiClient multi = new GoogleMultiClient();
        context.assets().stream()
            .filter(LLMVertexAIAsset.class::isInstance)
            .map(LLMVertexAIAsset.class::cast)
            .forEach(asset -> multi.register(asset.identity().id(), createClient(asset, secrets, client)));
        return multi;
    }

    private static GoogleClient createClient(LLMVertexAIAsset properties, SecretResolver secrets, Client client)
    {
        String serviceAccountJson = properties.usesServiceAccountKey()
            ? secrets.require(properties.credentialsKey())
            : null;

        Proxy proxy = null;
        if (properties.proxyUrl() != null && !properties.proxyUrl().isBlank())
        {
            URI uri = URI.create(properties.proxyUrl());
            if (!"http".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getPort() <= 0
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                || (uri.getPath() != null && !uri.getPath().isEmpty() && !"/".equals(uri.getPath())))
                throw new IllegalStateException("proxy-url must be an HTTP URL with a host and port and no credentials, path, query or fragment");
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(uri.getHost(), uri.getPort()));
        }

        Feign.Builder builder = Feign.builder()
            .decoder(new JacksonDecoder())
            .requestInterceptor(new GoogleAuthInterceptor(serviceAccountJson, proxy));

        if (client != null)
            builder.client(client);
        else if (proxy != null)
            builder.client(new Client.Proxied(null, null, proxy));

        return builder.target(GoogleClient.class, properties.googleUrl());
    }
}
