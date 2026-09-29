package tech.illuin.wombat.core.connector.ecologits.impact;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.activity.llm.LLMActivityData;
import tech.illuin.wombat.core.activity.llm.LLMServiceActivity;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.profile.AssetProfile;
import tech.illuin.wombat.core.asset.profile.LLMProfile;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.core.asset.type.ActivityRegime;
import tech.illuin.wombat.core.asset.type.AssetType;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.connector.ecologits.connector.EcologitsClient;
import tech.illuin.wombat.core.connector.ecologits.connector.model.EcologitsEstimationResponse;
import tech.illuin.wombat.core.evaluation.AssetEvaluation;
import tech.illuin.wombat.core.evaluation.impact.commons.AssetImpact;
import tech.illuin.wombat.core.evaluation.impact.llm.LLMImpact;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EcologitsEvaluationResolverTest
{
    private static final AssetType LLM_TYPE = AssetType.of("tech.test", "llm", ActivityRegime.MODELED, ServiceFamily.LLM);

    private final EcologitsClient dummyClient = req -> dummyResponse(req.outputTokenCount());
    private final EcologitsEvaluationResolver resolver = new EcologitsEvaluationResolver(this.dummyClient);

    @Test
    void accept_llmFamilyWithLLMProfile_returnsTrue()
    {
        assertTrue(this.resolver.accept(new TestAsset(LLM_TYPE, new TestLLMProfile(LLMProvider.mistralai, "mistral-large", "FRA"))));
    }

    @Test
    void accept_llmFamilyWithPlainProfile_returnsFalse()
    {
        assertFalse(this.resolver.accept(new TestAsset(LLM_TYPE, new AssetProfile() {})));
    }

    @Test
    void accept_llmFamilyWithNullProfile_returnsFalse()
    {
        assertFalse(this.resolver.accept(new TestAsset(LLM_TYPE, null)));
    }

    @Test
    void resolve_multiServiceActivity_calculatesSharesAndAggregatesFootprint() throws Exception
    {
        TestLLMProfile profile = new TestLLMProfile(LLMProvider.mistralai, "group-profile", "FRA");
        TestAsset asset = new TestAsset(LLM_TYPE, profile);

        LLMServiceActivity s1 = new LLMServiceActivity(LLMProvider.mistralai, "mistral-large", "FRA", 1000L, 10.0);
        LLMServiceActivity s2 = new LLMServiceActivity(LLMProvider.openai, "gpt-4o", "USA", 3000L, 30.0);

        TimeRange range = new TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1000));
        LLMActivityData activity = new LLMActivityData(
            ActivityRegime.MODELED,
            Set.of("mistral-large", "gpt-4o"),
            range,
            Map.of("mistral-large", s1, "gpt-4o", s2)
        );

        AssetEvaluation evaluation = this.resolver.resolve(asset, activity);
        assertInstanceOf(AssetImpact.class, evaluation);

        AssetImpact impact = (AssetImpact) evaluation;
        assertEquals("env", impact.environmentId());
        assertEquals("asset", impact.assetId());
        assertEquals(2, impact.serviceImpacts().size());

        // Check shares (1000 / 4000 = 0.25, 3000 / 4000 = 0.75)
        LLMImpact impact1 = impact.serviceImpacts().stream()
            .filter(si -> si.serviceId().equals("mistral-large"))
            .map(LLMImpact.class::cast)
            .findFirst()
            .orElseThrow();
        assertEquals(0.25, impact1.share(), 1e-4);
        assertEquals(LLMProvider.mistralai, impact1.provider());
        assertEquals("mistral-large", impact1.model());

        LLMImpact impact2 = impact.serviceImpacts().stream()
            .filter(si -> si.serviceId().equals("gpt-4o"))
            .map(LLMImpact.class::cast)
            .findFirst()
            .orElseThrow();
        assertEquals(0.75, impact2.share(), 1e-4);
        assertEquals(LLMProvider.openai, impact2.provider());
        assertEquals("gpt-4o", impact2.model());

        // Footprint total equals sum of service footprints
        float expectedGwp = (float) (impact1.footprint().gwp().totalValue() + impact2.footprint().gwp().totalValue());
        assertEquals(expectedGwp, impact.footprint().gwp().totalValue(), 1e-4);
    }

    private static EcologitsEstimationResponse dummyResponse(double value)
    {
        EcologitsEstimationResponse.Metric gwp = new EcologitsEstimationResponse.Metric("gwp", "GWP", EcologitsEstimationResponse.Metric.EcologitsRange.of(value), "kgCO2eq");
        EcologitsEstimationResponse.Metric pe = new EcologitsEstimationResponse.Metric("pe", "PE", EcologitsEstimationResponse.Metric.EcologitsRange.of(value * 2), "MJ");
        EcologitsEstimationResponse.Metric adpe = new EcologitsEstimationResponse.Metric("adpe", "ADPe", EcologitsEstimationResponse.Metric.EcologitsRange.of(value * 0.1), "kgSbeq");
        EcologitsEstimationResponse.Impacts impacts = new EcologitsEstimationResponse.Impacts(
            null, gwp, adpe, pe, null, null, null
        );
        return new EcologitsEstimationResponse(impacts);
    }

    private record TestLLMProfile(LLMProvider provider, String model, String location) implements LLMProfile
    {
    }

    private record TestAsset(AssetType type, AssetProfile profile) implements Asset
    {
        @Override
        public AssetIdentity identity()
        {
            return AssetIdentity.of("asset", "env", "Asset");
        }
    }
}
