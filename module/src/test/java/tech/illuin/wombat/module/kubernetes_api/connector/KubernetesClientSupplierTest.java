package tech.illuin.wombat.module.kubernetes_api.connector;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.secret.EnvironmentSecretResolver;
import tech.illuin.wombat.core.secret.MissingSecretException;
import tech.illuin.wombat.core.secret.SecretKey;
import tech.illuin.wombat.core.source.Period;
import tech.illuin.wombat.module.kubernetes_api.KubernetesAPIAsset;
import tech.illuin.wombat.module.kubernetes_api.KubernetesAPIModule;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KubernetesClientSupplierTest
{
    private final Map<String, String> environment = new HashMap<>(Map.of(EnvironmentSecretResolver.PREFIX + "KUBECONFIG_TEST", kubeconfig("first")));

    @Test
    void keepsTheClientWhileTheKubeconfigIsUnchanged()
    {
        try (KubernetesClientSupplier clients = this.clients())
        {
            assertSame(clients.get(), clients.get());
        }
    }

    @Test
    void replacesAndClosesTheClientOnceTheKubeconfigIsRotated()
    {
        try (KubernetesClientSupplier clients = this.clients())
        {
            KubernetesClient previous = clients.get();
            this.rotate(kubeconfig("second"));

            KubernetesClient rotated = clients.get();

            assertNotSame(previous, rotated);
            assertEquals("https://second.example:6443/", rotated.getMasterUrl().toString());
            assertTrue(previous.getHttpClient().isClosed());
            assertFalse(rotated.getHttpClient().isClosed());
        }
    }

    @Test
    void closingClosesTheCurrentClient()
    {
        KubernetesClientSupplier clients = this.clients();
        KubernetesClient client = clients.get();

        clients.close();

        assertTrue(client.getHttpClient().isClosed());
    }

    @Test
    void failsUpfrontWhenTheKubeconfigIsUnbound()
    {
        this.environment.clear();

        MissingSecretException e = assertThrows(MissingSecretException.class, this::clients);

        assertEquals("KUBECONFIG_TEST", e.key());
    }

    @Test
    void failsOnceTheKubeconfigIsUnbound()
    {
        try (KubernetesClientSupplier clients = this.clients())
        {
            this.environment.clear();

            assertThrows(MissingSecretException.class, clients::get);
        }
    }

    @Test
    void anUnusableRotationFailsAndIsTriedAgainOnTheNextCall()
    {
        try (KubernetesClientSupplier clients = this.clients(Optional.of("first")))
        {
            this.rotate(kubeconfig("second").replace("name: first", "name: other"));
            assertThrows(IllegalArgumentException.class, clients::get);
            assertThrows(IllegalArgumentException.class, clients::get);

            this.rotate(kubeconfig("second"));
            assertEquals("https://first.example:6443/", clients.get().getMasterUrl().toString());
        }
    }

    @Test
    void anUnparsableRotationFailsAndIsTriedAgainOnTheNextCall()
    {
        try (KubernetesClientSupplier clients = this.clients())
        {
            this.rotate("clusters: [");
            assertThrows(IllegalArgumentException.class, clients::get);

            this.rotate(kubeconfig("second"));
            assertEquals("https://second.example:6443/", clients.get().getMasterUrl().toString());
        }
    }

    @Test
    void handsOutAnInClusterClientAsIs()
    {
        KubernetesClient client = KubernetesClientConfig.create(asset(Optional.empty()), kubeconfig("first"));
        KubernetesClientSupplier clients = new KubernetesClientSupplier(client);

        assertSame(client, clients.get());
        clients.close();
        assertTrue(client.getHttpClient().isClosed());
    }

    private static KubernetesAPIAsset asset(Optional<String> context)
    {
        return new KubernetesAPIAsset(AssetIdentity.of("k", "env", KubernetesAPIModule.TYPE), SecretKey.of("KUBECONFIG_TEST"), null, context, Optional.empty(), Period.ZERO, null);
    }

    private void rotate(String kubeconfig)
    {
        this.environment.put(EnvironmentSecretResolver.PREFIX + "KUBECONFIG_TEST", kubeconfig);
    }

    private KubernetesClientSupplier clients()
    {
        return this.clients(Optional.empty());
    }

    private KubernetesClientSupplier clients(Optional<String> context)
    {
        return KubernetesClientConfig.create(
            new KubernetesAPIAsset(AssetIdentity.of("k", "env", KubernetesAPIModule.TYPE), SecretKey.of("KUBECONFIG_TEST"), null, context, Optional.empty(), Period.ZERO, null),
            new EnvironmentSecretResolver(this.environment::get)
        );
    }

    /* Two clusters, so a test can tell which one the client targets once the current context is rotated. */
    private static String kubeconfig(String currentContext)
    {
        return """
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
            current-context: %s
            users:
              - name: test
                user:
                  token: test-token
            """.formatted(currentContext);
    }
}
