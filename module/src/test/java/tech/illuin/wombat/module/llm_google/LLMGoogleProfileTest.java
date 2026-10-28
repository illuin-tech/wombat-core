package tech.illuin.wombat.module.llm_google;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LLMGoogleProfileTest
{
    @Test
    void theWindowDefaultsToOneMinute()
    {
        assertEquals(Duration.ofMinutes(1), new LLMGoogleProfile.DynamicProfile("f", null).windowOrDefault());
    }

    @Test
    void anExplicitWindowIsKept()
    {
        assertEquals(Duration.ofMinutes(5), new LLMGoogleProfile.DynamicProfile("f", Duration.ofMinutes(5)).windowOrDefault());
    }
}
