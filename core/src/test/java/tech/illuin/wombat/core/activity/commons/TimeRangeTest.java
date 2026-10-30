package tech.illuin.wombat.core.activity.commons;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TimeRangeTest
{
    private final ObjectMapper objectMapper = new ObjectMapper()
        .findAndRegisterModules()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);

    private static final Instant START = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();
    private static final Instant END = ZonedDateTime.of(2026, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC).toInstant(); // 24 hours
    private static final Duration DURATION_ONE_DAY = Duration.ofDays(1);
    private static final Duration DURATION_ONE_HOUR = Duration.ofHours(1);

    @Nested
    @DisplayName("Jackson Serialization & Deserialization")
    class JacksonSerializationTests
    {
        @Test
        void shouldDeserializeWithExplicitResolution() throws JsonProcessingException
        {
            String json = """
                {
                    "start": "2026-01-01T00:00:00Z",
                    "end": "2026-01-01T04:00:00Z",
                    "resolution": "PT1H"
                }
                """;

            TimeRange range = objectMapper.readValue(json, TimeRange.class);

            assertNotNull(range);
            assertEquals(Instant.parse("2026-01-01T00:00:00Z"), range.start());
            assertEquals(Instant.parse("2026-01-01T04:00:00Z"), range.end());
            assertEquals(Duration.ofHours(1), range.resolution());
        }

        @Test
        void shouldDeserializeWithoutResolutionDefaultingToFullDuration() throws JsonProcessingException
        {
            String json = """
                {
                    "start": "2026-01-01T00:00:00Z",
                    "end": "2026-01-01T04:00:00Z"
                }
                """;

            TimeRange range = objectMapper.readValue(json, TimeRange.class);

            assertNotNull(range);
            assertEquals(Instant.parse("2026-01-01T00:00:00Z"), range.start());
            assertEquals(Instant.parse("2026-01-01T04:00:00Z"), range.end());
            assertEquals(Duration.ofHours(4), range.resolution());
        }

        @Test
        void shouldDeserializeWithNullResolutionDefaultingToFullDuration() throws JsonProcessingException
        {
            String json = """
                {
                    "start": "2026-01-01T00:00:00Z",
                    "end": "2026-01-01T04:00:00Z",
                    "resolution": null
                }
                """;

            TimeRange range = objectMapper.readValue(json, TimeRange.class);

            assertNotNull(range);
            assertEquals(Duration.ofHours(4), range.resolution());
        }

        @Test
        void shouldSerializeToJsonString() throws JsonProcessingException
        {
            TimeRange range = new TimeRange(
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T04:00:00Z"),
                Duration.ofHours(1)
            );

            String json = objectMapper.writeValueAsString(range);

            assertTrue(json.contains("\"start\":\"2026-01-01T00:00:00Z\""));
            assertTrue(json.contains("\"end\":\"2026-01-01T04:00:00Z\""));
            assertTrue(json.contains("\"resolution\":\"PT1H\""));
        }

        @Test
        void shouldRoundTripSerializationAndDeserialization() throws JsonProcessingException
        {
            TimeRange original = new TimeRange(START, END, DURATION_ONE_HOUR);

            String json = objectMapper.writeValueAsString(original);
            TimeRange deserialized = objectMapper.readValue(json, TimeRange.class);

            assertEquals(original, deserialized);
        }

        @Test
        void shouldFailDeserializationWhenResolutionGreaterThanDuration()
        {
            String json = """
                {
                    "start": "2026-01-01T00:00:00Z",
                    "end": "2026-01-01T01:00:00Z",
                    "resolution": "PT2H"
                }
                """;

            assertThrows(JsonProcessingException.class, () -> objectMapper.readValue(json, TimeRange.class));
        }

        @ParameterizedTest
        @ValueSource(strings = {"{}", "{\"start\": \"invalid\"}", "\"not-an-object\""})
        void shouldFailDeserializationOnInvalidJson(String json)
        {
            assertThrows(JsonProcessingException.class, () -> objectMapper.readValue(json, TimeRange.class));
        }
    }

    @Nested
    @DisplayName("Constructors & Factory Methods")
    class ConstructorAndFactoryTests
    {
        @Test
        void shouldCreateInstanceWithValidResolution()
        {
            TimeRange range = new TimeRange(START, END, DURATION_ONE_HOUR);

            assertEquals(START, range.start());
            assertEquals(END, range.end());
            assertEquals(DURATION_ONE_HOUR, range.resolution());
        }

        @Test
        void shouldDefaultResolutionToFullDurationWhenOmitted()
        {
            TimeRange range = new TimeRange(START, END);

            assertEquals(START, range.start());
            assertEquals(END, range.end());
            assertEquals(DURATION_ONE_DAY, range.resolution());
        }

        @Test
        void shouldCreateInstanceViaBetween()
        {
            TimeRange range = TimeRange.between(START, END);

            assertEquals(START, range.start());
            assertEquals(END, range.end());
            assertEquals(DURATION_ONE_DAY, range.resolution());
        }

        @Test
        void shouldCloneExactCopy()
        {
            TimeRange original = new TimeRange(START, END, DURATION_ONE_HOUR);
            TimeRange cloned = TimeRange.clone(original);

            assertEquals(original.start(), cloned.start());
            assertEquals(original.end(), cloned.end());
            assertEquals(original.resolution(), cloned.resolution());
        }

        @Test
        void shouldCloneAndRescaleWithNewResolution()
        {
            TimeRange original = new TimeRange(START, END, DURATION_ONE_HOUR);
            TimeRange rescaled = TimeRange.cloneRescale(original, Duration.ofMinutes(30));

            assertEquals(START, rescaled.start());
            assertEquals(END, rescaled.end());
            assertEquals(Duration.ofMinutes(30), rescaled.resolution());
        }

        @Test
        void shouldCloneAndRescaleResettingToFullDuration()
        {
            TimeRange original = new TimeRange(START, END, DURATION_ONE_HOUR);
            TimeRange rescaled = TimeRange.cloneRescale(original);

            assertEquals(START, rescaled.start());
            assertEquals(END, rescaled.end());
            assertEquals(DURATION_ONE_DAY, rescaled.resolution());
        }

        @Test
        void shouldThrowExceptionWhenResolutionGreaterThanDuration()
        {
            Duration twoDays = Duration.ofDays(2);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                new TimeRange(START, END, twoDays);
            });

            assertEquals("Resolution must be less than or equal to the interval defined by the start and end timestamps.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Splitting & Slicing (split)")
    class SplitTests
    {
        @Test
        void shouldDivideEvenlyWithGivenResolution()
        {
            Instant fourHoursEnd = START.plus(Duration.ofHours(4));
            TimeRange range = new TimeRange(START, fourHoursEnd);
            Duration step = Duration.ofHours(1);

            List<TimeRange> slots = range.split(step);

            assertEquals(4, slots.size());

            assertEquals(Instant.parse("2026-01-01T00:00:00Z"), slots.get(0).start());
            assertEquals(Instant.parse("2026-01-01T01:00:00Z"), slots.get(0).end());
            assertEquals(step, slots.get(0).resolution());

            assertEquals(Instant.parse("2026-01-01T01:00:00Z"), slots.get(1).start());
            assertEquals(Instant.parse("2026-01-01T02:00:00Z"), slots.get(1).end());

            assertEquals(Instant.parse("2026-01-01T02:00:00Z"), slots.get(2).start());
            assertEquals(Instant.parse("2026-01-01T03:00:00Z"), slots.get(2).end());

            assertEquals(Instant.parse("2026-01-01T03:00:00Z"), slots.get(3).start());
            assertEquals(Instant.parse("2026-01-01T04:00:00Z"), slots.get(3).end());
        }

        @Test
        void shouldDiscardRemainderWhenNotDivisible()
        {
            Instant fourHoursEnd = START.plus(Duration.ofHours(4));
            TimeRange range = new TimeRange(START, fourHoursEnd);
            Duration step = Duration.ofHours(3);

            List<TimeRange> slots = range.split(step);

            assertEquals(1, slots.size());
            assertEquals(START, slots.getFirst().start());
            assertEquals(Instant.parse("2026-01-01T03:00:00Z"), slots.getFirst().end());
            assertEquals(step, slots.getFirst().resolution());
        }

        @Test
        void shouldReturnSingleSlotWhenResolutionEqualsDuration()
        {
            TimeRange range = new TimeRange(START, END);
            Duration step = DURATION_ONE_DAY;

            List<TimeRange> slots = range.split(step);

            assertEquals(1, slots.size());
            assertEquals(START, slots.getFirst().start());
            assertEquals(END, slots.getFirst().end());
            assertEquals(step, slots.getFirst().resolution());
        }

        @Test
        void shouldUseInstanceResolutionWhenNoArgsProvided()
        {
            Duration step = Duration.ofMinutes(30);
            TimeRange range = new TimeRange(START, START.plus(Duration.ofHours(2)), step);

            List<TimeRange> slots = range.split();

            assertEquals(4, slots.size());
            for (TimeRange slot : slots)
                assertEquals(step, slot.resolution());
            assertEquals(START, slots.get(0).start());
            assertEquals(START.plus(Duration.ofHours(2)), slots.get(3).end());
        }

        @Test
        void shouldReturnSingleSlotWithDefaultResolutionWhenNoArgsProvided()
        {
            TimeRange range = new TimeRange(START, END);

            List<TimeRange> slots = range.split();

            assertEquals(1, slots.size());
            assertEquals(START, slots.getFirst().start());
            assertEquals(END, slots.getFirst().end());
            assertEquals(DURATION_ONE_DAY, slots.getFirst().resolution());
        }

        @Test
        void shouldReturnUnmodifiableList()
        {
            TimeRange range = new TimeRange(START, END);
            List<TimeRange> slots = range.split(Duration.ofHours(1));

            TimeRange dummy = new TimeRange(START, START.plusSeconds(60));
            assertThrows(UnsupportedOperationException.class, () -> slots.add(dummy));
        }

        @Test
        void shouldThrowExceptionWhenResolutionIsNull()
        {
            TimeRange range = new TimeRange(START, END);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> range.split(null));
            assertEquals("Resolution cannot be null", exception.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenResolutionIsZeroOrNegative()
        {
            TimeRange range = new TimeRange(START, END);

            IllegalArgumentException zeroEx = assertThrows(IllegalArgumentException.class, () -> range.split(Duration.ZERO));
            assertEquals("Resolution must be strictly positive", zeroEx.getMessage());

            IllegalArgumentException negEx = assertThrows(IllegalArgumentException.class, () -> range.split(Duration.ofMinutes(-5)));
            assertEquals("Resolution must be strictly positive", negEx.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenResolutionIsLargerThanDuration()
        {
            TimeRange range = new TimeRange(START, END);
            Duration tooLarge = Duration.ofHours(25);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> range.split(tooLarge));
            assertEquals("Resolution must be less than or equal to the total time range duration", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Domain Methods & Calculations")
    class DomainMethodTests
    {
        @Test
        void shouldConvertInstantToEpochMs()
        {
            assertEquals(Long.MIN_VALUE, TimeRange.toEpochMs(Instant.MIN));
            assertEquals(Long.MAX_VALUE, TimeRange.toEpochMs(Instant.MAX));

            Instant normalInstant = Instant.ofEpochMilli(1672531200000L);
            assertEquals(1672531200000L, TimeRange.toEpochMs(normalInstant));
        }

        @Test
        void shouldCalculateCalendarFractionOfYearForNormalYear()
        {
            Instant halfYearEnd = ZonedDateTime.of(2026, 7, 2, 12, 0, 0, 0, ZoneOffset.UTC).toInstant();
            TimeRange range = new TimeRange(START, halfYearEnd);

            assertEquals(0.5, range.getCalendarFractionOfYear(), 0.0001);
        }

        @Test
        void shouldCalculateCalendarFractionOfYearForLeapYear()
        {
            Instant leapStart = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();
            Instant leapHalfEnd = ZonedDateTime.of(2024, 7, 2, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();
            TimeRange range = new TimeRange(leapStart, leapHalfEnd);

            assertEquals(0.5, range.getCalendarFractionOfYear(), 0.0001);
        }

        @Test
        void shouldThrowExceptionForInfiniteRangeFractionOfYear()
        {
            TimeRange minRange = new TimeRange(Instant.MIN, END);
            assertThrows(IllegalArgumentException.class, minRange::getCalendarFractionOfYear);

            TimeRange maxRange = new TimeRange(START, Instant.MAX);
            assertThrows(IllegalArgumentException.class, maxRange::getCalendarFractionOfYear);
        }
    }
}
