package tech.illuin.wombat.core.evaluation.cost;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.evaluation.cost.commons.Money;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTest
{
    @Test
    void multiply_keepsTheCurrency()
    {
        assertEquals(Money.of(36.5, "EUR"), Money.of(3, "EUR").multiply(365.0 / 30));
    }

    @Test
    void sum_addsUpEachCurrencyApart_inTheOrderMet()
    {
        List<Money> sum = Money.sum(List.of(Money.of(1, "USD"), Money.of(2, "EUR"), Money.of(3, "USD")));

        assertEquals(List.of(Money.of(4, "USD"), Money.of(2, "EUR")), sum);
    }

    @Test
    void sum_ofNothing_isEmpty()
    {
        assertTrue(Money.sum(List.of()).isEmpty());
    }
}
