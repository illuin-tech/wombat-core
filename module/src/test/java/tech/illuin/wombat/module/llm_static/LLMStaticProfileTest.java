package tech.illuin.wombat.module.llm_static;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
                        "zoneId": "Europe/Paris",
                        "scenarios": [
                            {
                                "type": "constant",
                                "outputTokenPerRequest": 250,
                                "requestPerYear": 10000.0
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
        assertEquals(ZoneId.of("Europe/Paris"), model.zoneId());
        assertEquals(1, model.scenarios().size());

        Scenario scenario = model.scenarios().get(0);
        assertInstanceOf(ConstantScenario.class, scenario);
        assertEquals(250, scenario.outputTokenPerRequest());
        assertEquals(10000.0, ((ConstantScenario) scenario).requestPerYear());
    }

    @Test
    void deserializesFromJsonWithKebabCaseAliases() throws JsonProcessingException
    {
        String json = """
            {
                "zone-id": "UTC",
                "models": [
                    {
                        "provider": "mistralai",
                        "model": "mistral-large-latest",
                        "location": "DEU",
                        "zone-id": "UTC",
                        "scenarios": [
                            {
                                "type": "constant",
                                "output-token-per-request": 100,
                                "request-per-year": 5000.0
                            }
                        ]
                    }
                ]
            }
            """;

        LLMStaticProfile profile = this.objectMapper.readValue(json, LLMStaticProfile.class);

        assertNotNull(profile);
        assertEquals(ZoneId.of("UTC"), profile.zoneId());
        assertEquals(1, profile.models().size());

        LLMStaticProfile.ModelConfig model = profile.models().get(0);
        assertEquals(LLMProvider.mistralai, model.provider());
        assertEquals("mistral-large-latest", model.model());
        assertSame(Country.GERMANY, model.location());
        assertEquals(ZoneId.of("UTC"), model.zoneId());
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
                        "scenarios": [
                            {
                                "type": "constant",
                                "outputTokenPerRequest": 200,
                                "requestPerYear": 1000.0
                            },
                            {
                                "type": "user_activity",
                                "outputTokenPerRequest": 300,
                                "baselineRequestPerYear": 100.0,
                                "peakRequestPerYear": 5000.0,
                                "openingTime": "09:00:00",
                                "closingTime": "17:00:00",
                                "curve": "sinusoidal"
                            },
                            {
                                "type": "batch_activity",
                                "outputTokenPerRequest": 1000,
                                "baselineRequestPerYear": 0.0,
                                "peakRequestPerYear": 10000.0,
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
                        "scenarios": [
                            {
                                "type": "constant",
                                "outputTokenPerRequest": 150,
                                "requestPerYear": 20000.0
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
        ConstantScenario constant = new ConstantScenario(200, 10000.0, null);
        UserActivityScenario userActivity = new UserActivityScenario(
            250,
            100.0,
            5000.0,
            LocalTime.of(8, 0),
            LocalTime.of(18, 0),
            UserActivityScenario.CurveShape.BELL,
            0.0,
            null
        );
        BatchActivityScenario batch = new BatchActivityScenario(
            800,
            0.0,
            12000.0,
            LocalTime.of(1, 0),
            Duration.ofHours(3),
            0.0,
            BatchActivityScenario.SmoothingMode.LINEAR_RAMP,
            null
        );

        LLMStaticProfile original = new LLMStaticProfile(
            ZoneId.of("Europe/Paris"),
            List.of(new LLMStaticProfile.ModelConfig(
                LLMProvider.openai,
                "gpt-4o",
                Country.FRANCE,
                ZoneId.of("Europe/Paris"),
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
                        "scenarios": [
                            {
                                "type": "constant",
                                "outputTokenPerRequest": 200,
                                "requestPerYear": 1000.0
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
    void constructorOverloadsWorkCorrectly()
    {
        ConstantScenario scenario = new ConstantScenario(200, 5000.0, null);

        LLMStaticProfile profile1 = new LLMStaticProfile(
            LLMProvider.openai,
            "gpt-4o",
            Country.FRANCE,
            List.of(scenario)
        );
        assertEquals(ZoneOffset.UTC, profile1.zoneId());
        assertEquals(1, profile1.models().size());
        assertEquals("gpt-4o", profile1.models().get(0).model());

        LLMStaticProfile profile2 = new LLMStaticProfile(
            LLMProvider.mistralai,
            "mistral-large-latest",
            Country.GERMANY,
            ZoneId.of("Europe/Berlin"),
            List.of(scenario)
        );
        assertEquals(ZoneId.of("Europe/Berlin"), profile2.zoneId());
        assertEquals(1, profile2.models().size());
        assertEquals(ZoneId.of("Europe/Berlin"), profile2.models().get(0).zoneId());
    }
}
