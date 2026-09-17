package tech.illuin.wombat.core.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetType;
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
    private final Map<AssetType, WombatSource> sources;
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

    public AssetMonitor register(Asset properties, WombatSource source)
    {
        if (this.sources.containsKey(properties.type()))
        {
            logger.warn("Source for {} was already registered with type {}", properties.type(), this.sources.get(properties.type()).getClass().getSimpleName());
            return this;
        }
        this.sources.put(properties.type(), source);
        this.semaphores.computeIfAbsent(source, s -> new Semaphore(1, true));
        return this;
    }

    public void trigger()
    {
        Instant heartbeat = Instant.now();
        int beat = this.heartbeatCount.incrementAndGet();

        for (Environment environment : this.contextProvider.provide().environments())
        {
            environment.assets().stream()
                .filter(properties -> properties instanceof Monitorable)
                .filter(properties -> this.sources.containsKey(properties.type()))
                .filter(properties -> this.sources.get(properties.type()).accept(properties))
                .filter(properties -> this.acceptHeartbeat((Monitorable) properties, beat))
                .forEach(properties -> {
                    WombatSource source = this.sources.get(properties.type());
                    Semaphore semaphore = this.semaphores.computeIfAbsent(source, s -> new Semaphore(1, true));
                    this.executorService.execute(() -> {
                        semaphore.acquireUninterruptibly();
                        try {
                            this.runSource(heartbeat, properties, source);
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

    private void runSource(Instant heartbeat, Asset properties, WombatSource source)
    {
        try {
            logger.debug("Monitor triggered for asset {}", properties.id());
            List<MetricData> data = source.source(heartbeat, properties);
            this.persister.persist(data);
        }
        catch (WombatSourceException | WombatPersistenceException e) {
            logger.error("Monitoring failed for asset {}: {}", properties.id(), e.getMessage(), e);
        }
    }

    @Override
    public void close()
    {
        this.executorService.close();
    }
}
