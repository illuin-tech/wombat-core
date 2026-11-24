package tech.illuin.wombat.module.ecologits;

import tech.illuin.wombat.core.asset.type.FamilyType;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.evaluation.WombatEvaluationResolver;
import tech.illuin.wombat.core.module.ModuleSettings;
import tech.illuin.wombat.core.module.ProcessorFactory;
import tech.illuin.wombat.core.module.WombatModule;
import tech.illuin.wombat.module.ecologits.connector.EcologitsClientConfig;
import tech.illuin.wombat.module.ecologits.impact.EcologitsEvaluationResolver;

import java.util.Optional;

public class EcologitsModule implements WombatModule
{
    public static final FamilyType TYPE = FamilyType.of("tech.illuin", "wombat-module", "ecologits", ServiceFamily.LLM);

    @Override
    public FamilyType type()
    {
        return TYPE;
    }

    @Override
    public Optional<Class<? extends ModuleSettings>> settingsClass()
    {
        return Optional.of(EcologitsSettings.class);
    }

    @Override
    public Optional<ProcessorFactory<WombatEvaluationResolver>> impactResolver()
    {
        return Optional.of((services, asset) -> {
            EcologitsSettings settings = services.settings(TYPE)
                .map(EcologitsSettings.class::cast)
                .orElse(EcologitsSettings.DEFAULT);
            return new EcologitsEvaluationResolver(EcologitsClientConfig.create(settings.uri(), settings.proxyUrl()));
        });
    }
}
