package tech.illuin.wombat.module.connector.boavizta;

import feign.Client;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import tech.illuin.wombat.core.connector.boavizta.connector.BoaviztaClient;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;

public class BoaviztaClientConfig
{
    /**
     * @param uri the API's base URI, version included, e.g. {@code https://api.boavizta.org/v1}
     * @param proxyUrl an HTTP proxy as {@code http://host:port}, {@code null} or blank for none
     */
    public static BoaviztaClient create(String uri, String proxyUrl)
    {
        return create(uri, proxyUrl, null);
    }

    /**
     * @param client the HTTP client to use, {@code null} for Feign's default (or a proxied one when a proxy-url is
     *               given); exposed so tests can capture requests without a network
     */
    public static BoaviztaClient create(String uri, String proxyUrl, Client client)
    {
        Feign.Builder builder = Feign.builder()
            .encoder(new JacksonEncoder())
            .decoder(new JacksonDecoder());

        if (client != null)
            builder.client(client);
        else if (proxyUrl != null && !proxyUrl.isBlank())
        {
            URI proxy = URI.create(proxyUrl);
            if (proxy.getHost() == null || proxy.getPort() == -1)
                throw new IllegalArgumentException("boavizta proxy-url must include a host and port, got: " + proxyUrl);
            builder.client(new Client.Proxied(null, null, new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy.getHost(), proxy.getPort()))));
        }

        return builder.target(BoaviztaClient.class, uri);
    }
}
