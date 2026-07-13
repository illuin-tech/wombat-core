package tech.illuin.wombat.asset;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.asset.model.Asset;
import tech.illuin.wombat.asset.model.KubernetesAsset;
import tech.illuin.wombat.asset.model.LLMAsset;
import tech.illuin.wombat.environment.ActiveEnvironments;
import tech.illuin.wombat.kubernetes.KubernetesAssetProperties;
import tech.illuin.wombat.llm.LLMProperties;
import tech.illuin.wombat.monitor.Environment;
import tech.illuin.wombat.monitor.AssetProperties;
import tech.illuin.wombat.profile.model.InfrastructureProfile;
import tech.illuin.wombat.profile.model.LLMProfile;
import tech.illuin.wombat.profile.model.ProfileType;
import tech.illuin.wombat.profile.persistence.ProfileEntity;
import tech.illuin.wombat.profile.persistence.ProfileRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class AssetService
{

    private static final Logger logger = LoggerFactory.getLogger(AssetService.class);

    private final ActiveEnvironments activeEnvironments;
    private final ProfileRepository profileRepository;

    public AssetService(ActiveEnvironments activeEnvironments, ProfileRepository profileRepository)
    {
        this.activeEnvironments = activeEnvironments;
        this.profileRepository = profileRepository;
    }

    public List<Asset> all()
    {
        List<Asset> result = new ArrayList<>();
        for (Map.Entry<String, Environment> entry : this.activeEnvironments.environments().entrySet())
        {
            String environmentId = entry.getKey();
            Environment environment = entry.getValue();
            for (AssetProperties asset : environment.assets())
            {
                switch (asset)
                {
                    case KubernetesAssetProperties cluster ->
                        this.findProfile(cluster.id(), cluster.profileId(), ProfileType.INFRASTRUCTURE)
                            .map(profile -> (Asset) new KubernetesAsset(environmentId, environment.name(), cluster.name(), InfrastructureProfile.from(profile), cluster))
                            .ifPresent(result::add);
                    case LLMProperties llm ->
                        this.findProfile(llm.id(), llm.profileId(), ProfileType.LLM)
                            .map(profile -> (Asset) new LLMAsset(environmentId, environment.name(), llm.name(), LLMProfile.from(profile), llm))
                            .ifPresent(result::add);
                    default -> logger.warn("Asset {} has unsupported type {}; skipping", asset.id(), asset.type());
                }
            }
        }
        return result;
    }

    private Optional<ProfileEntity> findProfile(String assetId, String profileId, ProfileType expectedType)
    {
        Optional<ProfileEntity> profile = this.profileRepository.findByIdOptional(profileId);
        if (profile.isEmpty())
        {
            logger.warn("Asset {} references unknown profile {}; skipping", assetId, profileId);
            return Optional.empty();
        }
        if (profile.get().type != expectedType)
        {
            logger.warn("Asset {} references profile {} of type {} (expected {}); skipping", assetId, profileId, profile.get().type, expectedType);
            return Optional.empty();
        }
        return profile;
    }

    public Map<String, Environment> environments()
    {
        return this.activeEnvironments.environments();
    }

    public Environment getEnvironmentProperties(String envId)
    {
        return this.activeEnvironments.environments().get(envId);
    }

    public List<Asset> resolve(String environmentId, List<String> assetIds)
    {
        List<Asset> assets = all();
        if (environmentId != null && !environmentId.isBlank())
            assets = assets.stream().filter(a -> a.environmentId().equals(environmentId)).toList();
        if (assetIds == null || assetIds.isEmpty()) return assets;
        Set<String> wanted = new HashSet<>(assetIds);
        return assets.stream().filter(a -> wanted.contains(a.properties().id())).toList();
    }
}
