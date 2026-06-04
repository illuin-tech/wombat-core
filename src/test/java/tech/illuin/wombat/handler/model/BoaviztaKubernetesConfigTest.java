package tech.illuin.wombat.handler.model;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactRequest;
import tech.illuin.wombat.k8s.K8SProperties;
import tech.illuin.wombat.monitor.MonitorProperties;
import tech.illuin.wombat.profile.ServerProfileEntity;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BoaviztaKubernetesConfigTest
{

    @Test
    void fromProfileEntity_emptyClusterIds_includesAllClusters()
    {
        ServerProfileEntity profile = buildProfile();
        MonitorProperties monitor = buildMonitor(List.of(
            cluster("c1", "ns-a"),
            cluster("c2", "ns-b")
        ));

        BoaviztaKubernetesConfig config = BoaviztaKubernetesConfig.fromProfileEntity(profile, monitor, List.of());

        assertEquals(List.of(new ClusterInfo("c1", "ns-a"), new ClusterInfo("c2", "ns-b")), config.clusters());
        assertEquals(BoaviztaInstanceImpactRequest.Provider.aws, config.provider());
        assertEquals("c5.large", config.instanceType());
        assertEquals("FRA", config.location());
        assertEquals(43800, config.lifespan());
    }

    @Test
    void fromProfileEntity_specificClusterIds_filtersToThoseOnly()
    {
        ServerProfileEntity profile = buildProfile();
        MonitorProperties monitor = buildMonitor(List.of(
            cluster("c1", "ns-a"),
            cluster("c2", "ns-b"),
            cluster("c3", "ns-c")
        ));

        BoaviztaKubernetesConfig config = BoaviztaKubernetesConfig.fromProfileEntity(profile, monitor, List.of("c1", "c3"));

        assertEquals(List.of(new ClusterInfo("c1", "ns-a"), new ClusterInfo("c3", "ns-c")), config.clusters());
    }

    private ServerProfileEntity buildProfile()
    {
        ServerProfileEntity profile = new ServerProfileEntity();
        profile.id = "aws-c5";
        profile.description = "AWS c5.large";
        profile.provider = BoaviztaInstanceImpactRequest.Provider.aws;
        profile.instanceType = "c5.large";
        profile.location = "FRA";
        profile.lifespan = 43800;
        return profile;
    }

    private MonitorProperties buildMonitor(List<K8SProperties.ClusterProperties> clusters)
    {
        K8SProperties k8s = () -> clusters;
        return new MonitorProperties()
        {
            @Override public String cron() { return "* * * * * ?"; }

            @Override public K8SProperties k8sConfigs() { return k8s; }
        };
    }

    private K8SProperties.ClusterProperties cluster(String id, String namespace)
    {
        return new K8SProperties.ClusterProperties()
        {
            @Override public String id() { return id; }

            @Override public String configPath() { return ""; }

            @Override public String namespace() { return namespace; }

            @Override public Optional<String> context() { return Optional.empty(); }

            @Override public Optional<K8SProperties.Duration> readTimeout() { return Optional.empty(); }
        };
    }

    @SuppressWarnings("unused")
    private K8SProperties.Duration anyDuration()
    {
        return new K8SProperties.Duration()
        {
            @Override public int duration() { return 0; }

            @Override public ChronoUnit unit() { return ChronoUnit.SECONDS; }
        };
    }
}