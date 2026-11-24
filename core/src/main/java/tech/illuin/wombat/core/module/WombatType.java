package tech.illuin.wombat.core.module;

import tech.illuin.wombat.core.asset.type.ServiceFamily;

public interface WombatType
{
    String namespace();

    String localName();

    ServiceFamily family();

    default String name()
    {
        return this.namespace() + "." + this.localName();
    }
}
