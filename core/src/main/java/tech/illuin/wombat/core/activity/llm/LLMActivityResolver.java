package tech.illuin.wombat.core.activity.llm;

import tech.illuin.wombat.core.activity.WombatActivityException;
import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.activity.commons.AssetFilter;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.ActivityRegime;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.ServiceFamily;
import tech.illuin.wombat.core.evaluation.impact.llm.LLMMetricResolver;

import java.util.Set;

import static tech.illuin.wombat.core.activity.commons.TimeRange.toEpochMs;

public class LLMActivityResolver implements WombatActivityResolver
{
    private final LLMMetricResolver metricResolver;

    public LLMActivityResolver(LLMMetricResolver metricResolver) {
        this.metricResolver = metricResolver;
    }

    @Override
    public boolean accept(Asset asset)
    {
        return asset.type().family() == ServiceFamily.LLM;
    }

    @Override
    public ActivityData resolve(Asset asset, TimeRange range, AssetFilter filter) throws WombatActivityException
    {
        //TODO: use filters
        long summedTokens = this.metricResolver.sumOutputTokens(
            toEpochMs(range.start()),
            toEpochMs(range.end()),
            asset.id()
        );
        return new LLMActivityData(ActivityRegime.MEASURED, Set.of(asset.id()), range, summedTokens, 1);
    }
}
