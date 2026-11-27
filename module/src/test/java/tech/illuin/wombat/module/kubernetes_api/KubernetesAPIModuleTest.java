package tech.illuin.wombat.module.kubernetes_api;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.profile.Lifetime;
import tech.illuin.wombat.core.asset.profile.ServerProvider;
import tech.illuin.wombat.core.module.ProcessorFactory;
import tech.illuin.wombat.core.evaluation.impact.kubernetes.KubernetesMetricResolver;
import tech.illuin.wombat.core.evaluation.impact.llm.LLMMetricResolver;
import tech.illuin.wombat.core.module.MetricResolvers;
import tech.illuin.wombat.core.module.WombatServices;
import tech.illuin.wombat.core.secret.EnvironmentSecretResolver;
import tech.illuin.wombat.core.secret.MissingSecretException;
import tech.illuin.wombat.core.secret.SecretKey;
import tech.illuin.wombat.core.source.Period;
import tech.illuin.wombat.core.source.WombatSource;
import tech.illuin.wombat.module.kubernetes_api.connector.KubernetesClientSupplier;
import tech.illuin.wombat.module.kubernetes_api.profile.KubernetesProfileResolver;
import tech.illuin.wombat.module.kubernetes_api.profile.ProfileInferenceException;
import tech.illuin.wombat.module.kubernetes_api.source.KubernetesAPISource;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
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
    void providesAProfileResolverKeepingADeclaredProfile() throws Exception
    {
        KubernetesAPICloudProfile declared = new KubernetesAPICloudProfile(ServerProvider.aws, "m5.xlarge", Country.FRANCE, Lifetime.ofYears(5));

        try (KubernetesProfileResolver resolver = (KubernetesProfileResolver) this.module.profileResolver().orElseThrow().create(services(Map.of()), asset(declared)))
        {
            assertTrue(resolver.accept(asset(declared)));
            // A declared profile needs no cluster, hence no kubeconfig
            assertSame(declared, resolver.resolve(asset(declared)));
        }
    }

    @Test
    void providesAProfileResolverInferringThroughTheAssetsKubeconfig()
    {
        // Without its kubeconfig bound, none of the sources of the polled namespace can be read, and each tells why
        try (KubernetesProfileResolver resolver = (KubernetesProfileResolver) this.module.profileResolver().orElseThrow().create(services(Map.of()), asset()))
        {
            assertTrue(resolver.accept(asset()));

            ProfileInferenceException e = assertThrows(ProfileInferenceException.class, () -> resolver.resolve(asset()));
            assertTrue(e.getMessage().startsWith("Cannot infer the server profile: nodes unreadable (cannot connect to the cluster"), e.getMessage());
            assertTrue(e.getMessage().contains("quotas of namespace default unreadable"), e.getMessage());
            assertTrue(e.getMessage().contains("pods unreadable"), e.getMessage());
        }
    }

    @Test
    void closingTheSourceClosesItsClients()
    {
        KubernetesClientSupplier clients = mock(KubernetesClientSupplier.class);

        new KubernetesAPISource(clients).close();

        verify(clients).close();
    }

    private static WombatServices services(Map<String, String> secrets)
    {
        return new WombatServices(
            new EnvironmentSecretResolver(secrets::get),
            new MetricResolvers(mock(KubernetesMetricResolver.class), mock(LLMMetricResolver.class)),
            List.of()
        );
    }

    private static KubernetesAPIAsset asset()
    {
        return asset(null);
    }

    private static KubernetesAPIAsset asset(KubernetesAPIServerProfile profile)
    {
        return new KubernetesAPIAsset(
            AssetIdentity.of("k", "env", KubernetesAPIModule.TYPE),
            SecretKey.of("KUBECONFIG_TEST"), Set.of("default"), Optional.empty(), Optional.empty(), Period.ZERO, profile
        );
    }
}
