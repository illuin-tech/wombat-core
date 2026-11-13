package tech.illuin.wombat.module.llm_vertexai.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.module.llm_vertexai.LLMVertexAIModule;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.Environment;
import tech.illuin.wombat.core.context.ResolvedContext;
import tech.illuin.wombat.core.context.WombatContext;
import tech.illuin.wombat.core.secret.EnvironmentSecretResolver;
import tech.illuin.wombat.core.secret.MissingSecretException;
import tech.illuin.wombat.module.llm_vertexai.LLMVertexAIAsset;
import tech.illuin.wombat.module.llm_vertexai.LLMVertexAIProfile;

import java.security.KeyPairGenerator;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GoogleMultiClientConfigTest
{
    @Test
    void buildsAClientPerAssetRelyingOnApplicationDefaultCredentials()
    {
        GoogleMultiClient multi = GoogleMultiClientConfig.create(
            context(Map.of(), asset("g", null)));

        assertTrue(multi.get("g").isPresent());
    }

    @Test
    void buildsAClientFromAServiceAccountKeyReadFromTheDeclaredVariable() throws Exception
    {
        GoogleMultiClient multi = GoogleMultiClientConfig.create(
            context(Map.of("GOOGLE_SA_KEY", serviceAccountJson()), asset("g", "GOOGLE_SA_KEY")));

        assertTrue(multi.get("g").isPresent());
    }

    @Test
    void failsWhenTheServiceAccountVariableIsUnset()
    {
        WombatContext context = context(Map.of(), asset("g", "GOOGLE_SA_KEY"));

        MissingSecretException e = assertThrows(MissingSecretException.class,
            () -> GoogleMultiClientConfig.create(context));

        assertEquals("GOOGLE_SA_KEY", e.key());
    }

    @Test
    void failsOnAMalformedServiceAccountKey()
    {
        WombatContext context = context(Map.of("GOOGLE_SA_KEY", "not json"), asset("g", "GOOGLE_SA_KEY"));

        assertThrows(IllegalStateException.class, () -> GoogleMultiClientConfig.create(context));
    }

    @Test
    void routesTokenRefreshAndMonitoringRequestsThroughTheProxy() throws Exception
    {
        List<String> requests = new CopyOnWriteArrayList<>();
        HttpServer proxy = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        proxy.createContext("/", exchange -> {
            requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI());
            exchange.getRequestBody().readAllBytes();
            String body = exchange.getRequestMethod().equals("POST")
                ? "{\"access_token\":\"proxy-test-token\",\"expires_in\":3600,\"token_type\":\"Bearer\"}"
                : "{\"timeSeries\":[]}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        proxy.start();
        try {
            ObjectMapper mapper = new ObjectMapper();
            var json = mapper.readTree(serviceAccountJson());
            ((com.fasterxml.jackson.databind.node.ObjectNode) json).put("token_uri", "http://oauth.invalid/token");
            LLMVertexAIAsset base = asset("g", "GOOGLE_SA_KEY");
            LLMVertexAIAsset proxied = new LLMVertexAIAsset(base.identity(), "http://monitoring.invalid", base.resourceName(),
                base.credentialsKey(), "http://127.0.0.1:" + proxy.getAddress().getPort(), base.profile());
            GoogleClient client = GoogleMultiClientConfig.create(context(Map.of("GOOGLE_SA_KEY", mapper.writeValueAsString(json)), proxied))
                .get("g").orElseThrow();

            assertEquals(List.of(), client.query("projects/my-project", "f", "start", "end", "3600s", null).timeSeries());
            assertTrue(requests.stream().anyMatch(request -> request.startsWith("POST http://oauth.invalid/token")), requests.toString());
            assertTrue(requests.stream().anyMatch(request -> request.startsWith("GET http://monitoring.invalid/v3/projects/my-project/timeSeries")), requests.toString());
        }
        finally {
            proxy.stop(0);
        }
    }

    @Test
    void rejectsInvalidProxyUrls()
    {
        LLMVertexAIAsset base = asset("g", null);
        for (String url : List.of("http://localhost", "https://localhost:8080", "http://user:pass@localhost:8080", "http://localhost:8080/path"))
        {
            LLMVertexAIAsset invalid = new LLMVertexAIAsset(base.identity(), base.googleUrl(), base.resourceName(), null, url, base.profile());
            assertThrows(IllegalStateException.class, () -> GoogleMultiClientConfig.create(context(Map.of(), invalid)), url);
        }
    }

    private static WombatContext context(Map<String, String> environment, LLMVertexAIAsset asset)
    {
        return new ResolvedContext(
            List.of(new Environment("env", List.of(asset))),
            new EnvironmentSecretResolver(environment::get));
    }

    private static LLMVertexAIAsset asset(String id, String credentialsKey)
    {
        return new LLMVertexAIAsset(AssetIdentity.of(id, "env", id, LLMVertexAIModule.TYPE), "https://monitoring.googleapis.com", "projects/my-project", credentialsKey,
            new LLMVertexAIProfile(500, List.of("gemini-2.5-flash")));
    }

    private static String serviceAccountJson() throws Exception
    {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        String body = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(generator.generateKeyPair().getPrivate().getEncoded());
        String pem = "-----BEGIN PRIVATE KEY-----\n" + body + "\n-----END PRIVATE KEY-----\n";

        return new ObjectMapper().writeValueAsString(Map.of(
            "type", "service_account",
            "project_id", "my-project",
            "private_key_id", "key-id",
            "private_key", pem,
            "client_email", "wombat@my-project.iam.gserviceaccount.com",
            "client_id", "1234567890",
            "token_uri", "https://oauth2.googleapis.com/token"
        ));
    }
}
