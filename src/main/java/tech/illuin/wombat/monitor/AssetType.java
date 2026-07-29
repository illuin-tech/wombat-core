package tech.illuin.wombat.monitor;

public enum AssetType
{
    KUBERNETES_API,
    LLM_STATIC,
    LLM_PROMETHEUS;

    public String measureLabel()
    {
        return this == LLM_STATIC ? "STATIC" : "DYNAMIC";
    }
}
