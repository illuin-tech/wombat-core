package tech.illuin.wombat.module.ecologits;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;
import tech.illuin.wombat.core.asset.type.ModuleType;
import tech.illuin.wombat.core.module.ModuleSettings;

import java.util.Objects;

@JsonClassDescription("The Ecologits API instance the Ecologits module calls")
public record EcologitsSettings(
    @NotBlank @JsonPropertyDescription("Base URI of the Ecologits API") String uri,
    @JsonPropertyDescription("HTTP proxy URL, with a host and a port") String proxyUrl
) implements ModuleSettings
{
    public static final String DEFAULT_URI = "https://api.ecologits.ai/";
    /** The public API, reached directly: what applies when the host gives the module no settings. */
    public static final EcologitsSettings DEFAULT = new EcologitsSettings(DEFAULT_URI, null);

    public EcologitsSettings
    {
        Objects.requireNonNull(uri, "uri cannot be null");
    }

    @Override
    public ModuleType type()
    {
        return EcologitsModule.TYPE;
    }
}
