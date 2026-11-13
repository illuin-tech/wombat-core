package tech.illuin.wombat.core.activity.commons;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TimeRangeValidator implements ConstraintValidator<ValidTimeRange, TimeRange>
{
    @Override
    public boolean isValid(TimeRange value, ConstraintValidatorContext context)
    {
        if (value == null)
            return true;
        return value.start() != null && value.end() != null && value.start().isBefore(value.end());
    }
}
