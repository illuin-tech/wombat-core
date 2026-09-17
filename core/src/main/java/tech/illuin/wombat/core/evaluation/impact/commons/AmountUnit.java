package tech.illuin.wombat.core.evaluation.impact.commons;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum AmountUnit
{
    mj("MJ"),
    kwh("KWh"),
    kg_co2eq("kgCO2eq"),
    kg_sbeq("kgSbeq"),
    ;

    private final String symbol;

    private static final Map<String, AmountUnit> symbolToUnit;

    static {
        symbolToUnit = new HashMap<>();
        for (AmountUnit unit : values())
            symbolToUnit.put(unit.symbol(), unit);
    }

    AmountUnit(String symbol)
    {
        this.symbol = symbol;
    }

    public String symbol()
    {
        return symbol;
    }

    public static Optional<AmountUnit> forSymbol(String symbol)
    {
        return Optional.ofNullable(symbolToUnit.get(symbol));
    }
}
