package tech.illuin.wombat.module.ecologits;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import tech.illuin.wombat.core.asset.type.ModuleType;
import tech.illuin.wombat.core.module.ModuleSettings;

@JsonClassDescription("The Ecologits API instance the Ecologits module calls")
public record EcologitsSettings(
    @JsonProperty(defaultValue = EcologitsSettings.DEFAULT_URI) @JsonPropertyDescription("Base URI of the Ecologits API") String uri,
    @JsonPropertyDescription("HTTP proxy URL, with a host and a port") String proxyUrl
) implements ModuleSettings
{
    public static final String DEFAULT_URI = "https://api.ecologits.org/v1/";
    /** The public API, reached directly: what applies when the host gives the module no settings. */
    public static final EcologitsSettings DEFAULT = new EcologitsSettings(null, null);

    public EcologitsSettings
    {
        if (uri == null || uri.isBlank())
            uri = DEFAULT_URI;
    }

    @Override
    public ModuleType type()
    {
        return EcologitsModule.TYPE;
    }
}
