package tech.illuin.wombat.core.asset.profile;

import tech.illuin.wombat.core.asset.ServiceFamily;

public interface ServerProfile extends Profile
{
    @Override
    default String id()
    {
        return String.join(".", this.provider().name(), this.instanceType(), this.location(), String.valueOf(this.lifespan()));
    }

    @Override
    default ServiceFamily serviceFamily()
    {
        return ServiceFamily.KUBERNETES_CONTAINER;
    }

    ServerProvider provider();

    String instanceType();

    String location();

    int lifespan();
}
