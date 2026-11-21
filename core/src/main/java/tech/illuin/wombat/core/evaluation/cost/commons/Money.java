package tech.illuin.wombat.core.evaluation.cost.commons;

import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An amount of money, e.g. what an asset costs over a range. A {@code double}, as {@code Amount} is: costs are estimates,
 * scaled by ratios.
 */
public record Money(
    double value,
    Currency currency
) {
    public Money
    {
        Objects.requireNonNull(currency, "currency cannot be null");
    }

    public static Money of(double value, String currencyCode)
    {
        return new Money(value, Currency.getInstance(currencyCode));
    }

    public Money multiply(double factor)
    {
        return new Money(this.value * factor, this.currency);
    }

    /**
     * Sums the amounts per currency, never converting one into another.
     *
     * @return one amount per currency, in the order the currencies are met; empty for no amount
     */
    public static List<Money> sum(List<Money> amounts)
    {
        Map<Currency, Double> totals = new LinkedHashMap<>();
        for (Money amount : amounts)
            totals.merge(amount.currency(), amount.value(), Double::sum);
        return totals.entrySet().stream()
            .map(total -> new Money(total.getValue(), total.getKey()))
            .toList();
    }
}
