package tech.illuin.wombat.core.asset.profile;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public non-sealed interface BareServerProfile extends ServerProfile
{
    @NotNull
    CPU cpu();

    @NotNull
    RAM ram();

    Disk ssd();

    Disk hdd();

    GPU gpu();

    @JsonClassDescription("Processors of a server")
    record CPU(
        @Positive @JsonPropertyDescription("Number of processors") int units,
        @Positive @JsonPropertyDescription("Number of cores per processor") int coreUnits,
        @Positive @JsonProperty(defaultValue = "2") @JsonPropertyDescription("Number of hardware threads per core") int threadsPerCore,
        @JsonPropertyDescription("Processor model, e.g. Intel Xeon Gold 6342, Boavizta's default when omitted") String name
    ) {
        public CPU
        {
            if (threadsPerCore <= 0)
                threadsPerCore = 2;
        }

        public int vcpu()
        {
            return this.units * this.coreUnits * this.threadsPerCore;
        }
    }

    @JsonClassDescription("Memory of a server")
    record RAM(
        @Positive @JsonPropertyDescription("Number of memory modules") int units,
        @Positive @JsonPropertyDescription("Capacity of a module, in GB") int capacity
    ) {}

    @JsonClassDescription("Disks of a server")
    record Disk(
        @Positive @JsonPropertyDescription("Number of disks") int units,
        @Positive @JsonPropertyDescription("Capacity of a disk, in GB") int capacity
    ) {}

    @JsonClassDescription("GPUs of a server")
    record GPU(
        @Positive @JsonPropertyDescription("Number of GPUs") int units,
        @JsonPropertyDescription("GPU model, e.g. NVIDIA A100, Boavizta's default when omitted") String name
    ) {}
}
