package tech.illuin.wombat.module.boavizta;

import tech.illuin.wombat.core.asset.type.FamilyType;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.evaluation.WombatEvaluationResolver;
import tech.illuin.wombat.core.module.ModuleSettings;
import tech.illuin.wombat.core.module.ProcessorFactory;
import tech.illuin.wombat.core.module.WombatModule;
import tech.illuin.wombat.module.boavizta.connector.BoaviztaClientConfig;
import tech.illuin.wombat.module.boavizta.impact.BoaviztaEvaluationResolver;

import java.util.Optional;

public class BoaviztaModule implements WombatModule
{
    public static final FamilyType TYPE = FamilyType.of("tech.illuin", "wombat-module", "boavizta", ServiceFamily.KUBERNETES_CONTAINER);

    @Override
    public FamilyType type()
    {
        return TYPE;
    }

    @Override
    public Optional<Class<? extends ModuleSettings>> settingsClass()
    {
        return Optional.of(BoaviztaSettings.class);
    }

    @Override
    public Optional<ProcessorFactory<WombatEvaluationResolver>> impactResolver()
    {
        return Optional.of((services, asset) -> {
            BoaviztaSettings settings = services.settings(TYPE)
                .map(BoaviztaSettings.class::cast)
                .orElse(BoaviztaSettings.DEFAULT);
            return new BoaviztaEvaluationResolver(BoaviztaClientConfig.create(settings.uri(), settings.proxyUrl()));
        });
    }
}
