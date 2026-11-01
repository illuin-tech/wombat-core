package tech.illuin.wombat.module.kubernetes_static;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.profile.Lifetime;
import tech.illuin.wombat.core.asset.profile.ServerProvider;
import tech.illuin.wombat.module.kubernetes_static.scenario.BatchActivityScenario;
import tech.illuin.wombat.module.kubernetes_static.scenario.ConstantScenario;
import tech.illuin.wombat.module.kubernetes_static.scenario.Scenario;
import tech.illuin.wombat.module.kubernetes_static.scenario.UserActivityScenario;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class KubernetesStaticProfileTest
{
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void deserializesFromJsonWithCamelCaseProperties() throws JsonProcessingException
    {
        String json = """
            {
                "provider": "aws",
                "instanceType": "m5.large",
                "location": "FRA",
                "lifespan": "5y",
                "zoneId": "Europe/Paris",
                "scenarios": [
                    {
                        "type": "constant",
                        "load": 0.25
                    }
                ]
            }
            """;

        KubernetesStaticProfile profile = this.objectMapper.readValue(json, KubernetesStaticProfile.class);

        assertNotNull(profile);
        assertEquals(ServerProvider.aws, profile.provider());
        assertEquals("m5.large", profile.instanceType());
        assertSame(Country.FRANCE, profile.location());
        assertEquals(Lifetime.ofYears(5), profile.lifespan());
        assertEquals(ZoneId.of("Europe/Paris"), profile.zoneId());
        assertEquals(1, profile.scenarios().size());

        Scenario scenario = profile.scenarios().get(0);
        assertInstanceOf(ConstantScenario.class, scenario);
        assertEquals(0.25, ((ConstantScenario) scenario).load());
    }

    @Test
    void deserializesFromJsonWithKebabCaseAliases() throws JsonProcessingException
    {
        String json = """
            {
                "provider": "gcp",
                "instance-type": "e2-standard-4",
                "location": "DEU",
                "lifespan": "4y",
                "zone-id": "UTC",
                "scenarios": [
                    {
                        "type": "constant",
                        "load": 0.10
                    }
                ]
            }
            """;

        KubernetesStaticProfile profile = this.objectMapper.readValue(json, KubernetesStaticProfile.class);

        assertNotNull(profile);
        assertEquals(ServerProvider.gcp, profile.provider());
        assertEquals("e2-standard-4", profile.instanceType());
        assertSame(Country.GERMANY, profile.location());
        assertEquals(Lifetime.ofYears(4), profile.lifespan());
        assertEquals(ZoneId.of("UTC"), profile.zoneId());
    }

    @Test
    void deserializesNodeCountInCamelCaseAndKebabCase() throws JsonProcessingException
    {
        String camelCaseJson = """
            {
                "provider": "aws",
                "instanceType": "c5.xlarge",
                "location": "FRA",
                "lifespan": "5y",
                "nodeCount": 3,
                "scenarios": [
                    {
                        "type": "constant",
                        "load": 0.50
                    }
                ]
            }
            """;

        KubernetesStaticProfile camelProfile = this.objectMapper.readValue(camelCaseJson, KubernetesStaticProfile.class);
        assertNotNull(camelProfile);
        assertEquals(3, camelProfile.nodeCount());

        String kebabCaseJson = """
            {
                "provider": "aws",
                "instance-type": "c5.xlarge",
                "location": "FRA",
                "lifespan": "5y",
                "node-count": 4,
                "scenarios": [
                    {
                        "type": "constant",
                        "load": 0.50
                    }
                ]
            }
            """;

        KubernetesStaticProfile kebabProfile = this.objectMapper.readValue(kebabCaseJson, KubernetesStaticProfile.class);
        assertNotNull(kebabProfile);
        assertEquals(4, kebabProfile.nodeCount());
    }

    @Test
    void defaultsZoneIdToUtcWhenOmittedInJson() throws JsonProcessingException
    {
        String json = """
            {
                "provider": "aws",
                "instanceType": "c5.xlarge",
                "location": "FRA",
                "lifespan": "5y",
                "scenarios": [
                    {
                        "type": "user_activity",
                        "baselineLoad": 0.10,
                        "peakLoad": 0.75,
                        "openingTime": "09:00:00",
                        "closingTime": "18:00:00"
                    }
                ]
            }
            """;

        KubernetesStaticProfile profile = this.objectMapper.readValue(json, KubernetesStaticProfile.class);

        assertNotNull(profile);
        assertEquals(1, profile.nodeCount());
        assertEquals(ZoneOffset.UTC, profile.zoneId());
        assertEquals(1, profile.scenarios().size());

        Scenario scenario = profile.scenarios().get(0);
        assertInstanceOf(UserActivityScenario.class, scenario);
        UserActivityScenario userActivity = (UserActivityScenario) scenario;
        assertEquals(0.10, userActivity.baselineLoad());
        assertEquals(0.75, userActivity.peakLoad());
    }

    @Test
    void deserializesMultipleScenariosPolymorphically() throws JsonProcessingException
    {
        String json = """
            {
                "provider": "aws",
                "instanceType": "r5.2xlarge",
                "location": "FRA",
                "lifespan": "5y",
                "scenarios": [
                    {
                        "type": "constant",
                        "load": 0.10
                    },
                    {
                        "type": "user_activity",
                        "baselineLoad": 0.10,
                        "peakLoad": 0.75,
                        "openingTime": "08:00:00",
                        "closingTime": "19:00:00",
                        "curve": "sinusoidal"
                    },
                    {
                        "type": "batch_activity",
                        "baselineLoad": 0.0,
                        "peakLoad": 0.90,
                        "triggerTime": "02:00:00",
                        "duration": "PT2H",
                        "smoothing": "linear_ramp"
                    }
                ]
            }
            """;

        KubernetesStaticProfile profile = this.objectMapper.readValue(json, KubernetesStaticProfile.class);

        assertNotNull(profile);
        assertEquals(3, profile.scenarios().size());
        assertInstanceOf(ConstantScenario.class, profile.scenarios().get(0));
        assertInstanceOf(UserActivityScenario.class, profile.scenarios().get(1));
        assertInstanceOf(BatchActivityScenario.class, profile.scenarios().get(2));
    }

    @Test
    void serializesAndDeserializesRoundTrip() throws JsonProcessingException
    {
        KubernetesStaticProfile profile = new KubernetesStaticProfile(
            ServerProvider.aws,
            "m5.xlarge",
            Country.FRANCE,
            Lifetime.ofYears(5),
            ZoneId.of("Europe/Paris"),
            List.of(
                new ConstantScenario(0.10, null),
                new BatchActivityScenario(0.0, 0.85, LocalTime.of(3, 0), Duration.ofHours(1), 0.0, null, null)
            )
        );

        String json = this.objectMapper.writeValueAsString(profile);
        KubernetesStaticProfile deserialized = this.objectMapper.readValue(json, KubernetesStaticProfile.class);

        assertEquals(profile, deserialized);
        assertEquals(profile.zoneId(), deserialized.zoneId());
        assertEquals(profile.scenarios(), deserialized.scenarios());
    }

    @Test
    void constructorDefaultsZoneIdAndScenariosWhenNull()
    {
        KubernetesStaticProfile profile = new KubernetesStaticProfile(
            ServerProvider.aws,
            "m5.large",
            Country.FRANCE,
            Lifetime.ofYears(5),
            null,
            null
        );

        assertEquals(1, profile.nodeCount());
        assertEquals(ZoneOffset.UTC, profile.zoneId());
        assertEquals(List.of(), profile.scenarios());
    }
}
