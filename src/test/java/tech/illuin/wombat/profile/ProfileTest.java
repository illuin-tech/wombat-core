package tech.illuin.wombat.profile;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactRequest;
import tech.illuin.wombat.profile.model.InfrastructureProfile;
import tech.illuin.wombat.profile.model.ProfileType;
import tech.illuin.wombat.profile.persistence.ProfileData;
import tech.illuin.wombat.profile.persistence.ProfileEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProfileTest
{

    @Test
    void from_mapsEntityFieldsOntoDto()
    {
        ProfileEntity entity = new ProfileEntity();
        entity.id = "aws-c5-large";
        entity.description = "AWS c5.large FRA";
        entity.type = ProfileType.INFRASTRUCTURE;
        entity.provider = "aws";
        entity.location = "FRA";
        entity.data = new ProfileData.InfrastructureData("c5.large", 43800);

        InfrastructureProfile dto = InfrastructureProfile.from(entity);

        assertEquals("aws-c5-large", dto.id());
        assertEquals("AWS c5.large FRA", dto.description());
        assertEquals(BoaviztaInstanceImpactRequest.Provider.aws, dto.provider());
        assertEquals("c5.large", dto.instanceType());
        assertEquals("FRA", dto.location());
        assertEquals(43800, dto.lifespan());
    }
}
