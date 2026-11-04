package tech.illuin.wombat.module.llm_static.scenario.tools;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.DayOfWeek;
import java.time.LocalDate;
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

    public double annualActiveFraction()
    {
        if (this.daysOfWeek.isEmpty() && this.months.isEmpty())
            return 1.0;

        int activeDays = 0;
        LocalDate start = LocalDate.of(2026, 1, 1);
        for (int i = 0 ; i < 365 ; i++)
        {
            LocalDate date = start.plusDays(i);
            boolean dayMatch = this.daysOfWeek.isEmpty() || this.daysOfWeek.contains(date.getDayOfWeek());
            boolean monthMatch = this.months.isEmpty() || this.months.contains(date.getMonth());
            if (dayMatch && monthMatch)
                activeDays++;
        }
        return (double) activeDays / 365.0;
    }
}
