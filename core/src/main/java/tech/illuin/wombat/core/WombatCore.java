package tech.illuin.wombat.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.asset.AssetType;
import tech.illuin.wombat.core.asset.ServiceFamily;
import tech.illuin.wombat.core.context.WombatContext;
import tech.illuin.wombat.core.context.WombatContextProvider;
import tech.illuin.wombat.core.evaluation.WombatEvaluationResolver;
import tech.illuin.wombat.core.evaluation.AssetEvaluator;
import tech.illuin.wombat.core.module.WombatModule;
import tech.illuin.wombat.core.source.AssetMonitor;
import tech.illuin.wombat.core.source.WombatSource;
import tech.illuin.wombat.core.source.persistence.WombatMetricPersister;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class WombatCore implements AutoCloseable
{
    private final Map<AssetType, WombatModule> modules;
    private final WombatContextProvider contextProvider;
    private final WombatMetricPersister persister;
    private final CoreDefaults defaults;

    private static final Logger logger = LoggerFactory.getLogger(WombatCore.class);

    public WombatCore(
        WombatContextProvider contextProvider,
        WombatMetricPersister persister,
        Collection<WombatModule> modules,
        Consumer<CoreDefaults> defaultsAdjuster
    ) {
        this.contextProvider = contextProvider;
        this.persister = persister;
        this.modules = new HashMap<>();
        this.defaults = new CoreDefaults();
        defaultsAdjuster.accept(this.defaults);
        for (WombatModule module : modules)
            this.register(module);
    }

    private void register(WombatModule module)
    {
        if (this.modules.containsKey(module.type()))
            throw new IllegalArgumentException("A module was already registered for asset-type " + module.type() + " with type " + this.modules.get(module.type()).getClass().getSimpleName());

        this.modules.put(module.type(), module);
        logger.info("Registered module {} for asset-type {}", module.getClass().getSimpleName(), module.type());
    }

    public AssetMonitor createMonitor()
    {
        return this.createMonitor(Executors.newVirtualThreadPerTaskExecutor());
    }

    public AssetMonitor createMonitor(ExecutorService executorService)
    {
        logger.info("Creating wombat source-monitor out of {} registered modules", this.modules.size());
        AssetMonitor monitor = new AssetMonitor(this.contextProvider, this.persister, executorService);
        WombatContext context = this.contextProvider.provide();
        context.assets().stream()
            .filter(asset -> this.modules.containsKey(asset.type()))
            .forEach(asset -> {
                WombatModule module = this.modules.get(asset.type());
                WombatContext scoped = context.scope(asset.type());

                Optional<WombatSource> source = module.createSource(scoped);
                if (source.isEmpty())
                    logger.debug("Skipping monitor registration for asset-type {}", asset.type());
                else {
                    monitor.register(asset, source.get());
                    logger.debug("Registered wombat source {} for asset-type {}", source.getClass().getSimpleName(), asset.type());
                }
            });
        return monitor;
    }

    public AssetEvaluator createEvaluator()
    {
        logger.info("Creating wombat impact-calculator out of {} registered modules", this.modules.size());
        AssetEvaluator calculator = new AssetEvaluator(this.contextProvider);
        WombatContext context = this.contextProvider.provide();

        for (WombatModule module : this.modules.values())
        {
            ServiceFamily family = module.type().family();

            WombatActivityResolver activityResolver = module.createActivityResolver().or(() -> this.defaults.getActivityResolver(family)).orElseThrow();
            calculator.registerActivityResolver(module.type(), activityResolver);
            logger.debug("Registered wombat activity-resolver {} for asset-type {}", activityResolver.getClass().getSimpleName(), module.type());

            WombatEvaluationResolver impactResolver = module.createImpactResolver().or(() -> this.defaults.getImpactResolver(family)).orElseThrow();
            calculator.registerImpactResolver(module.type(), impactResolver);
            logger.debug("Registered wombat impact-resolver {} for asset-type {}", impactResolver.getClass().getSimpleName(), module.type());

            WombatEvaluationResolver costResolver = module.createCostResolver().or(() -> this.defaults.getCostResolver(family)).orElseThrow();
            calculator.registerCostResolver(module.type(), costResolver);
            logger.debug("Registered wombat cost-resolver {} for asset-type {}", impactResolver.getClass().getSimpleName(), module.type());
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
        private final Map<ServiceFamily, WombatActivityResolver> defaultActivityResolvers;
        private final Map<ServiceFamily, WombatEvaluationResolver> defaultImpactResolvers;
        private final Map<ServiceFamily, WombatEvaluationResolver> defaultCostResolvers;

        private CoreDefaults()
        {
            this.defaultActivityResolvers = new HashMap<>();
            this.defaultImpactResolvers = new HashMap<>();
            this.defaultCostResolvers = new HashMap<>();
        }

        public CoreDefaults register(
            ServiceFamily family,
            WombatActivityResolver activityResolver,
            WombatEvaluationResolver impactResolver,
            WombatEvaluationResolver costResolver
        ) {
            this.defaultActivityResolvers.put(family, activityResolver);
            this.defaultImpactResolvers.put(family, impactResolver);
            this.defaultCostResolvers.put(family, costResolver);
            return this;
        }

        public Optional<WombatActivityResolver> getActivityResolver(ServiceFamily family)
        {
            return Optional.ofNullable(this.defaultActivityResolvers.get(family));
        }

        public Optional<WombatEvaluationResolver> getImpactResolver(ServiceFamily family)
        {
            return Optional.ofNullable(this.defaultImpactResolvers.get(family));
        }

        public Optional<WombatEvaluationResolver> getCostResolver(ServiceFamily family)
        {
            return Optional.ofNullable(this.defaultCostResolvers.get(family));
        }
    }
}
