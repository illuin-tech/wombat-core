package tech.illuin.wombat.core.connector.ecologits.connector.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.illuin.wombat.core.asset.profile.LLMProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record EcologitsEstimationRequest(
    @JsonProperty("provider") Provider provider,
    @JsonProperty("model_name") String modelName,
    @JsonProperty("output_token_count") long outputTokenCount,
    @JsonProperty("electricity_mix_zone") String electricityMixZone
) {
    public enum Provider
    {
        anthropic, mistralai, openai, huggingface_hub, cohere, google_genai;

        private static final Map<String, Provider> index;

        static {
            index = new HashMap<>();
            for (Provider p : Provider.values())
                index.put(p.name(), p);
        }

        public static Optional<Provider> forName(LLMProvider provider)
        {
            return Optional.ofNullable(index.get(provider.name()));
        }
    }
}
