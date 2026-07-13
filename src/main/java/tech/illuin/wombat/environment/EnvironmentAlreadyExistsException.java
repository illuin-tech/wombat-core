package tech.illuin.wombat.environment;

public class EnvironmentAlreadyExistsException extends RuntimeException
{
    public EnvironmentAlreadyExistsException(String environmentId)
    {
        super("Environment already exists: " + environmentId);
    }
}
