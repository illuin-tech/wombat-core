package tech.illuin.wombat.core.evaluation.cost.commons;

import tech.illuin.wombat.core.asset.profile.AssetProfile;
import tech.illuin.wombat.core.asset.type.AssetType;

/**
 * What one service of an asset costs, the counterpart of a {@code ServiceImpact}: a cost resolver implements it with
 * its own pricing details next to these.
 */
public interface ServiceCost
{
    String serviceId();

    Money cost();

    AssetProfile profile();

    AssetType assetType();
}
