package tech.illuin.wombat.core.asset.type;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * The type of a module serving every asset of a {@link ServiceFamily}, such as an impact methodology, rather than
 * defining an asset-type of its own.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FamilyType(
    @NotBlank String namespace,
    @NotBlank String localName,
    @NotNull ServiceFamily family
) implements ModuleType {
    public FamilyType
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
        return ModuleType.super.name();
    }

    public static FamilyType of(String namespace, String localName, ServiceFamily family)
    {
        return new FamilyType(namespace, localName, family);
    }

    public static FamilyType of(String groupId, String artifactId, String localName, ServiceFamily family)
    {
        return new FamilyType(groupId + "." + artifactId, localName, family);
    }
}
