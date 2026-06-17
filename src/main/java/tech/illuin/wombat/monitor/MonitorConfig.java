package tech.illuin.wombat.monitor;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import tech.illuin.wombat.k8s.ClusterProperties;
import tech.illuin.wombat.k8s.K8SResourceHandler;
import tech.illuin.wombat.k8s.K8SResourceService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@ApplicationScoped
public class MonitorConfig
{
    @Singleton
    public K8SResourceHandler provideKubernetesResourceHandler(K8SResourceService k8sResourceService)
    {
        return new K8SResourceHandler(k8sResourceService);
    }

    @Singleton
    public MonitoredResources provideMonitoredResources(MonitorProperties properties)
    {
        String location = properties.resourcesFile();
        YAMLMapper mapper = YAMLMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
            .findAndAddModules()
            .build();
        mapper.registerSubtypes(
            new NamedType(ClusterProperties.class, MonitoredResourceType.KUBERNETES.name()),
            new NamedType(CustomResourceProperties.class, MonitoredResourceType.CUSTOM.name())
        );
        try (InputStream in = open(location))
        {
            return mapper.readValue(in, MonitoredResources.class);
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to load monitored resources from '" + location + "'", e);
        }
    }

    @Singleton
    public Monitor provideMonitor(K8SResourceHandler k8sResourceHandler, MonitoredResources resources)
    {
        return new Monitor(k8sResourceHandler, resources);
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
