package tech.illuin.wombat.core.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.Environment;
import tech.illuin.wombat.core.context.WombatContextProvider;
import tech.illuin.wombat.core.source.data.MetricData;
import tech.illuin.wombat.core.source.persistence.WombatMetricPersister;
import tech.illuin.wombat.core.source.persistence.WombatPersistenceException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class AssetMonitor implements AutoCloseable
{
    private final WombatContextProvider contextProvider;
    private final Map<String, WombatSource> sources;
    private final WombatMetricPersister persister;
    private final ExecutorService executorService;
    private final Map<WombatSource, Semaphore> semaphores;
    private final Map<WombatSource, Instant> lastActivation;

    private static final Logger logger = LoggerFactory.getLogger(AssetMonitor.class);

    public AssetMonitor(WombatContextProvider contextProvider, WombatMetricPersister persister)
    {
        this(contextProvider, persister, Executors.newVirtualThreadPerTaskExecutor());
    }

    public AssetMonitor(WombatContextProvider contextProvider, WombatMetricPersister persister, ExecutorService executorService)
    {
        this.sources = new ConcurrentHashMap<>();
        this.contextProvider = contextProvider;
        this.persister = persister;
        this.executorService = executorService;
        this.semaphores = new ConcurrentHashMap<>();
        this.lastActivation = new ConcurrentHashMap<>();
    }

    public AssetMonitor register(Asset asset, WombatSource source)
    {
        if (this.sources.containsKey(asset.type().name()))
        {
            logger.trace("Source for {} was already registered with type {}", asset.type(), this.sources.get(asset.type().name()).getClass().getSimpleName());
            return this;
        }
        this.sources.put(asset.type().name(), source);
        this.semaphores.put(source, new Semaphore(1, true));
        this.lastActivation.put(source, Instant.MIN);

        logger.debug("Registered wombat source {} for asset-type {}", source.getClass().getSimpleName(), asset.type().name());
        return this;
    }

    public void trigger()
    {
        Instant heartbeat = Instant.now();

        for (Environment environment : this.contextProvider.provide().environments())
        {
            environment.assets().stream()
                .filter(asset -> asset instanceof Monitorable)
                .filter(asset -> this.sources.containsKey(asset.type().name()))
                .filter(asset -> this.sources.get(asset.type().name()).accept(asset))
                .forEach(asset -> {
                    WombatSource source = this.sources.get(asset.type().name());
                    Instant lastActivated = this.lastActivation.get(source);

                    if (!lastActivated.plus(((Monitorable) asset).period().asDuration()).isBefore(heartbeat))
                        return;

                    Semaphore semaphore = this.semaphores.get(source);
                    this.executorService.execute(() -> {
                        semaphore.acquireUninterruptibly();
                        try {
                            this.runSource(
                                heartbeat,
                                lastActivated != Instant.MIN ? lastActivated : null,
                                asset,
                                source
                            );
                        }
                        finally {
                            semaphore.release();
                        }
                    });
                });
        }
    }

    private void runSource(Instant heartbeat, Instant lastActivation, Asset asset, WombatSource source)
    {
        try {
            logger.debug("Monitor triggered for asset {}", asset.identity().id());
            List<MetricData> data = source.source(heartbeat, lastActivation, asset);
            this.persister.persist(asset.identity(), asset.type(), data);
        }
        catch (WombatSourceException | WombatPersistenceException e) {
            logger.error("Monitoring failed for asset {}: {}", asset.identity().id(), e.getMessage(), e);
        }
    }

    @Override
    public void close()
    {
        this.executorService.close();
    }
}
