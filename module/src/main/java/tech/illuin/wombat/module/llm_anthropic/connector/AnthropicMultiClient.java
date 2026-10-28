package tech.illuin.wombat.module.llm_anthropic.connector;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AnthropicMultiClient
{
    private final Map<String, AnthropicAdminClient> clients;

    public AnthropicMultiClient()
    {
        this.clients = new HashMap<>();
    }

    public Optional<AnthropicAdminClient> get(String id)
    {
        return Optional.ofNullable(this.clients.get(id));
    }

    public void register(String id, AnthropicAdminClient client)
    {
        this.clients.put(id, client);
    }
}
