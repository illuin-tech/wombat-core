package tech.illuin.wombat.module.kubernetes_api.connector;


import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class KubernetesMultiClusterClient
{
    private final Map<String, KubernetesClient> clients;

    public KubernetesMultiClusterClient()
    {
        this.clients = new HashMap<>();
    }

    public Optional<KubernetesClient> get(String id)
    {
        return Optional.ofNullable(this.clients.get(id));
    }

    public void register(String id, KubernetesClient client)
    {
        this.clients.put(id, client);
    }
}
