package tech.illuin.wombat.core.connector.ecologits.impact;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.asset.ActivityRegime;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetType;
import tech.illuin.wombat.core.asset.ServiceFamily;
import tech.illuin.wombat.core.asset.profile.LLMProfile;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.core.asset.profile.Profile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EcologitsEvaluationResolverTest
{
    private static final AssetType LLM_TYPE = AssetType.of("tech.test", "llm", ActivityRegime.MODELED, ServiceFamily.LLM);
    private static final AssetType K8S_TYPE = AssetType.of("tech.test", "k8s", ActivityRegime.MEASURED, ServiceFamily.KUBERNETES_CONTAINER);

    private final EcologitsEvaluationResolver resolver = new EcologitsEvaluationResolver(null);

    @Test
    void accept_llmFamilyWithLLMProfile_returnsTrue()
    {
        assertTrue(this.resolver.accept(new TestAsset(LLM_TYPE, new TestLLMProfile(LLMProvider.mistralai, "mistral-large", "FRA"))));
    }

    @Test
    void accept_llmFamilyWithPlainProfile_returnsFalse()
    {
        assertFalse(this.resolver.accept(new TestAsset(LLM_TYPE, () -> "plain")));
    }

    @Test
    void accept_llmFamilyWithNullProfile_returnsFalse()
    {
        assertFalse(this.resolver.accept(new TestAsset(LLM_TYPE, null)));
    }

    private record TestLLMProfile(LLMProvider provider, String model, String location) implements LLMProfile {}

    private record TestAsset(AssetType type, Profile profile) implements Asset
    {
        @Override
        public String id()
        {
            return "asset";
        }

        @Override
        public String environmentId()
        {
            return "env";
        }

        @Override
        public String name()
        {
            return "Asset";
        }
    }
}
