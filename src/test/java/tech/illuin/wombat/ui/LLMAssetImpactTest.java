package tech.illuin.wombat.ui;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.ecologits.model.EcologitsEstimationRequest;
import tech.illuin.wombat.ecologits.model.EcologitsEstimationResponse;
import tech.illuin.wombat.monitor.AssetType;
import tech.illuin.wombat.model.Footprint;
import tech.illuin.wombat.asset.model.profile.LLMProfile;
import tech.illuin.wombat.asset.model.profile.StaticLLMProfile;
import tech.illuin.wombat.model.ImpactProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LLMAssetImpactTest
{

    @Test
    void toFootprint_mapsUsageAndEmbodiedPhasesScaledByRequestCount()
    {
        LLMAssetImpact impact = new LLMStaticAssetImpact("My LLM", profile(), estimation(), 500, 10.0, true);

        Footprint footprint = impact.toFootprint();

        assertEquals("my-model", footprint.service());
        assertEquals(ImpactProvider.ECOLOGITS, footprint.impactProvider());
        assertEquals(AssetType.LLM_STATIC, footprint.type());
        assertEquals(0.016f, footprint.gwp().use().value(), 1e-6);
        assertEquals(0.004f, footprint.gwp().embedded().value(), 1e-6);
        assertEquals("kgCO2eq", footprint.gwp().unit());
        assertEquals("MJ", footprint.pe().unit());
        assertEquals("kgSbeq", footprint.adp().unit());
    }

    @Test
    void toFootprint_withoutPhaseSplit_reportsTotalAsUse()
    {
        EcologitsEstimationResponse noPhases = new EcologitsEstimationResponse(new EcologitsEstimationResponse.Impacts(
            metric("energy", "kWh", 0.1, 0.2),
            metric("GWP", "kgCO2eq", 0.001, 0.003),
            metric("ADPe", "kgSbeq", 0.0000001, 0.0000002),
            metric("PE", "MJ", 0.01, 0.02),
            metric("WCF", "L", 0.001, 0.002),
            null,
            null
        ));
        LLMAssetImpact impact = new LLMStaticAssetImpact("My LLM", profile(), noPhases, 500, 10.0, true);

        Footprint footprint = impact.toFootprint();

        assertEquals(0.02f, footprint.gwp().use().value(), 1e-6);
        assertEquals(0f, footprint.gwp().embedded().value(), 1e-6);
    }

    private static LLMProfile profile()
    {
        return new StaticLLMProfile(
            EcologitsEstimationRequest.Provider.mistralai,
            "my-model",
            "FRA",
            new StaticLLMProfile.RequestProfile(500, 1000000)
        );
    }

    private static EcologitsEstimationResponse estimation()
    {
        return new EcologitsEstimationResponse(new EcologitsEstimationResponse.Impacts(
            metric("energy", "kWh", 0.1, 0.2),
            metric("GWP", "kgCO2eq", 0.001, 0.003),
            metric("ADPe", "kgSbeq", 0.0000001, 0.0000002),
            metric("PE", "MJ", 0.01, 0.02),
            metric("WCF", "L", 0.001, 0.002),
            new EcologitsEstimationResponse.Usage(
                "usage", "Usage",
                metric("energy", "kWh", 0.08, 0.16),
                metric("GWP", "kgCO2eq", 0.0008, 0.0024),
                metric("ADPe", "kgSbeq", 0.00000008, 0.00000016),
                metric("PE", "MJ", 0.008, 0.016),
                metric("WCF", "L", 0.0008, 0.0016)
            ),
            new EcologitsEstimationResponse.Embodied(
                "embodied", "Embodied",
                metric("GWP", "kgCO2eq", 0.0002, 0.0006),
                metric("ADPe", "kgSbeq", 0.00000002, 0.00000004),
                metric("PE", "MJ", 0.002, 0.004)
            )
        ));
    }

    private static EcologitsEstimationResponse.Metric metric(String name, String unit, double min, double max)
    {
        return new EcologitsEstimationResponse.Metric("impact", name, new EcologitsEstimationResponse.Metric.EcologitsRange(min, max), unit);
    }
}
