package tech.illuin.wombat.module.llm_metrics;

import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.asset.type.FamilyType;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.module.ProcessorFactory;
import tech.illuin.wombat.core.module.WombatModule;
import tech.illuin.wombat.module.llm_metrics.activity.LLMActivityResolver;

import java.util.Optional;

/**
 * Resolves the activity of the LLM assets whose metrics are sourced, from the metric store they were persisted to.
 */
public class LLMMetricsModule implements WombatModule
{
    public static final FamilyType TYPE = FamilyType.of("tech.illuin", "wombat-module", "llm-metrics", ServiceFamily.LLM);

    @Override
    public FamilyType type()
    {
        return TYPE;
    }

    @Override
    public Optional<ProcessorFactory<WombatActivityResolver>> activityResolver()
    {
        return Optional.of((services, asset) -> new LLMActivityResolver(services.metrics().llm()));
    }
}
