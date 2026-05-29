package tech.illuin.wombat.persistence;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "persistence")
public interface PersistenceProperties
{
    Boolean enableMemoryTarget();
}
