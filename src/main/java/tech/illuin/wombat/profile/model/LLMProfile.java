package tech.illuin.wombat.profile.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.wombat.ecologits.model.EcologitsEstimationRequest;
import tech.illuin.wombat.profile.persistence.ProfileData;
import tech.illuin.wombat.profile.persistence.ProfileEntity;

public record LLMProfile(
    @JsonProperty("id") String id,
    @JsonProperty("description") String description,
    @JsonProperty("provider") EcologitsEstimationRequest.Provider provider,
    @JsonProperty("model") String model,
    @JsonProperty("location") String location,
    @JsonProperty("request_profile") RequestProfile requestProfile
) implements Profile
{
    @Override
    public MeasureType measureType()
    {
        return MeasureType.STATIC;
    }

    public static LLMProfile from(ProfileEntity entity)
    {
        if (!(entity.data instanceof ProfileData.LLMData(String model1, int outputTokenCount, int requestPerYear)))
            throw new IllegalStateException("Profile " + entity.id + " of type " + entity.type + " carries no LLM data");
        return new LLMProfile(
            entity.id,
            entity.description,
            EcologitsEstimationRequest.Provider.valueOf(entity.provider),
            model1,
            entity.location,
            new RequestProfile(outputTokenCount, requestPerYear)
        );
    }

    public record RequestProfile(
        @JsonProperty("output_token_count") int outputTokenCount,
        @JsonProperty("request_per_year") int requestPerYear
    ) {}
}
