package tech.illuin.wombat.module.llm_google.connector;

import com.google.auth.oauth2.GoogleCredentials;
import feign.RequestInterceptor;
import feign.RequestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class GoogleAuthInterceptor implements RequestInterceptor
{
    private static final String AUTHORIZATION = "Authorization";
    private static final List<String> SCOPES = List.of("https://www.googleapis.com/auth/monitoring.read");

    private volatile GoogleCredentials credentials;

    public GoogleAuthInterceptor(String serviceAccountJson)
    {
        if (serviceAccountJson != null && !serviceAccountJson.isBlank())
            this.credentials = fromJson(serviceAccountJson);
    }

    GoogleAuthInterceptor(GoogleCredentials credentials)
    {
        this.credentials = credentials;
    }

    @Override
    public void apply(RequestTemplate template)
    {
        try {
            GoogleCredentials current = this.credentials();
            current.refreshIfExpired();
            template.header(AUTHORIZATION, "Bearer " + current.getAccessToken().getTokenValue());
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to obtain a Google access token: " + e.getMessage(), e);
        }
    }

    private GoogleCredentials credentials() throws IOException
    {
        GoogleCredentials current = this.credentials;
        if (current == null)
        {
            synchronized (this)
            {
                current = this.credentials;
                if (current == null)
                    this.credentials = current = GoogleCredentials.getApplicationDefault().createScoped(SCOPES);
            }
        }
        return current;
    }

    private static GoogleCredentials fromJson(String json)
    {
        try {
            return GoogleCredentials.fromStream(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))).createScoped(SCOPES);
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to load the Google service-account key: " + e.getMessage(), e);
        }
    }
}
