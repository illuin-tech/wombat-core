package tech.illuin.wombat.environment.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AssetDataConverter implements AttributeConverter<AssetData, String>
{
    private static final ObjectMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    @Override
    public String convertToDatabaseColumn(AssetData attribute)
    {
        if (attribute == null)
            return null;
        try
        {
            return MAPPER.writeValueAsString(attribute);
        }
        catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize asset data", e);
        }
    }

    @Override
    public AssetData convertToEntityAttribute(String dbData)
    {
        if (dbData == null || dbData.isBlank())
            return null;
        try
        {
            return MAPPER.readValue(dbData, AssetData.class);
        }
        catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize asset data: " + dbData, e);
        }
    }
}
