package tech.illuin.wombat.kubernetes;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KubernetesMultiClusterApiTest
{

    @Test
    void register_andGet_returnsRegisteredClient()
    {
        KubernetesMultiClusterApi api = new KubernetesMultiClusterApi();
        KubernetesClient client = Mockito.mock(KubernetesClient.class);

        api.register("c1", client);

        Optional<KubernetesClient> result = api.get("c1");
        assertTrue(result.isPresent());
        assertEquals(client, result.get());
    }

    @Test
    void get_unknownId_returnsEmpty()
    {
        KubernetesMultiClusterApi api = new KubernetesMultiClusterApi();
        assertTrue(api.get("missing").isEmpty());
    }

    @Test
    void register_overwritesPreviousClientForSameId()
    {
        KubernetesMultiClusterApi api = new KubernetesMultiClusterApi();
        KubernetesClient first = Mockito.mock(KubernetesClient.class);
        KubernetesClient second = Mockito.mock(KubernetesClient.class);

        api.register("c1", first);
        api.register("c1", second);

        assertEquals(second, api.get("c1").orElseThrow());
    }
}
