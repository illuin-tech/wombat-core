package tech.illuin.vigilantwombat.handler.footprint_resolver;

import tech.illuin.vigilantwombat.model.Footprint;

public interface FootprintResolver<I> {
    Footprint resolveFootprint(I impact, String service, Double share);
}
