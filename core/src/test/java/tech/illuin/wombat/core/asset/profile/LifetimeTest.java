package tech.illuin.wombat.core.asset.profile;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LifetimeTest
{
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    @DisplayName("Jackson Serialization & Deserialization")
    class JacksonSerializationTests
    {
        @ParameterizedTest(name = "Deserialize \"{0}\" -> Lifetime({1}, {2})")
        @CsvSource({
            "\"1d\", 1, DAYS",
            "\"1825d\", 1825, DAYS",
            "\"1m\", 1, MONTHS",
            "\"60m\", 60, MONTHS",
            "\"1y\", 1, YEARS",
            "\"5y\", 5, YEARS",
            "\" 10 Y \", 10, YEARS",
            "\"30D\", 30, DAYS",
            "\"12M\", 12, MONTHS",
            "\"10h\", 10, HOURS",
            "\"43800H\", 43800, HOURS",
            "\"43800\", 43800, HOURS",
            "\" 24 \", 24, HOURS"
        })
        void shouldDeserializeFromJsonString(String json, long expectedValue, Lifetime.Unit expectedUnit) throws JsonProcessingException
        {
            Lifetime lifetime = objectMapper.readValue(json, Lifetime.class);

            assertNotNull(lifetime);
            assertEquals(expectedValue, lifetime.value());
            assertEquals(expectedUnit, lifetime.unit());
        }

        @ParameterizedTest(name = "Deserialize JSON number {0} -> Lifetime({0}, HOURS)")
        @ValueSource(longs = {0L, 1L, 24L, 730L, 8760L, 43800L})
        void shouldDeserializeFromJsonNumber(long hours) throws JsonProcessingException
        {
            Lifetime lifetime = objectMapper.readValue(String.valueOf(hours), Lifetime.class);

            assertNotNull(lifetime);
            assertEquals(hours, lifetime.value());
            assertEquals(Lifetime.Unit.HOURS, lifetime.unit());
            assertEquals(hours, lifetime.asHours());
        }

        @ParameterizedTest(name = "Serialize Lifetime({0}, {1}) -> \"{2}\"")
        @CsvSource({
            "10, HOURS, \"10h\"",
            "1, DAYS, \"1d\"",
            "60, MONTHS, \"60m\"",
            "5, YEARS, \"5y\""
        })
        void shouldSerializeToJsonString(long value, Lifetime.Unit unit, String expectedJson) throws JsonProcessingException
        {
            Lifetime lifetime = new Lifetime(value, unit);
            String json = objectMapper.writeValueAsString(lifetime);
            assertEquals(expectedJson, json);
        }

        @Test
        void shouldFailDeserializationOnInvalidJsonString()
        {
            assertThrows(JsonProcessingException.class, () -> objectMapper.readValue("\"invalid\"", Lifetime.class));
            assertThrows(JsonProcessingException.class, () -> objectMapper.readValue("\"30s\"", Lifetime.class));
            assertThrows(JsonProcessingException.class, () -> objectMapper.readValue("\"10ms\"", Lifetime.class));
        }
    }

    @Nested
    @DisplayName("Factory & Parsing (Lifetime.parse)")
    class ParseTests
    {
        @ParameterizedTest
        @CsvSource({
            "1d, 1, DAYS",
            "1825d, 1825, DAYS",
            "1m, 1, MONTHS",
            "60m, 60, MONTHS",
            "1y, 1, YEARS",
            "5y, 5, YEARS",
            "1Y, 1, YEARS",
            "' 5 d ', 5, DAYS",
            "10h, 10, HOURS",
            "43800h, 43800, HOURS",
            "43800, 43800, HOURS",
            "' 24 ', 24, HOURS"
        })
        void shouldParseValidStrings(String input, long expectedValue, Lifetime.Unit expectedUnit)
        {
            Lifetime lifetime = Lifetime.parse(input);

            assertEquals(expectedValue, lifetime.value());
            assertEquals(expectedUnit, lifetime.unit());
        }

        @Test
        void shouldParseNumericHours()
        {
            Lifetime lifetime = Lifetime.parse(43800L);

            assertEquals(43800L, lifetime.value());
            assertEquals(Lifetime.Unit.HOURS, lifetime.unit());
            assertEquals(43800L, lifetime.asHours());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        void shouldThrowExceptionWhenParsingNullOrBlank(String input)
        {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Lifetime.parse(input));
            assertTrue(ex.getMessage().contains("cannot be null or blank"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"10s", "10ms", "-1y", "-10", "years", "5y 10m", "abc"})
        void shouldThrowExceptionWhenParsingInvalidFormat(String input)
        {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Lifetime.parse(input));
            assertTrue(ex.getMessage().contains("Invalid lifetime format"));
        }
    }

    @Nested
    @DisplayName("Domain Methods & Conversions")
    class DomainMethodTests
    {
        @Test
        void shouldConvertAsHours()
        {
            assertEquals(10L, Lifetime.ofHours(10).asHours());
            assertEquals(24L, Lifetime.ofDays(1).asHours());
            assertEquals(43800L, Lifetime.ofDays(1825).asHours());
            assertEquals(730L, Lifetime.ofMonths(1).asHours());
            assertEquals(43800L, Lifetime.ofMonths(60).asHours());
            assertEquals(8760L, Lifetime.ofYears(1).asHours());
            assertEquals(43800L, Lifetime.ofYears(5).asHours());
        }

        @Test
        void shouldConvertAsDuration()
        {
            assertEquals(Duration.ofHours(10), Lifetime.ofHours(10).asDuration());
            assertEquals(Duration.ofHours(24), Lifetime.ofDays(1).asDuration());
            assertEquals(Duration.ofHours(730), Lifetime.ofMonths(1).asDuration());
            assertEquals(Duration.ofHours(8760), Lifetime.ofYears(1).asDuration());
        }

        @Test
        void shouldAddLifetimesWithSameUnit()
        {
            Lifetime a = Lifetime.ofYears(3);
            Lifetime b = Lifetime.ofYears(2);

            Lifetime result = a.plus(b);

            assertEquals(5L, result.value());
            assertEquals(Lifetime.Unit.YEARS, result.unit());
        }

        @Test
        void shouldMultiplyLifetime()
        {
            Lifetime lifetime = Lifetime.ofMonths(12);

            Lifetime result = lifetime.multiply(5);

            assertEquals(60L, result.value());
            assertEquals(Lifetime.Unit.MONTHS, result.unit());
        }

        @Test
        void shouldFormatViaAsText()
        {
            assertEquals("10h", Lifetime.ofHours(10).asText());
            assertEquals("1d", Lifetime.ONE_DAY.asText());
            assertEquals("1m", Lifetime.ONE_MONTH.asText());
            assertEquals("1y", Lifetime.ONE_YEAR.asText());
        }

        @Test
        void shouldMatchPredefinedConstants()
        {
            assertEquals(new Lifetime(0, Lifetime.Unit.DAYS), Lifetime.ZERO);
            assertEquals(new Lifetime(1, Lifetime.Unit.HOURS), Lifetime.ONE_HOUR);
            assertEquals(new Lifetime(1, Lifetime.Unit.DAYS), Lifetime.ONE_DAY);
            assertEquals(new Lifetime(1, Lifetime.Unit.MONTHS), Lifetime.ONE_MONTH);
            assertEquals(new Lifetime(1, Lifetime.Unit.YEARS), Lifetime.ONE_YEAR);
        }

        @Test
        void shouldCreateViaFactoryMethods()
        {
            assertEquals(new Lifetime(24, Lifetime.Unit.HOURS), Lifetime.of(24, Lifetime.Unit.HOURS));
            assertEquals(new Lifetime(24, Lifetime.Unit.HOURS), Lifetime.ofHours(24));
            assertEquals(new Lifetime(10, Lifetime.Unit.DAYS), Lifetime.of(10, Lifetime.Unit.DAYS));
            assertEquals(new Lifetime(10, Lifetime.Unit.DAYS), Lifetime.ofDays(10));
            assertEquals(new Lifetime(12, Lifetime.Unit.MONTHS), Lifetime.ofMonths(12));
            assertEquals(new Lifetime(5, Lifetime.Unit.YEARS), Lifetime.ofYears(5));
        }
    }
}
