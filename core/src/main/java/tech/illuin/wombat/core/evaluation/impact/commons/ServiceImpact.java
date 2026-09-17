package tech.illuin.wombat.core.evaluation.impact.commons;

import tech.illuin.wombat.core.asset.AssetType;
import tech.illuin.wombat.core.asset.profile.Profile;

public interface ServiceImpact
{
    String serviceId();

    double share();

    Footprint footprint();

    Profile profile();

    AssetType assetType();
}
