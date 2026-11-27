package tech.illuin.wombat.module.kubernetes_api.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tech.illuin.wombat.core.asset.profile.ServerProvider;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CloudPlacementTest
{
    @ParameterizedTest(name = "{0} is {1}")
    @CsvSource({
        "aws:///eu-west-3a/i-0123, aws",
        "gce://my-project/europe-west9-a/node-1, gcp",
        "azure:///subscriptions/abc/resourceGroups/rg/providers/Microsoft.Compute/virtualMachineScaleSets/pool/virtualMachines/0, azure",
        "scaleway://instance/fr-par-1/abc, scaleway",
    })
    void theProviderIdSchemeTellsTheProvider(String providerId, ServerProvider provider)
    {
        assertEquals(Optional.of(provider), CloudPlacement.provider(providerId, Map.of()));
    }

    @Test
    void openstackNodesAreOvhcloudOnesOnlyWithItsLabels()
    {
        assertEquals(Optional.empty(), CloudPlacement.provider("openstack:///abc", Map.of()));
        assertEquals(Optional.of(ServerProvider.ovhcloud), CloudPlacement.provider("openstack:///abc", Map.of("node.k8s.ovh/type", "standard")));
    }

    @Test
    void unknownSchemesTellNoProvider()
    {
        assertEquals(Optional.empty(), CloudPlacement.provider("kind://docker/kind/node", Map.of()));
        assertEquals(Optional.empty(), CloudPlacement.provider(null, Map.of()));
    }

    @ParameterizedTest(name = "{0} is {1}")
    @CsvSource({
        "ABC123.gr7.eu-west-3.eks.amazonaws.com, aws",
        "my-cluster-dns-abc.hcp.francecentral.azmk8s.io, azure",
        "my-cluster.privatelink.westeurope.azmk8s.io, azure",
        "abc.c1.gra9.k8s.ovh.net, ovhcloud",
        "abc.api.k8s.fr-par.scw.cloud, scaleway",
        "gke-abc.europe-west9.gke.goog, gcp",
    })
    void managedApiServersTellTheirProvider(String host, ServerProvider provider)
    {
        assertEquals(Optional.of(provider), CloudPlacement.apiServerProvider(host));
    }

    @Test
    void otherApiServersTellNothing()
    {
        assertEquals(Optional.empty(), CloudPlacement.apiServerProvider("34.155.12.1"));
        assertEquals(Optional.empty(), CloudPlacement.apiServerProvider("k8s.internal.example.com"));
    }
}
