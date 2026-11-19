package tech.illuin.wombat.core.context;

import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.Environment;

import java.util.List;

public interface WombatContext
{
    List<Environment> environments();

    List<Asset> assets();
}
