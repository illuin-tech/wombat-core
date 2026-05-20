package tech.illuin.vigilantwombat.handler.impact_provider;

import tech.illuin.vigilantwombat.handler.model.ProviderConfig;
import tech.illuin.vigilantwombat.persistence.model.TimeRange;

public interface ImpactProvider<R> {
    R resolveImpact(ProviderConfig config, TimeRange timeRange, double load);
}
