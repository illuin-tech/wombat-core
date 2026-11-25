package tech.illuin.wombat.core.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.Environment;
import tech.illuin.wombat.core.asset.type.ModuleType;
import tech.illuin.wombat.core.context.WombatContext;
import tech.illuin.wombat.core.context.WombatContextProvider;
import tech.illuin.wombat.core.module.ProcessorFactory;
import tech.illuin.wombat.core.module.ProcessorPool;
import tech.illuin.wombat.core.module.WombatServices;
import tech.illuin.wombat.core.source.data.MetricData;
import tech.illuin.wombat.core.source.persistence.WombatMetricPersister;
import tech.illuin.wombat.core.source.persistence.WombatPersistenceException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class AssetMonitor implements AutoCloseable
{
    private final WombatContextProvider contextProvider;
    private final ProcessorPool<WombatSource> sources;
    private final WombatMetricPersister persister;
    private final ExecutorService executorService;
    private final Map<AssetIdentity, Semaphore> semaphores;
    private final Map<AssetIdentity, Instant> lastActivation;

    private static final Logger logger = LoggerFactory.getLogger(AssetMonitor.class);

    public AssetMonitor(WombatContextProvider contextProvider, WombatServices services, WombatMetricPersister persister)
    {
        this(contextProvider, services, persister, Executors.newVirtualThreadPerTaskExecutor());
    }

    public AssetMonitor(WombatContextProvider contextProvider, WombatServices services, WombatMetricPersister persister, ExecutorService executorService)
    {
        this.sources = new ProcessorPool<>("source", services);
        this.contextProvider = contextProvider;
        this.persister = persister;
        this.executorService = executorService;
        this.semaphores = new ConcurrentHashMap<>();
        this.lastActivation = new ConcurrentHashMap<>();
    }

    public AssetMonitor register(ModuleType type, ProcessorFactory<WombatSource> factory)
    {
        if (this.sources.registered(type))
        {
            logger.trace("Source for {} was already registered", type.name());
            return this;
        }
        this.sources.register(type, factory);
        return this;
    }

    public void trigger()
    {
        Instant heartbeat = Instant.now();
        WombatContext context = this.contextProvider.provide();

        this.prune(context.assets().stream()
            .map(Asset::identity)
            .collect(Collectors.toSet())
        );

        for (Environment environment : context.environments())
        {
            environment.assets().stream()
                .filter(asset -> asset instanceof Monitorable)
                .filter(this.sources::supports)
                .forEach(asset -> {
                    Instant lastActivated = this.lastActivation.computeIfAbsent(asset.identity(), id -> Instant.MIN);

                    Monitorable monitorable = (Monitorable) asset;
                    Period period = monitorable.period() != null ? monitorable.period() : Period.ZERO;
                    if (!lastActivated.plus(period.asDuration()).isBefore(heartbeat))
                    {
                        logger.trace("Asset {} not ready to be triggered", asset);
                        return;
                    }
                    this.lastActivation.put(asset.identity(), heartbeat);

                    Semaphore semaphore = this.semaphore(asset.identity());
                    this.executorService.execute(() -> {
                        semaphore.acquireUninterruptibly();
                        try {
                            this.runSource(
                                heartbeat,
                                lastActivated != Instant.MIN ? lastActivated : null,
                                asset
                            );
                        }
                        finally {
                            semaphore.release();
                        }
                    });
                });
        }
    }

    /**
     * Discards the source of the asset identified by {@code identity}, so it is built again on the next trigger.
     * <p>
     * Meant to be called when an asset's settings changed: its source is otherwise kept for as long as the asset is in
     * the context. Waits for an ongoing run of that asset to complete before discarding its source.
     */
    public void refresh(AssetIdentity identity)
    {
        Semaphore semaphore = this.semaphore(identity);
        semaphore.acquireUninterruptibly();
        try {
            this.sources.invalidate(identity);
        }
        finally {
            semaphore.release();
        }
    }

    private void runSource(Instant heartbeat, Instant lastActivation, Asset asset)
    {
        Optional<WombatSource> source = this.sources.get(asset);
        if (source.isEmpty())
        {
            logger.trace("Skipping asset {}: no source accepts asset-type {}", asset.identity().id(), asset.type().name());
            return;
        }

        try {
            logger.debug("Monitor triggered for asset {}", asset.identity().id());
            List<MetricData> data = source.get().source(heartbeat, lastActivation, asset);
            this.persister.persist(asset.identity(), asset.type(), data);
        }
        catch (WombatSourceException | WombatPersistenceException e) {
            logger.error("Monitoring failed for asset {}: {}", asset.identity().id(), e.getMessage(), e);
        }
    }

    /**
     * Forgets everything kept for assets that are no longer in the context.
     */
    private void prune(Set<AssetIdentity> identities)
    {
        this.lastActivation.keySet().retainAll(identities);

        List<AssetIdentity> removed = this.semaphores.keySet().stream()
            .filter(id -> !identities.contains(id))
            .toList();

        // Discarded from the executor, so that a run still in progress for a removed asset does not hold up the trigger.
        for (AssetIdentity identity : removed)
        {
            this.executorService.execute(() -> {
                this.refresh(identity);
                this.semaphores.remove(identity);
            });
        }
    }

    private Semaphore semaphore(AssetIdentity identity)
    {
        return this.semaphores.computeIfAbsent(identity, id -> new Semaphore(1, true));
    }

    @Override
    public void close()
    {
        this.executorService.close();
        this.sources.close();
    }
}
