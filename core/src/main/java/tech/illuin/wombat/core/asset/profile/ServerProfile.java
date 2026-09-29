package tech.illuin.wombat.core.asset.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public interface ServerProfile extends AssetProfile
{
    @Override
    default String id()
    {
        return String.join(".", this.provider().name(), this.instanceType(), this.location(), String.valueOf(this.lifespan()));
    }

    @NotNull
    ServerProvider provider();

    @NotBlank
    String instanceType();

    @NotBlank
    String location();

    @Positive
    int lifespan();
}
