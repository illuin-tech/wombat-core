package tech.illuin.wombat.module.conformance.canonical;

import tech.illuin.wombat.core.evaluation.impact.commons.Footprint;

/**
 * What a provider returns for a canonical request, in {@code double}, on Wombat's three criteria.
 */
public record CanonicalImpacts(
    Phases gwp,
    Phases pe,
    Phases adp
) {
    /** A Wombat footprint in the same shape, to compare criterion by criterion. */
    public static CanonicalImpacts of(Footprint footprint)
    {
        return new CanonicalImpacts(Phases.of(footprint.gwp()), Phases.of(footprint.pe()), Phases.of(footprint.adp()));
    }

    public CanonicalImpacts plus(CanonicalImpacts other)
    {
        return new CanonicalImpacts(this.gwp.plus(other.gwp), this.pe.plus(other.pe), this.adp.plus(other.adp));
    }

    public CanonicalImpacts multiply(double factor)
    {
        return new CanonicalImpacts(this.gwp.multiply(factor), this.pe.multiply(factor), this.adp.multiply(factor));
    }

    /**
     * One criterion, split by phase. The rounding is how far the provider's rounding may have moved each figure, scaled
     * and summed along with it: zero when the provider does not round.
     */
    public record Phases(
        String unit,
        double embedded,
        double use,
        double embeddedRounding,
        double useRounding
    ) {
        public Phases(String unit, double embedded, double use)
        {
            this(unit, embedded, use, 0.0, 0.0);
        }

        public static Phases of(Footprint.FootprintImpact impact)
        {
            return new Phases(impact.unit(), impact.embedded().value(), impact.use().value());
        }

        public Phases plus(Phases other)
        {
            return new Phases(
                this.unit,
                this.embedded + other.embedded,
                this.use + other.use,
                this.embeddedRounding + other.embeddedRounding,
                this.useRounding + other.useRounding
            );
        }

        public Phases multiply(double factor)
        {
            return new Phases(
                this.unit,
                this.embedded * factor,
                this.use * factor,
                this.embeddedRounding * Math.abs(factor),
                this.useRounding * Math.abs(factor)
            );
        }
    }
}
