package tech.illuin.wombat.core.asset.profile;

import tech.illuin.wombat.core.asset.ServiceFamily;

public interface Profile
{
    String id();

    ServiceFamily serviceFamily();
}
