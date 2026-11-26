package tech.illuin.wombat.module.kubernetes_api.profile;

/**
 * A part of the {@link ClusterInventory} could not be read, typically because the credentials are not allowed to.
 */
public class InventoryUnavailableException extends Exception
{
    public InventoryUnavailableException(String message)
    {
        super(message);
    }

    public InventoryUnavailableException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
