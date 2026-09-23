package tech.illuin.wombat.core.asset.profile;

public interface ServerProfile extends Profile
{
    @Override
    default String id()
    {
        return String.join(".", this.provider().name(), this.instanceType(), this.location(), String.valueOf(this.lifespan()));
    }

    ServerProvider provider();

    String instanceType();

    String location();

    int lifespan();
}
