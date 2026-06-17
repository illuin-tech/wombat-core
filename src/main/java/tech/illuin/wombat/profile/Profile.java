package tech.illuin.wombat.profile;

import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactRequest;

public record Profile(
    String id,
    String description,
    BoaviztaInstanceImpactRequest.Provider provider,
    String instanceType,
    String location,
    int lifespan
)
{

    public static Profile from(ServerProfileEntity entity)
    {
        return new Profile(entity.id, entity.description, entity.provider, entity.instanceType, entity.location, entity.lifespan);
    }
}
