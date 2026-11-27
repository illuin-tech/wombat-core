package tech.illuin.wombat.module.kubernetes_api.connector;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.secret.SecretKey;
import tech.illuin.wombat.core.source.Period;
import tech.illuin.wombat.module.kubernetes_api.KubernetesAPIAsset;
import tech.illuin.wombat.module.kubernetes_api.KubernetesAPIModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KubernetesClientConfigTest
{
    /* Two clusters, so a test can tell which context the client targets. */
    private static final String KUBECONFIG = """
        apiVersion: v1
        kind: Config
        clusters:
          - name: first
            cluster:
              server: https://first.example:6443
          - name: second
            cluster:
              server: https://second.example:6443
        contexts:
          - name: first
            context:
              cluster: first
              user: test
          - name: second
            context:
              cluster: second
              user: test
        current-context: first
        users:
          - name: test
            user:
              token: test-token
        """;

    @Test
    void targetsTheCurrentContextByDefault()
    {
        try (KubernetesClient client = KubernetesClientConfig.create(asset(Optional.empty()), KUBECONFIG))
        {
            assertEquals("https://first.example:6443/", client.getMasterUrl().toString());
        }
    }

    @Test
    void targetsTheRequestedContext()
    {
        try (KubernetesClient client = KubernetesClientConfig.create(asset(Optional.of("second")), KUBECONFIG))
        {
            assertEquals("https://second.example:6443/", client.getMasterUrl().toString());
        }
    }

    @Test
    void failsOnAnUnknownContext()
    {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> KubernetesClientConfig.create(asset(Optional.of("third")), KUBECONFIG));

        assertTrue(e.getMessage().contains("third"), e.getMessage());
    }

    @Test
    void failsOnAnUnreadableKubeconfig()
    {
        for (String kubeconfig : List.of("clusters: [", "\t\tbad: tab", "a: *unknown", "{\"a\": ", "just a string", " "))
            for (Optional<String> context : List.of(Optional.<String>empty(), Optional.of("first")))
                assertThrows(IllegalArgumentException.class, () -> KubernetesClientConfig.create(asset(context), kubeconfig), kubeconfig);
    }

    @Test
    void inCluster_targetsTheClusterOfThePod(@TempDir Path serviceAccount) throws IOException
    {
        mountServiceAccount(serviceAccount, "first-token");

        try (KubernetesClient client = KubernetesClientConfig.createInCluster(inCluster(), Map.of("KUBERNETES_SERVICE_HOST", "10.0.0.1", "KUBERNETES_SERVICE_PORT", "6443"), serviceAccount))
        {
            assertEquals("https://10.0.0.1:6443/", client.getMasterUrl().toString());
            assertEquals(serviceAccount.resolve("ca.crt").toString(), client.getConfiguration().getCaCertFile());
        }
    }

    @Test
    void inCluster_bracketsAnIpv6Host(@TempDir Path serviceAccount) throws IOException
    {
        mountServiceAccount(serviceAccount, "token");

        try (KubernetesClient client = KubernetesClientConfig.createInCluster(inCluster(), Map.of("KUBERNETES_SERVICE_HOST", "fd00::1"), serviceAccount))
        {
            assertEquals("https://[fd00::1]:443/", client.getMasterUrl().toString());
        }
    }

    /* The kubelet rotates the projected token: each request must send the current one. */
    @Test
    void inCluster_readsTheTokenAgainOnEveryRequest(@TempDir Path serviceAccount) throws IOException
    {
        mountServiceAccount(serviceAccount, "first-token\n");

        try (KubernetesClient client = KubernetesClientConfig.createInCluster(inCluster(), Map.of("KUBERNETES_SERVICE_HOST", "10.0.0.1"), serviceAccount))
        {
            assertEquals("first-token", client.getConfiguration().getOauthTokenProvider().getToken());

            Files.writeString(serviceAccount.resolve("token"), "rotated-token");

            assertEquals("rotated-token", client.getConfiguration().getOauthTokenProvider().getToken());
        }
    }

    @Test
    void inCluster_failsOutsideAPod(@TempDir Path serviceAccount) throws IOException
    {
        IllegalStateException noHost = assertThrows(IllegalStateException.class,
            () -> KubernetesClientConfig.createInCluster(inCluster(), Map.of(), serviceAccount));
        assertTrue(noHost.getMessage().contains("in-cluster"), noHost.getMessage());

        assertThrows(IllegalStateException.class,
            () -> KubernetesClientConfig.createInCluster(inCluster(), Map.of("KUBERNETES_SERVICE_HOST", "10.0.0.1"), serviceAccount));

        Files.writeString(serviceAccount.resolve("token"), "token");
        assertThrows(IllegalStateException.class,
            () -> KubernetesClientConfig.createInCluster(inCluster(), Map.of("KUBERNETES_SERVICE_HOST", "10.0.0.1"), serviceAccount));
    }

    private static void mountServiceAccount(Path directory, String token) throws IOException
    {
        Files.writeString(directory.resolve("token"), token);
        Files.writeString(directory.resolve("ca.crt"), "not read until a request is made");
    }

    private static KubernetesAPIAsset inCluster()
    {
        return new KubernetesAPIAsset(
            AssetIdentity.of("k", "env", KubernetesAPIModule.TYPE),
            null, true, null, Optional.empty(), Optional.empty(), Period.ZERO, null
        );
    }

    private static KubernetesAPIAsset asset(Optional<String> context)
    {
        return new KubernetesAPIAsset(
            AssetIdentity.of("k", "env", KubernetesAPIModule.TYPE),
            SecretKey.of("KUBECONFIG_TEST"), null, context, Optional.empty(), Period.ZERO, null
        );
    }
}
