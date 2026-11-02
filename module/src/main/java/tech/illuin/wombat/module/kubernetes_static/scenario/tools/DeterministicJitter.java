package tech.illuin.wombat.module.kubernetes_static.scenario.tools;

import java.time.ZonedDateTime;

public final class DeterministicJitter
{
    private static final long HASH_MULTIPLIER = 0x45d9f3bL;
    private static final double HASH_MASK_DOUBLE = 0xFFFFFFF;
    private static final long SECONDS_PER_DAY = 86400L;

    private DeterministicJitter() {}

    public static double apply(double value, double jitterRatio, ZonedDateTime dateTime)
    {
        if (jitterRatio <= 0.0 || value <= 0.0 || dateTime == null)
            return value;

        long seed = dateTime.toLocalDate().toEpochDay() * SECONDS_PER_DAY + dateTime.toLocalTime().toSecondOfDay();
        double variance = pseudoRandomFactor(seed) * 2.0 - 1.0;
        return Math.max(0.0, value * (1.0 + variance * jitterRatio));
    }

    private static double pseudoRandomFactor(long seed)
    {
        long hash = seed ^ (seed >>> 16);
        hash *= HASH_MULTIPLIER;
        hash ^= (hash >>> 16);
        return (double) (hash & 0xFFFFFFF) / HASH_MASK_DOUBLE;
    }
}
