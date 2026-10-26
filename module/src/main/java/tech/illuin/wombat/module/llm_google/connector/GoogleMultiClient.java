package tech.illuin.wombat.module.llm_google.connector;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class GoogleMultiClient
{
    private final Map<String, GoogleClient> clients;

    public GoogleMultiClient()
    {
        this.clients = new HashMap<>();
    }

    public Optional<GoogleClient> get(String id)
    {
        return Optional.ofNullable(this.clients.get(id));
    }

    public void register(String id, GoogleClient client)
    {
        this.clients.put(id, client);
    }
}
