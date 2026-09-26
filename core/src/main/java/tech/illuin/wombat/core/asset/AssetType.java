package tech.illuin.wombat.core.asset;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AssetType(
    @NotBlank @JsonProperty("namespace") String namespace,
    @NotBlank @JsonProperty("localName") String localName,
    @NotNull @JsonProperty("regime") ActivityRegime regime,
    @NotNull @JsonProperty("family") ServiceFamily family
) {
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

    @JsonProperty("name")
    public String name()
    {
        return this.namespace + "." + this.localName;
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
