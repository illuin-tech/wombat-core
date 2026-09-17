package tech.illuin.wombat.core.evaluation;

public class WombatEvaluationException extends Exception
{
    public WombatEvaluationException(String message)
    {
        super(message);
    }

    public WombatEvaluationException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
