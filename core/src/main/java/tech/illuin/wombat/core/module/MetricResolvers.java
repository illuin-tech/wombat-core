package tech.illuin.wombat.core.module;

import tech.illuin.wombat.core.evaluation.impact.kubernetes.KubernetesMetricResolver;
import tech.illuin.wombat.core.evaluation.impact.llm.LLMMetricResolver;

import java.util.Objects;

/**
 * The read side of the metric store the core persists sourced metrics to, one resolver per family.
 */
public record MetricResolvers(
    KubernetesMetricResolver kubernetes,
    LLMMetricResolver llm
) {
    public MetricResolvers
    {
        Objects.requireNonNull(kubernetes, "kubernetes cannot be null");
        Objects.requireNonNull(llm, "llm cannot be null");
    }
}
