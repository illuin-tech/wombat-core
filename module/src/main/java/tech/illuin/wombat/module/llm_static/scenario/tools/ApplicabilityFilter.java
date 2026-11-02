package tech.illuin.wombat.module.llm_static.scenario.tools;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.DayOfWeek;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

public record ApplicabilityFilter(
    @JsonProperty("daysOfWeek") Set<DayOfWeek> daysOfWeek,
    @JsonProperty("months") Set<Month> months,
    @JsonProperty("zoneId") ZoneId zoneId
) {
    public static final ApplicabilityFilter ALWAYS = new ApplicabilityFilter(Set.of(), Set.of(), null);

    public ApplicabilityFilter
    {
        daysOfWeek = daysOfWeek != null ? Set.copyOf(daysOfWeek) : Set.of();
        months = months != null ? Set.copyOf(months) : Set.of();
    }

    public boolean isApplicable(ZonedDateTime dateTime)
    {
        if (dateTime == null)
            return false;

        ZonedDateTime localDateTime = this.zoneId != null
            ? dateTime.withZoneSameInstant(this.zoneId)
            : dateTime;

        if (!this.daysOfWeek.isEmpty() && !this.daysOfWeek.contains(localDateTime.getDayOfWeek()))
            return false;

        if (!this.months.isEmpty() && !this.months.contains(localDateTime.getMonth()))
            return false;

        return true;
    }
}
