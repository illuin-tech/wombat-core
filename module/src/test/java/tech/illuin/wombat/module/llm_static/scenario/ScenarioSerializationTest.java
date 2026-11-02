package tech.illuin.wombat.module.llm_static.scenario;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.module.llm_static.scenario.tools.ApplicabilityFilter;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ScenarioSerializationTest
{
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void deserializesConstantScenario() throws JsonProcessingException
    {
        String json = """
            {
                "type": "constant",
                "outputTokenPerRequest": 200,
                "requestPerYear": 50000.0,
                "applicability": {
                    "daysOfWeek": ["MONDAY", "TUESDAY"],
                    "months": ["JANUARY", "FEBRUARY"],
                    "zoneId": "UTC"
                }
            }
            """;

        Scenario scenario = this.objectMapper.readValue(json, Scenario.class);
        assertInstanceOf(ConstantScenario.class, scenario);

        ConstantScenario constant = (ConstantScenario) scenario;
        assertEquals(200, constant.outputTokenPerRequest());
        assertEquals(50000.0, constant.requestPerYear());
        assertNotNull(constant.applicability());
        assertEquals(Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY), constant.applicability().daysOfWeek());
        assertEquals(Set.of(Month.JANUARY, Month.FEBRUARY), constant.applicability().months());
        assertEquals(ZoneId.of("UTC"), constant.applicability().zoneId());
    }

    @Test
    void deserializesConstantScenarioWithKebabCaseAliases() throws JsonProcessingException
    {
        String json = """
            {
                "type": "constant",
                "output-token-per-request": 150,
                "request-per-year": 12000.0
            }
            """;

        Scenario scenario = this.objectMapper.readValue(json, Scenario.class);
        assertInstanceOf(ConstantScenario.class, scenario);

        ConstantScenario constant = (ConstantScenario) scenario;
        assertEquals(150, constant.outputTokenPerRequest());
        assertEquals(12000.0, constant.requestPerYear());
        assertEquals(ApplicabilityFilter.ALWAYS, constant.applicability());
    }

    @Test
    void deserializesUserActivityScenario() throws JsonProcessingException
    {
        String json = """
            {
                "type": "user_activity",
                "outputTokenPerRequest": 250,
                "baselineRequestPerYear": 1000.0,
                "peakRequestPerYear": 8000.0,
                "openingTime": "09:00:00",
                "closingTime": "18:00:00",
                "curve": "sinusoidal",
                "jitter": 0.05
            }
            """;

        Scenario scenario = this.objectMapper.readValue(json, Scenario.class);
        assertInstanceOf(UserActivityScenario.class, scenario);

        UserActivityScenario userActivity = (UserActivityScenario) scenario;
        assertEquals(250, userActivity.outputTokenPerRequest());
        assertEquals(1000.0, userActivity.baselineRequestPerYear());
        assertEquals(8000.0, userActivity.peakRequestPerYear());
        assertEquals(LocalTime.of(9, 0), userActivity.openingTime());
        assertEquals(LocalTime.of(18, 0), userActivity.closingTime());
        assertEquals(UserActivityScenario.CurveShape.SINUSOIDAL, userActivity.curve());
        assertEquals(0.05, userActivity.jitter());
        assertEquals(ApplicabilityFilter.ALWAYS, userActivity.applicability());
    }

    @Test
    void deserializesUserActivityScenarioWithKebabCaseAliases() throws JsonProcessingException
    {
        String json = """
            {
                "type": "user_activity",
                "output-token-count": 300,
                "baseline-request-per-year": 500.0,
                "peak-request-per-year": 5000.0,
                "opening-time": "08:30:00",
                "closing-time": "17:30:00"
            }
            """;

        Scenario scenario = this.objectMapper.readValue(json, Scenario.class);
        assertInstanceOf(UserActivityScenario.class, scenario);

        UserActivityScenario userActivity = (UserActivityScenario) scenario;
        assertEquals(300, userActivity.outputTokenPerRequest());
        assertEquals(500.0, userActivity.baselineRequestPerYear());
        assertEquals(5000.0, userActivity.peakRequestPerYear());
        assertEquals(LocalTime.of(8, 30), userActivity.openingTime());
        assertEquals(LocalTime.of(17, 30), userActivity.closingTime());
        assertEquals(UserActivityScenario.DEFAULT_CURVE, userActivity.curve());
    }

    @Test
    void deserializesBatchActivityScenario() throws JsonProcessingException
    {
        String json = """
            {
                "type": "batch_activity",
                "outputTokenPerRequest": 1000,
                "baselineRequestPerYear": 0.0,
                "peakRequestPerYear": 20000.0,
                "triggerTime": "02:00:00",
                "duration": "PT2H",
                "jitter": 0.1,
                "smoothing": "linear_ramp"
            }
            """;

        Scenario scenario = this.objectMapper.readValue(json, Scenario.class);
        assertInstanceOf(BatchActivityScenario.class, scenario);

        BatchActivityScenario batchActivity = (BatchActivityScenario) scenario;
        assertEquals(1000, batchActivity.outputTokenPerRequest());
        assertEquals(0.0, batchActivity.baselineRequestPerYear());
        assertEquals(20000.0, batchActivity.peakRequestPerYear());
        assertEquals(LocalTime.of(2, 0), batchActivity.triggerTime());
        assertEquals(Duration.ofHours(2), batchActivity.duration());
        assertEquals(0.1, batchActivity.jitter());
        assertEquals(BatchActivityScenario.SmoothingMode.LINEAR_RAMP, batchActivity.smoothing());
        assertEquals(ApplicabilityFilter.ALWAYS, batchActivity.applicability());
    }

    @Test
    void deserializesBatchActivityScenarioWithKebabCaseAliases() throws JsonProcessingException
    {
        String json = """
            {
                "type": "batch_activity",
                "output-token-per-request": 1200,
                "baseline-request-per-year": 0.0,
                "peak-request-per-year": 15000.0,
                "trigger-time": "01:00:00",
                "duration": "PT3H"
            }
            """;

        Scenario scenario = this.objectMapper.readValue(json, Scenario.class);
        assertInstanceOf(BatchActivityScenario.class, scenario);

        BatchActivityScenario batchActivity = (BatchActivityScenario) scenario;
        assertEquals(1200, batchActivity.outputTokenPerRequest());
        assertEquals(0.0, batchActivity.baselineRequestPerYear());
        assertEquals(15000.0, batchActivity.peakRequestPerYear());
        assertEquals(LocalTime.of(1, 0), batchActivity.triggerTime());
        assertEquals(Duration.ofHours(3), batchActivity.duration());
        assertEquals(BatchActivityScenario.DEFAULT_SMOOTHING, batchActivity.smoothing());
    }

    @Test
    void roundTripSerializationForScenarios() throws JsonProcessingException
    {
        ConstantScenario constant = new ConstantScenario(
            200,
            10000.0,
            new ApplicabilityFilter(Set.of(DayOfWeek.WEDNESDAY), null, ZoneId.of("Europe/Paris"))
        );
        String constantJson = this.objectMapper.writeValueAsString(constant);
        Scenario deserializedConstant = this.objectMapper.readValue(constantJson, Scenario.class);
        assertEquals(constant, deserializedConstant);

        UserActivityScenario userActivity = new UserActivityScenario(
            150,
            500.0,
            4000.0,
            LocalTime.of(8, 30),
            LocalTime.of(19, 0),
            UserActivityScenario.CurveShape.TRAPEZOIDAL,
            0.0,
            null
        );
        String userJson = this.objectMapper.writeValueAsString(userActivity);
        Scenario deserializedUser = this.objectMapper.readValue(userJson, Scenario.class);
        assertEquals(userActivity, deserializedUser);

        BatchActivityScenario batchActivity = new BatchActivityScenario(
            800,
            0.0,
            12000.0,
            LocalTime.of(23, 0),
            Duration.ofMinutes(90),
            0.0,
            BatchActivityScenario.SmoothingMode.SMOOTH_STEP,
            null
        );
        String batchJson = this.objectMapper.writeValueAsString(batchActivity);
        Scenario deserializedBatch = this.objectMapper.readValue(batchJson, Scenario.class);
        assertEquals(batchActivity, deserializedBatch);
    }

    @Test
    void defaultValuesAreAssignedWhenPropertiesAreOmitted() throws JsonProcessingException
    {
        String userJson = """
            {
                "type": "user_activity",
                "outputTokenPerRequest": 100,
                "baselineRequestPerYear": 0.0,
                "peakRequestPerYear": 1000.0,
                "openingTime": "09:00:00",
                "closingTime": "17:00:00"
            }
            """;
        UserActivityScenario userActivity = (UserActivityScenario) this.objectMapper.readValue(userJson, Scenario.class);
        assertEquals(UserActivityScenario.DEFAULT_CURVE, userActivity.curve());
        assertEquals(ApplicabilityFilter.ALWAYS, userActivity.applicability());

        String batchJson = """
            {
                "type": "batch_activity",
                "outputTokenPerRequest": 500,
                "baselineRequestPerYear": 0.0,
                "peakRequestPerYear": 2000.0,
                "triggerTime": "02:00:00",
                "duration": "PT1H"
            }
            """;
        BatchActivityScenario batchActivity = (BatchActivityScenario) this.objectMapper.readValue(batchJson, Scenario.class);
        assertEquals(BatchActivityScenario.DEFAULT_SMOOTHING, batchActivity.smoothing());
        assertEquals(ApplicabilityFilter.ALWAYS, batchActivity.applicability());

        String constantJson = """
            {
                "type": "constant",
                "outputTokenPerRequest": 250,
                "requestPerYear": 1000.0
            }
            """;
        ConstantScenario constant = (ConstantScenario) this.objectMapper.readValue(constantJson, Scenario.class);
        assertEquals(ApplicabilityFilter.ALWAYS, constant.applicability());
    }
}
