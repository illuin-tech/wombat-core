package tech.illuin.wombat.core.asset.type;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.module.WombatType;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AssetType(
    @NotBlank String namespace,
    @NotBlank String localName,
    @NotNull ActivityRegime regime,
    @NotNull ServiceFamily family
) implements WombatType {
    public AssetType
    {
        Objects.requireNonNull(namespace, "namespace must not be null");
        Objects.requireNonNull(localName, "localName must not be null");
        Objects.requireNonNull(regime, "regime must not be null");
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

    public static AssetType of(String namespace, String localName, ActivityRegime regime, ServiceFamily family)
    {
        return new AssetType(namespace, localName, regime, family);
    }

    public static AssetType of(String groupId, String artifactId, String localName, ActivityRegime regime, ServiceFamily family)
    {
        return new AssetType(groupId + "." + artifactId, localName, regime, family);
    }
}
