package tech.illuin.wombat.module.llm_prometheus.connector;

import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.secret.EnvironmentSecretResolver;
import tech.illuin.wombat.core.secret.MissingSecretException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BasicAuthInterceptorTest
{
    private final Map<String, String> environment = new HashMap<>(Map.of(EnvironmentSecretResolver.PREFIX + "PROM_PASSWORD", "s3cret"));
    private final BasicAuthInterceptor interceptor = new BasicAuthInterceptor("user", new EnvironmentSecretResolver(this.environment::get).requireAsync("PROM_PASSWORD"));

    @Test
    void sendsTheCredentialsAsABasicHeader()
    {
        assertEquals("user:s3cret", this.credentials());
    }

    @Test
    void sendsARotatedPasswordOnTheNextRequest()
    {
        this.credentials();
        this.environment.put(EnvironmentSecretResolver.PREFIX + "PROM_PASSWORD", "rotated");

        assertEquals("user:rotated", this.credentials());
    }

    @Test
    void failsTheRequestOnceThePasswordIsUnbound()
    {
        this.environment.clear();

        MissingSecretException e = assertThrows(MissingSecretException.class, this::credentials);

        assertEquals("PROM_PASSWORD", e.key());
    }

    private String credentials()
    {
        RequestTemplate template = new RequestTemplate();
        this.interceptor.apply(template);
        String header = template.headers().get("Authorization").iterator().next();
        return new String(Base64.getDecoder().decode(header.substring("Basic ".length())), StandardCharsets.UTF_8);
    }
}
