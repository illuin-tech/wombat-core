package tech.illuin.wombat.module.llm_static.activity;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.commons.AssetActivity;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.activity.llm.LLMActivityData;
import tech.illuin.wombat.core.activity.llm.LLMServiceActivity;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.profile.LLMProvider;
import tech.illuin.wombat.module.llm_static.LLMStaticAsset;
import tech.illuin.wombat.module.llm_static.LLMStaticProfile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LLMStaticActivityResolverTest
{
    private final LLMStaticActivityResolver resolver = new LLMStaticActivityResolver();

    @Test
    void resolve_multiModelStaticProfile_computesActivityForEachModel() throws Exception
    {
        LLMStaticProfile.ModelConfig m1 = new LLMStaticProfile.ModelConfig(
            LLMProvider.mistralai,
            "mistral-large-latest",
            Country.FRANCE,
            new LLMStaticProfile.RequestProfile(500, 31536000) // 1 req/sec -> 31536000 req/year
        );
        LLMStaticProfile.ModelConfig m2 = new LLMStaticProfile.ModelConfig(
            LLMProvider.openai,
            "gpt-4o",
            Country.US,
            new LLMStaticProfile.RequestProfile(1000, 63072000) // 2 req/sec
        );

        LLMStaticProfile profile = new LLMStaticProfile(List.of(m1, m2));
        LLMStaticAsset asset = new LLMStaticAsset(AssetIdentity.of("llm-asset", "test-env", "Test Static Asset"), profile);

        // 100 seconds range
        TimeRange range = new TimeRange(Instant.ofEpochSecond(0), Instant.ofEpochSecond(100));

        Optional<AssetActivity> activityOpt = this.resolver.resolve(asset, range, null);
        assertTrue(activityOpt.isPresent());
        assertInstanceOf(LLMActivityData.class, activityOpt.get().activity().getFirst());

        AssetActivity activity = activityOpt.get();
        LLMActivityData llmActivity = (LLMActivityData) activity.activity().getFirst();
        assertEquals(2, llmActivity.services().size());

        // 100 seconds out of 365 days (31536000s in year)
        LLMServiceActivity a1 = llmActivity.services().get("mistral-large-latest");
        assertNotNull(a1);
        assertEquals(LLMProvider.mistralai, a1.provider());
        assertEquals("mistral-large-latest", a1.model());
        assertEquals(100, (int) a1.requestCount());
        assertEquals(50000L, a1.outputTokenCount()); // 500 * 100

        LLMServiceActivity a2 = llmActivity.services().get("gpt-4o");
        assertNotNull(a2);
        assertEquals(LLMProvider.openai, a2.provider());
        assertEquals("gpt-4o", a2.model());
        assertEquals(200, (int) a2.requestCount());
        assertEquals(200000L, a2.outputTokenCount()); // 1000 * 200

        assertEquals(250000L, llmActivity.outputTokenCount());
        assertEquals(300.0, llmActivity.requestCount());
    }
}
