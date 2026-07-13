package tech.illuin.wombat.profile.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tech.illuin.wombat.profile.persistence.ProfileEntity;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = InfrastructureProfile.class, name = "INFRASTRUCTURE"),
    @JsonSubTypes.Type(value = LLMProfile.class, name = "LLM")
})
public interface Profile
{

    String id();

    String description();

    MeasureType measureType();

    static Profile from(ProfileEntity entity)
    {
        return switch (entity.type)
        {
            case INFRASTRUCTURE -> InfrastructureProfile.from(entity);
            case LLM -> LLMProfile.from(entity);
        };
    }
}
