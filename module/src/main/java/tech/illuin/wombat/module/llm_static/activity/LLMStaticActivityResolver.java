package tech.illuin.wombat.module.llm_static.activity;

import tech.illuin.wombat.core.activity.WombatActivityException;
import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.activity.commons.AssetFilter;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.activity.llm.LLMActivityData;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.module.llm_static.LLMStaticAsset;
import tech.illuin.wombat.module.llm_static.LLMStaticProfile;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

public class LLMStaticActivityResolver implements WombatActivityResolver
{
    private static final Duration ONE_YEAR = Duration.ofDays(365);

    @Override
    public boolean accept(Asset asset) {
        return asset instanceof LLMStaticAsset;
    }

    @Override
    public Optional<ActivityData> resolve(Asset asset, TimeRange range, AssetFilter filter) throws WombatActivityException
    {
        LLMStaticProfile profile = (LLMStaticProfile) asset.profile();
        LLMStaticProfile.RequestProfile requestProfile = profile.requestProfile();
        int requestCount = requestCountOver(requestProfile.requestPerYear(), range);
        return Optional.of(new LLMActivityData(
            ActivityRegime.MODELED,
            Set.of(asset.identity().id()),
            range,
            (long) requestProfile.outputTokenCount() * requestCount,
            requestCount
        ));
    }

    private static int requestCountOver(int requestsPerYear, TimeRange timeRange)
    {
        Duration period = Duration.between(timeRange.start(), timeRange.end());
        return (int) (requestsPerYear * ((double) period.toSeconds() / ONE_YEAR.toSeconds()));
    }
}
