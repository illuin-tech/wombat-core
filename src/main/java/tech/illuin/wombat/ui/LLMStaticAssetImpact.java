package tech.illuin.wombat.ui;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.wombat.ecologits.model.EcologitsEstimationResponse;
import tech.illuin.wombat.asset.model.profile.LLMProfile;
import tech.illuin.wombat.monitor.AssetType;

public record LLMStaticAssetImpact(
    @JsonProperty("name") String name,
    @JsonProperty("profile") LLMProfile profile,
    @JsonProperty("estimation") EcologitsEstimationResponse estimation,
    @JsonProperty("output_token_count") int outputTokenCount,
    @JsonProperty("request_count") double requestCount,
    @JsonProperty("service_included") boolean serviceIncluded
) implements LLMAssetImpact
{
    @Override
    public AssetType type()
    {
        return AssetType.LLM_STATIC;
    }
}
