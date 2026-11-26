package tech.illuin.wombat.module.kubernetes_api.profile;

import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.profile.BareServerProfile;
import tech.illuin.wombat.core.asset.profile.InferredProfile;
import tech.illuin.wombat.core.asset.profile.Lifetime;
import tech.illuin.wombat.core.asset.profile.ProfileOrigin;

import java.util.List;

/**
 * A server sized after an amount of resources, its disks and GPUs being unknown.
 *
 * @param nodeCount the number of nodes of this size, 1 when sized after a slice of the cluster
 */
public record InferredBareServerProfile(
    Country location,
    Lifetime lifespan,
    CPU cpu,
    RAM ram,
    int nodeCount,
    ProfileOrigin origin,
    List<String> warnings
) implements BareServerProfile, InferredProfile
{
    public InferredBareServerProfile
    {
        warnings = warnings != null ? List.copyOf(warnings) : List.of();
    }

    private static final double BYTES_PER_GB = 1024.0 * 1024.0 * 1024.0;

    /**
     * Sizes a server after {@code resources}, rounded up to whole vCPUs and GB. An even number of vCPUs is read as
     * hyper-threaded cores, an odd one as single-threaded ones.
     */
    public static InferredBareServerProfile of(ResourceAmount resources, Country location, Lifetime lifespan, int nodeCount, ProfileOrigin origin, List<String> warnings)
    {
        int vcpu = Math.max(1, (int) Math.ceil(resources.cpuCores()));
        int memory = Math.max(1, (int) Math.ceil(resources.memoryBytes() / BYTES_PER_GB));
        CPU cpu = vcpu % 2 == 0 ? new CPU(1, vcpu / 2, 2, null) : new CPU(1, vcpu, 1, null);
        return new InferredBareServerProfile(location, lifespan, cpu, new RAM(1, memory), nodeCount, origin, warnings);
    }

    @Override
    public Disk ssd()
    {
        return null;
    }

    @Override
    public Disk hdd()
    {
        return null;
    }

    @Override
    public GPU gpu()
    {
        return null;
    }
}
