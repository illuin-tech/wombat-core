package tech.illuin.wombat.llm;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import tech.illuin.wombat.monitor.MonitoredEnvironments;
import tech.illuin.wombat.persistence.ModelMetricRepository;
import tech.illuin.wombat.prometheus.BasicAuthFilter;
import tech.illuin.wombat.prometheus.PrometheusClient;
import tech.illuin.wombat.prometheus.PrometheusMultiClientApi;

import java.net.URI;

@ApplicationScoped
public class LLMConfig
{
    @Singleton
    public LLMMetricsCollector provideLLMMetricsCollector(PrometheusMultiClientApi prometheusMultiClientApi, ModelMetricRepository modelMetricRepository)
    {
        return new LLMMetricsCollector(prometheusMultiClientApi, modelMetricRepository);
    }

    @Singleton
    public PrometheusMultiClientApi providePrometheusMultiClientApi(MonitoredEnvironments monitoredEnvironments)
    {
        PrometheusMultiClientApi multi = new PrometheusMultiClientApi();
        monitoredEnvironments.allAssets().stream()
            .filter(LLMPrometheusProperties.class::isInstance)
            .map(LLMPrometheusProperties.class::cast)
            .forEach(asset -> multi.register(asset.id(), createClient(asset)));
        return multi;
    }

    private static PrometheusClient createClient(LLMPrometheusProperties properties)
    {
        RestClientBuilder builder = RestClientBuilder.newBuilder()
            .baseUri(URI.create(properties.prometheusUrl()));

        if (properties.proxyUrl() != null && !properties.proxyUrl().isBlank())
        {
            URI proxy = URI.create(properties.proxyUrl());
            if (proxy.getHost() == null || proxy.getPort() == -1)
                throw new IllegalStateException("proxy-url must include a host and port, got: " + properties.proxyUrl());
            builder.proxyAddress(proxy.getHost(), proxy.getPort());
        }

        if (properties.username() != null && !properties.username().isBlank())
            builder.register(new BasicAuthFilter(properties.username(), properties.password()));

        return builder.build(PrometheusClient.class);
    }
}
