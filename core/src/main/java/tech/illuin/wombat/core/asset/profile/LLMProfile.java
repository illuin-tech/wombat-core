package tech.illuin.wombat.core.asset.profile;

import tech.illuin.wombat.core.asset.ServiceFamily;

public interface LLMProfile extends Profile
{
    @Override
    default String id()
    {
        return this.model();
    }

    @Override
    default ServiceFamily serviceFamily()
    {
        return ServiceFamily.LLM;
    }

    LLMProvider provider();

    String model();

    String location();
}
