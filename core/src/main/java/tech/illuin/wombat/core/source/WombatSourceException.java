package tech.illuin.wombat.core.source;

public class WombatSourceException extends Exception
{
    public WombatSourceException(String message)
    {
        super(message);
    }

    public WombatSourceException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
