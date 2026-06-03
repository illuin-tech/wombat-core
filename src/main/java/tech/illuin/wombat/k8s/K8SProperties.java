package tech.illuin.wombat.k8s;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

public interface K8SProperties
{
    List<ClusterProperties> clusters();

    interface ClusterProperties
    {
        String id();

        String configPath();

        String namespace();

        Optional<String> context();

        Optional<Duration> readTimeout();
    }

    interface Duration
    {
        int duration();

        ChronoUnit unit();
    }
}

