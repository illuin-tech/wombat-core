package tech.illuin.wombat.core.source;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class PeriodTest
{
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    @DisplayName("Jackson Serialization & Deserialization")
    class JacksonSerializationTests
    {
        @ParameterizedTest(name = "Deserialize \"{0}\" -> Period({1}, {2})")
        @CsvSource({
            "\"1s\", 1, SECONDS",
            "\"45s\", 45, SECONDS",
            "\"2m\", 2, MINUTES",
            "\"30m\", 30, MINUTES",
            "\"3h\", 3, HOURS",
            "\"24h\", 24, HOURS",
            "\" 10 m \", 10, MINUTES",
            "\"5H\", 5, HOURS"
        })
        void shouldDeserializeFromJsonString(String json, long expectedValue, Period.Unit expectedUnit) throws JsonProcessingException
        {
            Period period = objectMapper.readValue(json, Period.class);

            assertNotNull(period);
            assertEquals(expectedValue, period.value());
            assertEquals(expectedUnit, period.unit());
        }

        @ParameterizedTest(name = "Serialize Period({0}, {1}) -> \"{2}\"")
        @CsvSource({
            "1, SECONDS, \"1s\"",
            "2, MINUTES, \"2m\"",
            "3, HOURS, \"3h\""
        })
        void shouldSerializeToJsonString(long value, Period.Unit unit, String expectedJson) throws JsonProcessingException
        {
            Period period = new Period(value, unit);
            String json = objectMapper.writeValueAsString(period);
            assertEquals(expectedJson, json);
        }

        @Test
        void shouldFailDeserializationOnInvalidJsonString()
        {
            assertThrows(JsonProcessingException.class, () -> objectMapper.readValue("\"invalid\"", Period.class));
            assertThrows(JsonProcessingException.class, () -> objectMapper.readValue("\"10d\"", Period.class));
        }
    }

    @Nested
    @DisplayName("Factory & Parsing (Period.parse)")
    class ParseTests
    {
        @ParameterizedTest
        @CsvSource({
            "1s, 1, SECONDS",
            "15s, 15, SECONDS",
            "2m, 2, MINUTES",
            "120m, 120, MINUTES",
            "3h, 3, HOURS",
            "1H, 1, HOURS",
            "' 5 s ', 5, SECONDS"
        })
        void shouldParseValidStrings(String input, long expectedValue, Period.Unit expectedUnit)
        {
            Period period = Period.parse(input);

            assertEquals(expectedValue, period.value());
            assertEquals(expectedUnit, period.unit());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        void shouldThrowExceptionWhenParsingNullOrBlank(String input)
        {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Period.parse(input));
            assertTrue(ex.getMessage().contains("cannot be null or blank"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"10d", "10ms", "-1s", "10", "hours", "10 h 20 m", "abc"})
        void shouldThrowExceptionWhenParsingInvalidFormat(String input)
        {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Period.parse(input));
            assertTrue(ex.getMessage().contains("Invalid period format"));
        }
    }

    @Nested
    @DisplayName("Domain Methods & Conversions")
    class DomainMethodTests
    {
        @Test
        void shouldConvertAsDuration()
        {
            assertEquals(Duration.ofSeconds(10), new Period(10, Period.Unit.SECONDS).asDuration());
            assertEquals(Duration.ofMinutes(5), new Period(5, Period.Unit.MINUTES).asDuration());
            assertEquals(Duration.ofHours(2), new Period(2, Period.Unit.HOURS).asDuration());
        }

        @Test
        void shouldAddPeriodsWithSameUnit()
        {
            Period a = new Period(10, Period.Unit.MINUTES);
            Period b = new Period(5, Period.Unit.MINUTES);

            Period result = a.plus(b);

            assertEquals(15, result.value());
            assertEquals(Period.Unit.MINUTES, result.unit());
        }

        @Test
        void shouldMultiplyPeriod()
        {
            Period period = new Period(15, Period.Unit.SECONDS);

            Period result = period.multiply(3);

            assertEquals(45, result.value());
            assertEquals(Period.Unit.SECONDS, result.unit());
        }

        @Test
        void shouldFormatViaAsText()
        {
            assertEquals("1s", Period.ONE_SECOND.asText());
            assertEquals("1m", Period.ONE_MINUTE.asText());
            assertEquals("1h", Period.ONE_HOUR.asText());
        }

        @Test
        void shouldMatchPredefinedConstants()
        {
            assertEquals(new Period(1, Period.Unit.SECONDS), Period.ONE_SECOND);
            assertEquals(new Period(1, Period.Unit.MINUTES), Period.ONE_MINUTE);
            assertEquals(new Period(1, Period.Unit.HOURS), Period.ONE_HOUR);
        }
    }
}
