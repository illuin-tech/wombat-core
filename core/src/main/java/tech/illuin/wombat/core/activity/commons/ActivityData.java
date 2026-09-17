package tech.illuin.wombat.core.activity.commons;

import tech.illuin.wombat.core.asset.ActivityRegime;
import tech.illuin.wombat.core.asset.ServiceFamily;

import java.util.Set;

public interface ActivityData
{
    ServiceFamily family();

    ActivityRegime regime();

    Set<String> serviceIds();

    TimeRange range();
}
