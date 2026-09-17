package tech.illuin.wombat.core.source.data;

import tech.illuin.wombat.core.asset.ServiceFamily;

public interface MetricData
{
    String serviceId();

    String assetId();

    String environmentId();

    ServiceFamily serviceFamily();
}
