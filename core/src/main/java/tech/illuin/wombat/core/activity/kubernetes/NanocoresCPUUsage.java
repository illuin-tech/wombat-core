package tech.illuin.wombat.core.activity.kubernetes;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.PositiveOrZero;

public record NanocoresCPUUsage(
    @PositiveOrZero @JsonProperty("nanocores") double nanocores
) implements CPUUsage {}
