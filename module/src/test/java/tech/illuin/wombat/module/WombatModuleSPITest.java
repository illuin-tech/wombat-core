package tech.illuin.wombat.module;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.module.WombatModule;
import tech.illuin.wombat.module.boavizta.BoaviztaModule;
import tech.illuin.wombat.module.ecologits.EcologitsModule;
import tech.illuin.wombat.module.kubernetes_api.KubernetesAPIModule;
import tech.illuin.wombat.module.kubernetes_metrics.KubernetesMetricsModule;
import tech.illuin.wombat.module.kubernetes_static.KubernetesStaticModule;
import tech.illuin.wombat.module.llm_anthropic.LLMAnthropicModule;
import tech.illuin.wombat.module.llm_metrics.LLMMetricsModule;
import tech.illuin.wombat.module.llm_vertexai.LLMVertexAIModule;
import tech.illuin.wombat.module.llm_prometheus.LLMPrometheusModule;
import tech.illuin.wombat.module.llm_static.LLMStaticModule;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WombatModuleSPITest
{
    @Test
    void serviceLoader_discoversAllBuiltInModules()
    {
        ServiceLoader<WombatModule> loader = ServiceLoader.load(WombatModule.class);
        List<WombatModule> modules = new ArrayList<>();
        loader.forEach(modules::add);

        assertEquals(10, modules.size());
        Set<Class<? extends WombatModule>> moduleClasses = modules.stream()
            .map(WombatModule::getClass)
            .collect(Collectors.toSet());

        assertTrue(moduleClasses.contains(KubernetesAPIModule.class));
        assertTrue(moduleClasses.contains(KubernetesStaticModule.class));
        assertTrue(moduleClasses.contains(LLMAnthropicModule.class));
        assertTrue(moduleClasses.contains(LLMVertexAIModule.class));
        assertTrue(moduleClasses.contains(LLMPrometheusModule.class));
        assertTrue(moduleClasses.contains(LLMStaticModule.class));
        assertTrue(moduleClasses.contains(KubernetesMetricsModule.class));
        assertTrue(moduleClasses.contains(LLMMetricsModule.class));
        assertTrue(moduleClasses.contains(BoaviztaModule.class));
        assertTrue(moduleClasses.contains(EcologitsModule.class));
    }

    @Test
    void coreModules_listTheServiceLoaderModulesInTheSameOrder()
    {
        List<Class<? extends WombatModule>> discovered = ServiceLoader.load(WombatModule.class).stream()
            .map(ServiceLoader.Provider::type)
            .toList();
        List<Class<? extends WombatModule>> listed = CoreModules.all().stream()
            .<Class<? extends WombatModule>>map(WombatModule::getClass)
            .toList();

        assertEquals(discovered, listed);
    }

    @Test
    void moduleTypes_useCorrectNamespaceAndNames()
    {
        assertEquals("tech.illuin.wombat-module.kubernetes-api", KubernetesAPIModule.TYPE.name());
        assertEquals("tech.illuin.wombat-module.llm-anthropic", LLMAnthropicModule.TYPE.name());
        assertEquals("tech.illuin.wombat-module.kubernetes-static", KubernetesStaticModule.TYPE.name());
        assertEquals("tech.illuin.wombat-module.llm-vertexai", LLMVertexAIModule.TYPE.name());
        assertEquals("tech.illuin.wombat-module.llm-prometheus", LLMPrometheusModule.TYPE.name());
        assertEquals("tech.illuin.wombat-module.llm-static", LLMStaticModule.TYPE.name());
        assertEquals("tech.illuin.wombat-module.kubernetes-metrics", KubernetesMetricsModule.TYPE.name());
        assertEquals("tech.illuin.wombat-module.llm-metrics", LLMMetricsModule.TYPE.name());
        assertEquals("tech.illuin.wombat-module.boavizta", BoaviztaModule.TYPE.name());
        assertEquals("tech.illuin.wombat-module.ecologits", EcologitsModule.TYPE.name());
    }
}
