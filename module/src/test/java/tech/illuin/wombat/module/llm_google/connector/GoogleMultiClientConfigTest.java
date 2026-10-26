package tech.illuin.wombat.module.llm_google.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.Environment;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.core.context.ResolvedContext;
import tech.illuin.wombat.core.context.WombatContext;
import tech.illuin.wombat.core.secret.EnvironmentSecretResolver;
import tech.illuin.wombat.core.secret.MissingSecretException;
import tech.illuin.wombat.module.llm_google.LLMGoogleAsset;
import tech.illuin.wombat.module.llm_google.LLMGoogleProfile;

import java.security.KeyPairGenerator;
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

    private static WombatContext context(Map<String, String> environment, LLMGoogleAsset asset)
    {
        return new ResolvedContext(
            List.of(new Environment("env", List.of(asset))),
            new EnvironmentSecretResolver(environment::get));
    }

    private static LLMGoogleAsset asset(String id, String credentialsKey)
    {
        return new LLMGoogleAsset(AssetIdentity.of(id, "env", id), "https://monitoring.googleapis.com", "my-project", credentialsKey, 0,
            new LLMGoogleProfile(LLMProvider.google_genai, "gemini-2.5-flash", Country.US, new LLMGoogleProfile.DynamicProfile("f", null)));
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
