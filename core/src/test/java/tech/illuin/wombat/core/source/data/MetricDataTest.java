package tech.illuin.wombat.core.source.data;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.profile.LLMProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void metricData_deserializesDirectlyToItsConcreteType() throws Exception
    {
        assertEquals(KUBERNETES, MAPPER.readValue(MAPPER.writeValueAsString(KUBERNETES), KubernetesData.class));
        assertEquals(LLM, MAPPER.readValue(MAPPER.writeValueAsString(LLM), LLMData.class));
    }

    @Test
    void metricData_deserializesLegacyPayloadWithoutFamilyToConcreteType() throws Exception
    {
        for (MetricData data : List.of(KUBERNETES, LLM))
        {
            ObjectNode tree = MAPPER.valueToTree(data);
            tree.remove("family");

            assertEquals(data, MAPPER.readValue(tree.toString(), data.getClass()));
        }
    }

    @Test
    void metricData_rejectsPolymorphicDeserializationWithoutFamily()
    {
        for (MetricData data : List.of(KUBERNETES, LLM))
        {
            ObjectNode tree = MAPPER.valueToTree(data);
            tree.remove("family");
            String untaggedJson = tree.toString();

            assertThrows(InvalidTypeIdException.class, () -> MAPPER.readValue(untaggedJson, MetricData.class));
        }
    }

    @Test
    void metricData_rejectsUnknownOrMismatchedFamily() throws Exception
    {
        String unknownFamily = """
            {"family":"UNKNOWN","serviceId":"api"}
            """;

        assertThrows(InvalidTypeIdException.class, () -> MAPPER.readValue(unknownFamily, MetricData.class));
        assertThrows(ValueInstantiationException.class, () -> MAPPER.readValue(unknownFamily, KubernetesData.class));
        assertThrows(ValueInstantiationException.class, () -> MAPPER.readValue(unknownFamily, LLMData.class));

        String llmJson = MAPPER.writeValueAsString(LLM);
        String k8sJson = MAPPER.writeValueAsString(KUBERNETES);

        assertThrows(InvalidTypeIdException.class, () -> MAPPER.readValue(llmJson, KubernetesData.class));
        assertThrows(InvalidTypeIdException.class, () -> MAPPER.readValue(k8sJson, LLMData.class));
    }
}
