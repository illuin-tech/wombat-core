package tech.illuin.wombat.handler.impact_provider;

import tech.illuin.wombat.handler.model.ProviderConfig;
import tech.illuin.wombat.persistence.model.TimeRange;

public interface ImpactProvider<R> {
    R resolveImpact(ProviderConfig config, TimeRange timeRange, double load);
}
