package tech.illuin.wombat.module.kubernetes_static;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.WombatActivityException;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.profile.Lifetime;
import tech.illuin.wombat.core.asset.profile.ServerProvider;
import tech.illuin.wombat.core.evaluation.WombatEvaluationException;
import tech.illuin.wombat.module.conformance.ConformanceCore;
import tech.illuin.wombat.module.conformance.ConformanceSidecars;
import tech.illuin.wombat.module.kubernetes_static.scenario.BatchActivityScenario;
import tech.illuin.wombat.module.kubernetes_static.scenario.ConstantScenario;
import tech.illuin.wombat.module.kubernetes_static.scenario.Scenario;
import tech.illuin.wombat.module.kubernetes_static.scenario.UserActivityScenario;
import tech.illuin.wombat.module.kubernetes_static.scenario.tools.ApplicabilityFilter;
import tech.illuin.wombat.module.conformance.canonical.CanonicalBoavizta;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static java.time.DayOfWeek.*;
import static tech.illuin.wombat.module.conformance.ConformanceAssertions.assertConforms;

/**
 * The Kubernetes static module evaluated through Boavizta, against one canonical Boavizta call over the whole range.
 */
@Tag("conformance")
class KubernetesStaticConformanceTest
{
    private static final CanonicalBoavizta boavizta = new CanonicalBoavizta(ConformanceSidecars.boaviztaUri());
    private static final ConformanceCore core = new ConformanceCore(new KubernetesStaticModule(), boavizta);
    private static final String INSTANCE_TYPE = "c5.2xlarge";
    /* A lifespan equal to Boavizta's default lifetime keeps H1 out of the cases that are meant to match */
    private static final Lifetime DEFAULT_LIFETIME = Lifetime.ofHours((long) boavizta.defaultHoursLifeTime("aws", INSTANCE_TYPE, "FRA"));
    private static final Instant MONDAY_START = Instant.parse("2026-11-02T00:00:00Z");

    @Test
    void matchesBoaviztaForAConstantLoad() throws WombatActivityException, WombatEvaluationException
    {
        TimeRange range = new TimeRange(MONDAY_START, MONDAY_START.plus(Duration.ofDays(1)), Duration.ofHours(1));

        assertConforms(core, asset(DEFAULT_LIFETIME, 1, new ConstantScenario(0.30, null)), range);
    }

    @Test
    void scalesWithTheNodeCount() throws WombatActivityException, WombatEvaluationException
    {
        TimeRange range = new TimeRange(MONDAY_START, MONDAY_START.plus(Duration.ofDays(1)), Duration.ofHours(1));

        assertConforms(core, asset(DEFAULT_LIFETIME, 3, new ConstantScenario(0.30, null)), range);
    }

    @Test
    void followsTheLoadProfileAtHourlyResolution() throws WombatActivityException, WombatEvaluationException
    {
        TimeRange range = new TimeRange(MONDAY_START, MONDAY_START.plus(Duration.ofDays(7)), Duration.ofHours(1));

        assertConforms(core, asset(DEFAULT_LIFETIME, 1, officeWeek()), range);
    }

    @Test
    void followsTheProfileLifespan() throws WombatActivityException, WombatEvaluationException
    {
        TimeRange range = new TimeRange(MONDAY_START, MONDAY_START.plus(Duration.ofDays(1)), Duration.ofHours(1));

        assertConforms(core, asset(Lifetime.ofYears(3), 1, new ConstantScenario(0.30, null)), range);
    }

    private static KubernetesStaticAsset asset(Lifetime lifespan, int nodeCount, Scenario... scenarios)
    {
        return new KubernetesStaticAsset(
            AssetIdentity.of("k8s-conformance", "conformance", KubernetesStaticModule.TYPE),
            new KubernetesStaticProfile(ServerProvider.aws, INSTANCE_TYPE, Country.FRANCE, lifespan, nodeCount, ZoneOffset.UTC, List.of(scenarios))
        );
    }

    /* A baseline, office hours on weekdays and a nightly batch: the shapes the samples ship */
    private static Scenario[] officeWeek()
    {
        return new Scenario[] {
            new ConstantScenario(0.10, null),
            new UserActivityScenario(0.10, 0.75, LocalTime.of(9, 0), LocalTime.of(18, 0), UserActivityScenario.CurveShape.BELL, 0.0, new ApplicabilityFilter(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY), Set.of())),
            new BatchActivityScenario(0.0, 0.90, LocalTime.of(2, 0), Duration.ofHours(2), 0.0, BatchActivityScenario.SmoothingMode.SMOOTH_STEP, null),
        };
    }
}
