package tech.illuin.wombat.core.activity.commons;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class CountryTest
{
    @ParameterizedTest
    @CsvSource({
        "fr, FR, FRA, France",
        "FR, FR, FRA, France",
        "Fr, FR, FRA, France",
        "de, DE, DEU, Germany",
        "DE, DE, DEU, Germany",
        "jp, JP, JPN, Japan",
        "gb, GB, GBR, United Kingdom"
    })
    void testOfAlpha2Valid(String input, String expectedAlpha2, String expectedAlpha3, String expectedDisplayName)
    {
        Country country = Country.ofAlpha2(input);
        assertNotNull(country);
        assertEquals(expectedAlpha2, country.alpha2());
        assertEquals(expectedAlpha3, country.alpha3());
        assertEquals(expectedDisplayName, country.displayName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  ", " INVALID ", "ZZ", "12", "ABC", "FR "})
    void testOfAlpha2Invalid(String input)
    {
        assertNull(Country.ofAlpha2(input));
    }

    @Test
    void testOfAlpha2Null()
    {
        assertNull(Country.ofAlpha2(null));
    }

    @ParameterizedTest
    @CsvSource({
        "fra, FR, FRA, France",
        "FRA, FR, FRA, France",
        "Fra, FR, FRA, France",
        "deu, DE, DEU, Germany",
        "DEU, DE, DEU, Germany",
        "jpn, JP, JPN, Japan",
        "gbr, GB, GBR, United Kingdom"
    })
    void testOfAlpha3Valid(String input, String expectedAlpha2, String expectedAlpha3, String expectedDisplayName)
    {
        Country country = Country.ofAlpha3(input);
        assertNotNull(country);
        assertEquals(expectedAlpha2, country.alpha2());
        assertEquals(expectedAlpha3, country.alpha3());
        assertEquals(expectedDisplayName, country.displayName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  ", " INVALID ", "ZZZ", "123", "AB", "FRA "})
    void testOfAlpha3Invalid(String input)
    {
        Country country = Country.ofAlpha3(input);
        assertNull(country);
    }

    @Test
    void testOfAlpha3Null()
    {
        Country country = Country.ofAlpha3(null);
        assertNull(country);
    }

    @Test
    void testOfAlpha2AndAlpha3ReturnSameInstance()
    {
        Country fromAlpha2 = Country.ofAlpha2("FR");
        Country fromAlpha3 = Country.ofAlpha3("FRA");

        assertNotNull(fromAlpha2);
        assertNotNull(fromAlpha3);
        assertSame(fromAlpha2, fromAlpha3);
        assertSame(Country.FRANCE, fromAlpha3);
    }

    @Test
    void testDisplayNameWithLocale()
    {
        Country france = Country.FRANCE;
        assertEquals("France", france.displayName(Locale.ENGLISH));
        assertEquals("France", france.displayName(Locale.FRENCH));
        assertEquals("フランス", france.displayName(Locale.JAPANESE));

        Country germany = Country.GERMANY;
        assertEquals("Germany", germany.displayName(Locale.ENGLISH));
        assertEquals("Allemagne", germany.displayName(Locale.FRENCH));
        assertEquals("ドイツ", germany.displayName(Locale.JAPANESE));
    }

    @Test
    void testAllISOCountriesIndexed()
    {
        for (String code : Locale.getISOCountries())
        {
            Country country = Country.ofAlpha2(code);
            assertNotNull(country, "Expected country for ISO code " + code + " to be present");
            assertEquals(code, country.alpha2());
            assertNotNull(country.alpha3());
            assertFalse(country.alpha3().isBlank());
            assertNotNull(country.displayName());
            assertFalse(country.displayName().isBlank());

            Country fromAlpha3 = Country.ofAlpha3(country.alpha3());
            assertNotNull(fromAlpha3, "Expected country for ISO3 code " + country.alpha3() + " to be present");
            assertSame(country, fromAlpha3);
        }
    }
}
