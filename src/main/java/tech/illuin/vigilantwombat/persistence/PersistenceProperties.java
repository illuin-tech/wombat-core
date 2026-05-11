package tech.illuin.vigilantwombat.persistence;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "persistence")
public interface PersistenceProperties {
    Boolean enableMeterTarget();

    Boolean enableMemoryTarget();
}
