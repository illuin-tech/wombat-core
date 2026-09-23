package tech.illuin.wombat.core.evaluation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.core.activity.*;
import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.activity.commons.AssetFilter;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetType;
import tech.illuin.wombat.core.asset.Environment;
import tech.illuin.wombat.core.context.WombatContextProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AssetEvaluator implements AutoCloseable
{
    private final WombatContextProvider contextProvider;
    private final Map<AssetType, WombatActivityResolver> activityResolvers;
    private final Map<AssetType, WombatEvaluationResolver> impactResolvers;
    private final Map<AssetType, WombatEvaluationResolver> costResolvers;

    private static final Logger logger = LoggerFactory.getLogger(AssetEvaluator.class);

    public AssetEvaluator(WombatContextProvider contextProvider)
    {
        this.activityResolvers = new HashMap<>();
        this.impactResolvers = new HashMap<>();
        this.costResolvers = new HashMap<>();
        this.contextProvider = contextProvider;
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
        try {
            List<AssetEvaluation> outputs = new ArrayList<>();
            for (Environment environment : this.contextProvider.provide().environments())
            {
                for (Asset asset : environment.assets())
                {
                    if (filter != null && !filter.accepts(environment.id(), asset.id()))
                        continue;

                    Optional<ActivityData> activity = this.computeActivity(asset, range, filter);
                    if (activity.isEmpty())
                        continue;

                    this.computeImpact(asset, activity.get()).ifPresent(outputs::add);
                    this.computeCost(asset, activity.get()).ifPresent(outputs::add);
                }
            }
            return outputs;
        }
        catch (WombatActivityException e) {
            throw new WombatEvaluationException("An error occurred during evaluation", e);
        }
    }

    public Optional<ActivityData> computeActivity(Asset asset, TimeRange range, AssetFilter filter) throws WombatActivityException
    {
        WombatActivityResolver resolver = this.activityResolvers.get(asset.type());

        if (resolver == null || !resolver.accept(asset))
        {
            logger.debug("Skipping asset {}: no activity resolver accepts asset-type {}", asset.id(), asset.type());
            return Optional.empty();
        }
        return Optional.of(resolver.resolve(asset, range, filter));
    }

    public Optional<AssetEvaluation> computeImpact(Asset asset, ActivityData activity) throws WombatEvaluationException
    {
        WombatEvaluationResolver resolver = this.impactResolvers.get(asset.type());

        if (resolver == null || !resolver.accept(asset))
        {
            logger.debug("Skipping impact of asset {}: no impact resolver accepts asset-type {}", asset.id(), asset.type());
            return Optional.empty();
        }
        return Optional.of(resolver.resolve(asset, activity));
    }

    public Optional<AssetEvaluation> computeCost(Asset asset, ActivityData activity) throws WombatEvaluationException
    {
        WombatEvaluationResolver resolver = this.costResolvers.get(asset.type());

        if (resolver == null || !resolver.accept(asset))
        {
            logger.debug("Skipping cost of asset {}: no cost resolver accepts asset-type {}", asset.id(), asset.type());
            return Optional.empty();
        }
        return Optional.of(resolver.resolve(asset, activity));
    }

    @Override
    public void close() throws Exception {}
}
