package tech.illuin.wombat.monitor;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import tech.illuin.wombat.environment.ActiveEnvironments;
import tech.illuin.wombat.kubernetes.KubernetesAPIAssetProperties;
import tech.illuin.wombat.kubernetes.KubernetesMonitorHandler;
import tech.illuin.wombat.kubernetes.KubernetesMetricsCollector;
import tech.illuin.wombat.llm.LLMMetricsCollector;
import tech.illuin.wombat.llm.LLMMonitorHandler;
import tech.illuin.wombat.llm.LLMPrometheusProperties;
import tech.illuin.wombat.llm.LLMStaticProperties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@ApplicationScoped
public class MonitorConfig
{
    @Singleton
    public MonitoredAssetHandler provideCompositeMonitoredAssetHandler(
        KubernetesMetricsCollector kubernetesMetricsCollector,
        LLMMetricsCollector llmMetricsCollector
    )
    {
        return new CompositeMonitoredAssetHandler(List.of(
            new KubernetesMonitorHandler(kubernetesMetricsCollector),
            new LLMMonitorHandler(llmMetricsCollector)
        ));
    }

    @Singleton
    public MonitoredEnvironments provideMonitoredEnvironments(MonitorProperties properties)
    {
        String location = properties.environmentsFile();
        YAMLMapper mapper = YAMLMapper.builder()
            .findAndAddModules()
            .build();
        mapper.registerSubtypes(
            new NamedType(KubernetesAPIAssetProperties.class, AssetType.KUBERNETES_API.name()),
            new NamedType(LLMStaticProperties.class, AssetType.LLM_STATIC.name()),
            new NamedType(LLMPrometheusProperties.class, AssetType.LLM_PROMETHEUS.name())
        );
        try (InputStream in = open(location))
        {
            return mapper.readValue(in, MonitoredEnvironments.class);
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to load monitored environments from '" + location + "'", e);
        }
    }

    @Singleton
    public Monitor provideMonitor(MonitoredAssetHandler handler, ActiveEnvironments activeEnvironments)
    {
        return new Monitor(handler, activeEnvironments);
    }

    private static InputStream open(String location) throws IOException
    {
        Path path = Path.of(location);
        if (Files.isReadable(path)) return Files.newInputStream(path);

        InputStream classpath = Thread.currentThread().getContextClassLoader().getResourceAsStream(location);
        if (classpath != null) return classpath;

        throw new IllegalStateException("Monitored resources file not found on filesystem or classpath: '" + location + "'");
    }
}
