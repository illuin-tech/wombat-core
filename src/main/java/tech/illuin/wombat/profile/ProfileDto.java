package tech.illuin.wombat.profile;

import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactRequest;

public record ProfileDto(
    String id,
    String description,
    BoaviztaInstanceImpactRequest.Provider provider,
    String instanceType,
    String location,
    int lifespan
)
{

    public static ProfileDto from(ServerProfileEntity entity)
    {
        return new ProfileDto(entity.id, entity.description, entity.provider, entity.instanceType, entity.location, entity.lifespan);
    }
}
