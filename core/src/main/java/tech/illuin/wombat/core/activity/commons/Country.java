package tech.illuin.wombat.core.activity.commons;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class Country
{
    private final Locale locale;

    private static final Map<String, Country> indexAlpha2;
    private static final Map<String, Country> indexAlpha3;

    static {
        indexAlpha2 = new HashMap<>();
        indexAlpha3 = new HashMap<>();
        for (String code : Locale.getISOCountries())
        {
            var country = new Country(code);
            indexAlpha2.put(country.alpha2(), country);
            indexAlpha3.put(country.alpha3(), country);
        }
    }

    public static final Country AUSTRALIA = ofAlpha2("AU");
    public static final Country BRAZIL = ofAlpha2("BR");
    public static final Country CANADA = ofAlpha2("CA");
    public static final Country CHINA = ofAlpha2("CN");
    public static final Country FRANCE = ofAlpha2("FR");
    public static final Country GERMANY = ofAlpha2("DE");
    public static final Country INDIA = ofAlpha2("IN");
    public static final Country IRELAND = ofAlpha2("IE");
    public static final Country JAPAN = ofAlpha2("JP");
    public static final Country NETHERLANDS = ofAlpha2("NL");
    public static final Country SINGAPORE = ofAlpha2("SG");
    public static final Country SOUTH_KOREA = ofAlpha2("KR");
    public static final Country SWEDEN = ofAlpha2("SE");
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

    public static Optional<Country> ofCode(String code)
    {
        return Optional.ofNullable(ofAlpha2(code))
            .or(() -> Optional.ofNullable(ofAlpha3(code)));
    }

    private Country(Locale locale)
    {
        this.locale = locale;
    }

    private Country(String alpha2Code)
    {
        this(Locale.of("", alpha2Code.toUpperCase()));
    }

    public String alpha2()
    {
        return this.locale.getCountry();
    }

    @JsonValue
    public String alpha3()
    {
        return this.locale.getISO3Country();
    }

    public String displayName()
    {
        return this.locale.getDisplayCountry(Locale.ENGLISH);
    }

    public String displayName(Locale locale)
    {
        return this.locale.getDisplayCountry(locale);
    }
}
