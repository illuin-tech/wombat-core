package tech.illuin.wombat.core.asset;

public enum AssetType
{
    KUBERNETES_API(ActivityRegime.MEASURED, ServiceFamily.KUBERNETES_CONTAINER),
    KUBERNETES_SIMULATED(ActivityRegime.MODELED, ServiceFamily.KUBERNETES_CONTAINER),
    LLM_STATIC(ActivityRegime.MODELED, ServiceFamily.LLM),
    LLM_PROMETHEUS(ActivityRegime.MEASURED, ServiceFamily.LLM),
    LLM_SIMULATED(ActivityRegime.MODELED, ServiceFamily.LLM);

    private final ActivityRegime regime;
    private final ServiceFamily family;

    AssetType(ActivityRegime regime, ServiceFamily family)
    {
        this.regime = regime;
        this.family = family;
    }

    public ActivityRegime regime()
    {
        return this.regime;
    }

    public ServiceFamily family()
    {
        return this.family;
    }
}
