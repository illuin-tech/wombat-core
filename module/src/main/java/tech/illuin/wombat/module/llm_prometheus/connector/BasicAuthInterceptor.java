package tech.illuin.wombat.module.llm_prometheus.connector;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import tech.illuin.wombat.core.secret.Secret;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class BasicAuthInterceptor implements RequestInterceptor
{
    private final String username;
    private final Secret password;

    private static final String AUTHORIZATION = "Authorization";

    public BasicAuthInterceptor(String username, Secret password)
    {
        this.username = username;
        this.password = password;
    }

    @Override
    public void apply(RequestTemplate requestTemplate)
    {
        String credentials = this.username + ":" + this.password.fetch();
        requestTemplate.header(AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
    }
}
