package tech.illuin.wombat.persistence;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.persistence.model.KubernetesMetricEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class KubernetesMetricRepositoryTest
{

    @Inject
    KubernetesMetricRepository repository;

    @BeforeEach
    @Transactional
    void clean()
    {
        repository.deleteAll();
    }

    @Test
    void save_persistsEntity()
    {
        repository.save(row(1000L, "c1", "ns", "container-1"));

        List<KubernetesMetricEntity> result = repository.findByRange(0L, 5000L);
        assertEquals(1, result.size());
        assertEquals("container-1", result.getFirst().container);
        assertEquals("c1", result.getFirst().cluster);
        assertEquals("ns", result.getFirst().namespace);
    }

    @Test
    void findByRange_returnsOnlyWithinRange()
    {
        repository.save(row(100L, "c1", "ns", "a"));
        repository.save(row(200L, "c1", "ns", "b"));
        repository.save(row(300L, "c1", "ns", "c"));

        List<KubernetesMetricEntity> result = repository.findByRange(100L, 250L);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(e -> "a".equals(e.container)));
        assertTrue(result.stream().anyMatch(e -> "b".equals(e.container)));
        assertFalse(result.stream().anyMatch(e -> "c".equals(e.container)));
    }

    @Test
    void findByRangeAndClusters_emptyList_returnsAllInRange()
    {
        repository.save(row(100L, "c1", "ns", "a"));
        repository.save(row(200L, "c2", "ns", "b"));

        List<KubernetesMetricEntity> result = repository.findByRangeAndClusters(0L, 5000L, List.of());

        assertEquals(2, result.size());
    }

    @Test
    void findByRangeAndClusters_filtersByCluster()
    {
        repository.save(row(100L, "c1", "ns", "a"));
        repository.save(row(150L, "c2", "ns", "b"));
        repository.save(row(200L, "c3", "ns", "c"));

        List<KubernetesMetricEntity> result = repository.findByRangeAndClusters(0L, 5000L, List.of("c1", "c3"));

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(e -> "a".equals(e.container)));
        assertTrue(result.stream().anyMatch(e -> "c".equals(e.container)));
        assertFalse(result.stream().anyMatch(e -> "b".equals(e.container)));
    }

    @Test
    void findByRangeAndClusters_combinesRangeAndClusterFilters()
    {
        repository.save(row(100L, "c1", "ns", "in-range-c1"));
        repository.save(row(400L, "c1", "ns", "out-of-range-c1"));
        repository.save(row(200L, "c2", "ns", "in-range-c2"));

        List<KubernetesMetricEntity> result = repository.findByRangeAndClusters(0L, 300L, List.of("c1"));

        assertEquals(1, result.size());
        assertEquals("in-range-c1", result.getFirst().container);
    }

    private static KubernetesMetricEntity row(long instantMs, String cluster, String namespace, String container)
    {
        KubernetesMetricEntity row = new KubernetesMetricEntity();
        row.instantMs = instantMs;
        row.cluster = cluster;
        row.namespace = namespace;
        row.pod = "pod";
        row.container = container;
        row.cpuNanocores = 1.0;
        return row;
    }
}
