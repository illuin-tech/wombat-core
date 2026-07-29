package tech.illuin.wombat.handler;

import tech.illuin.wombat.ecologits.EcologitsClient;
import tech.illuin.wombat.ecologits.model.EcologitsEstimationRequest;
import tech.illuin.wombat.ecologits.model.EcologitsEstimationResponse;
import tech.illuin.wombat.handler.model.LLMImpact;
import tech.illuin.wombat.persistence.ModelMetricRepository;
import tech.illuin.wombat.asset.model.profile.DynamicLLMProfile;
import tech.illuin.wombat.asset.model.profile.LLMProfile;
import tech.illuin.wombat.asset.model.profile.StaticLLMProfile;
import tech.illuin.wombat.persistence.model.TimeRange;

import java.time.Duration;
import java.time.Instant;


public class LLMImpactService
{
    private static final Duration ONE_YEAR = Duration.ofDays(365);

    private final EcologitsClient ecologitsClient;
    private final ModelMetricRepository modelMetricRepository;

    public LLMImpactService(EcologitsClient ecologitsClient, ModelMetricRepository modelMetricRepository)
    {
        this.ecologitsClient = ecologitsClient;
        this.modelMetricRepository = modelMetricRepository;
    }

    public LLMImpact computeImpact(String assetId, LLMProfile profile, TimeRange timeRange)
    {
        return switch (profile)
        {
            case StaticLLMProfile staticProfile -> this.computeStatic(staticProfile, timeRange);
            case DynamicLLMProfile dynamicProfile -> this.computeDynamic(assetId, dynamicProfile, timeRange);
        };
    }

    private LLMImpact computeStatic(StaticLLMProfile profile, TimeRange timeRange)
    {
        StaticLLMProfile.RequestProfile requestProfile = profile.requestProfile();
        EcologitsEstimationResponse estimation = this.estimate(profile, requestProfile.outputTokenCount());
        double requestCount = requestCountOver(requestProfile.requestPerYear(), timeRange);
        return new LLMImpact(estimation, requestProfile.outputTokenCount(), requestCount);
    }

    private LLMImpact computeDynamic(String assetId, DynamicLLMProfile profile, TimeRange timeRange)
    {
        long summedTokens = this.modelMetricRepository.sumOutputTokens(
            toEpochMs(timeRange.start()), toEpochMs(timeRange.end()), assetId);
        int outputTokenCount = (int) Math.min(summedTokens, Integer.MAX_VALUE);
        EcologitsEstimationResponse estimation = this.estimate(profile, outputTokenCount);
        return new LLMImpact(estimation, outputTokenCount, 1.0);
    }

    private EcologitsEstimationResponse estimate(LLMProfile profile, int outputTokenCount)
    {
        EcologitsEstimationRequest request = new EcologitsEstimationRequest(
            profile.provider(),
            profile.model(),
            outputTokenCount,
            profile.location()
        );
        return this.ecologitsClient.estimate(request);
    }

    private static double requestCountOver(int requestsPerYear, TimeRange timeRange)
    {
        Duration period = Duration.between(timeRange.start(), timeRange.end());
        return requestsPerYear * ((double) period.toSeconds() / ONE_YEAR.toSeconds());
    }

    private static long toEpochMs(Instant instant)
    {
        if (instant.equals(Instant.MIN)) return Long.MIN_VALUE;
        if (instant.equals(Instant.MAX)) return Long.MAX_VALUE;
        return instant.toEpochMilli();
    }
}
