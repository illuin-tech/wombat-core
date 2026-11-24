package tech.illuin.wombat.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.context.WombatContextProvider;
import tech.illuin.wombat.core.evaluation.WombatEvaluationResolver;
import tech.illuin.wombat.core.evaluation.AssetEvaluator;
import tech.illuin.wombat.core.module.asset.AssetProcessorFactory;
import tech.illuin.wombat.core.module.WombatModule;
import tech.illuin.wombat.core.module.WombatServices;
import tech.illuin.wombat.core.secret.SecretResolver;
import tech.illuin.wombat.core.source.AssetMonitor;
import tech.illuin.wombat.core.source.persistence.WombatMetricPersister;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class WombatCore implements AutoCloseable
{
    private final Map<String, WombatModule> modules;
    private final WombatContextProvider contextProvider;
    private final WombatServices services;
    private final WombatMetricPersister persister;
    private final CoreDefaults defaults;

    private static final Logger logger = LoggerFactory.getLogger(WombatCore.class);

    /** Resolves secrets from the process environment, which is where a deployment supplies them. */
    public WombatCore(
        WombatContextProvider contextProvider,
        WombatMetricPersister persister,
        Collection<WombatModule> modules,
        Consumer<CoreDefaults> defaultsAdjuster
    ) {
        this(contextProvider, new WombatServices(SecretResolver.standard()), persister, modules, defaultsAdjuster);
    }

    public WombatCore(
        WombatContextProvider contextProvider,
        WombatServices services,
        WombatMetricPersister persister,
        Collection<WombatModule> modules,
        Consumer<CoreDefaults> defaultsAdjuster
    ) {
        this.contextProvider = contextProvider;
        this.services = services;
        this.persister = persister;
        this.modules = new HashMap<>();
        this.defaults = new CoreDefaults();
        defaultsAdjuster.accept(this.defaults);
        for (WombatModule module : modules)
            this.register(module);
    }

    private void register(WombatModule module)
    {
        String name = module.type().name();
        if (this.modules.containsKey(name))
            throw new IllegalArgumentException("A module was already registered for asset-type " + name + " with type " + this.modules.get(name).getClass().getSimpleName());

        this.modules.put(name, module);
        logger.info("Registered module {} for asset-type {}", module.getClass().getSimpleName(), name);
    }

    public AssetMonitor createMonitor()
    {
        return this.createMonitor(Executors.newVirtualThreadPerTaskExecutor());
    }

    public AssetMonitor createMonitor(ExecutorService executorService)
    {
        logger.info("Creating wombat source-monitor out of {} registered modules", this.modules.size());
        AssetMonitor monitor = new AssetMonitor(this.contextProvider, this.services, this.persister, executorService);

        for (WombatModule module : this.modules.values())
        {
            module.source().ifPresentOrElse(
                factory -> monitor.register(module.type(), factory),
                () -> logger.debug("Skipping monitor registration for asset-type {}", module.type().name())
            );
        }
        return monitor;
    }

    public AssetEvaluator createEvaluator()
    {
        logger.info("Creating wombat impact-calculator out of {} registered modules", this.modules.size());
        AssetEvaluator calculator = new AssetEvaluator(this.contextProvider, this.services);

        for (WombatModule module : this.modules.values())
        {
            ServiceFamily family = module.type().family();
            String typeName = module.type().name();

            module.activityResolver().or(() -> this.defaults.getActivityResolver(family)).ifPresentOrElse(
                factory -> calculator.registerActivityResolver(module.type(), factory),
                () -> logger.error("No activity-resolver available for asset-type {}, its assets will not be evaluated", typeName)
            );
            module.impactResolver().or(() -> this.defaults.getImpactResolver(family)).ifPresentOrElse(
                factory -> calculator.registerImpactResolver(module.type(), factory),
                () -> logger.error("No impact-resolver available for asset-type {}, impacts of its assets will not be evaluated", typeName)
            );
            module.costResolver().or(() -> this.defaults.getCostResolver(family)).ifPresentOrElse(
                factory -> calculator.registerCostResolver(module.type(), factory),
                () -> logger.error("No cost-resolver available for asset-type {}, costs of its assets will not be evaluated", typeName)
            );
        }

        return calculator;
    }

    @Override
    public void close()
    {
        for (WombatModule module : this.modules.values())
            module.close();
    }

    public static final class CoreDefaults
    {
        private final Map<ServiceFamily, AssetProcessorFactory<WombatActivityResolver>> defaultActivityResolvers;
        private final Map<ServiceFamily, AssetProcessorFactory<WombatEvaluationResolver>> defaultImpactResolvers;
        private final Map<ServiceFamily, AssetProcessorFactory<WombatEvaluationResolver>> defaultCostResolvers;

        private CoreDefaults()
        {
            this.defaultActivityResolvers = new HashMap<>();
            this.defaultImpactResolvers = new HashMap<>();
            this.defaultCostResolvers = new HashMap<>();
        }

        public CoreDefaults register(
            ServiceFamily family,
            AssetProcessorFactory<WombatActivityResolver> activityResolver,
            AssetProcessorFactory<WombatEvaluationResolver> impactResolver,
            AssetProcessorFactory<WombatEvaluationResolver> costResolver
        ) {
            this.defaultActivityResolvers.put(family, activityResolver);
            this.defaultImpactResolvers.put(family, impactResolver);
            this.defaultCostResolvers.put(family, costResolver);
            return this;
        }

        public Optional<AssetProcessorFactory<WombatActivityResolver>> getActivityResolver(ServiceFamily family)
        {
            return Optional.ofNullable(this.defaultActivityResolvers.get(family));
        }

        public Optional<AssetProcessorFactory<WombatEvaluationResolver>> getImpactResolver(ServiceFamily family)
        {
            return Optional.ofNullable(this.defaultImpactResolvers.get(family));
        }

        public Optional<AssetProcessorFactory<WombatEvaluationResolver>> getCostResolver(ServiceFamily family)
        {
            return Optional.ofNullable(this.defaultCostResolvers.get(family));
        }
    }
}
