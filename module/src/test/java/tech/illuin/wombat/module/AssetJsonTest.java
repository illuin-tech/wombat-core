package tech.illuin.wombat.module;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.module.llm_static.LLMStaticAsset;
import tech.illuin.wombat.module.llm_static.LLMStaticModule;
import tech.illuin.wombat.module.llm_static.LLMStaticProfile;
import tech.illuin.wombat.module.llm_static.scenario.ConstantScenario;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * The {@code type} property of an asset both selects its class and is its identity's {@link AssetIdentity#assetType()}.
 */
class AssetJsonTest
{
    private static final String TYPE = LLMStaticModule.TYPE.name();

    private static final LLMStaticAsset ASSET = new LLMStaticAsset(
        AssetIdentity.of("llm", "env", "LLM", LLMStaticModule.TYPE),
        new LLMStaticProfile(List.of(new LLMStaticProfile.ModelConfig(
            LLMProvider.mistralai, "mistral-large-latest", Country.FRANCE, List.of(new ConstantScenario(500, 1000, null))
        )))
    );

    @Test
    void read_fillsTheIdentityTypeFromTheTypeProperty() throws Exception
    {
        Asset asset = register(JsonMapper.builder().build()).readValue("""
            {
              "type": "%s",
              "id": "llm",
              "environment-id": "env",
              "name": "LLM",
              "profile": {
                "models": [{
                  "provider": "mistralai",
                  "model": "mistral-large-latest",
                  "location": "FRA",
                  "scenarios": [{"type": "constant", "output-token-per-request": 500, "request-per-year": 1000}]
                }]
              }
            }
            """.formatted(TYPE), Asset.class);

        LLMStaticAsset read = assertInstanceOf(LLMStaticAsset.class, asset);
        assertEquals(AssetIdentity.of("llm", "env", "LLM", TYPE), read.identity());
    }

    @Test
    void write_emitsTheTypePropertyOnce() throws Exception
    {
        ObjectMapper mapper = register(JsonMapper.builder().build());
        String json = mapper.writeValueAsString(ASSET);

        JsonNode node = mapper.readTree(json);
        assertEquals(TYPE, node.get("type").asText());
        assertEquals(1, json.split("\"type\":\"" + TYPE + "\"", -1).length - 1);
        assertEquals(ASSET, mapper.readValue(json, Asset.class));
    }

    @Test
    void identity_writesItsTypeOnItsOwn() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder().build();
        AssetIdentity identity = ASSET.identity();

        assertEquals(TYPE, mapper.readTree(mapper.writeValueAsString(identity)).get("type").asText());
        assertEquals(identity, mapper.readValue(mapper.writeValueAsString(identity), AssetIdentity.class));
    }

    private static <M extends ObjectMapper> M register(M mapper)
    {
        mapper.findAndRegisterModules();
        mapper.registerSubtypes(new NamedType(LLMStaticAsset.class, TYPE));
        return mapper;
    }
}
