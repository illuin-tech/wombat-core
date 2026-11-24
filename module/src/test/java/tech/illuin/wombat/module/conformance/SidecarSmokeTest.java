package tech.illuin.wombat.module.conformance;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.connector.boavizta.connector.model.BoaviztaImpactResponse;
import tech.illuin.wombat.core.connector.boavizta.connector.model.BoaviztaCloudInstanceImpactRequest;
import tech.illuin.wombat.core.connector.boavizta.connector.model.BoaviztaUsage;
import tech.illuin.wombat.core.connector.boavizta.connector.model.BoaviztaServerProvider;
import tech.illuin.wombat.core.connector.ecologits.connector.model.EcologitsEstimationRequest;
import tech.illuin.wombat.core.connector.ecologits.connector.model.EcologitsEstimationResponse;
import tech.illuin.wombat.module.connector.boavizta.BoaviztaClientConfig;
import tech.illuin.wombat.module.connector.ecologits.EcologitsClientConfig;
import tech.illuin.wombat.module.conformance.canonical.CanonicalBoavizta;
import tech.illuin.wombat.module.conformance.canonical.CanonicalEcologits;
import tech.illuin.wombat.module.conformance.canonical.CanonicalImpacts;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Wombat's clients and the canonical engines read the same figures for the same request, so that any gap the conformance
 * tests find lies in Wombat's calculation, not in the plumbing.
 */
@Tag("conformance")
class SidecarSmokeTest
{
    /* Wombat's client reads floats */
    private static final double FLOAT = 1e-6;

    @Test
    void readsTheVersionsTheImagesEmbed()
    {
        assertNotEquals("unknown", ConformanceSidecars.boaviztaVersion());
        assertNotEquals("unknown", ConformanceSidecars.ecologitsVersion());
    }

    @Test
    void wombatsBoaviztaClientReadsWhatBoaviztaSends()
    {
        BoaviztaImpactResponse wombat = BoaviztaClientConfig.create(ConformanceSidecars.boaviztaUri().toString(), null).getInstanceImpact(
            true,
            8760,
            Set.of("gwp", "adp", "pe"),
            new BoaviztaCloudInstanceImpactRequest(
                BoaviztaServerProvider.aws,
                "c5.2xlarge",
                new BoaviztaUsage("FRA", List.of(new BoaviztaUsage.LoadSegment(100, 30.0)))
            )
        );
        CanonicalImpacts canonical = new CanonicalBoavizta(ConformanceSidecars.boaviztaUri())
            .instance("aws", "c5.2xlarge", "FRA", null, 8760, List.of(new CanonicalBoavizta.Segment(100.0, 30.0)));

        assertAll(
            () -> assertSame(canonical.gwp(), wombat.impacts().get("gwp")),
            () -> assertSame(canonical.pe(), wombat.impacts().get("pe")),
            () -> assertSame(canonical.adp(), wombat.impacts().get("adp"))
        );
    }

    @Test
    void wombatsEcologitsClientReadsWhatEcologitsSends()
    {
        EcologitsEstimationResponse.Impacts wombat = EcologitsClientConfig.create(ConformanceSidecars.ecologitsUri().toString(), null).estimate(
            new EcologitsEstimationRequest(EcologitsEstimationRequest.Provider.openai, "gpt-4o", 300, "FRA")
        ).impacts();
        CanonicalImpacts canonical = new CanonicalEcologits(ConformanceSidecars.ecologitsUri())
            .request("openai", "gpt-4o", 300, "FRA");

        assertAll(
            () -> assertSame(canonical.gwp(), wombat.embodied().gwp(), wombat.usage().gwp()),
            () -> assertSame(canonical.pe(), wombat.embodied().pe(), wombat.usage().pe()),
            () -> assertSame(canonical.adp(), wombat.embodied().adpe(), wombat.usage().adpe())
        );
    }

    private static void assertSame(CanonicalImpacts.Phases expected, BoaviztaImpactResponse.Impact actual)
    {
        assertEquals(expected.unit(), actual.unit());
        assertEquals(expected.embedded(), actual.embedded().value(), Math.abs(expected.embedded()) * FLOAT);
        assertEquals(expected.use(), actual.use().value(), Math.abs(expected.use()) * FLOAT);
    }

    private static void assertSame(CanonicalImpacts.Phases expected, EcologitsEstimationResponse.Metric embodied, EcologitsEstimationResponse.Metric usage)
    {
        assertEquals(expected.unit(), usage.unit());
        assertEquals(expected.embedded(), EcologitsEstimationResponse.Metric.mean(embodied), Math.abs(expected.embedded()) * FLOAT);
        assertEquals(expected.use(), EcologitsEstimationResponse.Metric.mean(usage), Math.abs(expected.use()) * FLOAT);
    }
}
