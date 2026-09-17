package tech.illuin.wombat.core.asset;

public enum AssetType
{
    KUBERNETES_API(ActivityRegime.MEASURED),
    LLM_STATIC(ActivityRegime.MODELED),
    LLM_PROMETHEUS(ActivityRegime.MEASURED);

    private final ActivityRegime activityRegime;

    AssetType(ActivityRegime regime)
    {
        this.activityRegime = regime;
    }

    public ActivityRegime regime()
    {
        return this.activityRegime;
    }
}
