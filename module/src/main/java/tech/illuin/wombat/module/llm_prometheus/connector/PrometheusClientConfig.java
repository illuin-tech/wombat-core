package tech.illuin.wombat.module.llm_prometheus.connector;

import feign.Client;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import tech.illuin.wombat.core.secret.SecretResolver;
import tech.illuin.wombat.module.llm_prometheus.LLMPrometheusAsset;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;

public class PrometheusClientConfig
{
    public static PrometheusClient create(LLMPrometheusAsset properties, SecretResolver secrets)
    {
        Feign.Builder builder = Feign.builder()
            .decoder(new JacksonDecoder());

        if (properties.proxyUrl() != null && !properties.proxyUrl().isBlank())
        {
            URI proxy = URI.create(properties.proxyUrl());
            if (proxy.getHost() == null || proxy.getPort() == -1)
                throw new IllegalStateException("proxy-url must include a host and port, got: " + properties.proxyUrl());
            // Client.Proxied tolerates a null SSLSocketFactory / HostnameVerifier, falling back to the JDK defaults.
            builder.client(new Client.Proxied(null, null, new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy.getHost(), proxy.getPort()))));
        }

        // requireAsync() rather than async(): a basic-auth asset whose variable is unset must fail, not silently
        // authenticate with a blank password.
        if (properties.usesBasicAuth())
            builder.requestInterceptor(new BasicAuthInterceptor(properties.username(), secrets.requireAsync(properties.passwordKey().value())));

        return builder.target(PrometheusClient.class, properties.prometheusUrl());
    }
}
