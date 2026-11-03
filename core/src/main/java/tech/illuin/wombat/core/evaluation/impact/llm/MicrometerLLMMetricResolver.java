package tech.illuin.wombat.core.evaluation.impact.llm;

import io.micrometer.core.instrument.MeterRegistry;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.activity.llm.LLMServiceActivity;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.core.source.persistence.micrometer.data.Aggregation;
import tech.illuin.wombat.core.source.persistence.micrometer.data.MetricGroup;
import tech.illuin.wombat.core.source.persistence.micrometer.data.TagGroup;

import java.util.*;

import static tech.illuin.wombat.core.activity.commons.TimeRange.toEpochMs;
import static tech.illuin.wombat.core.source.persistence.micrometer.MicrometerTags.*;

public class MicrometerLLMMetricResolver implements LLMMetricResolver
{
    private final MeterRegistry registry;

    private static final Map<String, Aggregation> VALUES = Map.of(
        METRIC_LLM_OUTPUT_TOKENS, Aggregation.SUM,
        METRIC_LLM_REQUEST_COUNT, Aggregation.SUM
    );

    private static final Set<String> TAGS = Set.of(
        TAG_ENVIRONMENT, TAG_ASSET, TAG_ASSET_NAME, TAG_ASSET_TYPE,
        TAG_SERVICE, TAG_LLM_PROVIDER, TAG_LLM_MODEL, TAG_LLM_LOCATION
    );

    public MicrometerLLMMetricResolver(MeterRegistry registry)
    {
        this.registry = registry;
    }

    @Override
    public Map<Long, Map<String, LLMServiceActivity>> serviceActivitiesPerBucket(TimeRange range, String assetId)
    {
        Map<String, LLMServiceActivity> activities = serviceActivities(assetId);
        return activities.isEmpty() ? Map.of() : perSlot(range, activities);
    }

    private Map<String, LLMServiceActivity> serviceActivities(String assetId)
    {
        Map<String, LLMServiceActivity> activities = new LinkedHashMap<>();

        for (MetricGroup group : gatherGroups(assetId))
        {
            String serviceId = group.tag(TAG_SERVICE).orElse(null);
            LLMProvider provider = group.tag(TAG_LLM_PROVIDER).map(LLMProvider::valueOf).orElse(null);
            String model = group.tag(TAG_LLM_MODEL).orElse(null);
            Country location = group.tag(TAG_LLM_LOCATION).flatMap(Country::ofCode).orElse(null);
            long tokens = group.value(METRIC_LLM_OUTPUT_TOKENS).map(Double::longValue).orElse(0L);
            double requests = group.value(METRIC_LLM_REQUEST_COUNT).orElse(0.0);

            if (serviceId != null)
            {
                activities.merge(serviceId, new LLMServiceActivity(provider, model, location, tokens, requests),
                    (oldVal, newVal) -> new LLMServiceActivity(
                        newVal.provider() != null ? newVal.provider() : oldVal.provider(),
                        newVal.model() != null ? newVal.model() : oldVal.model(),
                        newVal.location() != null ? newVal.location() : oldVal.location(),
                        oldVal.outputTokenCount() + newVal.outputTokenCount(),
                        oldVal.requestCount() + newVal.requestCount()
                    )
                );
            }
        }
        return activities;
    }

    private List<MetricGroup> gatherGroups(String assetId)
    {
        Map<TagGroup, MetricGroup> groups = new HashMap<>();
        for (var meter : this.registry.getMeters())
        {
            TagGroup.from(meter.getId(), TAGS)
                .filter(tg -> assetId == null || assetId.equals(tg.get(TAG_ASSET)))
                .map(tg -> groups.computeIfAbsent(tg, MetricGroup::new))
                .ifPresent(mg -> mg.recordValues(meter, VALUES));
        }
        return new ArrayList<>(groups.values());
    }

    private static <V> Map<Long, V> perSlot(TimeRange range, V value)
    {
        Map<Long, V> perSlot = new LinkedHashMap<>();
        range.split().forEach(slot -> perSlot.put(toEpochMs(slot.start()), value));
        return perSlot;
    }
}
