package tech.illuin.wombat.module.kubernetes_api.source;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.ContainerMetrics;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetricsList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.PodMetricOperation;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.secret.SecretKey;
import tech.illuin.wombat.core.source.Period;
import tech.illuin.wombat.core.source.WombatSourceException;
import tech.illuin.wombat.core.source.data.KubernetesData;
import tech.illuin.wombat.core.source.data.MetricData;
import tech.illuin.wombat.module.kubernetes_api.KubernetesAPIAsset;
import tech.illuin.wombat.module.kubernetes_api.KubernetesAPIModule;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KubernetesAPISourceTest
{
    private final KubernetesClient client = mock(KubernetesClient.class, RETURNS_DEEP_STUBS);
    private final PodMetricOperation pods = this.client.top().pods();

    @Test
    void withoutNamespacesEveryNamespaceIsPolled() throws WombatSourceException
    {
        when(this.pods.metrics()).thenReturn(list(pod("payments", "api"), pod("analytics", "batch")));

        List<MetricData> data = this.source(Set.of());

        assertEquals(Set.of("payments/api", "analytics/batch"), locations(data));
        verify(this.pods, never()).metrics(anyString());
    }

    @Test
    void withNamespacesOnlyThoseArePolled() throws WombatSourceException
    {
        when(this.pods.metrics("payments")).thenReturn(list(pod("payments", "api")));
        when(this.pods.metrics("analytics")).thenReturn(list(pod("analytics", "batch")));

        List<MetricData> data = this.source(Set.of("payments", "analytics"));

        assertEquals(Set.of("payments/api", "analytics/batch"), locations(data));
        verify(this.pods, never()).metrics();
    }

    @Test
    void aForbiddenClusterWidePollPointsAtTheWhitelist()
    {
        when(this.pods.metrics()).thenThrow(new KubernetesClientException("pods.metrics.k8s.io is forbidden", 403, null));

        WombatSourceException e = assertThrows(WombatSourceException.class, () -> this.source(Set.of()));

        assertTrue(e.getMessage().contains("restrict namespaces"), e.getMessage());
        assertInstanceOf(KubernetesClientException.class, e.getCause());
    }

    @Test
    void aFailingNamespaceIsSkipped() throws WombatSourceException
    {
        when(this.pods.metrics("payments")).thenThrow(new KubernetesClientException("pods.metrics.k8s.io is forbidden", 403, null));
        when(this.pods.metrics("analytics")).thenReturn(list(pod("analytics", "batch")));

        List<MetricData> data = this.source(Set.of("payments", "analytics"));

        assertEquals(Set.of("analytics/batch"), locations(data));
    }

    private List<MetricData> source(Set<String> namespaces) throws WombatSourceException
    {
        return new KubernetesAPISource(this.client).source(Instant.EPOCH, Instant.EPOCH, asset(namespaces));
    }

    private static Set<String> locations(List<MetricData> data)
    {
        return data.stream()
            .map(KubernetesData.class::cast)
            .map(d -> d.namespace() + "/" + d.pod())
            .collect(Collectors.toSet());
    }

    private static PodMetricsList list(PodMetrics... pods)
    {
        PodMetricsList list = new PodMetricsList();
        list.setItems(List.of(pods));
        return list;
    }

    private static PodMetrics pod(String namespace, String name)
    {
        ContainerMetrics container = new ContainerMetrics();
        container.setName("main");
        container.setUsage(Map.of("cpu", new Quantity("0.5"), "memory", new Quantity("1Mi")));

        PodMetrics pod = new PodMetrics();
        pod.setMetadata(new ObjectMetaBuilder().withNamespace(namespace).withName(name).build());
        pod.setContainers(List.of(container));
        return pod;
    }

    private static KubernetesAPIAsset asset(Set<String> namespaces)
    {
        return new KubernetesAPIAsset(
            AssetIdentity.of("k", "env", KubernetesAPIModule.TYPE),
            SecretKey.of("KUBECONFIG_TEST"), namespaces, Optional.empty(), Optional.empty(), Period.ZERO, null
        );
    }
}
