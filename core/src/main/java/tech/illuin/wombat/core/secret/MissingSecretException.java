package tech.illuin.wombat.core.secret;

public class MissingSecretException extends RuntimeException
{
    private final String key;

    public MissingSecretException(String key)
    {
        super("no value provided for required secret " + key);
        this.key = key;
    }

    public String key()
    {
        return this.key;
    }
}
