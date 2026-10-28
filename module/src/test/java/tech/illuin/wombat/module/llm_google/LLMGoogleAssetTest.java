package tech.illuin.wombat.module.llm_google;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.profile.LLMProvider;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LLMGoogleAssetTest
{
    @Test
    void anAssetRelyingOnApplicationDefaultCredentialsNeedsNoSecret()
    {
        LLMGoogleAsset asset = asset(null);

        assertFalse(asset.usesServiceAccountKey());
        assertEquals(Set.of(), asset.requiredSecretKeys());
    }

    @Test
    void aBlankCredentialsKeyIsTreatedAsAbsent()
    {
        LLMGoogleAsset asset = asset("  ");

        assertFalse(asset.usesServiceAccountKey());
        assertEquals(Set.of(), asset.requiredSecretKeys());
    }

    @Test
    void aServiceAccountAssetDeclaresTheVariableItNeeds()
    {
        LLMGoogleAsset asset = asset("GOOGLE_SA_KEY");

        assertTrue(asset.usesServiceAccountKey());
        assertEquals(Set.of("GOOGLE_SA_KEY"), asset.requiredSecretKeys());
    }

    @Test
    void declaresTheLLMGoogleAssetType()
    {
        assertEquals(LLMGoogleModule.TYPE, asset(null).type());
    }

    private static LLMGoogleAsset asset(String credentialsKey)
    {
        return new LLMGoogleAsset(AssetIdentity.of("g", "env", "G"), "https://monitoring.googleapis.com", "my-project", credentialsKey, 0,
            new LLMGoogleProfile(LLMProvider.google_genai, "gemini-2.5-flash", Country.US, new LLMGoogleProfile.DynamicProfile("f", null)));
    }
}
