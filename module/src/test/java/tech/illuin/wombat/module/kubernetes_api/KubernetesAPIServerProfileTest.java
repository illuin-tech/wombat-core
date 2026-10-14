package tech.illuin.wombat.module.kubernetes_api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.profile.ServerProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class KubernetesAPIServerProfileTest
{
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesFromJsonString() throws Exception
    {
        String json = """
            {
                "provider": "aws",
                "instance-type": "m5.large",
                "location": "FRA",
                "lifespan": 43800
            }
            """;

        KubernetesAPIServerProfile profile = this.objectMapper.readValue(json, KubernetesAPIServerProfile.class);

        assertNotNull(profile);
        assertEquals(ServerProvider.aws, profile.provider());
        assertEquals("m5.large", profile.instanceType());
        assertSame(Country.FRANCE, profile.location());
        assertEquals(43800, profile.lifespan());
    }

    @Test
    void serializesToJsonString() throws Exception
    {
        KubernetesAPIServerProfile profile = new KubernetesAPIServerProfile(
            ServerProvider.aws,
            "m5.large",
            Country.FRANCE,
            43800
        );

        String json = this.objectMapper.writeValueAsString(profile);
        KubernetesAPIServerProfile deserialized = this.objectMapper.readValue(json, KubernetesAPIServerProfile.class);

        assertEquals(profile, deserialized);
        assertSame(Country.FRANCE, deserialized.location());
    }
}
