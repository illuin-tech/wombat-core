package tech.illuin.wombat.core.activity.commons.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TimeRangeValidator.class)
@Documented
public @interface ValidTimeRange
{
    String message() default "TimeRange must have non-null start and end, and start must be before end";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
