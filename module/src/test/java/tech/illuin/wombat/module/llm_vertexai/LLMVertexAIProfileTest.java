package tech.illuin.wombat.module.llm_vertexai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.commons.Country;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LLMVertexAIProfileTest
{
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void omittedModelsMeansAllModels() throws Exception
    {
        LLMVertexAIProfile profile = this.mapper.readValue("""
            {"output-tokens-per-request":500}
            """, LLMVertexAIProfile.class);

        assertEquals(List.of(), profile.models());
    }

    @Test
    void modelListIsCopiedAndImmutable()
    {
        List<String> models = new ArrayList<>(List.of("gemini-2.5-flash", "gemini-2.5-pro"));
        LLMVertexAIProfile profile = new LLMVertexAIProfile(500, models);
        models.clear();

        assertEquals(2, profile.models().size());
        assertThrows(UnsupportedOperationException.class, () -> profile.models().add("other"));
    }

    @Test
    void oldRawFilterConfigurationIsRejected()
    {
        assertThrows(UnrecognizedPropertyException.class, () -> this.mapper.readValue("""
            {"output-tokens-per-request":500,
             "dynamic-profile":{"filter":"anything"}}
            """, LLMVertexAIProfile.class));
    }
}
