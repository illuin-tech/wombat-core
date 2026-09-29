package tech.illuin.wombat.core.activity.llm;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.asset.profile.LLMProvider;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LLMActivityDataTest
{
    private static final TimeRange RANGE = new TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1000));

    @Test
    void multiServiceActivityData_computesTotalsCorrectly()
    {
        LLMServiceActivity s1 = new LLMServiceActivity(LLMProvider.mistralai, "mistral-large", "FRA", 500L, 10.0);
        LLMServiceActivity s2 = new LLMServiceActivity(LLMProvider.openai, "gpt-4o", "USA", 1500L, 30.0);

        LLMActivityData activityData = new LLMActivityData(
            ActivityRegime.MODELED,
            Set.of("mistral-large", "gpt-4o"),
            RANGE,
            Map.of("mistral-large", s1, "gpt-4o", s2)
        );

        assertEquals(ServiceFamily.LLM, activityData.family());
        assertEquals(2000L, activityData.outputTokenCount());
        assertEquals(40.0, activityData.requestCount());
        assertEquals(2, activityData.services().size());
        assertEquals(Set.of("mistral-large", "gpt-4o"), activityData.serviceIds());
    }

    @Test
    void legacyConstructor_createsDefaultServiceActivity()
    {
        LLMActivityData activityData = new LLMActivityData(
            ActivityRegime.MEASURED,
            "model-1",
            RANGE,
            1234L,
            5
        );

        assertEquals(1234L, activityData.outputTokenCount());
        assertEquals(5.0, activityData.requestCount());
        assertEquals(1, activityData.services().size());
    }
}
