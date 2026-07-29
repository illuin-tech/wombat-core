package tech.illuin.wombat.asset;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.asset.model.Asset;
import tech.illuin.wombat.asset.model.KubernetesAPIAsset;
import tech.illuin.wombat.asset.model.LLMPrometheusAsset;
import tech.illuin.wombat.asset.model.LLMStaticAsset;
import tech.illuin.wombat.environment.ActiveEnvironments;
import tech.illuin.wombat.kubernetes.KubernetesAPIAssetProperties;
import tech.illuin.wombat.llm.LLMPrometheusProperties;
import tech.illuin.wombat.llm.LLMStaticProperties;
import tech.illuin.wombat.monitor.AssetProperties;
import tech.illuin.wombat.monitor.Environment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class AssetService
{

    private static final Logger logger = LoggerFactory.getLogger(AssetService.class);

    private final ActiveEnvironments activeEnvironments;

    public AssetService(ActiveEnvironments activeEnvironments)
    {
        this.activeEnvironments = activeEnvironments;
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
                    case KubernetesAPIAssetProperties cluster ->
                        result.add(new KubernetesAPIAsset(environmentId, environment.name(), cluster.name(), cluster.profile(), cluster));
                    case LLMStaticProperties llm ->
                        result.add(new LLMStaticAsset(environmentId, environment.name(), llm.name(), llm.profile(), llm));
                    case LLMPrometheusProperties llm ->
                        result.add(new LLMPrometheusAsset(environmentId, environment.name(), llm.name(), llm.profile(), llm));
                    default -> logger.warn("Asset {} has unsupported type {}; skipping", asset.id(), asset.type());
                }
            }
        }
        return result;
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
