package tech.illuin.wombat.handler.impact_provider;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tech.illuin.wombat.boavizta.BoaviztaClient;
import tech.illuin.wombat.boavizta.BoaviztaTestData;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactRequest;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactResponse;
import tech.illuin.wombat.handler.impl.impact_provider.BoaviztaImpactProvider;
import tech.illuin.wombat.handler.model.BoaviztaKubernetesConfig;
import tech.illuin.wombat.persistence.model.TimeRange;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BoaviztaImpactProviderTest
{

    private static final int VCPU = 10;
    private static final TimeRange RANGE = new TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1000));

    private final BoaviztaClient client = Mockito.mock(BoaviztaClient.class);

    private BoaviztaImpactProvider provider()
    {
        when(this.client.getInstanceConfig(any(), any())).thenReturn(BoaviztaTestData.fakeInstanceConfig(VCPU));
        when(this.client.getInstanceImpact(anyBoolean(), anyInt(), any(), any())).thenAnswer(invocation -> {
            BoaviztaInstanceImpactRequest request = invocation.getArgument(3);
            double loadPercentage = request.usage().timeWorkload().getFirst().loadPercentage();
            BoaviztaInstanceImpactResponse.Impact.ImpactItem item =
                new BoaviztaInstanceImpactResponse.Impact.ImpactItem((float) loadPercentage, 0f, 0f, List.of());
            return new BoaviztaInstanceImpactResponse(
                Map.of("gwp", new BoaviztaInstanceImpactResponse.Impact("u", "d", item, item)), 0);
        });
        return new BoaviztaImpactProvider(this.client);
    }

    private static BoaviztaKubernetesConfig config()
    {
        return new BoaviztaKubernetesConfig(
            BoaviztaInstanceImpactRequest.Provider.aws, "c5.large", "FRA", 35040, List.of());
    }

    private static double nanocores(double cores)
    {
        return cores * 1_000_000_000.0;
    }

    @Test
    void resolveImpact_loadWithinOneNode_singleRequestAtThatLoad()
    {
        BoaviztaImpactProvider provider = provider();

        BoaviztaInstanceImpactResponse response = provider.resolveImpact(config(), RANGE, nanocores(5));

        assertEquals(1, response.nodeCount());
        assertEquals(50.0f, response.impacts().get("gwp").use().value(), 0.001);
        verify(this.client, times(1)).getInstanceImpact(anyBoolean(), anyInt(), any(), any());
    }

    @Test
    void resolveImpact_loadAboveOneNode_scalesFullNodesAndAddsRemainder()
    {
        BoaviztaImpactProvider provider = provider();

        BoaviztaInstanceImpactResponse response = provider.resolveImpact(config(), RANGE, nanocores(32));

        assertEquals(4, response.nodeCount());
        assertEquals(3 * 100.0f + 20.0f, response.impacts().get("gwp").embedded().value(), 0.01);
        assertEquals(3 * 100.0f + 20.0f, response.impacts().get("gwp").use().value(), 0.01);
        verify(this.client, times(2)).getInstanceImpact(anyBoolean(), anyInt(), any(), any());
    }

    @Test
    void resolveImpact_loadExactMultiple_noRemainderRequest()
    {
        BoaviztaImpactProvider provider = provider();

        BoaviztaInstanceImpactResponse response = provider.resolveImpact(config(), RANGE, nanocores(30));

        assertEquals(3, response.nodeCount());
        assertEquals(300.0f, response.impacts().get("gwp").use().value(), 0.01);
        verify(this.client, times(1)).getInstanceImpact(anyBoolean(), anyInt(), any(), any());
    }
}
