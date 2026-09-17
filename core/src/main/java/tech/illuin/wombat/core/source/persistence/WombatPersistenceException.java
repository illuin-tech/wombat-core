package tech.illuin.wombat.core.source.persistence;

public class WombatPersistenceException extends Exception
{
    public WombatPersistenceException(String message)
    {
        super(message);
    }

    public WombatPersistenceException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
