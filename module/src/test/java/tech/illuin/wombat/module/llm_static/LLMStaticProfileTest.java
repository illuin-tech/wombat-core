package tech.illuin.wombat.module.llm_static;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.module.llm_static.scenario.BatchActivityScenario;
import tech.illuin.wombat.module.llm_static.scenario.ConstantScenario;
import tech.illuin.wombat.module.llm_static.scenario.Scenario;
import tech.illuin.wombat.module.llm_static.scenario.UserActivityScenario;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LLMStaticProfileTest
{
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    /** Reads configuration files as the app does: kebab-case, from the naming strategy alone. */
    private final ObjectMapper yamlMapper = YAMLMapper.builder().findAndAddModules().propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE).build();

    @Test
    void deserializesFromJsonWithCamelCaseProperties() throws JsonProcessingException
    {
        String json = """
            {
                "zoneId": "Europe/Paris",
                "models": [
                    {
                        "provider": "openai",
                        "model": "gpt-4o",
                        "location": "FRA",
                        "requestPerYear": 10000,
                        "outputTokenPerRequest": 250,
                        "scenarios": [
                            {
                                "type": "constant",
                                "level": 0.5
                            }
                        ]
                    }
                ]
            }
            """;

        LLMStaticProfile profile = this.objectMapper.readValue(json, LLMStaticProfile.class);

        assertNotNull(profile);
        assertEquals(ZoneId.of("Europe/Paris"), profile.zoneId());
        assertEquals(1, profile.models().size());

        LLMStaticProfile.ModelConfig model = profile.models().get(0);
        assertEquals(LLMProvider.openai, model.provider());
        assertEquals("gpt-4o", model.model());
        assertSame(Country.FRANCE, model.location());
        assertEquals(1, model.scenarios().size());

        assertEquals(10_000L, model.requestPerYear());
        assertEquals(250, model.outputTokenPerRequest());
        Scenario scenario = model.scenarios().get(0);
        assertEquals(0.5, assertInstanceOf(ConstantScenario.class, scenario).level());
    }

    @Test
    void deserializesFromKebabCaseYaml() throws JsonProcessingException
    {
        String json = """
            {
                "zone-id": "UTC",
                "models": [
                    {
                        "provider": "mistralai",
                        "model": "mistral-large-latest",
                        "location": "DEU",
                        "request-per-year": 5000,
                        "output-token-per-request": 100,
                        "scenarios": [
                            {
                                "type": "constant"
                            }
                        ]
                    }
                ]
            }
            """;

        LLMStaticProfile profile = this.yamlMapper.readValue(json, LLMStaticProfile.class);

        assertNotNull(profile);
        assertEquals(ZoneId.of("UTC"), profile.zoneId());
        assertEquals(1, profile.models().size());

        LLMStaticProfile.ModelConfig model = profile.models().get(0);
        assertEquals(LLMProvider.mistralai, model.provider());
        assertEquals("mistral-large-latest", model.model());
        assertSame(Country.GERMANY, model.location());
        assertEquals(5_000L, model.requestPerYear());
        assertEquals(100, model.outputTokenPerRequest());
    }

    @Test
    void deserializesMultipleModelsAndScenarios() throws JsonProcessingException
    {
        String json = """
            {
                "zoneId": "UTC",
                "models": [
                    {
                        "provider": "openai",
                        "model": "gpt-4o",
                        "location": "USA",
                        "requestPerYear": 1000000,
                        "outputTokenPerRequest": 300,
                        "scenarios": [
                            {
                                "type": "constant",
                                "level": 0.1
                            },
                            {
                                "type": "user_activity",
                                "openingTime": "09:00:00",
                                "closingTime": "17:00:00",
                                "curve": "sinusoidal"
                            },
                            {
                                "type": "batch_activity",
                                "level": 0.5,
                                "triggerTime": "02:00:00",
                                "duration": "PT2H",
                                "smoothing": "smooth_step"
                            }
                        ]
                    },
                    {
                        "provider": "mistralai",
                        "model": "mistral-small",
                        "location": "FRA",
                        "requestPerYear": 20000,
                        "outputTokenPerRequest": 150,
                        "scenarios": [
                            {
                                "type": "constant"
                            }
                        ]
                    }
                ]
            }
            """;

        LLMStaticProfile profile = this.objectMapper.readValue(json, LLMStaticProfile.class);

        assertNotNull(profile);
        assertEquals(2, profile.models().size());

        LLMStaticProfile.ModelConfig model1 = profile.models().get(0);
        assertEquals(3, model1.scenarios().size());
        assertInstanceOf(ConstantScenario.class, model1.scenarios().get(0));
        assertInstanceOf(UserActivityScenario.class, model1.scenarios().get(1));
        assertInstanceOf(BatchActivityScenario.class, model1.scenarios().get(2));

        LLMStaticProfile.ModelConfig model2 = profile.models().get(1);
        assertEquals(1, model2.scenarios().size());
        assertInstanceOf(ConstantScenario.class, model2.scenarios().get(0));
    }

    @Test
    void serializesToJsonAndBack() throws JsonProcessingException
    {
        ConstantScenario constant = new ConstantScenario(0.1);
        UserActivityScenario userActivity = new UserActivityScenario(1.0, LocalTime.of(8, 0), LocalTime.of(18, 0), UserActivityScenario.CurveShape.BELL);
        BatchActivityScenario batch = new BatchActivityScenario(0.5, LocalTime.of(1, 0), Duration.ofHours(3), BatchActivityScenario.SmoothingMode.LINEAR_RAMP);

        LLMStaticProfile original = new LLMStaticProfile(
            ZoneId.of("Europe/Paris"),
            List.of(new LLMStaticProfile.ModelConfig(
                LLMProvider.openai,
                "gpt-4o",
                Country.FRANCE,
                1_000_000L,
                800,
                List.of(constant, userActivity, batch)
            ))
        );

        String json = this.objectMapper.writeValueAsString(original);
        LLMStaticProfile deserialized = this.objectMapper.readValue(json, LLMStaticProfile.class);

        assertEquals(original, deserialized);
    }

    @Test
    void defaultsZoneIdToUtcWhenOmitted() throws JsonProcessingException
    {
        String json = """
            {
                "models": [
                    {
                        "provider": "openai",
                        "model": "gpt-4o",
                        "location": "FRA",
                        "requestPerYear": 1000,
                        "outputTokenPerRequest": 200,
                        "scenarios": [
                            {
                                "type": "constant"
                            }
                        ]
                    }
                ]
            }
            """;

        LLMStaticProfile profile = this.objectMapper.readValue(json, LLMStaticProfile.class);
        assertEquals(ZoneOffset.UTC, profile.zoneId());
    }

    @Test
    void ignoresUnrecognizedPropertiesForBackwardCompatibility() throws JsonProcessingException
    {
        String json = """
            {
                "zoneId": "UTC",
                "legacyProfileField": "some-value",
                "models": [
                    {
                        "provider": "openai",
                        "model": "gpt-4o",
                        "location": "FRA",
                        "requestProfile": {
                            "type": "standard",
                            "rate": 100
                        },
                        "legacyModelField": 12345,
                        "requestPerYear": 1000,
                        "outputTokenPerRequest": 200,
                        "scenarios": [
                            {
                                "type": "constant"
                            }
                        ]
                    }
                ]
            }
            """;

        LLMStaticProfile profile = this.objectMapper.readValue(json, LLMStaticProfile.class);
        assertNotNull(profile);
        assertEquals(1, profile.models().size());

        LLMStaticProfile.ModelConfig model = profile.models().getFirst();
        assertEquals(LLMProvider.openai, model.provider());
        assertEquals("gpt-4o", model.model());
        assertSame(Country.FRANCE, model.location());
        assertEquals(1, model.scenarios().size());
    }
}
