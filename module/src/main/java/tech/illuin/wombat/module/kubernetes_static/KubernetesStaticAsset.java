package tech.illuin.wombat.module.kubernetes_static;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.validation.constraints.NotNull;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.type.AssetType;

@JsonClassDescription("Kubernetes cluster whose load is modeled from static scenarios")
public record KubernetesStaticAsset(
    @JsonUnwrapped AssetIdentity identity,
    @NotNull @JsonPropertyDescription("Server hardware profile and load scenarios") KubernetesStaticProfile profile
) implements Asset
{
    @Override
    public AssetType type()
    {
        return KubernetesStaticModule.TYPE;
    }
}
