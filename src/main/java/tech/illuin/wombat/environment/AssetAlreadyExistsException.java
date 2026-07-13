package tech.illuin.wombat.environment;

public class AssetAlreadyExistsException extends RuntimeException
{
    public AssetAlreadyExistsException(String assetId)
    {
        super("Asset already exists: " + assetId);
    }
}
