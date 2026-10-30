package tech.illuin.wombat.module.llm_anthropic.connector;

import feign.Client;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import tech.illuin.wombat.core.context.WombatContext;
import tech.illuin.wombat.core.secret.SecretResolver;
import tech.illuin.wombat.module.llm_anthropic.LLMAnthropicAsset;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;

public class AnthropicMultiClientConfig
{
    public static AnthropicMultiClient create(WombatContext context)
    {
        return create(context, null);
    }

    /**
     * @param client the HTTP client to use, {@code null} for Feign's default (or a proxied one when the asset sets a
     *               proxy-url); exposed so tests can capture requests without a network
     */
    public static AnthropicMultiClient create(WombatContext context, Client client)
    {
        SecretResolver secrets = context.secrets();
        AnthropicMultiClient multi = new AnthropicMultiClient();
        context.assets().stream()
            .filter(LLMAnthropicAsset.class::isInstance)
            .map(LLMAnthropicAsset.class::cast)
            .forEach(asset -> multi.register(asset.identity().id(), createClient(asset, secrets, client)));
        return multi;
    }

    private static AnthropicAdminClient createClient(LLMAnthropicAsset properties, SecretResolver secrets, Client client)
    {
        Feign.Builder builder = Feign.builder()
            .decoder(new JacksonDecoder())
            .requestInterceptor(new AnthropicHeadersInterceptor(secrets.require(properties.adminKey())));

        if (client != null)
            builder.client(client);
        else if (properties.proxyUrl() != null && !properties.proxyUrl().isBlank())
        {
            URI proxy = URI.create(properties.proxyUrl());
            if (proxy.getHost() == null || proxy.getPort() == -1)
                throw new IllegalStateException("proxy-url must include a host and port, got: " + properties.proxyUrl());
            builder.client(new Client.Proxied(null, null, new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy.getHost(), proxy.getPort()))));
        }

        return builder.target(AnthropicAdminClient.class, properties.resolvedApiUrl());
    }
}
