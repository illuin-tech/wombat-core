package tech.illuin.wombat.prometheus;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PrometheusMultiClientApi
{
    private final Map<String, PrometheusClient> clients;

    public PrometheusMultiClientApi()
    {
        this.clients = new HashMap<>();
    }

    public Optional<PrometheusClient> get(String id)
    {
        return Optional.ofNullable(this.clients.get(id));
    }

    public void register(String id, PrometheusClient client)
    {
        this.clients.put(id, client);
    }
}
