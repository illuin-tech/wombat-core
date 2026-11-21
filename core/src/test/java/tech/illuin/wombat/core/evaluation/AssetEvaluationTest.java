package tech.illuin.wombat.core.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.evaluation.cost.commons.AssetCost;
import tech.illuin.wombat.core.evaluation.impact.commons.AssetImpact;
import tech.illuin.wombat.core.evaluation.impact.commons.Footprint;
import tech.illuin.wombat.core.evaluation.impact.commons.ImpactProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssetEvaluationTest
{
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private static final AssetIdentity IDENTITY = AssetIdentity.of("chatbot", "prod", "tech.illuin.wombat-module.llm-static");

    @Test
    void type_isSerialized_soThatAnImpactIsToldFromACost()
    {
        JsonNode impact = this.objectMapper.valueToTree(new AssetImpact(IDENTITY, Footprint.sum(List.of()), List.of(), ImpactProvider.ECOLOGITS, null));
        JsonNode cost = this.objectMapper.valueToTree(new AssetCost(IDENTITY, null));

        assertEquals("IMPACT", impact.get("type").asText());
        assertEquals("COST", cost.get("type").asText());
    }

    @Test
    void type_isIgnored_whenReadBack() throws Exception
    {
        AssetCost cost = new AssetCost(IDENTITY, null);

        assertEquals(cost, this.objectMapper.readValue(this.objectMapper.writeValueAsString(cost), AssetCost.class));
    }
}
