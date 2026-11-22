package tech.illuin.wombat.module.kubernetes_static.scenario;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.profile.Lifetime;
import tech.illuin.wombat.core.asset.profile.ServerProvider;
import tech.illuin.wombat.module.kubernetes_static.KubernetesStaticAsset;
import tech.illuin.wombat.module.kubernetes_static.KubernetesStaticModule;
import tech.illuin.wombat.module.kubernetes_static.KubernetesStaticProfile;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A load above 100% would reach Boavizta, which extrapolates rather than rejects it: the asset is rejected instead, as
 * the app validates what it receives through its API.
 */
class WithinCapacityTest
{
    /* Messages without expressions: no Expression Language implementation needed */
    private static final Validator validator = Validation.byDefaultProvider()
        .configure()
        .messageInterpolator(new ParameterMessageInterpolator())
        .buildValidatorFactory()
        .getValidator();
    private static final String WITHIN_CAPACITY = "can reach a load above 100% of the server capacity, jitter included";

    @Test
    void acceptsLoadsUpToFullCapacity()
    {
        Set<ConstraintViolation<KubernetesStaticAsset>> violations = validator.validate(asset(
            new ConstantScenario(1.0),
            /* 0.9 with ±10%: 0.99 at most */
            new UserActivityScenario(0.1, 0.9, LocalTime.of(9, 0), LocalTime.of(18, 0), UserActivityScenario.CurveShape.BELL, 0.1),
            new BatchActivityScenario(0.0, 1.0, LocalTime.of(2, 0), Duration.ofHours(2), 0.0, BatchActivityScenario.SmoothingMode.NONE)
        ));

        assertEquals(Set.of(), violations);
    }

    @Test
    void rejectsALoadAboveFullCapacity()
    {
        Set<ConstraintViolation<KubernetesStaticAsset>> violations = validator.validate(asset(new ConstantScenario(1.2)));

        assertEquals(Set.of("profile.scenarios[0].load", "profile.scenarios[0]"), paths(violations));
    }

    @Test
    void rejectsAPeakThatJitterPushesAboveFullCapacity()
    {
        Set<ConstraintViolation<KubernetesStaticAsset>> violations = validator.validate(asset(
            new ConstantScenario(0.1),
            /* 0.95 with ±10%: up to 1.045 */
            new UserActivityScenario(0.1, 0.95, LocalTime.of(9, 0), LocalTime.of(18, 0), UserActivityScenario.CurveShape.BELL, 0.1),
            new BatchActivityScenario(0.0, 0.95, LocalTime.of(2, 0), Duration.ofHours(2), 0.1, BatchActivityScenario.SmoothingMode.NONE)
        ));

        assertEquals(Set.of("profile.scenarios[1]", "profile.scenarios[2]"), paths(violations));
        assertTrue(violations.stream().allMatch(violation -> WITHIN_CAPACITY.equals(violation.getMessage())), violations::toString);
    }

    @Test
    void maxLoad_includesTheJitterOverTheBusiestLevel()
    {
        assertEquals(0.6, new ConstantScenario(0.6).maxLoad());
        assertEquals(0.88, new UserActivityScenario(0.1, 0.8, LocalTime.of(9, 0), LocalTime.of(18, 0), UserActivityScenario.CurveShape.BELL, 0.1).maxLoad(), 1e-12);
        /* A baseline above the peak is the busiest level */
        assertEquals(0.5, new BatchActivityScenario(0.5, 0.2, LocalTime.of(2, 0), Duration.ofHours(2), 0.0, BatchActivityScenario.SmoothingMode.NONE).maxLoad(), 1e-12);
    }

    private static Set<String> paths(Set<ConstraintViolation<KubernetesStaticAsset>> violations)
    {
        return violations.stream()
            .map(violation -> violation.getPropertyPath().toString())
            .collect(Collectors.toSet());
    }

    private static KubernetesStaticAsset asset(Scenario... scenarios)
    {
        return new KubernetesStaticAsset(
            AssetIdentity.of("k8s-capacity", "env-test", KubernetesStaticModule.TYPE),
            new KubernetesStaticProfile(ServerProvider.aws, "c5.2xlarge", Country.FRANCE, Lifetime.ofYears(4), 1, ZoneOffset.UTC, List.of(scenarios))
        );
    }
}
