package tech.illuin.wombat.module.ecologits.connector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.module.ecologits.connector.model.EcologitsEstimationRequest;
import tech.illuin.wombat.module.ecologits.connector.model.EcologitsEstimationResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class EcologitsClientConfigTest
{
    private static final String BODY = """
        {
          "impacts": {
            "gwp": {"type": "GWP", "name": "Global Warming Potential", "value": {"min": 0.001, "max": 0.003}, "unit": "kgCO2eq"},
            "usage": {"type": "usage", "name": "Usage", "gwp": {"type": "GWP", "name": "Global Warming Potential", "value": 0.0015, "unit": "kgCO2eq"}}
          }
        }
        """;

    @Test
    void encodesTheRequestAndDecodesTheEstimation() throws IOException
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
        EcologitsClient client = EcologitsClientConfig.create("https://api.ecologits.ai/", null, http);
        EcologitsEstimationResponse response = client.estimate(new EcologitsEstimationRequest(EcologitsEstimationRequest.Provider.openai, "gpt-4o", 300, "FRA"));

        Request request = captured.get();
        assertEquals("https://api.ecologits.ai/v1beta/estimations", request.url());
        JsonNode body = new ObjectMapper().readTree(request.body());
        assertEquals("gpt-4o", body.path("model_name").asText());
        assertEquals(300, body.path("output_token_count").asLong());

        assertEquals(0.002, EcologitsEstimationResponse.Metric.mean(response.impacts().gwp()), 1e-9);
        assertEquals(0.0015, EcologitsEstimationResponse.Metric.mean(response.impacts().usage().gwp()), 1e-9);
    }

    @Test
    void rejectsAProxyWithoutPort()
    {
        assertThrows(IllegalArgumentException.class, () -> EcologitsClientConfig.create("https://api.ecologits.ai/", "http://proxy.example.com"));
    }

    @Test
    void ignoresABlankProxy()
    {
        assertNotNull(EcologitsClientConfig.create("https://api.ecologits.ai/", " "));
    }
}
