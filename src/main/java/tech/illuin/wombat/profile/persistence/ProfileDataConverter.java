package tech.illuin.wombat.profile.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ProfileDataConverter implements AttributeConverter<ProfileData, String>
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(ProfileData attribute)
    {
        if (attribute == null)
            return null;
        try
        {
            return MAPPER.writeValueAsString(attribute);
        }
        catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize profile data", e);
        }
    }

    @Override
    public ProfileData convertToEntityAttribute(String dbData)
    {
        if (dbData == null || dbData.isBlank())
            return null;
        try
        {
            return MAPPER.readValue(dbData, ProfileData.class);
        }
        catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize profile data: " + dbData, e);
        }
    }
}
