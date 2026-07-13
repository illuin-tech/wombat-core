package tech.illuin.wombat.profile.model;

import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactRequest;
import tech.illuin.wombat.profile.persistence.ProfileData;
import tech.illuin.wombat.profile.persistence.ProfileEntity;

public record InfrastructureProfile(
    String id,
    String description,
    BoaviztaInstanceImpactRequest.Provider provider,
    String instanceType,
    String location,
    int lifespan
) implements Profile
{

    @Override
    public MeasureType measureType()
    {
        return MeasureType.DYNAMIC;
    }

    public static InfrastructureProfile from(ProfileEntity entity)
    {
        if (!(entity.data instanceof ProfileData.InfrastructureData(String type, int lifespan1)))
            throw new IllegalStateException("Profile " + entity.id + " of type " + entity.type + " carries no infrastructure data");
        return new InfrastructureProfile(entity.id, entity.description, BoaviztaInstanceImpactRequest.Provider.valueOf(entity.provider), type, entity.location, lifespan1);
    }
}
