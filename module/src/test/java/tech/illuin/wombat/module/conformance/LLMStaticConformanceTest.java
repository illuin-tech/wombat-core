package tech.illuin.wombat.module.conformance;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import tech.illuin.wombat.core.activity.WombatActivityException;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.core.evaluation.WombatEvaluationException;
import tech.illuin.wombat.module.llm_static.LLMStaticAsset;
import tech.illuin.wombat.module.llm_static.LLMStaticModule;
import tech.illuin.wombat.module.llm_static.LLMStaticProfile;
import tech.illuin.wombat.module.llm_static.scenario.ConstantScenario;
import tech.illuin.wombat.module.llm_static.scenario.UserActivityScenario;
import tech.illuin.wombat.module.llm_static.scenario.tools.ApplicabilityFilter;
import tech.illuin.wombat.module.conformance.canonical.CanonicalEcologits;
import tech.illuin.wombat.module.conformance.canonical.CanonicalImpacts;

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
class LLMStaticConformanceTest
{
    private static final CanonicalEcologits ecologits = new CanonicalEcologits(ConformanceSidecars.ecologitsUri());
    private static final ConformanceCore core = new ConformanceCore(new LLMStaticModule(), ecologits);
    private static final Instant MONDAY_START = Instant.parse("2026-11-02T00:00:00Z");

    @Test
    void matchesEcologitsForAConstantTraffic() throws WombatActivityException, WombatEvaluationException
    {
        TimeRange range = new TimeRange(MONDAY_START, MONDAY_START.plus(Duration.ofDays(1)), Duration.ofHours(1));

        assertConforms(core, asset(new LLMStaticProfile.ModelConfig(LLMProvider.openai, "gpt-4o", Country.FRANCE, List.of(new ConstantScenario(1_000, 1_000_000)))), range);
    }

    @Test
    void followsTheTrafficAtHourlyResolution() throws WombatActivityException, WombatEvaluationException
    {
        TimeRange range = new TimeRange(MONDAY_START, MONDAY_START.plus(Duration.ofDays(7)), Duration.ofHours(1));

        assertConforms(core, asset(officeWeek()), range);
    }

    @Test
    void evaluatesEveryModel() throws WombatActivityException, WombatEvaluationException
    {
        TimeRange range = new TimeRange(MONDAY_START, MONDAY_START.plus(Duration.ofDays(7)), Duration.ofHours(1));
        LLMStaticProfile.ModelConfig mistral = new LLMStaticProfile.ModelConfig(LLMProvider.mistralai, "mistral-large-latest", Country.FRANCE, List.of(new ConstantScenario(500, 500_000)));

        assertConforms(core, asset(officeWeek(), mistral), range);
    }

    @Test
    @Disabled("H3: tokens per request are averaged over hours, not requests, -57% at 1 d and -67% at 1 week on an office week")
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

    private static LLMStaticAsset asset(LLMStaticProfile.ModelConfig... models)
    {
        return new LLMStaticAsset(AssetIdentity.of("llm-conformance", "conformance", LLMStaticModule.TYPE), new LLMStaticProfile(List.of(models)));
    }

    /* Short requests around the clock, and long ones during office hours on weekdays: two token counts mixed in a slot */
    private static LLMStaticProfile.ModelConfig officeWeek()
    {
        return new LLMStaticProfile.ModelConfig(
            LLMProvider.openai,
            "gpt-4o",
            Country.FRANCE,
            List.of(
                new ConstantScenario(100, 100_000),
                new UserActivityScenario(2_000, 1_000_000, LocalTime.of(9, 0), LocalTime.of(18, 0), UserActivityScenario.CurveShape.BELL, 0.0, new ApplicabilityFilter(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY), Set.of()))
            )
        );
    }
}
