package tech.illuin.wombat.core.evaluation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.core.activity.*;
import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.activity.commons.AssetActivity;
import tech.illuin.wombat.core.activity.commons.AssetFilter;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.type.ModuleType;
import tech.illuin.wombat.core.asset.Environment;
import tech.illuin.wombat.core.context.WombatContext;
import tech.illuin.wombat.core.context.WombatContextProvider;
import tech.illuin.wombat.core.module.ProcessorFactory;
import tech.illuin.wombat.core.module.ProcessorPool;
import tech.illuin.wombat.core.module.WombatServices;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AssetEvaluator implements AutoCloseable
{
    private final WombatContextProvider contextProvider;
    private final ProcessorPool<WombatActivityResolver> activityResolvers;
    private final ProcessorPool<WombatEvaluationResolver> impactResolvers;
    private final ProcessorPool<WombatEvaluationResolver> costResolvers;
    private final ExecutorService executorService;

    private static final Logger logger = LoggerFactory.getLogger(AssetEvaluator.class);

    public AssetEvaluator(WombatContextProvider contextProvider, WombatServices services)
    {
        this(contextProvider, services, Executors.newVirtualThreadPerTaskExecutor());
    }

    public AssetEvaluator(WombatContextProvider contextProvider, WombatServices services, ExecutorService executorService)
    {
        this.activityResolvers = new ProcessorPool<>("activity-resolver", services);
        this.impactResolvers = new ProcessorPool<>("impact-resolver", services);
        this.costResolvers = new ProcessorPool<>("cost-resolver", services);
        this.contextProvider = contextProvider;
        this.executorService = executorService;
    }

    public AssetEvaluator registerActivityResolver(ModuleType type, ProcessorFactory<WombatActivityResolver> factory)
    {
        this.activityResolvers.register(type, factory);
        return this;
    }

    public AssetEvaluator registerCostResolver(ModuleType type, ProcessorFactory<WombatEvaluationResolver> factory)
    {
        this.costResolvers.register(type, factory);
        return this;
    }

    public AssetEvaluator registerImpactResolver(ModuleType type, ProcessorFactory<WombatEvaluationResolver> factory)
    {
        this.impactResolvers.register(type, factory);
        return this;
    }

    public <E extends AssetEvaluation> List<E> evaluate(TimeRange range, AssetFilter filter, Class<E> evalType) throws WombatEvaluationException
    {
        return this.evaluate(range, filter).stream()
            .filter(evalType::isInstance)
            .map(evalType::cast)
            .toList();
    }

    public List<AssetEvaluation> evaluate(TimeRange range, AssetFilter filter) throws WombatEvaluationException
    {
        WombatContext context = this.contextProvider.provide();
        this.retain(context.assets().stream()
            .map(Asset::identity)
            .collect(Collectors.toSet())
        );

        Queue<AssetEvaluation> outputs = new ConcurrentLinkedQueue<>();
        for (Environment environment : context.environments())
        {
            List<CompletableFuture<Void>> futures = environment.assets().stream()
                .filter(asset -> filter == null || filter.accepts(environment.id(), asset.identity().id()))
                .map(asset -> CompletableFuture.runAsync(() -> this.runEvaluation(asset, range, filter, outputs), this.executorService))
                .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
        return List.copyOf(outputs);
    }

    /**
     * Discards the resolvers of the asset identified by {@code identity}, so they are built again on the next call.
     * <p>
     * Meant to be called when an asset's settings changed: its resolvers are otherwise kept for as long as the asset is
     * in the context.
     */
    public void refresh(AssetIdentity identity)
    {
        this.activityResolvers.invalidate(identity);
        this.impactResolvers.invalidate(identity);
        this.costResolvers.invalidate(identity);
    }

    private void retain(Set<AssetIdentity> identities)
    {
        this.activityResolvers.retain(identities);
        this.impactResolvers.retain(identities);
        this.costResolvers.retain(identities);
    }

    private void runEvaluation(Asset asset, TimeRange range, AssetFilter filter, Queue<AssetEvaluation> outputs)
    {
        try {
            Optional<AssetActivity> activity = this.computeActivity(asset, range, filter);
            if (activity.isEmpty())
                return;

            List<CompletableFuture<Void>> futures = activity.get().activity().stream()
                .flatMap(activityData -> Stream.of(
                    this.computeImpactAsync(asset, activityData).thenAccept(result -> result.ifPresent(outputs::add)),
                    this.computeCostAsync(asset, activityData).thenAccept(result -> result.ifPresent(outputs::add))
                ))
                .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
        catch (WombatActivityException e) {
           logger.error("An error occurred while evaluating asset {}", asset.identity(), e);
        }
    }

    public Optional<AssetActivity> computeActivity(Asset asset, TimeRange range, AssetFilter filter) throws WombatActivityException
    {
        Optional<WombatActivityResolver> resolver = this.activityResolvers.get(asset);

        if (resolver.isEmpty())
        {
            logger.debug("Skipping asset {}: no activity resolver accepts asset-type {}", asset.identity(), asset.type());
            return Optional.empty();
        }
        return resolver.get().resolve(asset, range, filter);
    }

    public Optional<AssetEvaluation> computeImpact(Asset asset, ActivityData activity) throws WombatEvaluationException
    {
        Optional<WombatEvaluationResolver> resolver = this.impactResolvers.get(asset);

        if (resolver.isEmpty() && !this.impactResolvers.supports(asset))
        {
            logger.debug("Skipping impact of asset {}: no impact resolver registered for asset-type {}", asset.identity().id(), asset.type());
            return Optional.empty();
        }
        if (resolver.isEmpty())
        {
            logger.warn(
                "Skipping impact of asset {}: no impact resolver accepts asset-type {} with profile {}",
                asset.identity().id(),
                asset.type().name(),
                asset.profile().getClass().getName()
            );
            return Optional.empty();
        }
        return Optional.of(resolver.get().resolve(asset, activity));
    }

    public CompletableFuture<Optional<AssetEvaluation>> computeImpactAsync(Asset asset, ActivityData activity)
    {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return this.computeImpact(asset, activity);
            }
            catch (WombatEvaluationException e) {
                logger.error("An error occurred while evaluating asset {}", asset.identity(), e);
                return Optional.empty();
            }
        }, this.executorService);
    }

    public Optional<AssetEvaluation> computeCost(Asset asset, ActivityData activity) throws WombatEvaluationException
    {
        Optional<WombatEvaluationResolver> resolver = this.costResolvers.get(asset);

        /* A warn although cost is optional: it was asked for, and no module can price the asset */
        if (resolver.isEmpty())
        {
            logger.warn("Skipping cost of asset {}: no cost resolver accepts asset-type {}", asset.identity().id(), asset.type().name());
            return Optional.empty();
        }
        return Optional.of(resolver.get().resolve(asset, activity));
    }

    public CompletableFuture<Optional<AssetEvaluation>> computeCostAsync(Asset asset, ActivityData activity)
    {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return this.computeCost(asset, activity);
            }
            catch (WombatEvaluationException e) {
                logger.error("An error occurred while evaluating asset {}", asset.identity(), e);
                return Optional.empty();
            }
        }, this.executorService);
    }

    @Override
    public void close()
    {
        this.executorService.close();
        this.activityResolvers.close();
        this.impactResolvers.close();
        this.costResolvers.close();
    }
}
