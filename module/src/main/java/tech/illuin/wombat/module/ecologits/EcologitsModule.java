package tech.illuin.wombat.module.ecologits;

import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.module.global.GlobalModule;
import tech.illuin.wombat.core.module.global.ModuleType;

public class EcologitsModule implements GlobalModule
{
    public static final ModuleType TYPE = ModuleType.of("tech.illuin", "wombat-module", "ecologits", ServiceFamily.LLM);

    @Override
    public ModuleType type()
    {
        return TYPE;
    }


}
