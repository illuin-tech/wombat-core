package tech.illuin.wombat.module.conformance;

import org.junit.jupiter.api.function.Executable;
import tech.illuin.wombat.core.activity.WombatActivityException;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.evaluation.WombatEvaluationException;
import tech.illuin.wombat.module.conformance.canonical.CanonicalImpacts;
import tech.illuin.wombat.module.conformance.canonical.Tolerance;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class ConformanceAssertions
{
    private ConformanceAssertions() {}

    /** Wombat's impacts of {@code asset} over {@code range}, within the engine's tolerance of what the engine itself gives. */
    static void assertConforms(ConformanceCore core, Asset asset, TimeRange range) throws WombatActivityException, WombatEvaluationException
    {
        assertMatches(core.reference(asset, range), core.total(asset, range), core.engine().tolerance());
    }

    /** Every criterion and phase of {@code actual} within {@code tolerance} of {@code expected}. */
    static void assertMatches(CanonicalImpacts expected, CanonicalImpacts actual, Tolerance tolerance)
    {
        assertAll(executables(expected, actual, tolerance, "").stream());
    }

    /** The assertions of {@link #assertMatches}, to group several comparisons into one {@code assertAll}. */
    static List<Executable> executables(CanonicalImpacts expected, CanonicalImpacts actual, Tolerance tolerance, String context)
    {
        return Stream.of(
            phases("gwp", expected.gwp(), actual.gwp(), tolerance, context),
            phases("pe", expected.pe(), actual.pe(), tolerance, context),
            phases("adp", expected.adp(), actual.adp(), tolerance, context)
        ).flatMap(List::stream).toList();
    }

    private static List<Executable> phases(String criterion, CanonicalImpacts.Phases expected, CanonicalImpacts.Phases actual, Tolerance tolerance, String context)
    {
        String prefix = context.isEmpty() ? criterion : context + ", " + criterion;
        return List.of(
            () -> assertEquals(expected.unit(), actual.unit(), prefix + " unit"),
            () -> assertEquals(expected.embedded(), actual.embedded(), tolerance.of(expected.embedded(), expected.embeddedRounding()), prefix + " embedded " + gap(expected.embedded(), actual.embedded())),
            () -> assertEquals(expected.use(), actual.use(), tolerance.of(expected.use(), expected.useRounding()), prefix + " use " + gap(expected.use(), actual.use()))
        );
    }

    private static String gap(double expected, double actual)
    {
        return expected == 0.0 ? "" : String.format("(%+.2f%%)", (actual - expected) / expected * 100.0);
    }
}
