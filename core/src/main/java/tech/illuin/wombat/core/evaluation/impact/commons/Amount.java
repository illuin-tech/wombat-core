package tech.illuin.wombat.core.evaluation.impact.commons;

public record Amount(
    double value,
    AmountUnit unit
) {
    public Amount multiply(double factor)
    {
        return new Amount(this.value * factor, this.unit);
    }
}
