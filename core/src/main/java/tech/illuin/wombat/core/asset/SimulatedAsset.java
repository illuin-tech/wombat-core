package tech.illuin.wombat.core.asset;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tech.illuin.wombat.core.activity.commons.ActivityData;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
public interface SimulatedAsset extends Asset
{
    ActivityData activity();

    @Override
    default AssetIdentity identity()
    {
        return AssetIdentity.of("simulated-asset", "simulated-environment", "simulated-asset");
    }
}
