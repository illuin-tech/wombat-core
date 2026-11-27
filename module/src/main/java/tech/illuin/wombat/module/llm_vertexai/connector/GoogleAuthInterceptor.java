package tech.illuin.wombat.module.llm_vertexai.connector;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.http.HttpTransportFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import tech.illuin.wombat.core.secret.MissingSecretException;
import tech.illuin.wombat.core.secret.Secret;

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
    private final Secret serviceAccountKey;
    /* The key the credentials were parsed from, so they are parsed again only once the key is rotated. */
    private String serviceAccountJson;

    /**
     * @param serviceAccountKey the service-account key, {@code null} to rely on Application Default Credentials
     */
    public GoogleAuthInterceptor(Secret serviceAccountKey)
    {
        this(serviceAccountKey, null);
    }

    public GoogleAuthInterceptor(Secret serviceAccountKey, Proxy proxy)
    {
        this.transportFactory = proxy == null ? NetHttpTransport::new : () -> new NetHttpTransport.Builder().setProxy(proxy).build();
        this.serviceAccountKey = serviceAccountKey;
        /* Parsed upfront, so a malformed key fails the component's creation rather than its first request. */
        if (serviceAccountKey != null)
            this.serviceAccountCredentials();
    }

    GoogleAuthInterceptor(GoogleCredentials credentials)
    {
        this.transportFactory = NetHttpTransport::new;
        this.serviceAccountKey = null;
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
        catch (IOException | MissingSecretException e) {
            throw new IllegalStateException("Unable to obtain a Google access token: " + e.getMessage(), e);
        }
    }

    /* Package-private so tests can tell whether the credentials were parsed again. */
    GoogleCredentials credentials() throws IOException
    {
        if (this.serviceAccountKey != null)
            return this.serviceAccountCredentials();

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

    private synchronized GoogleCredentials serviceAccountCredentials()
    {
        String json = this.serviceAccountKey.fetch();
        if (!json.equals(this.serviceAccountJson))
        {
            this.credentials = this.fromJson(json);
            this.serviceAccountJson = json;
        }
        return this.credentials;
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
