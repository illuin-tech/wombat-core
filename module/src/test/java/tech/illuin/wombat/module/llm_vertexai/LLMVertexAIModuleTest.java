package tech.illuin.wombat.module.llm_vertexai;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.WombatActivityResolver;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.module.ProcessorFactory;
import tech.illuin.wombat.core.module.WombatServices;
import tech.illuin.wombat.core.secret.EnvironmentSecretResolver;
import tech.illuin.wombat.core.secret.MissingSecretException;
import tech.illuin.wombat.module.llm_vertexai.activity.LLMVertexAIActivityResolver;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LLMVertexAIModuleTest
{
    private final LLMVertexAIModule module = new LLMVertexAIModule();

    @Test
    void declaresTheGoogleLLMAssetType()
    {
        assertEquals(LLMVertexAIModule.TYPE, this.module.type());
        assertEquals(LLMVertexAIAsset.class, this.module.assetClass());
    }

    @Test
    void providesAGoogleActivityResolverAndNoSource()
    {
        Optional<ProcessorFactory<WombatActivityResolver>> factory = this.module.activityResolver();
        assertTrue(factory.isPresent());
        assertTrue(this.module.source().isEmpty());
    }

    @Test
    void resolvingAnAssetWithoutCredentialsKeyDoesNotRequireGoogleCredentialsAtStartup()
    {
        // Application Default Credentials are resolved lazily on the first request, so building the resolver must
        // succeed even on a machine (CI, a laptop) that has none.
        ProcessorFactory<WombatActivityResolver> factory = this.module.activityResolver().orElseThrow();

        assertInstanceOf(LLMVertexAIActivityResolver.class, factory.create(services(Map.of()), asset(null)));
    }

    @Test
    void resolvingAServiceAccountAssetFailsWhenItsVariableIsUnset()
    {
        // Proves the module builds its clients from the services' resolver: the credential is read from the
        // environment at resolver creation, never from the asset the reconciler persisted.
        ProcessorFactory<WombatActivityResolver> factory = this.module.activityResolver().orElseThrow();

        MissingSecretException e = assertThrows(MissingSecretException.class, () -> factory.create(services(Map.of()), asset("GOOGLE_SA_KEY")));

        assertEquals("GOOGLE_SA_KEY", e.key());
    }

    private static WombatServices services(Map<String, String> environment)
    {
        return new WombatServices(new EnvironmentSecretResolver(environment::get));
    }

    private static LLMVertexAIAsset asset(String credentialsKey)
    {
        return new LLMVertexAIAsset(AssetIdentity.of("g", "env", LLMVertexAIModule.TYPE), "https://monitoring.googleapis.com", "projects/my-project", credentialsKey,
            new LLMVertexAIProfile(500, List.of("gemini-2.5-flash")));
    }
}
