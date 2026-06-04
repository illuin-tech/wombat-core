package tech.illuin.wombat.profile;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProfileDtoTest
{

    @Test
    void from_mapsEntityFieldsOntoDto()
    {
        ServerProfileEntity entity = new ServerProfileEntity();
        entity.id = "aws-c5-large";
        entity.description = "AWS c5.large FRA";
        entity.provider = BoaviztaInstanceImpactRequest.Provider.aws;
        entity.instanceType = "c5.large";
        entity.location = "FRA";
        entity.lifespan = 43800;

        ProfileDto dto = ProfileDto.from(entity);

        assertEquals("aws-c5-large", dto.id());
        assertEquals("AWS c5.large FRA", dto.description());
        assertEquals(BoaviztaInstanceImpactRequest.Provider.aws, dto.provider());
        assertEquals("c5.large", dto.instanceType());
        assertEquals("FRA", dto.location());
        assertEquals(43800, dto.lifespan());
    }
}
