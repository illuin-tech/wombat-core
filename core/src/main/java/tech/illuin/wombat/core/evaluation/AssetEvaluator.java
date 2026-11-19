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
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.asset.Environment;
import tech.illuin.wombat.core.context.WombatContext;
import tech.illuin.wombat.core.context.WombatContextProvider;
import tech.illuin.wombat.core.module.ProcessorFactory;
import tech.illuin.wombat.core.module.ProcessorPool;
import tech.illuin.wombat.core.module.WombatServices;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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

    public AssetEvaluator registerActivityResolver(AssetType type, ProcessorFactory<WombatActivityResolver> factory)
    {
        this.activityResolvers.register(type, factory);
        return this;
    }

    public AssetEvaluator registerCostResolver(AssetType type, ProcessorFactory<WombatEvaluationResolver> factory)
    {
        this.costResolvers.register(type, factory);
        return this;
    }

    public AssetEvaluator registerImpactResolver(AssetType type, ProcessorFactory<WombatEvaluationResolver> factory)
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

            for (ActivityData activityData : activity.get().activity())
            {
                this.computeImpact(asset, activityData).ifPresent(outputs::add);
                this.computeCost(asset, activityData).ifPresent(outputs::add);
            }
        }
        catch (WombatActivityException | WombatEvaluationException e) {
           logger.error("An error occurred while evaluating asset {}", asset.identity(), e);
        }
    }

    public Optional<AssetActivity> computeActivity(Asset asset, TimeRange range, AssetFilter filter) throws WombatActivityException
    {
        Optional<WombatActivityResolver> resolver = this.activityResolvers.get(asset);

        if (resolver.isEmpty() || !resolver.get().accept(asset))
        {
            logger.debug("Skipping asset {}: no activity resolver accepts asset-type {}", asset.identity(), asset.type());
            return Optional.empty();
        }
        return resolver.get().resolve(asset, range, filter);
    }

    public Optional<AssetEvaluation> computeImpact(Asset asset, ActivityData activity) throws WombatEvaluationException
    {
        Optional<WombatEvaluationResolver> resolver = this.impactResolvers.get(asset);

        if (resolver.isEmpty())
        {
            logger.debug("Skipping impact of asset {}: no impact resolver registered for asset-type {}", asset.identity().id(), asset.type());
            return Optional.empty();
        }
        if (!resolver.get().accept(asset))
        {
            logger.warn(
                "Skipping impact of asset {}: impact resolver {} rejects asset-type {} with profile {}",
                asset.identity().id(),
                resolver.get().getClass().getSimpleName(),
                asset.type().name(),
                asset.profile().getClass().getName()
            );
            return Optional.empty();
        }
        return Optional.of(resolver.get().resolve(asset, activity));
    }

    public Optional<AssetEvaluation> computeCost(Asset asset, ActivityData activity) throws WombatEvaluationException
    {
        Optional<WombatEvaluationResolver> resolver = this.costResolvers.get(asset);

        if (resolver.isEmpty() || !resolver.get().accept(asset))
        {
            logger.debug("Skipping cost of asset {}: no cost resolver accepts asset-type {}", asset.identity().id(), asset.type());
            return Optional.empty();
        }
        return Optional.of(resolver.get().resolve(asset, activity));
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
