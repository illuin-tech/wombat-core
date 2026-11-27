package tech.illuin.wombat.module.kubernetes_api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.asset.profile.BareServerProfile;
import tech.illuin.wombat.core.asset.profile.Lifetime;
import tech.illuin.wombat.core.asset.profile.ServerProvider;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class KubernetesAPIServerProfileTest
{
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final ValidatorFactory factory = Validation.byDefaultProvider()
        .configure()
        .messageInterpolator(new ParameterMessageInterpolator())
        .buildValidatorFactory();
    private static final Validator validator = factory.getValidator();

    @AfterAll
    static void closeFactory()
    {
        factory.close();
    }

    @Test
    void anUntypedProfileIsACloudOne() throws Exception
    {
        String json = """
            {
                "provider": "aws",
                "instance-type": "m5.large",
                "location": "FRA",
                "lifespan": "5y"
            }
            """;

        KubernetesAPICloudProfile profile = assertInstanceOf(KubernetesAPICloudProfile.class, this.objectMapper.readValue(json, KubernetesAPIServerProfile.class));

        assertEquals(ServerProvider.aws, profile.provider());
        assertEquals("m5.large", profile.instanceType());
        assertSame(Country.FRANCE, profile.location());
        assertEquals(Lifetime.ofYears(5), profile.lifespan());
        assertEquals(1, profile.nodeCount());
    }

    @Test
    void aServerProfileReadsItsComponents() throws Exception
    {
        String json = """
            {
                "type": "server",
                "location": "FRA",
                "lifespan": "5y",
                "cpu": {"units": 2, "coreUnits": 24, "name": "Intel Xeon Gold 6342"},
                "ram": {"units": 8, "capacity": 32},
                "ssd": {"units": 2, "capacity": 960},
                "gpu": {"units": 1, "name": "NVIDIA A100"}
            }
            """;

        KubernetesAPIBareServerProfile profile = assertInstanceOf(KubernetesAPIBareServerProfile.class, this.objectMapper.readValue(json, KubernetesAPIServerProfile.class));

        assertSame(Country.FRANCE, profile.location());
        assertEquals(new BareServerProfile.CPU(2, 24, 2, "Intel Xeon Gold 6342"), profile.cpu());
        assertEquals(96, profile.cpu().vcpu());
        assertEquals(new BareServerProfile.RAM(8, 32), profile.ram());
        assertEquals(new BareServerProfile.Disk(2, 960), profile.ssd());
        assertNull(profile.hdd());
        assertEquals(new BareServerProfile.GPU(1, "NVIDIA A100"), profile.gpu());
    }

    @Test
    void anInferredProfileOnlyReadsTheFieldsTheUserSets() throws Exception
    {
        KubernetesAPIInferredProfile bare = assertInstanceOf(KubernetesAPIInferredProfile.class, this.objectMapper.readValue("{\"type\": \"inferred\"}", KubernetesAPIServerProfile.class));
        KubernetesAPIInferredProfile hinted = assertInstanceOf(KubernetesAPIInferredProfile.class, this.objectMapper.readValue("""
            {"type": "inferred", "location": "FRA", "lifespan": "6y"}
            """, KubernetesAPIServerProfile.class));

        assertEquals(KubernetesAPIInferredProfile.DEFAULT, bare);
        assertEquals(new KubernetesAPIInferredProfile(Country.FRANCE, Lifetime.ofYears(6)), hinted);
        assertEquals(Set.of(), violations(bare));
    }

    @Test
    void everyProfileRoundTripsWithItsType() throws Exception
    {
        for (KubernetesAPIServerProfile profile : Set.of(cloud(), server(new BareServerProfile.CPU(1, 8, 1, null)), new KubernetesAPIInferredProfile(Country.FRANCE, null)))
        {
            String json = this.objectMapper.writeValueAsString(profile);

            assertEquals(profile, this.objectMapper.readValue(json, KubernetesAPIServerProfile.class), json);
        }
    }

    @Test
    void aServerProfileRequiresItsCpuAndRam()
    {
        KubernetesAPIBareServerProfile profile = new KubernetesAPIBareServerProfile(Country.FRANCE, Lifetime.ofYears(5), null, null, null, null, null);

        assertEquals(Set.of("cpu", "ram"), violations(profile));
    }

    @Test
    void aServerProfileRejectsEmptyComponents()
    {
        assertEquals(Set.of("cpu.units", "cpu.coreUnits"), violations(server(new BareServerProfile.CPU(0, 0, 0, null))));
    }

    private static Set<String> violations(KubernetesAPIServerProfile profile)
    {
        return validator.validate(profile).stream()
            .map(ConstraintViolation::getPropertyPath)
            .map(Object::toString)
            .collect(Collectors.toSet());
    }

    private static KubernetesAPICloudProfile cloud()
    {
        return new KubernetesAPICloudProfile(ServerProvider.aws, "m5.large", Country.FRANCE, Lifetime.ofYears(5));
    }

    private static KubernetesAPIBareServerProfile server(BareServerProfile.CPU cpu)
    {
        return new KubernetesAPIBareServerProfile(Country.FRANCE, Lifetime.ofYears(5), cpu, new BareServerProfile.RAM(4, 16), null, null, null);
    }
}
