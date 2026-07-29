package tech.illuin.wombat.environment.model;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.asset.model.profile.DynamicLLMProfile;
import tech.illuin.wombat.asset.model.profile.InfrastructureProfile;
import tech.illuin.wombat.asset.model.profile.StaticLLMProfile;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactRequest;
import tech.illuin.wombat.ecologits.model.EcologitsEstimationRequest;
import tech.illuin.wombat.environment.persistence.AssetEntity;
import tech.illuin.wombat.kubernetes.KubernetesAPIAssetProperties;
import tech.illuin.wombat.llm.LLMPrometheusProperties;
import tech.illuin.wombat.llm.LLMStaticProperties;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AssetInfoTest
{

    @Test
    void exposesKubernetesConnectionProfileAndTimestamps()
    {
        InfrastructureProfile profile = new InfrastructureProfile(BoaviztaInstanceImpactRequest.Provider.aws, "c5.large", "FRA", 43800);
        AssetEntity entity = stamp(AssetEntity.from("env", new KubernetesAPIAssetProperties(
            "k", "K", "/kube/config", "ns", Optional.empty(), Optional.empty(), 0, profile)));

        AssetInfo info = AssetInfo.from(entity);

        assertEquals("k", info.id());
        assertEquals("ns", info.namespace());
        assertEquals(profile, info.profile());
        assertEquals(Instant.ofEpochMilli(1), info.createdAt());
        assertEquals(Instant.ofEpochMilli(2), info.updatedAt());
        assertNull(info.deletedAt());
    }

    @Test
    void exposesPrometheusUrlAndUsernameButNeverThePassword()
    {
        DynamicLLMProfile profile = new DynamicLLMProfile(EcologitsEstimationRequest.Provider.mistralai, "m", "FRA",
            new DynamicLLMProfile.DynamicProfile("q"));
        AssetEntity entity = stamp(AssetEntity.from("env", new LLMPrometheusProperties(
            "p", "P", "http://prometheus", null, "user", "secret", 5, profile)));

        AssetInfo info = AssetInfo.from(entity);

        assertEquals("http://prometheus", info.prometheusUrl());
        assertEquals("user", info.username());
        assertEquals(profile, info.profile());
        // AssetInfo has no password component at all, so the secret can never leak through the API.
    }

    @Test
    void exposesStaticProfileWithoutConnectionFields()
    {
        StaticLLMProfile profile = new StaticLLMProfile(EcologitsEstimationRequest.Provider.mistralai, "m", "FRA",
            new StaticLLMProfile.RequestProfile(500, 1000));
        AssetEntity entity = stamp(AssetEntity.from("env", new LLMStaticProperties("s", "S", profile)));

        AssetInfo info = AssetInfo.from(entity);

        assertEquals(profile, info.profile());
        assertNull(info.prometheusUrl());
        assertNull(info.namespace());
    }

    private static AssetEntity stamp(AssetEntity entity)
    {
        entity.uuid = "uuid";
        entity.createdAt = Instant.ofEpochMilli(1);
        entity.updatedAt = Instant.ofEpochMilli(2);
        return entity;
    }
}
