package tech.illuin.wombat.module.llm_vertexai.connector;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.http.HttpTransportFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import feign.RequestInterceptor;
import feign.RequestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.Proxy;
import java.util.List;

public class GoogleAuthInterceptor implements RequestInterceptor
{
    private static final String AUTHORIZATION = "Authorization";
    private static final List<String> SCOPES = List.of("https://www.googleapis.com/auth/monitoring.read");

    private volatile GoogleCredentials credentials;
    private final HttpTransportFactory transportFactory;

    public GoogleAuthInterceptor(String serviceAccountJson)
    {
        this(serviceAccountJson, null);
    }

    public GoogleAuthInterceptor(String serviceAccountJson, Proxy proxy)
    {
        this.transportFactory = proxy == null ? NetHttpTransport::new : () -> new NetHttpTransport.Builder().setProxy(proxy).build();
        if (serviceAccountJson != null && !serviceAccountJson.isBlank())
            this.credentials = this.fromJson(serviceAccountJson);
    }

    GoogleAuthInterceptor(GoogleCredentials credentials)
    {
        this.transportFactory = NetHttpTransport::new;
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
                {
                    current = GoogleCredentials.getApplicationDefault(this.transportFactory).createScoped(SCOPES);
                    this.credentials = current;
                }
            }
        }
        return current;
    }

    private GoogleCredentials fromJson(String json)
    {
        try {
            return GoogleCredentials.fromStream(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), this.transportFactory).createScoped(SCOPES);
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to load the Google service-account key: " + e.getMessage(), e);
        }
    }
}
