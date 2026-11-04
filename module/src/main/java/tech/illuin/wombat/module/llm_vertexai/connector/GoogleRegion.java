package tech.illuin.wombat.module.llm_vertexai.connector;

import tech.illuin.wombat.core.activity.commons.Country;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum GoogleRegion
{
    GLOBAL("global", Country.WORLD),
    US_EAST5("us-east5", Country.US),
    US_SOUTH1("us-south1", Country.US),
    US_CENTRAL1("us-central1", Country.US),
    US_WEST4("us-west4", Country.US),
    US_WEST2("us-west2", Country.US),
    US_EAST1("us-east1", Country.US),
    US_EAST4("us-east4", Country.US),
    US_WEST1("us-west1", Country.US),
    US_WEST3("us-west3", Country.US),
    NORTHAMERICA_NORTHEAST1("northamerica-northeast1", Country.CANADA),
    NORTHAMERICA_NORTHEAST2("northamerica-northeast2", Country.CANADA),
    SOUTHAMERICA_WEST1("southamerica-west1", Country.CHILE),
    SOUTHAMERICA_EAST1("southamerica-east1", Country.BRAZIL),
    AFRICA_SOUTH1("africa-south1", Country.SOUTH_AFRICA),
    EUROPE_WEST1("europe-west1", Country.BELGIUM),
    EUROPE_NORTH1("europe-north1", Country.FINLAND),
    EUROPE_WEST3("europe-west3", Country.GERMANY),
    EUROPE_WEST2("europe-west2", Country.UK),
    EUROPE_SOUTHWEST1("europe-southwest1", Country.SPAIN),
    EUROPE_WEST8("europe-west8", Country.ITALY),
    EUROPE_WEST4("europe-west4", Country.NETHERLANDS),
    EUROPE_WEST9("europe-west9", Country.FRANCE),
    EUROPE_WEST12("europe-west12", Country.ITALY),
    EUROPE_CENTRAL2("europe-central2", Country.POLAND),
    EUROPE_WEST6("europe-west6", Country.SWITZERLAND),
    ASIA_EAST2("asia-east2", Country.HONG_KONG),
    ASIA_SOUTHEAST2("asia-southeast2", Country.INDONESIA),
    AUSTRALIA_SOUTHEAST2("australia-southeast2", Country.AUSTRALIA),
    ASIA_SOUTH1("asia-south1", Country.INDIA),
    ASIA_SOUTH2("asia-south2", Country.INDIA),
    ASIA_NORTHEAST2("asia-northeast2", Country.JAPAN),
    ASIA_NORTHEAST3("asia-northeast3", Country.SOUTH_KOREA),
    ASIA_SOUTHEAST1("asia-southeast1", Country.SINGAPORE),
    AUSTRALIA_SOUTHEAST1("australia-southeast1", Country.AUSTRALIA),
    ASIA_EAST1("asia-east1", Country.TAIWAN),
    ASIA_NORTHEAST1("asia-northeast1", Country.JAPAN),
    ME_CENTRAL2("me-central2", Country.SAUDI_ARABIA),
    ME_CENTRAL1("me-central1", Country.QATAR),
    ME_WEST1("me-west1", Country.ISRAEL),
    ;

    private final String identifier;
    private final Country country;

    private static final Map<String, GoogleRegion> index = new HashMap<>();

    static {
        for (GoogleRegion region : values())
            index.put(region.identifier(), region);
    }

    GoogleRegion(String identifier, Country country)
    {
        this.identifier = identifier;
        this.country = country;
    }

    public String identifier() {
        return this.identifier;
    }

    public Country country() {
        return this.country;
    }

    public static Optional<GoogleRegion> fromIdentifier(String identifier) {
        return Optional.ofNullable(index.get(identifier));
    }
}
