package tech.illuin.wombat.asset;

public class UnknownEnvironmentException extends RuntimeException
{
    public UnknownEnvironmentException(String environmentId)
    {
        super("Unknown environment: " + environmentId);
    }
}
