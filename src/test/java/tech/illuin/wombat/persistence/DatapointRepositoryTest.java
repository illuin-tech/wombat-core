package tech.illuin.wombat.persistence;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.persistence.model.DatapointEntity;

import java.util.List;
import java.util.Optional;

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
    void upsert_insertsWhenAbsent()
    {
        repository.upsert(1000L, "TYPE_A", existing -> "payload-1");

        Optional<DatapointEntity> entity = repository.findByInstantAndType(1000L, "TYPE_A");
        assertTrue(entity.isPresent());
        assertEquals("payload-1", entity.get().payload);
    }

    @Test
    void upsert_updatesWhenPresent()
    {
        repository.upsert(2000L, "TYPE_B", existing -> "first");
        repository.upsert(2000L, "TYPE_B", existing -> existing + "+second");

        DatapointEntity entity = repository.findByInstantAndType(2000L, "TYPE_B").orElseThrow();
        assertEquals("first+second", entity.payload);
    }

    @Test
    void findByTypeAndRange_returnsOnlyMatching()
    {
        repository.upsert(100L, "T", existing -> "a");
        repository.upsert(200L, "T", existing -> "b");
        repository.upsert(300L, "T", existing -> "c");
        repository.upsert(150L, "OTHER", existing -> "x");

        List<DatapointEntity> result = repository.findByTypeAndRange("T", 100L, 250L);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(e -> "a".equals(e.payload)));
        assertTrue(result.stream().anyMatch(e -> "b".equals(e.payload)));
        assertFalse(result.stream().anyMatch(e -> "x".equals(e.payload)));
    }

    @Test
    void findByInstantAndType_missing_returnsEmpty()
    {
        assertTrue(repository.findByInstantAndType(99999L, "MISSING").isEmpty());
    }
}
