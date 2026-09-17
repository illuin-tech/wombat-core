package tech.illuin.wombat.core.activity.kubernetes;

public class NoCPUUsageException extends Exception
{
    public NoCPUUsageException(String message)
    {
        super(message);
    }
}
