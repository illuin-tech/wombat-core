package tech.illuin.wombat.module.llm_static.scenario;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.module.llm_static.scenario.tools.ApplicabilityFilter;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.Month;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ScenarioSerializationTest
{
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    /** Reads configuration files as the app does: kebab-case, from the naming strategy alone. */
    private final ObjectMapper yamlMapper = YAMLMapper.builder().findAndAddModules().propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE).build();

    @Test
    void deserializesConstantScenario() throws JsonProcessingException
    {
        String json = """
            {
                "type": "constant",
                "level": 0.2,
                "applicability": {
                    "daysOfWeek": ["MONDAY", "TUESDAY"],
                    "months": ["JANUARY", "FEBRUARY"]
                }
            }
            """;

        ConstantScenario constant = assertInstanceOf(ConstantScenario.class, this.objectMapper.readValue(json, Scenario.class));
        assertEquals(0.2, constant.level());
        assertEquals(Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY), constant.applicability().daysOfWeek());
        assertEquals(Set.of(Month.JANUARY, Month.FEBRUARY), constant.applicability().months());
    }

    @Test
    void deserializesUserActivityScenarioFromKebabCaseYaml() throws JsonProcessingException
    {
        String yaml = """
            type: user_activity
            level: 0.8
            opening-time: "08:30:00"
            closing-time: "17:30:00"
            curve: sinusoidal
            jitter: 0.05
            applicability:
              days-of-week: [MONDAY, FRIDAY]
            """;

        UserActivityScenario userActivity = assertInstanceOf(UserActivityScenario.class, this.yamlMapper.readValue(yaml, Scenario.class));
        assertEquals(0.8, userActivity.level());
        assertEquals(LocalTime.of(8, 30), userActivity.openingTime());
        assertEquals(LocalTime.of(17, 30), userActivity.closingTime());
        assertEquals(UserActivityScenario.CurveShape.SINUSOIDAL, userActivity.curve());
        assertEquals(0.05, userActivity.jitter());
        assertEquals(Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), userActivity.applicability().daysOfWeek());
    }

    @Test
    void deserializesBatchActivityScenarioFromKebabCaseYaml() throws JsonProcessingException
    {
        String yaml = """
            type: batch_activity
            level: 0.5
            trigger-time: "01:00:00"
            duration: PT3H
            smoothing: linear_ramp
            """;

        BatchActivityScenario batchActivity = assertInstanceOf(BatchActivityScenario.class, this.yamlMapper.readValue(yaml, Scenario.class));
        assertEquals(0.5, batchActivity.level());
        assertEquals(LocalTime.of(1, 0), batchActivity.triggerTime());
        assertEquals(Duration.ofHours(3), batchActivity.duration());
        assertEquals(BatchActivityScenario.SmoothingMode.LINEAR_RAMP, batchActivity.smoothing());
    }

    @Test
    void roundTripSerializationForScenarios() throws JsonProcessingException
    {
        ConstantScenario constant = new ConstantScenario(0.1, new ApplicabilityFilter(Set.of(DayOfWeek.WEDNESDAY), null));
        assertEquals(constant, this.objectMapper.readValue(this.objectMapper.writeValueAsString(constant), Scenario.class));

        UserActivityScenario userActivity = new UserActivityScenario(1.0, LocalTime.of(8, 30), LocalTime.of(19, 0), UserActivityScenario.CurveShape.TRAPEZOIDAL);
        assertEquals(userActivity, this.objectMapper.readValue(this.objectMapper.writeValueAsString(userActivity), Scenario.class));

        BatchActivityScenario batchActivity = new BatchActivityScenario(0.4, LocalTime.of(23, 0), Duration.ofMinutes(90), BatchActivityScenario.SmoothingMode.SMOOTH_STEP);
        assertEquals(batchActivity, this.objectMapper.readValue(this.objectMapper.writeValueAsString(batchActivity), Scenario.class));
    }

    @Test
    void defaultValuesAreAssignedWhenPropertiesAreOmitted() throws JsonProcessingException
    {
        UserActivityScenario userActivity = (UserActivityScenario) this.objectMapper.readValue("""
            {"type": "user_activity", "openingTime": "09:00:00", "closingTime": "17:00:00"}
            """, Scenario.class);
        assertEquals(Scenario.DEFAULT_LEVEL, userActivity.level());
        assertEquals(UserActivityScenario.DEFAULT_CURVE, userActivity.curve());
        assertEquals(ApplicabilityFilter.ALWAYS, userActivity.applicability());

        BatchActivityScenario batchActivity = (BatchActivityScenario) this.objectMapper.readValue("""
            {"type": "batch_activity", "triggerTime": "02:00:00", "duration": "PT1H"}
            """, Scenario.class);
        assertEquals(Scenario.DEFAULT_LEVEL, batchActivity.level());
        assertEquals(BatchActivityScenario.DEFAULT_SMOOTHING, batchActivity.smoothing());

        ConstantScenario constant = (ConstantScenario) this.objectMapper.readValue("""
            {"type": "constant"}
            """, Scenario.class);
        assertEquals(Scenario.DEFAULT_LEVEL, constant.level());
        assertEquals(ApplicabilityFilter.ALWAYS, constant.applicability());
    }
}
