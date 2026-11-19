package tech.illuin.wombat.core.activity.commons;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@JsonClassDescription("ISO 3166-1 alpha-3 country code, XXX for the world")
public final class Country
{
    private final Locale locale;
    private final String alpha2;
    private final String alpha3;
    private final String name;

    private static final Map<String, Country> indexAlpha2;
    private static final Map<String, Country> indexAlpha3;
    private static final List<Country> values;

    static {
        indexAlpha2 = new HashMap<>();
        indexAlpha3 = new HashMap<>();
        for (String code : Locale.getISOCountries())
        {
            var country = new Country(code);
            indexAlpha2.put(country.alpha2(), country);
            indexAlpha3.put(country.alpha3(), country);
        }

        var world = new Country(Locale.ROOT, "XX", "XXX", "World");
        indexAlpha2.put(world.alpha2(), world);
        indexAlpha3.put(world.alpha3(), world);

        values = indexAlpha3.values().stream()
            .sorted(Comparator.comparing(Country::alpha3))
            .toList();
    }

    public static final Country WORLD = ofAlpha2("XX");
    public static final Country AUSTRALIA = ofAlpha2("AU");
    public static final Country BELGIUM = ofAlpha2("BE");
    public static final Country BRAZIL = ofAlpha2("BR");
    public static final Country CANADA = ofAlpha2("CA");
    public static final Country CHILE = ofAlpha2("CL");
    public static final Country CHINA = ofAlpha2("CN");
    public static final Country FINLAND = ofAlpha2("FI");
    public static final Country FRANCE = ofAlpha2("FR");
    public static final Country GERMANY = ofAlpha2("DE");
    public static final Country HONG_KONG = ofAlpha2("HK");
    public static final Country INDIA = ofAlpha2("IN");
    public static final Country INDONESIA = ofAlpha2("ID");
    public static final Country IRELAND = ofAlpha2("IE");
    public static final Country ISRAEL = ofAlpha2("IL");
    public static final Country ITALY = ofAlpha2("IT");
    public static final Country JAPAN = ofAlpha2("JP");
    public static final Country NETHERLANDS = ofAlpha2("NL");
    public static final Country POLAND = ofAlpha2("PL");
    public static final Country QATAR = ofAlpha2("QA");
    public static final Country SAUDI_ARABIA = ofAlpha2("SA");
    public static final Country SINGAPORE = ofAlpha2("SG");
    public static final Country SOUTH_AFRICA = ofAlpha2("ZA");
    public static final Country SOUTH_KOREA = ofAlpha2("KR");
    public static final Country SPAIN = ofAlpha2("ES");
    public static final Country SWEDEN = ofAlpha2("SE");
    public static final Country SWITZERLAND = ofAlpha2("CH");
    public static final Country TAIWAN = ofAlpha2("TW");
    public static final Country UAE = ofAlpha2("AE");
    public static final Country UK = ofAlpha2("GB");
    public static final Country US = ofAlpha2("US");

    public static Country ofAlpha2(String alpha2)
    {
        if (alpha2 == null)
            return null;
        return indexAlpha2.get(alpha2.toUpperCase());
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Country ofAlpha3(String alpha3)
    {
        if (alpha3 == null)
            return null;
        return indexAlpha3.get(alpha3.toUpperCase());
    }

    /**
     * @return every known country, ordered by alpha-3 code
     */
    public static List<Country> values()
    {
        return values;
    }

    public static Optional<Country> ofCode(String code)
    {
        return Optional.ofNullable(ofAlpha2(code))
            .or(() -> Optional.ofNullable(ofAlpha3(code)));
    }

    private Country(Locale locale, String alpha2, String alpha3, String name)
    {
        this.locale = locale;
        this.alpha2 = alpha2;
        this.alpha3 = alpha3;
        this.name = name;
    }

    private Country(Locale locale)
    {
        this(locale, locale.getCountry(), locale.getISO3Country(), null);
    }

    private Country(String alpha2Code)
    {
        this(Locale.of("", alpha2Code.toUpperCase()));
    }

    public String alpha2()
    {
        return this.alpha2;
    }

    @JsonValue
    public String alpha3()
    {
        return this.alpha3;
    }

    public String displayName()
    {
        return this.displayName(Locale.ENGLISH);
    }

    public String displayName(Locale locale)
    {
        return this.name != null ? this.name : this.locale.getDisplayCountry(locale);
    }
}
