package tech.illuin.wombat.core.module;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.type.AssetType;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds processors of a given kind from the factories registered per asset-type, and keeps one per asset identity.
 */
public final class ProcessorPool<P extends AssetProcessor> implements AutoCloseable
{
    private final String kind;
    private final WombatServices services;
    private final Map<String, ProcessorFactory<P>> factories;
    private final Map<AssetIdentity, P> processors;

    private static final Logger logger = LoggerFactory.getLogger(ProcessorPool.class);

    /**
     * @param kind the kind of processor being pooled (e.g. "source", "activity-resolver"), only used for logging
     */
    public ProcessorPool(String kind, WombatServices services)
    {
        this.kind = kind;
        this.services = services;
        this.factories = new ConcurrentHashMap<>();
        this.processors = new ConcurrentHashMap<>();
    }

    public ProcessorPool<P> register(AssetType type, ProcessorFactory<P> factory)
    {
        this.factories.put(type.name(), factory);
        logger.debug("Registered wombat {} factory for asset-type {}", this.kind, type.name());
        return this;
    }

    public boolean supports(AssetType type)
    {
        return this.factories.containsKey(type.name());
    }

    /**
     * Returns the processor serving {@code asset}, built on first use.
     * <p>
     * A failing factory propagates its exception and nothing is kept, so building is attempted again on the next call.
     *
     * @return the processor serving {@code asset}, empty when no factory is registered for its asset-type
     */
    public Optional<P> get(Asset asset)
    {
        ProcessorFactory<P> factory = this.factories.get(asset.type().name());
        if (factory == null)
            return Optional.empty();

        return Optional.of(this.processors.computeIfAbsent(asset.identity(), id -> this.create(factory, asset)));
    }

    /**
     * Discards the processors of every asset not in {@code identities}.
     */
    public void retain(Set<AssetIdentity> identities)
    {
        List<AssetIdentity> evicted = this.processors.keySet().stream()
            .filter(id -> !identities.contains(id))
            .toList();

        for (AssetIdentity identity : evicted)
            this.invalidate(identity);
    }

    /**
     * Discards the processor of {@code identity}, if any, so the next call builds a new one.
     */
    public void invalidate(AssetIdentity identity)
    {
        // Removed before being closed, so that no caller can obtain a processor that is being closed.
        P processor = this.processors.remove(identity);
        if (processor != null)
            this.release(identity, processor);
    }

    @Override
    public void close()
    {
        for (AssetIdentity identity : List.copyOf(this.processors.keySet()))
            this.invalidate(identity);
    }

    private P create(ProcessorFactory<P> factory, Asset asset)
    {
        P processor = Objects.requireNonNull(factory.create(this.services, asset), "factory returned a null " + this.kind);
        logger.debug("Created wombat {} {} for asset {}", this.kind, processor.getClass().getSimpleName(), asset.identity().id());
        return processor;
    }

    /* AutoCloseable.close() declares Exception: a processor failing to close must not prevent discarding the others. */
    @SuppressWarnings("checkstyle:IllegalCatch")
    private void release(AssetIdentity identity, P processor)
    {
        logger.debug("Discarding wombat {} {} for asset {}", this.kind, processor.getClass().getSimpleName(), identity.id());
        if (!(processor instanceof AutoCloseable closeable))
            return;

        try {
            closeable.close();
        }
        catch (Exception e) {
            logger.error("Could not close wombat {} for asset {}: {}", this.kind, identity.id(), e.getMessage(), e);
        }
    }
}
