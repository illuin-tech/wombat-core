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
import tech.illuin.wombat.core.asset.profile.AssetProfile;
import tech.illuin.wombat.core.asset.profile.inference.InferredProfile;
import tech.illuin.wombat.core.asset.type.ModuleType;
import tech.illuin.wombat.core.asset.Environment;
import tech.illuin.wombat.core.context.WombatContext;
import tech.illuin.wombat.core.context.WombatContextProvider;
import tech.illuin.wombat.core.evaluation.impact.commons.AssetImpact;
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
    private final ProcessorPool<WombatProfileResolver> profileResolvers;
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
        this.profileResolvers = new ProcessorPool<>("profile-resolver", services);
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

    public AssetEvaluator registerProfileResolver(ModuleType type, ProcessorFactory<WombatProfileResolver> factory)
    {
        this.profileResolvers.register(type, factory);
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
        return this.report(range, filter).evaluations();
    }

    /**
     * Evaluates the assets as {@link #evaluate(TimeRange, AssetFilter)} does, also telling which could not be and why.
     */
    public EvaluationReport report(TimeRange range, AssetFilter filter) throws WombatEvaluationException
    {
        WombatContext context = this.contextProvider.provide();
        this.retain(context.assets().stream()
            .map(Asset::identity)
            .collect(Collectors.toSet())
        );

        Queue<AssetEvaluation> outputs = new ConcurrentLinkedQueue<>();
        // A set, an asset failing alike for its impact and its cost, or for each of its activities
        Set<AssetEvaluationFailure> failures = ConcurrentHashMap.newKeySet();
        for (Environment environment : context.environments())
        {
            List<CompletableFuture<Void>> futures = environment.assets().stream()
                .filter(asset -> filter == null || filter.accepts(environment.id(), asset.identity().id()))
                .map(asset -> CompletableFuture.runAsync(() -> this.runEvaluation(asset, range, filter, outputs, failures), this.executorService))
                .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
        return new EvaluationReport(
            List.copyOf(outputs),
            failures.stream()
                .sorted(Comparator.comparing((AssetEvaluationFailure failure) -> failure.assetIdentity().id()).thenComparing(AssetEvaluationFailure::reason))
                .toList()
        );
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
        this.profileResolvers.invalidate(identity);
    }

    private void retain(Set<AssetIdentity> identities)
    {
        this.activityResolvers.retain(identities);
        this.impactResolvers.retain(identities);
        this.costResolvers.retain(identities);
        this.profileResolvers.retain(identities);
    }

    private void runEvaluation(Asset asset, TimeRange range, AssetFilter filter, Queue<AssetEvaluation> outputs, Set<AssetEvaluationFailure> failures)
    {
        try {
            Optional<AssetActivity> activity = this.computeActivity(asset, range, filter);
            if (activity.isEmpty())
                return;

            List<CompletableFuture<Void>> futures = activity.get().activity().stream()
                .flatMap(activityData -> Stream.of(
                    this.runAsync(asset, () -> this.computeImpact(asset, activityData), outputs, failures),
                    this.runAsync(asset, () -> this.computeCost(asset, activityData), outputs, failures)
                ))
                .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
        catch (WombatActivityException e) {
           logger.error("An error occurred while evaluating asset {}", asset.identity(), e);
           failures.add(new AssetEvaluationFailure(asset.identity(), e.getMessage()));
        }
    }

    /**
     * Unchecked exceptions are caught too, as they would otherwise fail every asset's evaluation instead of this one's.
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    private CompletableFuture<Void> runAsync(Asset asset, EvaluationCall call, Queue<AssetEvaluation> outputs, Set<AssetEvaluationFailure> failures)
    {
        return CompletableFuture.runAsync(() -> {
            try {
                call.run().ifPresent(outputs::add);
            }
            catch (WombatEvaluationException | RuntimeException e) {
                logger.error("An error occurred while evaluating asset {}", asset.identity(), e);
                failures.add(new AssetEvaluationFailure(asset.identity(), e.getMessage()));
            }
        }, this.executorService);
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

    public Optional<AssetEvaluation> computeImpact(Asset declared, ActivityData activity) throws WombatEvaluationException
    {
        Optional<AssetProfile> resolved = this.resolveProfile(declared);
        Optional<AssetEvaluation> evaluation = this.computeEvaluation("impact", this.impactResolvers, withProfile(declared, resolved), activity);
        if (evaluation.orElse(null) instanceof AssetImpact impact && resolved.orElse(null) instanceof InferredProfile inferred)
            return Optional.of(impact.withProfileOrigin(inferred.origin(), inferred.warnings()));
        return evaluation;
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

    public Optional<AssetEvaluation> computeCost(Asset declared, ActivityData activity) throws WombatEvaluationException
    {
        return this.computeEvaluation("cost", this.costResolvers, withProfile(declared, this.resolveProfile(declared)), activity);
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

    /**
     * A failing factory throws unchecked exceptions, which would fail every asset's evaluation instead of this one's.
     *
     * @return the profile the asset's profile resolver resolves, empty when none accepts it
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    private Optional<AssetProfile> resolveProfile(Asset asset) throws WombatEvaluationException
    {
        Optional<WombatProfileResolver> resolver;
        try {
            resolver = this.profileResolvers.get(asset);
        }
        catch (RuntimeException e) {
            throw new WombatEvaluationException("Cannot build the profile resolver of asset " + asset.identity().id() + ": " + e.getMessage(), e);
        }
        if (resolver.isEmpty())
            return Optional.empty();
        return Optional.of(resolver.get().resolve(asset));
    }

    /**
     * @return {@code asset} with the {@code resolved} profile, or as is when none was
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    private static Asset withProfile(Asset asset, Optional<AssetProfile> resolved) throws WombatEvaluationException
    {
        if (resolved.isEmpty() || resolved.get() == asset.profile())
            return asset;
        try {
            return asset.withProfile(resolved.get());
        }
        catch (RuntimeException e) {
            throw new WombatEvaluationException("Cannot evaluate asset " + asset.identity().id() + " with its resolved profile: " + e.getMessage(), e);
        }
    }

    @Override
    public void close()
    {
        this.executorService.close();
        this.activityResolvers.close();
        this.impactResolvers.close();
        this.costResolvers.close();
        this.profileResolvers.close();
    }

    private Optional<AssetEvaluation> computeEvaluation(
        String kind,
        ProcessorPool<WombatEvaluationResolver> resolvers,
        Asset asset,
        ActivityData activity
    ) throws WombatEvaluationException
    {
        Optional<WombatEvaluationResolver> resolver = resolvers.get(asset);

        if (resolver.isEmpty() && !resolvers.supports(asset))
        {
            logger.debug("Skipping {} of asset {}: no {}-resolver registered for asset-type {}", kind, asset.identity().id(), kind, asset.type().name());
            return Optional.empty();
        }
        if (resolver.isEmpty())
        {
            logger.warn(
                "Skipping {} of asset {}: no {}-resolver accepts asset-type {} with profile {}",
                kind,
                asset.identity().id(),
                kind,
                asset.type().name(),
                asset.profile().getClass().getName()
            );
            return Optional.empty();
        }
        return Optional.of(resolver.get().resolve(asset, activity));
    }

    @FunctionalInterface
    private interface EvaluationCall
    {
        Optional<AssetEvaluation> run() throws WombatEvaluationException;
    }
}
