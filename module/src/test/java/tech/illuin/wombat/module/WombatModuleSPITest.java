package tech.illuin.wombat.module;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.module.WombatModule;
import tech.illuin.wombat.module.kubernetes_api.KubernetesAPIModule;
import tech.illuin.wombat.module.kubernetes_simulated.KubernetesSimulatedModule;
import tech.illuin.wombat.module.llm_prometheus.LLMPrometheusModule;
import tech.illuin.wombat.module.llm_simulated.LLMSimulatedModule;
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

        assertEquals(5, modules.size());
        Set<Class<? extends WombatModule>> moduleClasses = modules.stream()
            .map(WombatModule::getClass)
            .collect(Collectors.toSet());

        assertTrue(moduleClasses.contains(KubernetesAPIModule.class));
        assertTrue(moduleClasses.contains(KubernetesSimulatedModule.class));
        assertTrue(moduleClasses.contains(LLMPrometheusModule.class));
        assertTrue(moduleClasses.contains(LLMSimulatedModule.class));
        assertTrue(moduleClasses.contains(LLMStaticModule.class));
    }

    @Test
    void moduleTypes_useCorrectNamespaceAndNames()
    {
        assertEquals("tech.illuin.wombat-module.kubernetes-api", KubernetesAPIModule.TYPE.name());
        assertEquals("tech.illuin.wombat-module.kubernetes-simulated", KubernetesSimulatedModule.TYPE.name());
        assertEquals("tech.illuin.wombat-module.llm-prometheus", LLMPrometheusModule.TYPE.name());
        assertEquals("tech.illuin.wombat-module.llm-simulated", LLMSimulatedModule.TYPE.name());
        assertEquals("tech.illuin.wombat-module.llm-static", LLMStaticModule.TYPE.name());
    }
}
