package tech.illuin.wombat.module.llm_vertexai.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.secret.EnvironmentSecretResolver;
import tech.illuin.wombat.core.secret.MissingSecretException;
import tech.illuin.wombat.core.secret.Secret;

import java.io.IOException;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoogleAuthInterceptorTest
{
    private final Map<String, String> environment = new HashMap<>();
    @Test
    void sendsTheAccessTokenAsABearerHeader() throws IOException
    {
        GoogleCredentials credentials = mock(GoogleCredentials.class);
        when(credentials.getAccessToken()).thenReturn(new AccessToken("ya29.token", new Date(System.currentTimeMillis() + 3_600_000)));
        RequestTemplate template = new RequestTemplate();

        new GoogleAuthInterceptor(credentials).apply(template);

        assertEquals("Bearer ya29.token", template.headers().get("Authorization").iterator().next());
        verify(credentials).refreshIfExpired();
    }

    @Test
    void wrapsTokenRefreshFailures() throws IOException
    {
        GoogleCredentials credentials = mock(GoogleCredentials.class);
        doThrow(new IOException("boom")).when(credentials).refreshIfExpired();

        IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> new GoogleAuthInterceptor(credentials).apply(new RequestTemplate()));

        assertEquals(IOException.class, e.getCause().getClass());
    }

    @Test
    void rejectsAMalformedServiceAccountKeyAtConstruction()
    {
        this.rotate("{}");
        Secret key = this.key();

        assertThrows(IllegalStateException.class, () -> new GoogleAuthInterceptor(key));
    }

    @Test
    void doesNotTouchApplicationDefaultCredentialsAtConstruction()
    {
        new GoogleAuthInterceptor((Secret) null);
    }

    @Test
    void parsesTheServiceAccountKeyAgainOnlyOnceItIsRotated() throws Exception
    {
        this.rotate(serviceAccountJson("first@my-project.iam.gserviceaccount.com"));
        GoogleAuthInterceptor interceptor = new GoogleAuthInterceptor(this.key());
        GoogleCredentials first = interceptor.credentials();

        assertSame(first, interceptor.credentials());

        this.rotate(serviceAccountJson("rotated@my-project.iam.gserviceaccount.com"));
        GoogleCredentials rotated = interceptor.credentials();

        assertNotSame(first, rotated);
        assertEquals("rotated@my-project.iam.gserviceaccount.com", ((ServiceAccountCredentials) rotated).getClientEmail());
    }

    @Test
    void failsTheRequestOnceTheServiceAccountKeyIsUnbound() throws Exception
    {
        this.rotate(serviceAccountJson("first@my-project.iam.gserviceaccount.com"));
        GoogleAuthInterceptor interceptor = new GoogleAuthInterceptor(this.key());
        this.environment.clear();

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> interceptor.apply(new RequestTemplate()));

        assertInstanceOf(MissingSecretException.class, e.getCause());
    }

    @Test
    void aMalformedRotationFailsAndIsParsedAgainOnTheNextCall() throws Exception
    {
        this.rotate(serviceAccountJson("first@my-project.iam.gserviceaccount.com"));
        GoogleAuthInterceptor interceptor = new GoogleAuthInterceptor(this.key());

        this.rotate("{}");
        assertThrows(IllegalStateException.class, interceptor::credentials);
        assertThrows(IllegalStateException.class, interceptor::credentials);

        this.rotate(serviceAccountJson("rotated@my-project.iam.gserviceaccount.com"));
        assertEquals("rotated@my-project.iam.gserviceaccount.com", ((ServiceAccountCredentials) interceptor.credentials()).getClientEmail());
    }

    private void rotate(String json)
    {
        this.environment.put(EnvironmentSecretResolver.PREFIX + "GOOGLE_SA_KEY", json);
    }

    private Secret key()
    {
        return new EnvironmentSecretResolver(this.environment::get).requireAsync("GOOGLE_SA_KEY");
    }

    private static String serviceAccountJson(String clientEmail) throws Exception
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
            "client_email", clientEmail,
            "client_id", "1234567890",
            "token_uri", "https://oauth2.googleapis.com/token"
        ));
    }
}
