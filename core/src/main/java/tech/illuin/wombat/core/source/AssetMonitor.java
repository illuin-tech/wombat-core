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
import java.util.concurrent.atomic.AtomicInteger;

public class AssetMonitor implements AutoCloseable
{
    private final WombatContextProvider contextProvider;
    private final Map<String, WombatSource> sources;
    private final WombatMetricPersister persister;
    private final AtomicInteger heartbeatCount;
    private final ExecutorService executorService;
    private final Map<WombatSource, Semaphore> semaphores;

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
        this.heartbeatCount = new AtomicInteger(0);
        this.executorService = executorService;
        this.semaphores = new ConcurrentHashMap<>();
    }

    public AssetMonitor register(Asset asset, WombatSource source)
    {
        if (this.sources.containsKey(asset.type().name()))
        {
            logger.trace("Source for {} was already registered with type {}", asset.type(), this.sources.get(asset.type().name()).getClass().getSimpleName());
            return this;
        }
        this.sources.put(asset.type().name(), source);
        this.semaphores.computeIfAbsent(source, s -> new Semaphore(1, true));

        logger.debug("Registered wombat source {} for asset-type {}", source.getClass().getSimpleName(), asset.type().name());
        return this;
    }

    public void trigger()
    {
        Instant heartbeat = Instant.now();
        int beat = this.heartbeatCount.incrementAndGet();

        for (Environment environment : this.contextProvider.provide().environments())
        {
            environment.assets().stream()
                .filter(asset -> asset instanceof Monitorable)
                .filter(asset -> this.sources.containsKey(asset.type().name()))
                .filter(asset -> this.sources.get(asset.type().name()).accept(asset))
                .filter(asset -> this.acceptHeartbeat((Monitorable) asset, beat))
                .forEach(asset -> {
                    WombatSource source = this.sources.get(asset.type().name());
                    Semaphore semaphore = this.semaphores.computeIfAbsent(source, s -> new Semaphore(1, true));
                    this.executorService.execute(() -> {
                        semaphore.acquireUninterruptibly();
                        try {
                            this.runSource(heartbeat, asset, source);
                        }
                        finally {
                            semaphore.release();
                        }
                    });
                });
        }
    }

    private boolean acceptHeartbeat(Monitorable monitorable, int beat)
    {
        int skip = monitorable.heartbeatSkip();
        // A skip of 0 or 1 means "run on every heartbeat"; guards against divide-by-zero.
        return skip <= 1 || beat % skip == 0;
    }

    private void runSource(Instant heartbeat, Asset asset, WombatSource source)
    {
        try {
            logger.debug("Monitor triggered for asset {}", asset.id());
            List<MetricData> data = source.source(heartbeat, asset);
            this.persister.persist(data);
        }
        catch (WombatSourceException | WombatPersistenceException e) {
            logger.error("Monitoring failed for asset {}: {}", asset.id(), e.getMessage(), e);
        }
    }

    @Override
    public void close()
    {
        this.executorService.close();
    }
}
