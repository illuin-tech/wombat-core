package tech.illuin.wombat.module.ecologits;

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
import tech.illuin.wombat.module.ecologits.impact.EcologitsEvaluationResolver;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class EcologitsModuleTest
{
    private final EcologitsModule module = new EcologitsModule();

    @Test
    void servesTheLLMFamilyWithoutAnAssetType()
    {
        assertEquals(ServiceFamily.LLM, this.module.type().family());
        assertTrue(this.module.assetClass().isEmpty());
        assertEquals(Optional.of(EcologitsSettings.class), this.module.settingsClass());
    }

    @Test
    void providesTheEcologitsImpactResolverWithItsDefaults()
    {
        // Building the client does not dial the API.
        ProcessorFactory<WombatEvaluationResolver> factory = this.module.impactResolver().orElseThrow();
        assertInstanceOf(EcologitsEvaluationResolver.class, factory.create(services(List.of()), mock(Asset.class)));
    }

    @Test
    void buildsItsClientFromTheSettingsInTheServices()
    {
        ProcessorFactory<WombatEvaluationResolver> factory = this.module.impactResolver().orElseThrow();
        WombatServices services = services(List.of(new EcologitsSettings("http://ecologits:5000/", "http://proxy.example.com")));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> factory.create(services, mock(Asset.class)));
        assertTrue(e.getMessage().contains("http://proxy.example.com"), e.getMessage());
    }

    @Test
    void settings_fallBackToThePublicApi()
    {
        // The literal, as documented: the client appends its own versioned path.
        assertEquals("https://api.ecologits.ai/", EcologitsSettings.DEFAULT.uri());
        assertEquals(EcologitsModule.TYPE, EcologitsSettings.DEFAULT.type());
    }

    @Test
    void settings_requireAUri()
    {
        // Settings given are complete: only a module given none falls back to the public API.
        assertThrows(NullPointerException.class, () -> new EcologitsSettings(null, "http://proxy.example.com:3128"));
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
