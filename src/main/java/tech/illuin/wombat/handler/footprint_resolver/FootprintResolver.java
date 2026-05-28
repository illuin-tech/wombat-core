package tech.illuin.wombat.handler.footprint_resolver;

import tech.illuin.wombat.model.Footprint;

public interface FootprintResolver<I>
{
    Footprint resolveFootprint(I impact, String service, Double share);
}
