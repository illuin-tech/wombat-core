package tech.illuin.wombat.core.asset;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AssetIdentity(
    @NotBlank @JsonProperty("id") String id,
    @NotBlank @JsonProperty("environment-id") @JsonAlias("environmentId") String environmentId,
    @NotBlank @JsonProperty("name") String name
) {
    public AssetIdentity
    {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(environmentId, "environmentId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        if (id.isBlank())
            throw new IllegalArgumentException("id must not be blank");
        if (environmentId.isBlank())
            throw new IllegalArgumentException("environmentId must not be blank");
        if (name.isBlank())
            throw new IllegalArgumentException("name must not be blank");
    }

    public static AssetIdentity of(String id, String environmentId, String name)
    {
        return new AssetIdentity(id, environmentId, name);
    }
}
