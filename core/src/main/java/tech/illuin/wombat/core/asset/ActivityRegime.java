package tech.illuin.wombat.core.asset;

public enum ActivityRegime
{
    MEASURED("Measured"),
    MODELED("Modeled"),
    ;

    private final String label;

    ActivityRegime(String label)
    {
        this.label = label;
    }

    public String label()
    {
        return this.label;
    }
}
