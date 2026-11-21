package tech.illuin.wombat.module.kubernetes_api.connector;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.secret.EnvironmentSecretResolver;
import tech.illuin.wombat.core.secret.SecretKey;
import tech.illuin.wombat.core.secret.SecretResolver;
import tech.illuin.wombat.core.source.Period;
import tech.illuin.wombat.module.kubernetes_api.KubernetesAPIAsset;
import tech.illuin.wombat.module.kubernetes_api.KubernetesAPIModule;

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
        try (KubernetesClient client = KubernetesClientConfig.create(asset(Optional.empty()), secrets()))
        {
            assertEquals("https://first.example:6443/", client.getMasterUrl().toString());
        }
    }

    @Test
    void targetsTheRequestedContext()
    {
        try (KubernetesClient client = KubernetesClientConfig.create(asset(Optional.of("second")), secrets()))
        {
            assertEquals("https://second.example:6443/", client.getMasterUrl().toString());
        }
    }

    @Test
    void failsOnAnUnknownContext()
    {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> KubernetesClientConfig.create(asset(Optional.of("third")), secrets()));

        assertTrue(e.getMessage().contains("third"), e.getMessage());
    }

    private static SecretResolver secrets()
    {
        return new EnvironmentSecretResolver(Map.of("KUBECONFIG_TEST", KUBECONFIG)::get);
    }

    private static KubernetesAPIAsset asset(Optional<String> context)
    {
        return new KubernetesAPIAsset(
            AssetIdentity.of("k", "env", KubernetesAPIModule.TYPE),
            SecretKey.of("KUBECONFIG_TEST"), null, context, Optional.empty(), Period.ZERO, null
        );
    }
}
