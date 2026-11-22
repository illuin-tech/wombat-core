package tech.illuin.wombat.module.kubernetes_static.scenario.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import tech.illuin.wombat.module.kubernetes_static.scenario.Scenario;

public class WithinCapacityValidator implements ConstraintValidator<WithinCapacity, Scenario>
{
    private static final double FULL_CAPACITY = 1.0;

    @Override
    public boolean isValid(Scenario scenario, ConstraintValidatorContext context)
    {
        return scenario == null || scenario.maxLoad() <= FULL_CAPACITY;
    }
}
