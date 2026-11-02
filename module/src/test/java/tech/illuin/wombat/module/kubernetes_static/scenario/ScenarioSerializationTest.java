package tech.illuin.wombat.module.kubernetes_static.scenario;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.module.kubernetes_static.scenario.tools.ApplicabilityFilter;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.Month;
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
                "load": 0.25,
                "applicability": {
                    "daysOfWeek": ["MONDAY", "TUESDAY"],
                    "months": ["JANUARY", "FEBRUARY"]
                }
            }
            """;

        Scenario scenario = this.objectMapper.readValue(json, Scenario.class);
        assertInstanceOf(ConstantScenario.class, scenario);

        ConstantScenario constant = (ConstantScenario) scenario;
        assertEquals(0.25, constant.load());
        assertNotNull(constant.applicability());
        assertEquals(Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY), constant.applicability().daysOfWeek());
        assertEquals(Set.of(Month.JANUARY, Month.FEBRUARY), constant.applicability().months());
    }

    @Test
    void deserializesUserActivityScenario() throws JsonProcessingException
    {
        String json = """
            {
                "type": "user_activity",
                "baselineLoad": 0.10,
                "peakLoad": 0.75,
                "openingTime": "09:00:00",
                "closingTime": "18:00:00",
                "curve": "sinusoidal",
                "jitter": 0.05
            }
            """;

        Scenario scenario = this.objectMapper.readValue(json, Scenario.class);
        assertInstanceOf(UserActivityScenario.class, scenario);

        UserActivityScenario userActivity = (UserActivityScenario) scenario;
        assertEquals(0.10, userActivity.baselineLoad());
        assertEquals(0.75, userActivity.peakLoad());
        assertEquals(LocalTime.of(9, 0), userActivity.openingTime());
        assertEquals(LocalTime.of(18, 0), userActivity.closingTime());
        assertEquals(UserActivityScenario.CurveShape.SINUSOIDAL, userActivity.curve());
        assertEquals(0.05, userActivity.jitter());
        assertEquals(ApplicabilityFilter.ALWAYS, userActivity.applicability());
    }

    @Test
    void deserializesBatchActivityScenario() throws JsonProcessingException
    {
        String json = """
            {
                "type": "batch_activity",
                "baselineLoad": 0.0,
                "peakLoad": 0.90,
                "triggerTime": "02:00:00",
                "duration": "PT2H",
                "jitter": 0.1,
                "smoothing": "linear_ramp"
            }
            """;

        Scenario scenario = this.objectMapper.readValue(json, Scenario.class);
        assertInstanceOf(BatchActivityScenario.class, scenario);

        BatchActivityScenario batchActivity = (BatchActivityScenario) scenario;
        assertEquals(0.0, batchActivity.baselineLoad());
        assertEquals(0.90, batchActivity.peakLoad());
        assertEquals(LocalTime.of(2, 0), batchActivity.triggerTime());
        assertEquals(Duration.ofHours(2), batchActivity.duration());
        assertEquals(0.1, batchActivity.jitter());
        assertEquals(BatchActivityScenario.SmoothingMode.LINEAR_RAMP, batchActivity.smoothing());
        assertEquals(ApplicabilityFilter.ALWAYS, batchActivity.applicability());
    }

    @Test
    void roundTripSerializationForScenarios() throws JsonProcessingException
    {
        ConstantScenario constant = new ConstantScenario(
            0.30,
            new ApplicabilityFilter(Set.of(DayOfWeek.WEDNESDAY), null)
        );
        String constantJson = this.objectMapper.writeValueAsString(constant);
        Scenario deserializedConstant = this.objectMapper.readValue(constantJson, Scenario.class);
        assertEquals(constant, deserializedConstant);

        UserActivityScenario userActivity = new UserActivityScenario(
            0.10,
            0.80,
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
            0.0,
            0.95,
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
                "baselineLoad": 0.0,
                "peakLoad": 1.0,
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
                "baselineLoad": 0.0,
                "peakLoad": 1.0,
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
                "load": 1.0
            }
            """;
        ConstantScenario constant = (ConstantScenario) this.objectMapper.readValue(constantJson, Scenario.class);
        assertEquals(ApplicabilityFilter.ALWAYS, constant.applicability());
    }
}
