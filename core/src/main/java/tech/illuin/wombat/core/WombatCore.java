package tech.illuin.wombat.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.asset.type.FamilyType;
import tech.illuin.wombat.core.asset.type.ModuleType;
import tech.illuin.wombat.core.context.WombatContextProvider;
import tech.illuin.wombat.core.evaluation.AssetEvaluator;
import tech.illuin.wombat.core.module.ProcessorFactory;
import tech.illuin.wombat.core.module.WombatModule;
import tech.illuin.wombat.core.module.WombatServices;
import tech.illuin.wombat.core.source.AssetMonitor;
import tech.illuin.wombat.core.source.persistence.WombatMetricPersister;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public final class WombatCore implements AutoCloseable
{
    /* Ordered: the family modules of a family are tried in registration order */
    private final Map<String, WombatModule> modules;
    private final WombatContextProvider contextProvider;
    private final WombatServices services;
    private final WombatMetricPersister persister;

    private static final Logger logger = LoggerFactory.getLogger(WombatCore.class);

    public WombatCore(
        WombatContextProvider contextProvider,
        WombatServices services,
        WombatMetricPersister persister,
        Collection<WombatModule> modules
    ) {
        this.contextProvider = contextProvider;
        this.services = services;
        this.persister = persister;
        this.modules = new LinkedHashMap<>();
        for (WombatModule module : modules)
            this.register(module);
        this.reportMissingFeatures();
    }

    private void register(WombatModule module)
    {
        String name = module.type().name();
        if (this.modules.containsKey(name))
            throw new IllegalArgumentException("A module was already registered under " + name + " with type " + this.modules.get(name).getClass().getSimpleName());

        switch (module.type())
        {
            case AssetType type when module.assetClass().isEmpty() ->
                throw new IllegalArgumentException("Module " + type.name() + " defines an asset-type but no asset class");
            case FamilyType type when module.assetClass().isPresent() ->
                throw new IllegalArgumentException("Module " + type.name() + " serves family " + type.family() + " but declares an asset class");
            default -> {}
        }

        this.modules.put(name, module);
        logger.info("Registered module {} under {}", module.getClass().getSimpleName(), name);
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
                () -> logger.debug("Skipping monitor registration for module {}", module.type().name())
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
            module.activityResolver().ifPresent(factory -> calculator.registerActivityResolver(module.type(), factory));
            module.impactResolver().ifPresent(factory -> calculator.registerImpactResolver(module.type(), factory));
            module.costResolver().ifPresent(factory -> calculator.registerCostResolver(module.type(), factory));
        }

        return calculator;
    }

    @Override
    public void close()
    {
        for (WombatModule module : this.modules.values())
            module.close();
    }

    /* Once per core, as a host may create many evaluators out of the same modules */
    private void reportMissingFeatures()
    {
        for (WombatModule module : this.modules.values())
        {
            if (!(module.type() instanceof AssetType type))
                continue;

            if (!this.provides(type, WombatModule::activityResolver))
                logger.error("No activity-resolver available for asset-type {}, its assets will not be evaluated", type.name());
            if (!this.provides(type, WombatModule::impactResolver))
                logger.error("No impact-resolver available for asset-type {}, impacts of its assets will not be evaluated", type.name());
            /* Cost is optional, and no built-in module prices anything yet */
            if (!this.provides(type, WombatModule::costResolver))
                logger.info("No cost-resolver available for asset-type {}, costs of its assets will not be evaluated", type.name());
        }
    }

    /**
     * @return whether the module of {@code type}, or a family module of its family, provides the feature
     */
    private boolean provides(AssetType type, Function<WombatModule, Optional<? extends ProcessorFactory<?>>> feature)
    {
        return this.modules.values().stream()
            .filter(module -> isServedBy(type, module.type()))
            .anyMatch(module -> feature.apply(module).isPresent());
    }

    private static boolean isServedBy(AssetType type, ModuleType moduleType)
    {
        return switch (moduleType)
        {
            case AssetType own -> own.name().equals(type.name());
            case FamilyType family -> family.family() == type.family();
        };
    }
}
