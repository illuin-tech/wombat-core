package tech.illuin.wombat.module.connector.boavizta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.module.boavizta.connector.BoaviztaClient;
import tech.illuin.wombat.module.boavizta.connector.model.BoaviztaImpactResponse;
import tech.illuin.wombat.module.boavizta.connector.model.BoaviztaInstanceImpactRequest;
import tech.illuin.wombat.module.boavizta.connector.model.BoaviztaServerProvider;
import tech.illuin.wombat.module.boavizta.connector.BoaviztaClientConfig;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class BoaviztaClientConfigTest
{
    private static final String BODY = """
        {
          "impacts": {
            "gwp": {"unit": "kgCO2eq", "description": "Total climate change", "embedded": {"value": 0.25, "min": 0.1, "max": 0.4, "warnings": []}, "use": {"value": 1.5, "min": 1.0, "max": 2.0, "warnings": []}}
          },
          "node_count": 1
        }
        """;

    @Test
    void encodesTheRequestAndDecodesTheImpacts() throws IOException
    {
        AtomicReference<Request> captured = new AtomicReference<>();
        feign.Client http = (request, options) -> {
            captured.set(request);
            return Response.builder()
                .request(request)
                .status(200)
                .headers(Map.of())
                .body(BODY, StandardCharsets.UTF_8)
                .build();
        };

        /* The app's default URI, trailing slash included */
        BoaviztaClient client = BoaviztaClientConfig.create("https://api.boavizta.org/v1/", null, http);
        BoaviztaImpactResponse response = client.getInstanceImpact(
            false,
            24,
            Set.of("gwp"),
            new BoaviztaInstanceImpactRequest(
                BoaviztaServerProvider.aws,
                "c5.2xlarge",
                new BoaviztaInstanceImpactRequest.Usage("FRA", List.of(new BoaviztaInstanceImpactRequest.Usage.LoadSegment(100, 30.0)))
            )
        );

        Request request = captured.get();
        String url = URLDecoder.decode(request.url(), StandardCharsets.UTF_8);
        assertTrue(url.startsWith("https://api.boavizta.org/v1/cloud/instance?"), url);
        assertTrue(url.contains("duration=24"), url);
        assertTrue(url.contains("criteria=gwp"), url);
        JsonNode body = new ObjectMapper().readTree(request.body());
        assertEquals("c5.2xlarge", body.path("instance_type").asText());
        assertEquals(30.0, body.path("usage").path("time_workload").path(0).path("load_percentage").asDouble());
        /* Left out when unset, for Boavizta to apply its default */
        assertFalse(body.path("usage").has("hours_life_time"), body.toString());

        assertEquals(0.25f, response.impacts().get("gwp").embedded().value());
        assertEquals(1.5f, response.impacts().get("gwp").use().value());
    }

    @Test
    void rejectsAProxyWithoutPort()
    {
        assertThrows(IllegalArgumentException.class, () -> BoaviztaClientConfig.create("https://api.boavizta.org/v1/", "http://proxy.example.com"));
    }

    @Test
    void ignoresABlankProxy()
    {
        assertNotNull(BoaviztaClientConfig.create("https://api.boavizta.org/v1/", " "));
    }
}
