package tech.illuin.wombat.module.kubernetes_static.scenario.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A scenario's load stays within the server's capacity, jitter included: Boavizta would extrapolate a load above 100%
 * rather than reject it.
 */
@Documented
@Constraint(validatedBy = WithinCapacityValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface WithinCapacity
{
    String message() default "can reach a load above 100% of the server capacity, jitter included";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
