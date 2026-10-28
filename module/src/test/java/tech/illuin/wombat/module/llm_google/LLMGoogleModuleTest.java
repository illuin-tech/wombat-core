package tech.illuin.wombat.module.llm_google;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.Environment;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.core.context.ResolvedContext;
import tech.illuin.wombat.core.secret.EnvironmentSecretResolver;
import tech.illuin.wombat.core.secret.MissingSecretException;
import tech.illuin.wombat.core.source.WombatSource;
import tech.illuin.wombat.module.llm_google.source.LLMGoogleSource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LLMGoogleModuleTest
{
    private final LLMGoogleModule module = new LLMGoogleModule();

    @Test
    void declaresTheGoogleLLMAssetType()
    {
        assertEquals(LLMGoogleModule.TYPE, this.module.type());
        assertEquals(LLMGoogleAsset.class, this.module.assetClass());
    }

    @Test
    void providesAGoogleSource()
    {
        // An empty context registers no Google client, so nothing is dialled.
        Optional<WombatSource> source = this.module.createSource(new ResolvedContext());
        assertTrue(source.isPresent());
        assertInstanceOf(LLMGoogleSource.class, source.get());
    }

    @Test
    void sourcingAnAssetWithoutCredentialsKeyDoesNotRequireGoogleCredentialsAtStartup()
    {
        // Application Default Credentials are resolved lazily on the first request, so wiring must succeed even on
        // a machine (CI, a laptop) that has none.
        ResolvedContext context = context(Map.of(), asset(null));

        assertTrue(this.module.createSource(context).isPresent());
    }

    @Test
    void sourcingAServiceAccountAssetFailsWhenItsVariableIsUnset()
    {
        // Proves the module builds its clients from the context's resolver: the credential is read from the
        // environment at source creation, never from the asset the reconciler persisted.
        ResolvedContext context = context(Map.of(), asset("GOOGLE_SA_KEY"));

        MissingSecretException e = assertThrows(MissingSecretException.class, () -> this.module.createSource(context));

        assertEquals("GOOGLE_SA_KEY", e.key());
    }

    private static ResolvedContext context(Map<String, String> environment, LLMGoogleAsset asset)
    {
        return new ResolvedContext(
            List.of(new Environment("env", List.of(asset))),
            new EnvironmentSecretResolver(environment::get));
    }

    private static LLMGoogleAsset asset(String credentialsKey)
    {
        return new LLMGoogleAsset(AssetIdentity.of("g", "env", "G"), "https://monitoring.googleapis.com", "my-project", credentialsKey, 0,
            new LLMGoogleProfile(LLMProvider.google_genai, "gemini-2.5-flash", Country.US, new LLMGoogleProfile.DynamicProfile("f", null)));
    }
}
