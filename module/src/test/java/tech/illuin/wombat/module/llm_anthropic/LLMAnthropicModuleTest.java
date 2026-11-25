package tech.illuin.wombat.module.llm_anthropic;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.module.ProcessorFactory;
import tech.illuin.wombat.core.evaluation.impact.kubernetes.KubernetesMetricResolver;
import tech.illuin.wombat.core.evaluation.impact.llm.LLMMetricResolver;
import tech.illuin.wombat.core.module.MetricResolvers;
import tech.illuin.wombat.core.module.WombatServices;
import tech.illuin.wombat.core.secret.EnvironmentSecretResolver;
import tech.illuin.wombat.core.secret.MissingSecretException;
import tech.illuin.wombat.core.secret.SecretKey;
import tech.illuin.wombat.module.llm_anthropic.activity.LLMAnthropicActivityResolver;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static tech.illuin.wombat.core.activity.commons.Country.FRANCE;
import static org.mockito.Mockito.mock;

public class LLMAnthropicModuleTest
{
    private final LLMAnthropicModule module = new LLMAnthropicModule();

    @Test
    void declaresTheAnthropicLLMAssetType()
    {
        assertEquals(LLMAnthropicModule.TYPE, this.module.type());
        assertEquals(Optional.of(LLMAnthropicAsset.class), this.module.assetClass());
    }

    @Test
    void providesAnActivityResolverAndNoSource()
    {
        // Building the client does not dial Anthropic.
        Optional<ProcessorFactory<WombatActivityResolver>> factory = this.module.activityResolver();
        assertTrue(factory.isPresent());
        assertInstanceOf(LLMAnthropicActivityResolver.class, factory.get().create(services(Map.of(EnvironmentSecretResolver.PREFIX + "ANTHROPIC_ADMIN_KEY", "k")), asset()));
        assertTrue(this.module.source().isEmpty());
    }

    @Test
    void creatingTheResolverFailsWhenTheAdminKeyIsUnset()
    {
        ProcessorFactory<WombatActivityResolver> factory = this.module.activityResolver().orElseThrow();
        MissingSecretException e = assertThrows(MissingSecretException.class, () -> factory.create(services(Map.of()), asset()));

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

    private static WombatServices services(Map<String, String> environment)
    {
        return new WombatServices(
            new EnvironmentSecretResolver(environment::get),
            new MetricResolvers(mock(KubernetesMetricResolver.class), mock(LLMMetricResolver.class)),
            List.of()
        );
    }

    private static LLMAnthropicAsset asset()
    {
        return new LLMAnthropicAsset(
            AssetIdentity.of("a", "env", LLMAnthropicModule.TYPE), null, null, SecretKey.of("ANTHROPIC_ADMIN_KEY"),
            new LLMAnthropicProfile(FRANCE, 500, null, null, null)
        );
    }
}
