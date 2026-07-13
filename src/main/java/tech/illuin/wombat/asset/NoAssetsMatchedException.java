package tech.illuin.wombat.asset;

public class NoAssetsMatchedException extends RuntimeException
{
    public NoAssetsMatchedException(String message)
    {
        super(message);
    }
}
