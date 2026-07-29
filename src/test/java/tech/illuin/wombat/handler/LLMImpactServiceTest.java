package tech.illuin.wombat.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tech.illuin.wombat.asset.model.profile.DynamicLLMProfile;
import tech.illuin.wombat.asset.model.profile.LLMProfile;
import tech.illuin.wombat.asset.model.profile.StaticLLMProfile;
import tech.illuin.wombat.ecologits.EcologitsClient;
import tech.illuin.wombat.ecologits.model.EcologitsEstimationRequest;
import tech.illuin.wombat.ecologits.model.EcologitsEstimationResponse;
import tech.illuin.wombat.handler.model.LLMImpact;
import tech.illuin.wombat.persistence.ModelMetricRepository;
import tech.illuin.wombat.persistence.model.TimeRange;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LLMImpactServiceTest
{

    private EcologitsClient ecologitsClient;
    private ModelMetricRepository modelMetricRepository;
    private LLMImpactService service;

    @BeforeEach
    void setup()
    {
        this.ecologitsClient = mock(EcologitsClient.class);
        this.modelMetricRepository = mock(ModelMetricRepository.class);
        when(this.ecologitsClient.estimate(any())).thenReturn(estimation());
        this.service = new LLMImpactService(this.ecologitsClient, this.modelMetricRepository);
    }

    @Test
    void computeImpact_static_usesProfileTokensAndProratesRequestsOverPeriod()
    {
        LLMProfile profile = new StaticLLMProfile(
            EcologitsEstimationRequest.Provider.mistralai, "m", "FRA",
            new StaticLLMProfile.RequestProfile(500, 730));
        // half a year → half the yearly requests
        TimeRange range = new TimeRange(Instant.EPOCH, Instant.EPOCH.plus(Duration.ofDays(365).dividedBy(2)));

        LLMImpact impact = this.service.computeImpact("llm-static", profile, range);

        assertEquals(500, capturedRequest().outputTokenCount());
        assertEquals(500, impact.outputTokenCount());
        assertEquals(365.0, impact.requestCount(), 1.0);
        verifyNoInteractions(this.modelMetricRepository);
    }

    @Test
    void computeImpact_dynamic_sumsMeasuredTokensAndUsesSingleRequest()
    {
        LLMProfile profile = new DynamicLLMProfile(
            EcologitsEstimationRequest.Provider.mistralai, "m", "FRA",
            new DynamicLLMProfile.DynamicProfile("q"));
        TimeRange range = new TimeRange(Instant.ofEpochMilli(1000), Instant.ofEpochMilli(2000));
        when(this.modelMetricRepository.sumOutputTokens(eq(1000L), eq(2000L), eq("llm-dynamic"))).thenReturn(4242L);

        LLMImpact impact = this.service.computeImpact("llm-dynamic", profile, range);

        assertEquals(4242, capturedRequest().outputTokenCount());
        assertEquals(4242, impact.outputTokenCount());
        assertEquals(1.0, impact.requestCount(), 1e-9);
    }

    private EcologitsEstimationRequest capturedRequest()
    {
        ArgumentCaptor<EcologitsEstimationRequest> captor = ArgumentCaptor.forClass(EcologitsEstimationRequest.class);
        verify(this.ecologitsClient).estimate(captor.capture());
        return captor.getValue();
    }

    private static EcologitsEstimationResponse estimation()
    {
        return new EcologitsEstimationResponse(new EcologitsEstimationResponse.Impacts(
            metric("energy", "kWh"), metric("GWP", "kgCO2eq"), metric("ADPe", "kgSbeq"),
            metric("PE", "MJ"), metric("WCF", "L"), null, null));
    }

    private static EcologitsEstimationResponse.Metric metric(String name, String unit)
    {
        return new EcologitsEstimationResponse.Metric("impact", name, new EcologitsEstimationResponse.Metric.EcologitsRange(0.1, 0.2), unit);
    }
}
