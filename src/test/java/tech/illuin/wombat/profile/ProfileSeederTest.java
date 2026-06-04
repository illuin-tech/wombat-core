package tech.illuin.wombat.profile;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ProfileSeederTest
{

    @Inject
    ServerProfileRepository repository;

    @Test
    void seedsFromApplicationYamlAreInsertedAtStartup()
    {
        List<ServerProfileEntity> profiles = repository.listAll();

        assertTrue(profiles.size() >= 2, "expected at least 2 seeded profiles, found " + profiles.size());
        assertTrue(profiles.stream().anyMatch(p -> "test-aws-c5".equals(p.id)));
        assertTrue(profiles.stream().anyMatch(p -> "test-gcp-n2".equals(p.id)));
    }

    @Test
    void seededAwsProfile_hasExpectedFields()
    {
        ServerProfileEntity profile = repository.findByIdOptional("test-aws-c5").orElse(null);
        assertNotNull(profile);
        assertEquals("Test AWS c5.large FRA", profile.description);
        assertEquals("c5.large", profile.instanceType);
        assertEquals("FRA", profile.location);
        assertEquals(43800, profile.lifespan);
    }
}
