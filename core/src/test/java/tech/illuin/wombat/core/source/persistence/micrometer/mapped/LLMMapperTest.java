package tech.illuin.wombat.core.source.persistence.micrometer.mapped;

import io.micrometer.core.instrument.Tag;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.source.data.LLMData;
import tech.illuin.wombat.core.source.persistence.micrometer.mapped.MappedMicrometerMetricPersister.Value;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.illuin.wombat.core.activity.commons.Country.FRANCE;
import static tech.illuin.wombat.core.source.persistence.micrometer.MicrometerTags.*;

class LLMMapperTest
{
    private static final AssetType TYPE_LLM = AssetType.of("tech.illuin", "wombat-module", "llm-prometheus", ActivityRegime.MEASURED, ServiceFamily.LLM);
    private static final AssetIdentity IDENTITY = AssetIdentity.of("asset-1", "env-1", "Asset 1");

    private final LLMMapper mapper = new LLMMapper();

    @Test
    void mapTags_includesProviderModelLocation()
    {
        LLMData data = new LLMData("service-1", LLMProvider.mistralai, "mistral-large-latest", FRANCE, 500L, 1000);
        List<Tag> tags = this.mapper.mapTags(IDENTITY, TYPE_LLM, data);

        assertTrue(tags.contains(Tag.of(TAG_ENVIRONMENT, "env-1")));
        assertTrue(tags.contains(Tag.of(TAG_ASSET, "asset-1")));
        assertTrue(tags.contains(Tag.of(TAG_ASSET_NAME, "Asset 1")));
        assertTrue(tags.contains(Tag.of(TAG_ASSET_TYPE, TYPE_LLM.name())));
        assertTrue(tags.contains(Tag.of(TAG_SERVICE, "service-1")));
        assertTrue(tags.contains(Tag.of(TAG_LLM_PROVIDER, "mistralai")));
        assertTrue(tags.contains(Tag.of(TAG_LLM_MODEL, "mistral-large-latest")));
        assertTrue(tags.contains(Tag.of(TAG_LLM_LOCATION, "FRA")));
    }

    @Test
    void mapValues_mapsOutputTokens()
    {
        LLMData data = new LLMData("service-1", LLMProvider.mistralai, "mistral-large-latest", FRANCE, 500L, 1000);
        Map<String, Double> values = this.mapper.mapValues(data).stream()
            .collect(Collectors.toMap(Value::key, Value::value));

        assertEquals(2, values.size());
        assertTrue(values.containsKey(METRIC_LLM_OUTPUT_TOKENS));
        assertEquals(500.0, values.get(METRIC_LLM_OUTPUT_TOKENS));
        assertTrue(values.containsKey(METRIC_LLM_REQUEST_COUNT));
        assertEquals(1000.0, values.get(METRIC_LLM_REQUEST_COUNT));
    }
}
