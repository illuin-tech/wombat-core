package tech.illuin.wombat.module.kubernetes_static.scenario.tools;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.DayOfWeek;
import java.time.Month;
import java.time.ZonedDateTime;
import java.util.Set;

public record ApplicabilityFilter(
    @JsonProperty("daysOfWeek") Set<DayOfWeek> daysOfWeek,
    @JsonProperty("months") Set<Month> months
) {
    public static final ApplicabilityFilter ALWAYS = new ApplicabilityFilter(Set.of(), Set.of());

    public ApplicabilityFilter
    {
        daysOfWeek = daysOfWeek != null ? Set.copyOf(daysOfWeek) : Set.of();
        months = months != null ? Set.copyOf(months) : Set.of();
    }

    public boolean isApplicable(ZonedDateTime dateTime)
    {
        if (dateTime == null)
            return false;

        if (!this.daysOfWeek.isEmpty() && !this.daysOfWeek.contains(dateTime.getDayOfWeek()))
            return false;

        if (!this.months.isEmpty() && !this.months.contains(dateTime.getMonth()))
            return false;

        return true;
    }
}
