package tech.illuin.wombat.prometheus;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class BasicAuthFilter implements ClientRequestFilter
{
    private final String header;

    public BasicAuthFilter(String username, String password)
    {
        String credentials = username + ":" + (password == null ? "" : password);
        this.header = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void filter(ClientRequestContext requestContext)
    {
        requestContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, this.header);
    }
}
