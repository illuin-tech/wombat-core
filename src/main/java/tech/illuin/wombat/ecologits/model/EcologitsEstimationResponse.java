package tech.illuin.wombat.ecologits.model;

import com.fasterxml.jackson.annotation.JsonCreator;


public record EcologitsEstimationResponse(
    Impacts impacts
)
{
    public record Impacts(
        Metric energy,
        Metric gwp,
        Metric adpe,
        Metric pe,
        Metric wcf,
        Usage usage,
        Embodied embodied
    ) {}

    public record Metric(
        String type,
        String name,
        EcologitsRange value,
        String unit
    ) {
        public record EcologitsRange(
            double min,
            double max
        ) {
            @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
            public EcologitsRange {}

            @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
            public static EcologitsRange of(double value)
            {
                return new EcologitsRange(value, value);
            }
        }
    }

    public record Usage(
        String type,
        String name,
        Metric energy,
        Metric gwp,
        Metric adpe,
        Metric pe,
        Metric wcf
    ) {}

    public record Embodied(
        String type,
        String name,
        Metric gwp,
        Metric adpe,
        Metric pe
    ) {}
}
