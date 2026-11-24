package tech.illuin.wombat.module.boavizta;

import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.evaluation.WombatEvaluationResolver;
import tech.illuin.wombat.core.module.global.GlobalModule;
import tech.illuin.wombat.core.module.global.GlobalProcessorFactory;
import tech.illuin.wombat.core.module.global.ModuleType;
import tech.illuin.wombat.core.secret.SecretResolver;
import tech.illuin.wombat.module.boavizta.connector.BoaviztaClient;
import tech.illuin.wombat.module.boavizta.connector.BoaviztaClientConfig;
import tech.illuin.wombat.module.boavizta.impact.BoaviztaEvaluationResolver;

import java.util.Optional;

public class BoaviztaModule implements GlobalModule
{
    public static final String URL_KEY = "boavizta_url";
    public static final String PROXY_KEY = "boavizta_proxy";

    public static final ModuleType TYPE = ModuleType.of("tech.illuin", "wombat-module", "boavizta", ServiceFamily.KUBERNETES_CONTAINER);

    @Override
    public ModuleType type()
    {
        return TYPE;
    }

    @Override
    public Optional<GlobalProcessorFactory<WombatEvaluationResolver>> impactResolver()
    {
        return Optional.of(services -> {
            SecretResolver secrets = services.secrets();
            BoaviztaClient client = BoaviztaClientConfig.create(secrets.require(URL_KEY), secrets.require(PROXY_KEY));
            return new BoaviztaEvaluationResolver(client);
        });
    }
}
