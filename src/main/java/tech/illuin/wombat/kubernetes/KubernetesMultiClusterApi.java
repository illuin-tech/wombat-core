package tech.illuin.wombat.kubernetes;


import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class KubernetesMultiClusterApi
{
    private final Map<String, KubernetesClient> clients;

    public KubernetesMultiClusterApi()
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
