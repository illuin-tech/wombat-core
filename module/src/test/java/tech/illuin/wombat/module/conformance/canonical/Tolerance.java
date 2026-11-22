package tech.illuin.wombat.module.conformance.canonical;

/** The absolute deviation allowed around an expected figure, on top of the provider's own rounding of it. */
@FunctionalInterface
public interface Tolerance
{
    double of(double expected, double rounding);

    static Tolerance relative(double ratio)
    {
        return (expected, rounding) -> rounding + Math.abs(expected) * ratio;
    }
}
