package tech.illuin.wombat.core.connector.boavizta.impact;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.asset.ActivityRegime;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetType;
import tech.illuin.wombat.core.asset.ServiceFamily;
import tech.illuin.wombat.core.asset.profile.Profile;
import tech.illuin.wombat.core.asset.profile.ServerProfile;
import tech.illuin.wombat.core.asset.profile.ServerProvider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoaviztaEvaluationResolverTest
{
    private static final AssetType K8S_TYPE = AssetType.of("tech.test", "k8s", ActivityRegime.MEASURED, ServiceFamily.KUBERNETES_CONTAINER);
    private static final AssetType LLM_TYPE = AssetType.of("tech.test", "llm", ActivityRegime.MODELED, ServiceFamily.LLM);

    private final BoaviztaEvaluationResolver resolver = new BoaviztaEvaluationResolver(null);

    @Test
    void accept_kubernetesFamilyWithServerProfile_returnsTrue()
    {
        assertTrue(this.resolver.accept(new TestAsset(K8S_TYPE, new TestServerProfile(ServerProvider.aws, "c5a.4xlarge", "FRA", 43800))));
    }

    @Test
    void accept_kubernetesFamilyWithPlainProfile_returnsFalse()
    {
        assertFalse(this.resolver.accept(new TestAsset(K8S_TYPE, () -> "plain")));
    }

    @Test
    void accept_kubernetesFamilyWithNullProfile_returnsFalse()
    {
        assertFalse(this.resolver.accept(new TestAsset(K8S_TYPE, null)));
    }

    private record TestServerProfile(ServerProvider provider, String instanceType, String location, int lifespan) implements ServerProfile {}

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
