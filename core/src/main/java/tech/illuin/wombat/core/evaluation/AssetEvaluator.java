package tech.illuin.wombat.core.evaluation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.core.activity.*;
import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.activity.commons.AssetActivity;
import tech.illuin.wombat.core.activity.commons.AssetFilter;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.asset.Environment;
import tech.illuin.wombat.core.context.WombatContextProvider;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AssetEvaluator implements AutoCloseable
{
    private final WombatContextProvider contextProvider;
    private final Map<AssetType, WombatActivityResolver> activityResolvers;
    private final Map<AssetType, WombatEvaluationResolver> impactResolvers;
    private final Map<AssetType, WombatEvaluationResolver> costResolvers;
    private final ExecutorService executorService;

    private static final Logger logger = LoggerFactory.getLogger(AssetEvaluator.class);

    public AssetEvaluator(WombatContextProvider contextProvider)
    {
        this(contextProvider, Executors.newVirtualThreadPerTaskExecutor());
    }

    public AssetEvaluator(WombatContextProvider contextProvider, ExecutorService executorService)
    {
        this.activityResolvers = new HashMap<>();
        this.impactResolvers = new HashMap<>();
        this.costResolvers = new HashMap<>();
        this.contextProvider = contextProvider;
        this.executorService = executorService;
    }

    public AssetEvaluator registerActivityResolver(AssetType type, WombatActivityResolver resolver)
    {
        this.activityResolvers.put(type, resolver);
        return this;
    }

    public AssetEvaluator registerCostResolver(AssetType type, WombatEvaluationResolver resolver)
    {
        this.costResolvers.put(type, resolver);
        return this;
    }

    public AssetEvaluator registerImpactResolver(AssetType type, WombatEvaluationResolver resolver)
    {
        this.impactResolvers.put(type, resolver);
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
        Queue<AssetEvaluation> outputs = new ConcurrentLinkedQueue<>();
        for (Environment environment : this.contextProvider.provide().environments())
        {
            List<CompletableFuture<Void>> futures = environment.assets().stream()
                .filter(asset -> filter == null || filter.accepts(environment.id(), asset.identity().id()))
                .map(asset -> CompletableFuture.runAsync(() -> this.runEvaluation(asset, range, filter, outputs)))
                .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
        return List.copyOf(outputs);
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
        WombatActivityResolver resolver = this.activityResolvers.get(asset.type());

        if (resolver == null || !resolver.accept(asset))
        {
            logger.debug("Skipping asset {}: no activity resolver accepts asset-type {}", asset.identity(), asset.type());
            return Optional.empty();
        }
        return resolver.resolve(asset, range, filter);
    }

    public Optional<AssetEvaluation> computeImpact(Asset asset, ActivityData activity) throws WombatEvaluationException
    {
        WombatEvaluationResolver resolver = this.impactResolvers.get(asset.type());

        if (resolver == null)
        {
            logger.debug("Skipping impact of asset {}: no impact resolver registered for asset-type {}", asset.identity().id(), asset.type());
            return Optional.empty();
        }
        if (!resolver.accept(asset))
        {
            logger.warn(
                "Skipping impact of asset {}: impact resolver {} rejects asset-type {} with profile {}",
                asset.identity().id(),
                resolver.getClass().getSimpleName(),
                asset.type().name(),
                asset.profile().getClass().getName()
            );
            return Optional.empty();
        }
        return Optional.of(resolver.resolve(asset, activity));
    }

    public Optional<AssetEvaluation> computeCost(Asset asset, ActivityData activity) throws WombatEvaluationException
    {
        WombatEvaluationResolver resolver = this.costResolvers.get(asset.type());

        if (resolver == null || !resolver.accept(asset))
        {
            logger.debug("Skipping cost of asset {}: no cost resolver accepts asset-type {}", asset.identity().id(), asset.type());
            return Optional.empty();
        }
        return Optional.of(resolver.resolve(asset, activity));
    }

    @Override
    public void close()
    {
        this.executorService.close();
    }
}
