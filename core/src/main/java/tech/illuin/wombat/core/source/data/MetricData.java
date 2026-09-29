package tech.illuin.wombat.core.source.data;

import tech.illuin.wombat.core.asset.type.ServiceFamily;

public interface MetricData
{
    String serviceId();

    ServiceFamily serviceFamily();
}
