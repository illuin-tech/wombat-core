package tech.illuin.wombat.core.module.global;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.module.WombatType;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ModuleType(
    @NotBlank String namespace,
    @NotBlank String localName,
    @NotNull ServiceFamily family
) implements WombatType {
    public ModuleType
    {
        Objects.requireNonNull(namespace, "namespace must not be null");
        Objects.requireNonNull(localName, "localName must not be null");
        Objects.requireNonNull(family, "family must not be null");
        if (namespace.isBlank())
            throw new IllegalArgumentException("namespace must not be blank");
        if (localName.isBlank())
            throw new IllegalArgumentException("localName must not be blank");
    }

    @Override
    @JsonProperty("name")
    public String name()
    {
        return super.name();
    }

    public static ModuleType of(String namespace, String localName, ServiceFamily family)
    {
        return new ModuleType(namespace, localName, family);
    }

    public static ModuleType of(String groupId, String artifactId, String localName, ServiceFamily family)
    {
        return new ModuleType(groupId + "." + artifactId, localName, family);
    }
}
