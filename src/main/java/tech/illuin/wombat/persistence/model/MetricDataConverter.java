package tech.illuin.wombat.persistence.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MetricDataConverter implements AttributeConverter<MetricData, String>
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(MetricData attribute)
    {
        if (attribute == null)
            return null;
        try
        {
            return MAPPER.writeValueAsString(attribute);
        }
        catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize metric data", e);
        }
    }

    @Override
    public MetricData convertToEntityAttribute(String dbData)
    {
        if (dbData == null || dbData.isBlank())
            return null;
        try
        {
            return MAPPER.readValue(dbData, MetricData.class);
        }
        catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize metric data: " + dbData, e);
        }
    }
}
