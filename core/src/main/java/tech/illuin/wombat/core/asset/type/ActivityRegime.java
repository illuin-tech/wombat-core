package tech.illuin.wombat.core.asset.type;

public enum ActivityRegime
{
    UNKNOWN("Unknown"),
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
