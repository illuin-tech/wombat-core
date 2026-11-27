package tech.illuin.wombat.module.llm_anthropic.connector;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import tech.illuin.wombat.core.secret.Secret;

public class AnthropicHeadersInterceptor implements RequestInterceptor
{
    private final Secret adminKey;

    private static final String API_KEY = "x-api-key";
    private static final String VERSION = "anthropic-version";
    private static final String VERSION_VALUE = "2023-06-01";

    public AnthropicHeadersInterceptor(Secret adminKey)
    {
        this.adminKey = adminKey;
    }

    @Override
    public void apply(RequestTemplate requestTemplate)
    {
        requestTemplate.header(API_KEY, this.adminKey.fetch());
        requestTemplate.header(VERSION, VERSION_VALUE);
    }
}
