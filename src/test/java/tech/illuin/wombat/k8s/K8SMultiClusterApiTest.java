package tech.illuin.wombat.k8s;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class K8SMultiClusterApiTest
{

    @Test
    void register_andGet_returnsRegisteredClient()
    {
        K8SMultiClusterApi api = new K8SMultiClusterApi();
        KubernetesClient client = Mockito.mock(KubernetesClient.class);

        api.register("c1", client);

        Optional<KubernetesClient> result = api.get("c1");
        assertTrue(result.isPresent());
        assertEquals(client, result.get());
    }

    @Test
    void get_unknownId_returnsEmpty()
    {
        K8SMultiClusterApi api = new K8SMultiClusterApi();
        assertTrue(api.get("missing").isEmpty());
    }

    @Test
    void register_overwritesPreviousClientForSameId()
    {
        K8SMultiClusterApi api = new K8SMultiClusterApi();
        KubernetesClient first = Mockito.mock(KubernetesClient.class);
        KubernetesClient second = Mockito.mock(KubernetesClient.class);

        api.register("c1", first);
        api.register("c1", second);

        assertEquals(second, api.get("c1").orElseThrow());
    }
}
