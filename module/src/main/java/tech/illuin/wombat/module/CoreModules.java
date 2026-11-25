package tech.illuin.wombat.module;

import tech.illuin.wombat.core.module.WombatModule;
import tech.illuin.wombat.module.boavizta.BoaviztaModule;
import tech.illuin.wombat.module.ecologits.EcologitsModule;
import tech.illuin.wombat.module.kubernetes_api.KubernetesAPIModule;
import tech.illuin.wombat.module.kubernetes_metrics.KubernetesMetricsModule;
import tech.illuin.wombat.module.kubernetes_static.KubernetesStaticModule;
import tech.illuin.wombat.module.llm_anthropic.LLMAnthropicModule;
import tech.illuin.wombat.module.llm_metrics.LLMMetricsModule;
import tech.illuin.wombat.module.llm_prometheus.LLMPrometheusModule;
import tech.illuin.wombat.module.llm_static.LLMStaticModule;
import tech.illuin.wombat.module.llm_vertexai.LLMVertexAIModule;

import java.util.List;

/**
 * The modules shipped in {@code wombat-core-module}, to hand them to a {@code WombatCore} without listing them one by
 * one.
 */
public final class CoreModules
{
    private CoreModules() {}

    /**
     * @return new instances, so that no two cores share one, in the order of the {@code META-INF/services} file: family
     *         modules last, in the order they are tried in
     */
    public static List<WombatModule> all()
    {
        return List.of(
            new KubernetesAPIModule(),
            new KubernetesStaticModule(),
            new LLMAnthropicModule(),
            new LLMVertexAIModule(),
            new LLMPrometheusModule(),
            new LLMStaticModule(),
            new KubernetesMetricsModule(),
            new LLMMetricsModule(),
            new BoaviztaModule(),
            new EcologitsModule()
        );
    }
}
