package tech.illuin.wombat.module.kubernetes_api;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.module.ProcessorFactory;
import tech.illuin.wombat.core.module.WombatServices;
import tech.illuin.wombat.core.secret.EnvironmentSecretResolver;
import tech.illuin.wombat.core.secret.MissingSecretException;
import tech.illuin.wombat.core.secret.SecretKey;
import tech.illuin.wombat.core.source.Period;
import tech.illuin.wombat.core.source.WombatSource;
import tech.illuin.wombat.module.kubernetes_api.source.KubernetesAPISource;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class KubernetesAPIModuleTest
{
    private static final String KUBECONFIG = """
        apiVersion: v1
        kind: Config
        clusters:
          - name: test
            cluster:
              server: https://127.0.0.1:6443
        contexts:
          - name: test
            context:
              cluster: test
              user: test
        current-context: test
        users:
          - name: test
            user:
              token: test-token
        """;

    private final KubernetesAPIModule module = new KubernetesAPIModule();

    @Test
    void providesASource()
    {
        // Building the client parses the kubeconfig, but does not connect to the cluster.
        Optional<ProcessorFactory<WombatSource>> factory = this.module.source();
        assertTrue(factory.isPresent());

        WombatSource source = factory.get().create(services(Map.of(EnvironmentSecretResolver.PREFIX + "KUBECONFIG_TEST", KUBECONFIG)), asset());
        assertInstanceOf(KubernetesAPISource.class, source);
        ((KubernetesAPISource) source).close();
    }

    @Test
    void anUnboundKubeconfigFailsTheSource()
    {
        MissingSecretException e = assertThrows(MissingSecretException.class, () -> this.module.source().orElseThrow().create(services(Map.of()), asset()));

        assertEquals("KUBECONFIG_TEST", e.key());
    }

    @Test
    void closingTheSourceClosesItsClient()
    {
        KubernetesClient client = mock(KubernetesClient.class);

        new KubernetesAPISource(client).close();

        verify(client).close();
    }

    private static WombatServices services(Map<String, String> secrets)
    {
        return new WombatServices(new EnvironmentSecretResolver(secrets::get));
    }

    private static KubernetesAPIAsset asset()
    {
        return new KubernetesAPIAsset(
            AssetIdentity.of("k", "env", KubernetesAPIModule.TYPE),
            SecretKey.of("KUBECONFIG_TEST"), Set.of("default"), Optional.empty(), Optional.empty(), Period.ZERO, null
        );
    }
}
