package tech.illuin.wombat.module.kubernetes_api;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.secret.SecretKey;
import tech.illuin.wombat.core.source.Period;

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

    private static KubernetesAPIAsset asset(String kubeconfigKey)
    {
        return new KubernetesAPIAsset(
            AssetIdentity.of("k", "env", KubernetesAPIModule.TYPE),
            SecretKey.of(kubeconfigKey), null, Optional.empty(), Optional.empty(), Period.ZERO, null
        );
    }
}
