package tech.illuin.wombat.core.activity;

public class WombatActivityException extends Exception
{
    public WombatActivityException(String message)
    {
        super(message);
    }

    public WombatActivityException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
