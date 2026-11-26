package tech.illuin.wombat.module.kubernetes_api;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.profile.AssetProfile;
import tech.illuin.wombat.core.asset.profile.BareServerProfile;
import tech.illuin.wombat.core.asset.profile.Lifetime;
import tech.illuin.wombat.core.asset.profile.ServerProvider;
import tech.illuin.wombat.core.asset.profile.inference.InferredBareServerProfile;
import tech.illuin.wombat.core.asset.profile.inference.InferredCloudServerProfile;
import tech.illuin.wombat.core.asset.profile.inference.ProfileOrigin;
import tech.illuin.wombat.core.asset.profile.inference.ResourceAmount;
import tech.illuin.wombat.core.secret.SecretKey;
import tech.illuin.wombat.core.source.Period;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KubernetesAPIAssetTest
{
    @Test
    void anAssetDeclaresTheKubeconfigItNeeds()
    {
        assertEquals(Set.of("KUBECONFIG_PROD"), asset("KUBECONFIG_PROD").requiredSecretKeys());
    }

    @Test
    void anAssetWithoutAKubeconfigIsRejected()
    {
        // Assets persisted with a kubeconfig path load without a key: surfacing it at startup beats a failing source.
        KubernetesAPIAsset asset = asset(null);
        IllegalStateException e = assertThrows(IllegalStateException.class, asset::requiredSecretKeys);

        assertTrue(e.getMessage().contains("kubeconfig-key"), e.getMessage());
    }

    @Test
    void omittedNamespacesMeanEveryNamespace()
    {
        assertEquals(Set.of(), asset("KUBECONFIG_PROD").namespaces());
    }

    @Test
    void anOmittedProfileIsInferred()
    {
        assertEquals(KubernetesAPIInferredProfile.DEFAULT, asset("KUBECONFIG_PROD").profile());
    }

    @Test
    void anInferredCloudProfileBecomesADeclaredOne()
    {
        KubernetesAPIAsset asset = asset("KUBECONFIG_PROD");
        KubernetesAPIAsset inferred = asset.withProfile(new InferredCloudServerProfile(ServerProvider.aws, "m5.xlarge", Country.WORLD, Lifetime.ofYears(5), 3, ProfileOrigin.NODES, List.of()));

        assertEquals(new KubernetesAPICloudProfile(ServerProvider.aws, "m5.xlarge", Country.WORLD, Lifetime.ofYears(5)), inferred.profile());
        assertEquals(asset.identity(), inferred.identity());
        assertEquals(asset.kubeconfigKey(), inferred.kubeconfigKey());
    }

    @Test
    void anInferredBareServerProfileBecomesADeclaredOne()
    {
        var profile = InferredBareServerProfile.of(new ResourceAmount(4, 16.0 * 1024 * 1024 * 1024), Country.FRANCE, Lifetime.ofYears(6), 1, ProfileOrigin.RESOURCE_QUOTAS, List.of());

        assertEquals(
            new KubernetesAPIBareServerProfile(Country.FRANCE, Lifetime.ofYears(6), new BareServerProfile.CPU(1, 2, 2, null), new BareServerProfile.RAM(1, 16), null, null, null),
            asset("KUBECONFIG_PROD").withProfile(profile).profile()
        );
    }

    @Test
    void otherProfilesAreRejected()
    {
        assertThrows(IllegalArgumentException.class, () -> asset("KUBECONFIG_PROD").withProfile(new AssetProfile() {}));
    }

    private static KubernetesAPIAsset asset(String kubeconfigKey)
    {
        return new KubernetesAPIAsset(
            AssetIdentity.of("k", "env", KubernetesAPIModule.TYPE),
            SecretKey.of(kubeconfigKey), null, Optional.empty(), Optional.empty(), Period.ZERO, null
        );
    }
}
