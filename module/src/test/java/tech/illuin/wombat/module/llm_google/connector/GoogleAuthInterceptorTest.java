package tech.illuin.wombat.module.llm_google.connector;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoogleAuthInterceptorTest
{
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
        assertThrows(IllegalStateException.class, () -> new GoogleAuthInterceptor("{}"));
    }

    @Test
    void doesNotTouchApplicationDefaultCredentialsAtConstruction()
    {
        new GoogleAuthInterceptor((String) null);
        new GoogleAuthInterceptor("  ");
    }
}
