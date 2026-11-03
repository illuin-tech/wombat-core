package tech.illuin.wombat.core.source.persistence.micrometer.data;

public enum Aggregation
{
    MEAN,
    SUM;

    public double of(double total, long count)
    {
        return switch (this)
        {
            case MEAN -> total / count;
            case SUM -> total;
        };
    }
}
