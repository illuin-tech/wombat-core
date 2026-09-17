package tech.illuin.wombat.module.llm_prometheus.connector;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PrometheusMultiClient
{
    private final Map<String, PrometheusClient> clients;

    public PrometheusMultiClient()
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
