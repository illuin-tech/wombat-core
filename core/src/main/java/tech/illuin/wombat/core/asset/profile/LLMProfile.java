package tech.illuin.wombat.core.asset.profile;

public interface LLMProfile extends Profile
{
    @Override
    default String id()
    {
        return this.model();
    }

    LLMProvider provider();

    String model();

    String location();
}
