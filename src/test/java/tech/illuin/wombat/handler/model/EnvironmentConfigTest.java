package tech.illuin.wombat.handler.model;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.kubernetes.KubernetesAssetProperties;
import tech.illuin.wombat.llm.LLMProperties;
import tech.illuin.wombat.monitor.AssetType;
import tech.illuin.wombat.monitor.Environment;
import tech.illuin.wombat.persistence.model.TimeRange;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EnvironmentConfigTest
{

    @Test
    void fromMapsEnvironmentAndAssetSummaries()
    {
        Environment environment = new Environment("Production", List.of(
            new KubernetesAssetProperties("cluster-1", "Cluster One", "p-infra", "/kube/config", "ns", Optional.empty(), Optional.empty()),
            new LLMProperties("llm-1", "LLM One", "p-llm")
        ));
        TimeRange timeRange = new TimeRange(Instant.EPOCH, Instant.EPOCH.plusSeconds(60));

        EnvironmentConfig config = EnvironmentConfig.from("prod", environment, timeRange);

        assertEquals("prod", config.id());
        assertEquals("Production", config.name());
        assertEquals(timeRange, config.timeRange());
        assertEquals(2, config.assets().size());
        assertEquals(new EnvironmentConfig.AssetSummary("cluster-1", "Cluster One", AssetType.KUBERNETES_API), config.assets().getFirst());
        assertEquals(new EnvironmentConfig.AssetSummary("llm-1", "LLM One", AssetType.LLM_STATIC), config.assets().getLast());
    }

    @Test
    void fromWithoutTimeRangeLeavesItNull()
    {
        Environment environment = new Environment("Production", List.of());

        EnvironmentConfig config = EnvironmentConfig.from("prod", environment);

        assertNull(config.timeRange());
        assertEquals(0, config.assets().size());
    }
}
