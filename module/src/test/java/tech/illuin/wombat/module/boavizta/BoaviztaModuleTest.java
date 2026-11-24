package tech.illuin.wombat.module.boavizta;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.evaluation.WombatEvaluationResolver;
import tech.illuin.wombat.core.evaluation.impact.kubernetes.KubernetesMetricResolver;
import tech.illuin.wombat.core.evaluation.impact.llm.LLMMetricResolver;
import tech.illuin.wombat.core.module.MetricResolvers;
import tech.illuin.wombat.core.module.ModuleSettings;
import tech.illuin.wombat.core.module.ProcessorFactory;
import tech.illuin.wombat.core.module.WombatServices;
import tech.illuin.wombat.core.secret.SecretResolver;
import tech.illuin.wombat.module.boavizta.impact.BoaviztaEvaluationResolver;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class BoaviztaModuleTest
{
    private final BoaviztaModule module = new BoaviztaModule();

    @Test
    void servesTheKubernetesFamilyWithoutAnAssetType()
    {
        assertEquals(ServiceFamily.KUBERNETES_CONTAINER, this.module.type().family());
        assertTrue(this.module.assetClass().isEmpty());
        assertEquals(Optional.of(BoaviztaSettings.class), this.module.settingsClass());
    }

    @Test
    void providesTheBoaviztaImpactResolverWithItsDefaults()
    {
        // Building the client does not dial the API.
        ProcessorFactory<WombatEvaluationResolver> factory = this.module.impactResolver().orElseThrow();
        assertInstanceOf(BoaviztaEvaluationResolver.class, factory.create(services(List.of()), mock(Asset.class)));
    }

    @Test
    void buildsItsClientFromTheSettingsInTheServices()
    {
        ProcessorFactory<WombatEvaluationResolver> factory = this.module.impactResolver().orElseThrow();
        WombatServices services = services(List.of(new BoaviztaSettings("http://boavizta:5000/v1/", "http://proxy.example.com")));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> factory.create(services, mock(Asset.class)));
        assertTrue(e.getMessage().contains("http://proxy.example.com"), e.getMessage());
    }

    @Test
    void settings_fallBackToThePublicApi()
    {
        assertEquals(BoaviztaSettings.DEFAULT_URI, BoaviztaSettings.DEFAULT.uri());
        assertEquals(BoaviztaSettings.DEFAULT_URI, new BoaviztaSettings(" ", null).uri());
        assertEquals(BoaviztaModule.TYPE, BoaviztaSettings.DEFAULT.type());
    }

    private static WombatServices services(List<ModuleSettings> settings)
    {
        return new WombatServices(
            SecretResolver.standard(),
            new MetricResolvers(mock(KubernetesMetricResolver.class), mock(LLMMetricResolver.class)),
            settings
        );
    }
}
