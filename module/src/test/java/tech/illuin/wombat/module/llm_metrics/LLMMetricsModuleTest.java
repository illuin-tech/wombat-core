package tech.illuin.wombat.module.llm_metrics;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.evaluation.impact.kubernetes.KubernetesMetricResolver;
import tech.illuin.wombat.core.evaluation.impact.llm.LLMMetricResolver;
import tech.illuin.wombat.core.module.MetricResolvers;
import tech.illuin.wombat.core.module.WombatServices;
import tech.illuin.wombat.core.secret.SecretResolver;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LLMMetricsModuleTest
{
    private final LLMMetricsModule module = new LLMMetricsModule();

    @Test
    void servesTheLLMFamilyWithoutAnAssetType()
    {
        assertEquals(ServiceFamily.LLM, this.module.type().family());
        assertTrue(this.module.assetClass().isEmpty());
        assertTrue(this.module.source().isEmpty());
    }

    @Test
    void resolvesActivityFromTheMetricStoreInTheServices() throws Exception
    {
        LLMMetricResolver metrics = mock(LLMMetricResolver.class);
        WombatServices services = new WombatServices(SecretResolver.standard(), new MetricResolvers(mock(KubernetesMetricResolver.class), metrics), List.of());
        Asset asset = mock(Asset.class);
        when(asset.identity()).thenReturn(AssetIdentity.of("llm", "env", "tech.illuin.wombat-module.llm-prometheus"));

        WombatActivityResolver resolver = this.module.activityResolver().orElseThrow().create(services, asset);

        assertTrue(resolver.resolve(asset, new TimeRange(Instant.EPOCH, Instant.EPOCH.plusSeconds(3_600)), null).isEmpty());
        verify(metrics).serviceActivitiesPerBucket(any(), eq("llm"));
    }
}
