package tech.illuin.wombat.core.secret;

public class MissingSecretException extends RuntimeException
{
    private final String key;

    public MissingSecretException(String key)
    {
        super("No value provided for required secret " + key + "; set the " + key + " environment variable");
        this.key = key;
    }

    public String key()
    {
        return this.key;
    }
}
