package tech.illuin.wombat.core.source.persistence.micrometer.mapped;

import io.micrometer.core.instrument.Tag;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.source.data.LLMData;
import tech.illuin.wombat.core.source.data.MetricData;
import tech.illuin.wombat.core.source.persistence.micrometer.mapped.MappedMicrometerMetricPersister.Value;

import java.util.List;

import static tech.illuin.wombat.core.source.persistence.micrometer.MicrometerTags.*;

public class LLMMapper implements MappedMicrometerMetricPersister.Mapper
{
    @Override
    public List<Tag> mapTags(AssetIdentity identity, AssetType type, MetricData data)
    {
        LLMData llmData = asLLMData(data);

        return List.of(
            Tag.of(TAG_ENVIRONMENT, identity.environmentId()),
            Tag.of(TAG_ASSET, identity.id()),
            Tag.of(TAG_ASSET_NAME, identity.name()),
            Tag.of(TAG_ASSET_TYPE, type.name()),
            Tag.of(TAG_SERVICE, data.serviceId()),
            Tag.of(TAG_LLM_PROVIDER, llmData.provider().name()),
            Tag.of(TAG_LLM_MODEL, llmData.model()),
            Tag.of(TAG_LLM_LOCATION, llmData.location())
        );
    }

    @Override
    public List<Value> mapValues(MetricData data)
    {
        LLMData llmData = asLLMData(data);

        return List.of(
            new Value(METRIC_LLM_OUTPUT_TOKENS, llmData.outputTokens())
        );
    }

    private static LLMData asLLMData(MetricData data)
    {
        if (data instanceof LLMData llmData)
            return llmData;
        throw new IllegalArgumentException("Data must be of type LLMData instead of provided " + data.getClass().getSimpleName());
    }
}
