package tech.illuin.wombat.module.kubernetes_api.connector;

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.NodeBuilder;
import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceQuotaBuilder;
import io.fabric8.kubernetes.api.model.ResourceQuotaList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.asset.profile.inference.ClusterInventory;
import tech.illuin.wombat.core.asset.profile.inference.InventoryUnavailableException;
import tech.illuin.wombat.core.asset.profile.inference.ResourceAmount;
import tech.illuin.wombat.core.secret.MissingSecretException;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KubernetesClusterInventoryTest
{
    private static final double GI = 1024.0 * 1024.0 * 1024.0;

    private final KubernetesClient client = mock(KubernetesClient.class, RETURNS_DEEP_STUBS);
    private final KubernetesClusterInventory inventory = new KubernetesClusterInventory(() -> this.client);

    @Test
    void readsTheNodesProviderLabelsAndCapacity() throws InventoryUnavailableException
    {
        when(this.client.nodes().list()).thenReturn(new NodeList(null, List.of(new NodeBuilder()
            .withNewMetadata().withName("n1").withLabels(Map.of("node.kubernetes.io/instance-type", "m5.xlarge")).endMetadata()
            .withNewSpec().withProviderID("aws:///eu-west-3a/i-1").endSpec()
            .withNewStatus().withCapacity(Map.of("cpu", new Quantity("4"), "memory", new Quantity("16Gi"))).endStatus()
            .build()), null, null));

        assertEquals(
            List.of(new ClusterInventory.Node("n1", "aws:///eu-west-3a/i-1", Map.of("node.kubernetes.io/instance-type", "m5.xlarge"), new ResourceAmount(4, 16 * GI))),
            this.inventory.nodes()
        );
    }

    @Test
    void readsQuotasInBaseUnits() throws InventoryUnavailableException
    {
        when(namespaced(this.client.resourceQuotas(), "payments").list()).thenReturn(new ResourceQuotaList(null, List.of(new ResourceQuotaBuilder()
            .withNewMetadata().withName("compute").endMetadata()
            .withNewSpec().withHard(Map.of("requests.cpu", new Quantity("500m"), "requests.memory", new Quantity("2Gi"))).endSpec()
            .build()), null, null));

        assertEquals(
            List.of(new ClusterInventory.Quota("payments", "compute", Map.of("requests.cpu", 0.5, "requests.memory", 2 * GI))),
            this.inventory.quotas("payments")
        );
    }

    @Test
    void readsContainersAndSidecars() throws InventoryUnavailableException
    {
        when(namespaced(this.client.pods(), "payments").list()).thenReturn(new PodList(null, List.of(new PodBuilder()
            .withNewMetadata().withNamespace("payments").withName("api").endMetadata()
            .withNewSpec()
                .withContainers(new ContainerBuilder().withName("app")
                    .withNewResources().withRequests(Map.of("cpu", new Quantity("250m"))).withLimits(Map.of("memory", new Quantity("1Gi"))).endResources()
                    .build())
                .withInitContainers(new ContainerBuilder().withName("proxy").withRestartPolicy("Always").build())
            .endSpec()
            .withNewStatus().withPhase("Running").endStatus()
            .build()), null, null));

        ClusterInventory.Pod pod = this.inventory.pods(Set.of("payments")).getFirst();

        assertEquals("Running", pod.phase());
        assertEquals(new ClusterInventory.Container(new ResourceAmount(0.25, 0), new ResourceAmount(0, GI), false), pod.containers().getFirst());
        assertTrue(pod.initContainers().getFirst().sidecar());
    }

    /**
     * Deep stubs cannot follow {@code inNamespace}, whose return type is generic.
     */
    @SuppressWarnings("unchecked")
    private static <T, L> NonNamespaceOperation<T, L, Resource<T>> namespaced(MixedOperation<T, L, ?> operation, String namespace)
    {
        NonNamespaceOperation<T, L, Resource<T>> inNamespace = mock(NonNamespaceOperation.class);
        when(operation.inNamespace(namespace)).thenReturn((NonNamespaceOperation) inNamespace);
        return inNamespace;
    }

    @Test
    void aForbiddenReadIsUnavailable()
    {
        when(this.client.nodes().list()).thenThrow(new KubernetesClientException("nodes is forbidden", 403, null));

        var e = assertThrows(InventoryUnavailableException.class, this.inventory::nodes);

        assertEquals("forbidden to list nodes", e.getMessage());
    }

    @Test
    void aMissingKubeconfigIsUnavailable()
    {
        KubernetesClusterInventory unconfigured = new KubernetesClusterInventory(() -> {
            throw new MissingSecretException("kubeconfig");
        });

        assertThrows(InventoryUnavailableException.class, unconfigured::nodes);
        assertEquals(Optional.empty(), unconfigured.apiServerHost());
    }

    @Test
    void theClientIsOnlyBuiltOnTheFirstRead() throws Exception
    {
        when(this.client.getMasterUrl()).thenReturn(URI.create("https://abc.gr7.eu-west-3.eks.amazonaws.com:443/").toURL());
        KubernetesClusterInventory lazy = new KubernetesClusterInventory(() -> this.client);

        lazy.close();
        verify(this.client, never()).close();

        assertEquals(Optional.of("abc.gr7.eu-west-3.eks.amazonaws.com"), lazy.apiServerHost());
        lazy.close();
        verify(this.client).close();
    }
}
