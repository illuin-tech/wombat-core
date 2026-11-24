package tech.illuin.wombat.module.boavizta;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import tech.illuin.wombat.core.asset.type.ModuleType;
import tech.illuin.wombat.core.module.ModuleSettings;

@JsonClassDescription("The BoaviztAPI instance the Boavizta module calls")
public record BoaviztaSettings(
    @JsonProperty(defaultValue = BoaviztaSettings.DEFAULT_URI) @JsonPropertyDescription("Base URI of the BoaviztAPI, version included") String uri,
    @JsonPropertyDescription("HTTP proxy URL, with a host and a port") String proxyUrl
) implements ModuleSettings
{
    public static final String DEFAULT_URI = "https://api.boavizta.org/v1/";
    /** The public API, reached directly: what applies when the host gives the module no settings. */
    public static final BoaviztaSettings DEFAULT = new BoaviztaSettings(null, null);

    public BoaviztaSettings
    {
        if (uri == null || uri.isBlank())
            uri = DEFAULT_URI;
    }

    @Override
    public ModuleType type()
    {
        return BoaviztaModule.TYPE;
    }
}
