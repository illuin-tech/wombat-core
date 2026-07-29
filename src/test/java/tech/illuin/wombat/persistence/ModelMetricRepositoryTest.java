package tech.illuin.wombat.persistence;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.persistence.model.MetricData;
import tech.illuin.wombat.persistence.model.ModelMetricEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class ModelMetricRepositoryTest
{

    @Inject
    ModelMetricRepository repository;

    @BeforeEach
    @Transactional
    void clean()
    {
        repository.deleteAll();
    }

    @Test
    void save_persistsEntityWithLLMData()
    {
        repository.save(row(1000L, "p-llm", "mistral-large-latest", 42L));

        ModelMetricEntity persisted = repository.findAll().firstResult();
        assertEquals(new MetricData.LLMData("p-llm", "mistral-large-latest"), persisted.data);
        assertEquals(42L, persisted.outputTokens);
    }

    @Test
    void sumOutputTokens_sumsDeltasWithinRangeForProfile()
    {
        repository.save(row(1000L, "p-llm", "m", 10L));
        repository.save(row(2000L, "p-llm", "m", 20L));
        repository.save(row(3000L, "p-llm", "m", 30L));

        assertEquals(30L, repository.sumOutputTokens(1000L, 2000L, "p-llm"));
        assertEquals(60L, repository.sumOutputTokens(0L, 5000L, "p-llm"));
    }

    @Test
    void sumOutputTokens_filtersByProfile()
    {
        repository.save(row(1000L, "p-llm", "m", 10L));
        repository.save(row(1000L, "other", "m", 99L));

        assertEquals(10L, repository.sumOutputTokens(0L, 5000L, "p-llm"));
    }

    @Test
    void sumOutputTokens_noRows_returnsZero()
    {
        assertEquals(0L, repository.sumOutputTokens(0L, 5000L, "p-llm"));
    }

    private static ModelMetricEntity row(long instantMs, String profileId, String model, long outputTokens)
    {
        ModelMetricEntity row = new ModelMetricEntity();
        row.instantMs = instantMs;
        row.data = new MetricData.LLMData(profileId, model);
        row.outputTokens = outputTokens;
        return row;
    }
}
