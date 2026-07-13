package tech.illuin.wombat.environment;

public class UnknownAssetException extends RuntimeException
{
    public UnknownAssetException(String environmentId, String assetId)
    {
        super("Unknown asset " + assetId + " in environment " + environmentId);
    }
}
