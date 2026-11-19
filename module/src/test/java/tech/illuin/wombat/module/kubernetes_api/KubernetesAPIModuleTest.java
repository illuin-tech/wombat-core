package tech.illuin.wombat.module.kubernetes_api;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.module.ProcessorFactory;
import tech.illuin.wombat.core.module.WombatServices;
import tech.illuin.wombat.core.secret.SecretResolver;
import tech.illuin.wombat.core.source.Period;
import tech.illuin.wombat.core.source.WombatSource;
import tech.illuin.wombat.module.kubernetes_api.source.KubernetesAPISource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
    void providesASource(@TempDir Path directory) throws IOException
    {
        // Building the client reads the kubeconfig, but does not connect to the cluster.
        Path kubeconfig = Files.writeString(directory.resolve("kubeconfig"), KUBECONFIG);

        Optional<ProcessorFactory<WombatSource>> factory = this.module.source();
        assertTrue(factory.isPresent());

        WombatSource source = factory.get().create(new WombatServices(SecretResolver.standard()), asset(kubeconfig));
        assertInstanceOf(KubernetesAPISource.class, source);
        ((KubernetesAPISource) source).close();
    }

    @Test
    void closingTheSourceClosesItsClient()
    {
        KubernetesClient client = mock(KubernetesClient.class);

        new KubernetesAPISource(client).close();

        verify(client).close();
    }

    private static KubernetesAPIAsset asset(Path kubeconfig)
    {
        return new KubernetesAPIAsset(
            AssetIdentity.of("k", "env", "K", KubernetesAPIModule.TYPE),
            kubeconfig.toString(), "default", Optional.empty(), Optional.empty(), Period.ZERO, null
        );
    }
}
