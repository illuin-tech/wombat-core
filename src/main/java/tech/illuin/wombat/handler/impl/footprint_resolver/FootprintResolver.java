package tech.illuin.wombat.handler.impl.footprint_resolver;

import tech.illuin.wombat.model.Footprint;
import tech.illuin.wombat.persistence.model.TimeRange;

public interface FootprintResolver<I>
{

    Footprint resolveFootprint(I impact, String service, Double share, TimeRange timeRange, int lifespanHours);
}
