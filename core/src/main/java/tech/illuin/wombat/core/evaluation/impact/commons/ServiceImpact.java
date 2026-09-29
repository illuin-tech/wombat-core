package tech.illuin.wombat.core.evaluation.impact.commons;

import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.asset.profile.AssetProfile;

public interface ServiceImpact
{
    String serviceId();

    double share();

    Footprint footprint();

    AssetProfile profile();

    AssetType assetType();
}
