package tech.illuin.wombat.module.llm_vertexai;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.asset.AssetIdentity;

import java.util.Set;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LLMVertexAIAssetTest
{
    @Test
    void anAssetRelyingOnApplicationDefaultCredentialsNeedsNoSecret()
    {
        LLMVertexAIAsset asset = asset(null);

        assertFalse(asset.usesServiceAccountKey());
        assertEquals(Set.of(), asset.requiredSecretKeys());
    }

    @Test
    void aBlankCredentialsKeyIsTreatedAsAbsent()
    {
        LLMVertexAIAsset asset = asset("  ");

        assertFalse(asset.usesServiceAccountKey());
        assertEquals(Set.of(), asset.requiredSecretKeys());
    }

    @Test
    void aServiceAccountAssetDeclaresTheVariableItNeeds()
    {
        LLMVertexAIAsset asset = asset("GOOGLE_SA_KEY");

        assertTrue(asset.usesServiceAccountKey());
        assertEquals(Set.of("GOOGLE_SA_KEY"), asset.requiredSecretKeys());
    }

    @Test
    void declaresTheLLMVertexAIAssetType()
    {
        assertEquals(LLMVertexAIModule.TYPE, asset(null).type());
    }

    private static LLMVertexAIAsset asset(String credentialsKey)
    {
        return new LLMVertexAIAsset(AssetIdentity.of("g", "env", "G", LLMVertexAIModule.TYPE), "https://monitoring.googleapis.com", "projects/my-project", credentialsKey,
            new LLMVertexAIProfile(500, List.of("gemini-2.5-flash")));
    }
}
