package tech.illuin.wombat.persistence;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.persistence.model.DatapointEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class DatapointRepositoryTest
{

    @Inject
    DatapointRepository repository;

    @BeforeEach
    @Transactional
    void clean()
    {
        repository.deleteAll();
    }

    @Test
    void save_persistsEntity()
    {
        repository.save(row(1000L, "TYPE_A", "c1", "ns", "payload-1"));

        List<DatapointEntity> result = repository.findByTypeAndRange("TYPE_A", 0L, 5000L);
        assertEquals(1, result.size());
        assertEquals("payload-1", result.get(0).payload);
        assertEquals("c1", result.get(0).cluster);
        assertEquals("ns", result.get(0).namespace);
    }

    @Test
    void findByTypeAndRange_returnsOnlyMatching()
    {
        repository.save(row(100L, "T", "c1", "ns", "a"));
        repository.save(row(200L, "T", "c1", "ns", "b"));
        repository.save(row(300L, "T", "c1", "ns", "c"));
        repository.save(row(150L, "OTHER", "c1", "ns", "x"));

        List<DatapointEntity> result = repository.findByTypeAndRange("T", 100L, 250L);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(e -> "a".equals(e.payload)));
        assertTrue(result.stream().anyMatch(e -> "b".equals(e.payload)));
        assertFalse(result.stream().anyMatch(e -> "x".equals(e.payload)));
    }

    @Test
    void findByTypeRangeAndClusters_emptyList_returnsAllForType()
    {
        repository.save(row(100L, "T", "c1", "ns", "a"));
        repository.save(row(200L, "T", "c2", "ns", "b"));

        List<DatapointEntity> result = repository.findByTypeRangeAndClusters("T", 0L, 5000L, List.of());

        assertEquals(2, result.size());
    }

    @Test
    void findByTypeRangeAndClusters_filtersByCluster()
    {
        repository.save(row(100L, "T", "c1", "ns", "a"));
        repository.save(row(150L, "T", "c2", "ns", "b"));
        repository.save(row(200L, "T", "c3", "ns", "c"));

        List<DatapointEntity> result = repository.findByTypeRangeAndClusters("T", 0L, 5000L, List.of("c1", "c3"));

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(e -> "a".equals(e.payload)));
        assertTrue(result.stream().anyMatch(e -> "c".equals(e.payload)));
        assertFalse(result.stream().anyMatch(e -> "b".equals(e.payload)));
    }

    @Test
    void findByTypeRangeAndClusters_combinesTypeRangeAndClusterFilters()
    {
        repository.save(row(100L, "T", "c1", "ns", "in-range-c1"));
        repository.save(row(400L, "T", "c1", "ns", "out-of-range-c1"));
        repository.save(row(150L, "OTHER", "c1", "ns", "wrong-type-c1"));
        repository.save(row(200L, "T", "c2", "ns", "in-range-c2"));

        List<DatapointEntity> result = repository.findByTypeRangeAndClusters("T", 0L, 300L, List.of("c1"));

        assertEquals(1, result.size());
        assertEquals("in-range-c1", result.get(0).payload);
    }

    private static DatapointEntity row(long instantMs, String type, String cluster, String namespace, String payload)
    {
        DatapointEntity row = new DatapointEntity();
        row.instantMs = instantMs;
        row.type = type;
        row.cluster = cluster;
        row.namespace = namespace;
        row.payload = payload;
        return row;
    }
}
