package tech.illuin.wombat.ui;

import org.junit.jupiter.api.Test;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceImpactRequest;
import tech.illuin.wombat.handler.model.BoaviztaKubernetesConfig;
import tech.illuin.wombat.handler.model.ClusterInfo;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemplateFormattersTest
{

    @Test
    void asLocalDateTime_formatsInstant()
    {
        assertEquals("2026-06-04T13:30", TemplateFormatters.asLocalDateTime(Instant.parse("2026-06-04T13:30:00Z")));
    }

    @Test
    void asLocalDateTime_nullOrSentinel_returnsEmpty()
    {
        assertEquals("", TemplateFormatters.asLocalDateTime(null));
        assertEquals("", TemplateFormatters.asLocalDateTime(Instant.MIN));
        assertEquals("", TemplateFormatters.asLocalDateTime(Instant.MAX));
    }

    @Test
    void asIsoUtc_returnsIsoString()
    {
        assertEquals("2026-06-04T13:30:00Z", TemplateFormatters.asIsoUtc(Instant.parse("2026-06-04T13:30:00Z")));
    }

    @Test
    void asPercent_multipliesByHundredAndAppendsSymbol()
    {
        assertEquals("42.50%", TemplateFormatters.asPercent(0.425d));
        assertEquals("0.00%", TemplateFormatters.asPercent(0.0d));
    }

    @Test
    void asPercent_null_returnsDash()
    {
        assertEquals("—", TemplateFormatters.asPercent(null));
    }

    @Test
    void asDecimal_largeValue_formattedWithTwoDecimals()
    {
        assertEquals("3.14", TemplateFormatters.asDecimal(3.14f));
    }

    @Test
    void asDecimal_trailingZerosTrimmed()
    {
        assertEquals("5", TemplateFormatters.asDecimal(5.0f));
    }

    @Test
    void asDecimal_verySmallValue_usesScientificNotation()
    {
        String result = TemplateFormatters.asDecimal(0.0001f);
        assertEquals("1E-4", result);
    }

    @Test
    void asDecimal_null_returnsDash()
    {
        assertEquals("—", TemplateFormatters.asDecimal((Float) null));
        assertEquals("—", TemplateFormatters.asDecimal((Double) null));
    }

    @Test
    void provider_returnsProviderName()
    {
        BoaviztaKubernetesConfig config = new BoaviztaKubernetesConfig(
            BoaviztaInstanceImpactRequest.Provider.aws, "c5", "FRA", 1, List.of());
        assertEquals("aws", TemplateFormatters.provider(config));
    }

    @Test
    void instanceType_returnsValue()
    {
        BoaviztaKubernetesConfig config = new BoaviztaKubernetesConfig(
            BoaviztaInstanceImpactRequest.Provider.aws, "c5", "FRA", 1, List.of());
        assertEquals("c5", TemplateFormatters.instanceType(config));
    }

    @Test
    void clusters_returnsList()
    {
        ClusterInfo cluster = new ClusterInfo("c1", "ns");
        BoaviztaKubernetesConfig config = new BoaviztaKubernetesConfig(
            BoaviztaInstanceImpactRequest.Provider.aws, "c5", "FRA", 1, List.of(cluster));
        assertEquals(List.of(cluster), TemplateFormatters.clusters(config));
    }

    @Test
    void lifespan_returnsValue()
    {
        BoaviztaKubernetesConfig config = new BoaviztaKubernetesConfig(
            BoaviztaInstanceImpactRequest.Provider.aws, "c5", "FRA", 43800, List.of());
        assertEquals(43800, TemplateFormatters.lifespan(config));
    }
}
