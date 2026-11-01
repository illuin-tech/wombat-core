package tech.illuin.wombat.core.activity.kubernetes;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.PositiveOrZero;

public record RelativeCPUUsage(
    @PositiveOrZero @JsonProperty("ratio") double ratio
) implements CPUUsage {}
