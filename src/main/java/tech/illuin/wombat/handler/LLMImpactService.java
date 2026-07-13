package tech.illuin.wombat.handler;

import tech.illuin.wombat.ecologits.EcologitsClient;
import tech.illuin.wombat.ecologits.model.EcologitsEstimationRequest;
import tech.illuin.wombat.ecologits.model.EcologitsEstimationResponse;
import tech.illuin.wombat.handler.model.LLMImpact;
import tech.illuin.wombat.persistence.model.TimeRange;
import tech.illuin.wombat.profile.model.LLMProfile;

import java.time.Duration;


public class LLMImpactService
{
    private static final Duration ONE_YEAR = Duration.ofDays(365);

    private final EcologitsClient ecologitsClient;

    public LLMImpactService(EcologitsClient ecologitsClient)
    {
        this.ecologitsClient = ecologitsClient;
    }

    public LLMImpact computeImpact(LLMProfile profile, TimeRange timeRange)
    {
        LLMProfile.RequestProfile requestProfile = profile.requestProfile();
        EcologitsEstimationRequest request = new EcologitsEstimationRequest(
            profile.provider(),
            profile.model(),
            requestProfile.outputTokenCount(),
            profile.location()
        );
        EcologitsEstimationResponse estimation = this.ecologitsClient.estimate(request);
        double requestCount = requestCountOver(requestProfile.requestPerYear(), timeRange);
        return new LLMImpact(estimation, requestCount);
    }

    private static double requestCountOver(int requestsPerYear, TimeRange timeRange)
    {
        Duration period = Duration.between(timeRange.start(), timeRange.end());
        return requestsPerYear * ((double) period.toSeconds() / ONE_YEAR.toSeconds());
    }
}
