package tech.illuin.wombat.module.llm_anthropic;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.Environment;
import tech.illuin.wombat.core.context.ResolvedContext;
import tech.illuin.wombat.core.secret.EnvironmentSecretResolver;
import tech.illuin.wombat.core.secret.MissingSecretException;
import tech.illuin.wombat.module.llm_anthropic.activity.LLMAnthropicActivityResolver;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static tech.illuin.wombat.core.activity.commons.Country.FRANCE;

public class LLMAnthropicModuleTest
{
    private final LLMAnthropicModule module = new LLMAnthropicModule();

    @Test
    void declaresTheAnthropicLLMAssetType()
    {
        assertEquals(LLMAnthropicModule.TYPE, this.module.type());
        assertEquals(LLMAnthropicAsset.class, this.module.assetClass());
    }

    @Test
    void providesAnActivityResolverAndNoSource()
    {
        // An empty context registers no Anthropic client, so nothing is dialled.
        Optional<WombatActivityResolver> resolver = this.module.createActivityResolver(new ResolvedContext());
        assertTrue(resolver.isPresent());
        assertInstanceOf(LLMAnthropicActivityResolver.class, resolver.get());
        assertTrue(this.module.createSource(new ResolvedContext()).isEmpty());
    }

    @Test
    void creatingTheResolverFailsWhenTheAdminKeyIsUnset()
    {
        ResolvedContext context = new ResolvedContext(
            List.of(new Environment("env", List.of(asset()))),
            new EnvironmentSecretResolver(Map.<String, String>of()::get)
        );
        MissingSecretException e = assertThrows(MissingSecretException.class, () -> this.module.createActivityResolver(context));

        assertEquals("ANTHROPIC_ADMIN_KEY", e.key());
    }

    @Test
    void requiresTheAdminKeySecret()
    {
        assertEquals(Set.of("ANTHROPIC_ADMIN_KEY"), asset().requiredSecretKeys());
    }

    @Test
    void defaultsToThePublicApiUrl()
    {
        assertEquals(LLMAnthropicAsset.DEFAULT_API_URL, asset().resolvedApiUrl());
    }

    private static LLMAnthropicAsset asset()
    {
        return new LLMAnthropicAsset(
            AssetIdentity.of("a", "env", "A"), null, null, "ANTHROPIC_ADMIN_KEY",
            new LLMAnthropicProfile(FRANCE, 500, null, null, null)
        );
    }
}
