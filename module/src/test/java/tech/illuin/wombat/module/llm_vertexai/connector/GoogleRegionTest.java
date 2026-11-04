package tech.illuin.wombat.module.llm_vertexai.connector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tech.illuin.wombat.core.activity.commons.Country;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GoogleRegionTest
{
    @ParameterizedTest
    @CsvSource({
        "us-east5, US",
        "us-south1, US",
        "us-central1, US",
        "us-west4, US",
        "us-west2, US",
        "us-east1, US",
        "us-east4, US",
        "us-west1, US",
        "us-west3, US",
        "northamerica-northeast1, CA",
        "northamerica-northeast2, CA",
        "southamerica-west1, CL",
        "southamerica-east1, BR",
        "africa-south1, ZA",
        "europe-west1, BE",
        "europe-north1, FI",
        "europe-west3, DE",
        "europe-west2, GB",
        "europe-southwest1, ES",
        "europe-west8, IT",
        "europe-west4, NL",
        "europe-west9, FR",
        "europe-west12, IT",
        "europe-central2, PL",
        "europe-west6, CH",
        "asia-east2, HK",
        "asia-southeast2, ID",
        "australia-southeast2, AU",
        "asia-south1, IN",
        "asia-south2, IN",
        "asia-northeast2, JP",
        "asia-northeast3, KR",
        "asia-southeast1, SG",
        "australia-southeast1, AU",
        "asia-east1, TW",
        "asia-northeast1, JP",
        "me-central2, SA",
        "me-central1, QA",
        "me-west1, IL",
        "global, ''"
    })
    void testTranslate(String regionIdentifier, String expectedAlpha2)
    {
        Optional<Country> translated = GoogleRegion.fromIdentifier(regionIdentifier).map(GoogleRegion::country);
        assertTrue(translated.isPresent(), "Expected country for region: " + regionIdentifier);
        assertEquals(expectedAlpha2, translated.get().alpha2());
    }

    @Test
    void testTranslateUnknown()
    {
        assertTrue(GoogleRegion.fromIdentifier("unknown-region").map(GoogleRegion::country).isEmpty());
        assertTrue(GoogleRegion.fromIdentifier(null).map(GoogleRegion::country).isEmpty());
    }

    @Test
    void testAllEnumValuesHaveNonNullFields()
    {
        assertEquals(40, GoogleRegion.values().length);
        for (GoogleRegion region : GoogleRegion.values())
        {
            assertNotNull(region.identifier());
            assertFalse(region.identifier().isBlank());
            assertNotNull(region.country());
            assertEquals(region, GoogleRegion.fromIdentifier(region.identifier()).orElse(null));
        }
    }
}
