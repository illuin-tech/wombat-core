package tech.illuin.wombat.monitor;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.asset.model.profile.DynamicLLMProfile;
import tech.illuin.wombat.ecologits.model.EcologitsEstimationRequest;
import tech.illuin.wombat.environment.ActiveEnvironments;
import tech.illuin.wombat.llm.LLMPrometheusProperties;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MonitorTest
{

    @Test
    void heartbeatSkipOfZeroOrOne_runsOnEveryHeartbeat()
    {
        RecordingHandler handler = new RecordingHandler();
        Monitor monitor = monitorFor(handler, prometheus("skip-0", 0), prometheus("skip-1", 1));

        beat(monitor, 3);

        assertEquals(3, handler.calls.getOrDefault("skip-0", 0));
        assertEquals(3, handler.calls.getOrDefault("skip-1", 0));
    }

    @Test
    void heartbeatSkipOfN_runsOnEveryNthHeartbeat()
    {
        RecordingHandler handler = new RecordingHandler();
        Monitor monitor = monitorFor(handler, prometheus("skip-3", 3));

        beat(monitor, 7); // beats 1..7 -> fires on 3 and 6

        assertEquals(2, handler.calls.getOrDefault("skip-3", 0));
    }

    @Test
    void heartbeatSkipIsIndependentPerAsset()
    {
        // Regression: a shared per-asset-evaluation counter made each asset's cadence depend on the
        // others, so an asset could fire on every heartbeat or never. The cadence must be per-asset.
        RecordingHandler handler = new RecordingHandler();
        Monitor monitor = monitorFor(handler, prometheus("every-2", 2), prometheus("every-3", 3));

        beat(monitor, 6);

        assertEquals(3, handler.calls.getOrDefault("every-2", 0)); // beats 2, 4, 6
        assertEquals(2, handler.calls.getOrDefault("every-3", 0)); // beats 3, 6
    }

    private static Monitor monitorFor(RecordingHandler handler, LLMPrometheusProperties... assets)
    {
        ActiveEnvironments activeEnvironments = mock(ActiveEnvironments.class);
        when(activeEnvironments.allAssets()).thenReturn(List.of(assets));
        return new Monitor(handler, activeEnvironments);
    }

    private static void beat(Monitor monitor, int times)
    {
        for (int i = 0; i < times; i++)
            monitor.monitor();
    }

    private static LLMPrometheusProperties prometheus(String id, int heartbeatSkip)
    {
        return new LLMPrometheusProperties(id, id, "http://prometheus", null, null, null, heartbeatSkip,
            new DynamicLLMProfile(EcologitsEstimationRequest.Provider.mistralai, "m", "FRA", new DynamicLLMProfile.DynamicProfile("q")));
    }

    private static final class RecordingHandler implements MonitoredAssetHandler<LLMPrometheusProperties>
    {
        private final Map<String, Integer> calls = new LinkedHashMap<>();

        @Override
        public boolean accept(AssetProperties config)
        {
            return true;
        }

        @Override
        public void handle(Instant instant, LLMPrometheusProperties config)
        {
            this.calls.merge(config.id(), 1, Integer::sum);
        }

        @Override
        public int heartbeatSkip(LLMPrometheusProperties config)
        {
            return config.heartbeatSkip();
        }
    }
}
