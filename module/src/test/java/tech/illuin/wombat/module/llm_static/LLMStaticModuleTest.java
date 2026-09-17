package tech.illuin.wombat.module.llm_static;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.asset.AssetType;
import tech.illuin.wombat.module.llm_static.activity.LLMStaticActivityResolver;
import tech.illuin.wombat.core.context.ResolvedContext;
import tech.illuin.wombat.core.activity.WombatActivityResolver;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LLMStaticModuleTest
{
    private final LLMStaticModule module = new LLMStaticModule();

    @Test
    void declaresTheStaticLLMAssetType()
    {
        assertEquals(AssetType.LLM_STATIC, this.module.type());
        assertEquals(LLMStaticAsset.class, this.module.assetClass());
    }

    @Test
    void providesAStaticActivityResolver()
    {
        Optional<WombatActivityResolver> resolver = this.module.createActivityResolver(new ResolvedContext());
        assertTrue(resolver.isPresent());
        assertInstanceOf(LLMStaticActivityResolver.class, resolver.get());
    }

    /**
     * The module models activity and leaves the impact to whatever serves the LLM family — the EcoLogits resolver
     * the app registers as a default — so it deliberately contributes no impact-resolver of its own.
     */
    @Test
    void leavesTheImpactToTheFamilyDefault()
    {
        assertTrue(this.module.createImpactResolver(new ResolvedContext()).isEmpty());
    }
}
