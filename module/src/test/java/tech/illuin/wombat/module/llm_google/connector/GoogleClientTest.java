package tech.illuin.wombat.module.llm_google.connector;

import feign.Feign;
import feign.Request;
import feign.Response;
import feign.jackson.JacksonDecoder;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GoogleClientTest
{
    @Test
    void supportsProjectFolderAndOrganizationPathsAndGroupsByModel()
    {
        AtomicReference<Request> captured = new AtomicReference<>();
        GoogleClient client = Feign.builder()
            .decoder(new JacksonDecoder())
            .client((request, options) -> {
                captured.set(request);
                return Response.builder().request(request).status(200).headers(Map.of())
                    .body("{\"timeSeries\":[]}", StandardCharsets.UTF_8).build();
            })
            .target(GoogleClient.class, "https://monitoring.googleapis.com");

        for (String name : new String[] {"projects/my-project", "folders/123", "organizations/456"})
        {
            client.query(name, "metric.type=\"tokens\" AND metric.labels.type=\"output\"", "2026-10-01T00:00:00Z", "2026-10-02T00:00:00Z", "3600s", null);

            String url = captured.get().url();
            assertTrue(url.startsWith("https://monitoring.googleapis.com/v3/" + name + "/timeSeries?"), url);
            String decoded = URLDecoder.decode(url, StandardCharsets.UTF_8);
            assertTrue(decoded.contains("aggregation.groupByFields=resource.labels.model_user_id"), decoded);
            assertTrue(decoded.contains("view=FULL"), decoded);
            assertTrue(decoded.contains("metric.labels.type=\"output\""), decoded);
            assertFalse(decoded.contains("pageToken="), decoded);
        }

        client.query("projects/my-project", "f", "start", "end", "3600s", "next-page");
        assertTrue(captured.get().url().contains("pageToken=next-page"));
    }
}
