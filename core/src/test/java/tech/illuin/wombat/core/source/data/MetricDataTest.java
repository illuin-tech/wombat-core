package tech.illuin.wombat.core.source.data;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.profile.LLMProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MetricDataTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final KubernetesData KUBERNETES = new KubernetesData("api", "cluster", "namespace", "pod", 100.0, 2048.0);
    private static final LLMData LLM = new LLMData("chat", LLMProvider.mistralai, "mistral-large-latest", Country.ofCode("FRA").orElseThrow(), 120L, 1L);

    @Test
    void metricData_isTaggedWithItsFamily()
    {
        JsonNode kubernetes = MAPPER.valueToTree(KUBERNETES);

        assertEquals("KUBERNETES_CONTAINER", kubernetes.get("family").asText());
        assertEquals("LLM", MAPPER.<JsonNode>valueToTree(LLM).get("family").asText());
        assertFalse(kubernetes.has("serviceFamily"));
    }

    @Test
    void metricData_roundTripsToItsVariant() throws Exception
    {
        for (MetricData data : List.of(KUBERNETES, LLM))
            assertEquals(data, MAPPER.readValue(MAPPER.writeValueAsString(data), MetricData.class));
    }

    @Test
    void familyNames_matchTheServiceFamilyOfEachVariant()
    {
        for (JsonSubTypes.Type type : MetricData.class.getAnnotation(JsonSubTypes.class).value())
        {
            MetricData sample = List.of(KUBERNETES, LLM).stream().filter(type.value()::isInstance).findFirst().orElseThrow();
            assertEquals(sample.serviceFamily().name(), type.name());
        }
    }
}
