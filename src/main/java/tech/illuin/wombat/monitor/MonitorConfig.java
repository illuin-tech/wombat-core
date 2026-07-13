package tech.illuin.wombat.monitor;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import tech.illuin.wombat.environment.ActiveEnvironments;
import tech.illuin.wombat.kubernetes.KubernetesAssetProperties;
import tech.illuin.wombat.kubernetes.KubernetesMonitorHandler;
import tech.illuin.wombat.kubernetes.KubernetesMetricsCollector;
import tech.illuin.wombat.llm.LLMProperties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@ApplicationScoped
public class MonitorConfig
{
    @Singleton
    public MonitoredAssetHandler provideCompositeMonitoredAssetHandler(KubernetesMetricsCollector metricsCollector)
    {
        return new CompositeMonitoredAssetHandler(List.of(
            new KubernetesMonitorHandler(metricsCollector)
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
            new NamedType(KubernetesAssetProperties.class, AssetType.KUBERNETES_API.name()),
            new NamedType(LLMProperties.class, AssetType.LLM_STATIC.name())
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
