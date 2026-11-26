package tech.illuin.wombat.core.asset.profile.inference;

import tech.illuin.wombat.core.evaluation.WombatEvaluationException;

/**
 * No source of the cluster could tell its hardware.
 */
public class ProfileInferenceException extends WombatEvaluationException
{
    public ProfileInferenceException(String message)
    {
        super(message);
    }

    public ProfileInferenceException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
