package tech.illuin.wombat.module.llm_static;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import tech.illuin.wombat.core.activity.WombatActivityException;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.core.evaluation.WombatEvaluationException;
import tech.illuin.wombat.module.conformance.ConformanceCore;
import tech.illuin.wombat.module.conformance.ConformanceSidecars;
import tech.illuin.wombat.module.conformance.canonical.CanonicalImpacts;
import tech.illuin.wombat.module.llm_static.scenario.ConstantScenario;
import tech.illuin.wombat.module.llm_static.scenario.UserActivityScenario;
import tech.illuin.wombat.module.llm_static.scenario.tools.ApplicabilityFilter;
import tech.illuin.wombat.module.conformance.canonical.CanonicalEcologits;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.time.DayOfWeek.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static tech.illuin.wombat.module.conformance.ConformanceAssertions.assertConforms;
import static tech.illuin.wombat.module.conformance.ConformanceAssertions.executables;

/**
 * The LLM static module evaluated through Ecologits, against Ecologits called per request.
 */
@Tag("conformance")
class LLMStaticConformanceTest
{
    private static final CanonicalEcologits ecologits = new CanonicalEcologits(ConformanceSidecars.ecologitsUri());
    private static final ConformanceCore core = new ConformanceCore(new LLMStaticModule(), ecologits);
    private static final Instant MONDAY_START = Instant.parse("2026-11-02T00:00:00Z");

    @Test
    void matchesEcologitsForAConstantTraffic() throws WombatActivityException, WombatEvaluationException
    {
        TimeRange range = new TimeRange(MONDAY_START, MONDAY_START.plus(Duration.ofDays(1)), Duration.ofHours(1));

        assertConforms(core, asset(model(LLMProvider.openai, "gpt-4o", 1_000_000, 1_000, new ConstantScenario())), range);
    }

    @Test
    void followsTheTrafficAtHourlyResolution() throws WombatActivityException, WombatEvaluationException
    {
        TimeRange range = new TimeRange(MONDAY_START, MONDAY_START.plus(Duration.ofDays(7)), Duration.ofHours(1));

        assertConforms(core, asset(officeWeek()), range);
    }

    /** A slot of any length carries its requests and their tokens, so that one mean request is exact. */
    @Test
    void followsTheTrafficAtEveryResolution() throws WombatActivityException, WombatEvaluationException
    {
        LLMStaticAsset asset = asset(officeWeek());
        Instant end = MONDAY_START.plus(Duration.ofDays(7));
        CanonicalImpacts expected = core.reference(asset, new TimeRange(MONDAY_START, end));

        List<Executable> assertions = new ArrayList<>();
        for (Duration resolution : List.of(Duration.ofDays(1), Duration.ofDays(7)))
            assertions.addAll(executables(expected, core.total(asset, new TimeRange(MONDAY_START, end, resolution)), ecologits.tolerance(), "at " + resolution));
        assertAll(assertions.stream());
    }

    @Test
    void evaluatesEveryModel() throws WombatActivityException, WombatEvaluationException
    {
        TimeRange range = new TimeRange(MONDAY_START, MONDAY_START.plus(Duration.ofDays(7)), Duration.ofHours(1));

        assertConforms(core, asset(officeWeek(), model(LLMProvider.mistralai, "mistral-large-latest", 500_000, 500, new ConstantScenario())), range);
    }

    private static LLMStaticAsset asset(LLMStaticProfile.ModelConfig... models)
    {
        return new LLMStaticAsset(AssetIdentity.of("llm-conformance", "conformance", LLMStaticModule.TYPE), new LLMStaticProfile(List.of(models)));
    }

    private static LLMStaticProfile.ModelConfig model(LLMProvider provider, String name, long requestPerYear, int outputTokenPerRequest, ConstantScenario scenario)
    {
        return new LLMStaticProfile.ModelConfig(provider, name, Country.FRANCE, requestPerYear, outputTokenPerRequest, List.of(scenario));
    }

    /* A constant at a tenth of an office-hours bell on weekdays */
    private static LLMStaticProfile.ModelConfig officeWeek()
    {
        return new LLMStaticProfile.ModelConfig(
            LLMProvider.openai,
            "gpt-4o",
            Country.FRANCE,
            1_100_000L,
            800,
            List.of(
                new ConstantScenario(0.1),
                new UserActivityScenario(1.0, LocalTime.of(9, 0), LocalTime.of(18, 0), UserActivityScenario.CurveShape.BELL, 0.0, new ApplicabilityFilter(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY), Set.of()))
            )
        );
    }
}
