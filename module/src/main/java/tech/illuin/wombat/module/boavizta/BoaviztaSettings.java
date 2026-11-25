package tech.illuin.wombat.module.boavizta;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;
import tech.illuin.wombat.core.asset.type.ModuleType;
import tech.illuin.wombat.core.module.ModuleSettings;

import java.util.Objects;

@JsonClassDescription("The BoaviztAPI instance the Boavizta module calls")
public record BoaviztaSettings(
    @NotBlank @JsonPropertyDescription("Base URI of the BoaviztAPI, version included") String uri,
    @JsonPropertyDescription("HTTP proxy URL, with a host and a port") String proxyUrl
) implements ModuleSettings
{
    public static final String DEFAULT_URI = "https://api.boavizta.org/v1/";
    /** The public API, reached directly: what applies when the host gives the module no settings. */
    public static final BoaviztaSettings DEFAULT = new BoaviztaSettings(DEFAULT_URI, null);

    public BoaviztaSettings
    {
        Objects.requireNonNull(uri, "uri cannot be null");
    }

    @Override
    public ModuleType type()
    {
        return BoaviztaModule.TYPE;
    }
}
