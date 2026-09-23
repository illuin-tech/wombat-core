package tech.illuin.wombat.core.asset;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tech.illuin.wombat.core.activity.commons.ActivityData;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
public interface SimulatedAsset extends Asset
{
    ActivityData activity();

    @Override
    default String id()
    {
        return "simulated-asset";
    }

    @Override
    default String environmentId()
    {
        return "simulated-environment";
    }

    @Override
    default String name()
    {
        return this.id();
    }
}
