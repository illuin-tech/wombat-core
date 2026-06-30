package tech.illuin.wombat.asset;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.illuin.wombat.k8s.ClusterProperties;
import tech.illuin.wombat.monitor.MonitoredResourceProperties;
import tech.illuin.wombat.monitor.MonitoredResources;
import tech.illuin.wombat.profile.Profile;
import tech.illuin.wombat.profile.ServerProfileEntity;
import tech.illuin.wombat.profile.ServerProfileRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class AssetService
{

    private static final Logger logger = LoggerFactory.getLogger(AssetService.class);

    private final MonitoredResources resources;
    private final ServerProfileRepository profileRepository;

    public AssetService(MonitoredResources resources, ServerProfileRepository profileRepository)
    {
        this.resources = resources;
        this.profileRepository = profileRepository;
    }

    public List<AssetConfig> all()
    {
        List<AssetConfig> result = new ArrayList<>();
        for (MonitoredResourceProperties resource : this.resources.resources())
        {
            if (!(resource instanceof ClusterProperties cluster)) continue;
            Optional<ServerProfileEntity> profile = this.profileRepository.findByIdOptional(cluster.profileId());
            if (profile.isEmpty())
            {
                logger.warn("Cluster {} references unknown profile {}; skipping", cluster.id(), cluster.profileId());
                continue;
            }
            result.add(new AssetConfig(cluster.name(), Profile.from(profile.get()), cluster));
        }
        return result;
    }

    public Optional<AssetConfig> byClusterId(String clusterId)
    {
        return all().stream().filter(a -> a.clusterProperties().id().equals(clusterId)).findFirst();
    }

    public List<AssetConfig> resolve(List<String> clusterIds)
    {
        List<AssetConfig> all = all();
        if (clusterIds == null || clusterIds.isEmpty()) return all;
        Set<String> wanted = new HashSet<>(clusterIds);
        return all.stream().filter(a -> wanted.contains(a.clusterProperties().id())).toList();
    }
}
