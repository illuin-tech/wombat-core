package tech.illuin.wombat.boavizta;

import tech.illuin.wombat.boavizta.model.BoaviztaInstanceConfigResponse;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactResponse;

import java.util.List;
import java.util.Map;

public final class BoaviztaTestData
{

    private BoaviztaTestData() {}

    public static BoaviztaInstanceConfigResponse fakeInstanceConfig(int vcpu)
    {
        return new BoaviztaInstanceConfigResponse(
            new BoaviztaInstanceConfigResponse.IntConfigItem(vcpu),
            new BoaviztaInstanceConfigResponse.IntConfigItem(16),
            new BoaviztaInstanceConfigResponse.IntConfigItem(0),
            new BoaviztaInstanceConfigResponse.IntConfigItem(0),
            new BoaviztaInstanceConfigResponse.IntConfigItem(0),
            new BoaviztaInstanceConfigResponse.StringConfigItem("linux")
        );
    }

    public static BoaviztaInstanceImpactResponse fakeImpactResponse()
    {
        BoaviztaInstanceImpactResponse.Impact gwp = impact("kgCO2eq", "Global warming potential", 100f, 200f);
        BoaviztaInstanceImpactResponse.Impact pe = impact("MJ", "Primary energy", 300f, 400f);
        BoaviztaInstanceImpactResponse.Impact adp = impact("kgSbeq", "Abiotic resource depletion", 0.001f, 0.002f);
        return new BoaviztaInstanceImpactResponse(Map.of("gwp", gwp, "pe", pe, "adp", adp), 1);
    }

    private static BoaviztaInstanceImpactResponse.Impact impact(String unit, String description, float embedded, float use)
    {
        return new BoaviztaInstanceImpactResponse.Impact(
            unit, description,
            new BoaviztaInstanceImpactResponse.Impact.ImpactItem(embedded, embedded, embedded, List.of()),
            new BoaviztaInstanceImpactResponse.Impact.ImpactItem(use, use, use, List.of())
        );
    }
}
