package tech.illuin.wombat.core.module;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.asset.type.FamilyType;
import tech.illuin.wombat.core.asset.type.ModuleType;
import tech.illuin.wombat.core.asset.type.ServiceFamily;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Builds processors of a given kind from the factories registered per module, and keeps one per asset identity.
 * <p>
 * The candidates for an asset are the factory of its asset-type's module, then those of its family's modules in the
 * order they were registered: the first processor to accept the asset serves it.
 */
public final class ProcessorPool<P extends AssetProcessor> implements AutoCloseable
{
    private final String kind;
    private final WombatServices services;
    private final Map<String, Registration<P>> typeFactories;
    private final Map<ServiceFamily, List<Registration<P>>> familyFactories;
    /* Empty when no candidate accepts the asset, so that the candidates are not built again on every call */
    private final Map<AssetIdentity, Optional<P>> processors;

    private static final Logger logger = LoggerFactory.getLogger(ProcessorPool.class);

    /**
     * @param kind the kind of processor being pooled (e.g. "source", "activity-resolver"), only used for logging
     */
    public ProcessorPool(String kind, WombatServices services)
    {
        this.kind = kind;
        this.services = services;
        this.typeFactories = new ConcurrentHashMap<>();
        this.familyFactories = new ConcurrentHashMap<>();
        this.processors = new ConcurrentHashMap<>();
    }

    public ProcessorPool<P> register(ModuleType type, ProcessorFactory<P> factory)
    {
        Registration<P> registration = new Registration<>(type, factory);
        switch (type)
        {
            case AssetType assetType -> this.typeFactories.put(assetType.name(), registration);
            case FamilyType familyType -> this.familyFactories.computeIfAbsent(familyType.family(), family -> new CopyOnWriteArrayList<>()).add(registration);
        }
        logger.debug("Registered wombat {} factory for module {}", this.kind, type.name());
        return this;
    }

    /**
     * @return whether a factory was registered for the module of type {@code type}
     */
    public boolean registered(ModuleType type)
    {
        return switch (type)
        {
            case AssetType assetType -> this.typeFactories.containsKey(assetType.name());
            case FamilyType familyType -> this.familyFactories.getOrDefault(familyType.family(), List.of()).stream()
                .anyMatch(registration -> registration.type().name().equals(familyType.name()));
        };
    }

    /**
     * @return whether any factory may serve {@code asset}, regardless of whether its processor accepts it
     */
    public boolean supports(Asset asset)
    {
        return !this.candidates(asset).isEmpty();
    }

    /**
     * Returns the processor serving {@code asset}, chosen and built on first use.
     * <p>
     * The choice is kept, like the processor itself, until the asset is refreshed or leaves the context: the processor
     * is not asked again on each call, and an asset no candidate accepts stays unserved.
     * <p>
     * A failing factory propagates its exception and nothing is kept, so building is attempted again on the next call.
     *
     * @return the processor serving {@code asset}, empty when no candidate accepts it
     */
    public Optional<P> get(Asset asset)
    {
        return this.processors.computeIfAbsent(asset.identity(), id -> this.select(asset));
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
     * Discards the processor of {@code identity}, if any, so the next call chooses and builds one again.
     */
    public void invalidate(AssetIdentity identity)
    {
        // Removed before being closed, so that no caller can obtain a processor that is being closed.
        Optional<P> processor = this.processors.remove(identity);
        if (processor != null)
            processor.ifPresent(p -> this.release(identity, p));
    }

    @Override
    public void close()
    {
        for (AssetIdentity identity : List.copyOf(this.processors.keySet()))
            this.invalidate(identity);
    }

    private List<Registration<P>> candidates(Asset asset)
    {
        List<Registration<P>> candidates = new ArrayList<>();
        Registration<P> own = this.typeFactories.get(asset.type().name());
        if (own != null)
            candidates.add(own);
        candidates.addAll(this.familyFactories.getOrDefault(asset.type().family(), List.of()));
        return candidates;
    }

    private Optional<P> select(Asset asset)
    {
        for (Registration<P> candidate : this.candidates(asset))
        {
            P processor = this.create(candidate, asset);
            boolean accepted = false;
            try {
                accepted = processor.accept(asset);
                if (accepted)
                    return Optional.of(processor);

                logger.debug("Wombat {} {} of module {} rejects asset {}", this.kind, processor.getClass().getSimpleName(), candidate.type().name(), asset.identity().id());
            }
            finally {
                // Rejecting the asset, or failing to tell whether it accepts it: nothing else holds it.
                if (!accepted)
                    this.release(asset.identity(), processor);
            }
        }
        return Optional.empty();
    }

    private P create(Registration<P> registration, Asset asset)
    {
        P processor = Objects.requireNonNull(registration.factory().create(this.services, asset), "factory returned a null " + this.kind);
        logger.debug("Created wombat {} {} of module {} for asset {}", this.kind, processor.getClass().getSimpleName(), registration.type().name(), asset.identity().id());
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

    private record Registration<P extends AssetProcessor>(
        ModuleType type,
        ProcessorFactory<P> factory
    ) {}
}
