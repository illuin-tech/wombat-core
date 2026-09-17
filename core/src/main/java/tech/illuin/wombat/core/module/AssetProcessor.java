package tech.illuin.wombat.core.module;

import tech.illuin.wombat.core.asset.Asset;

public interface AssetProcessor
{
    default boolean accept(Asset asset)
    {
        return true;
    }
}
