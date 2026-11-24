package tech.illuin.wombat.core.module.global;

import tech.illuin.wombat.core.evaluation.WombatEvaluationResolver;
import tech.illuin.wombat.core.module.WombatModule;
import tech.illuin.wombat.core.module.asset.AssetProcessorFactory;

import java.util.Optional;

public interface GlobalModule extends WombatModule
{
    ModuleType type();

    default Optional<GlobalProcessorFactory<WombatEvaluationResolver>> impactResolver()
    {
        return Optional.empty();
    }

    default Optional<GlobalProcessorFactory<WombatEvaluationResolver>> costResolver()
    {
        return Optional.empty();
    }
}
