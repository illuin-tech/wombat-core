package tech.illuin.wombat.core.activity.kubernetes;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = NanocoresCPUUsage.class, name = "nanocores"),
    @JsonSubTypes.Type(value = RelativeCPUUsage.class, name = "relative")
})
public sealed interface CPUUsage permits NanocoresCPUUsage, RelativeCPUUsage
{
    static NanocoresCPUUsage nanocores(double nanocores)
    {
        return new NanocoresCPUUsage(nanocores);
    }

    static RelativeCPUUsage relative(double ratio)
    {
        return new RelativeCPUUsage(ratio);
    }
}
