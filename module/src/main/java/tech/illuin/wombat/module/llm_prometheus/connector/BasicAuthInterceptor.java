package tech.illuin.wombat.module.llm_prometheus.connector;

import feign.RequestInterceptor;
import feign.RequestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class BasicAuthInterceptor implements RequestInterceptor
{
    private final String header;

    private static final String AUTHORIZATION = "Authorization";

    public BasicAuthInterceptor(String username, String password)
    {
        String credentials = username + ":" + (password == null ? "" : password);
        this.header = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void apply(RequestTemplate requestTemplate)
    {
        requestTemplate.header(AUTHORIZATION, this.header);
    }
}
