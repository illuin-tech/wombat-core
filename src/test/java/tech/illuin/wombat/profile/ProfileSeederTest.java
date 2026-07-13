package tech.illuin.wombat.profile;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import tech.illuin.wombat.profile.persistence.ProfileData;
import tech.illuin.wombat.profile.persistence.ProfileEntity;
import tech.illuin.wombat.profile.persistence.ProfileRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ProfileSeederTest
{

    @Inject
    ProfileRepository repository;

    @Inject
    ProfileSeeder seeder;

    @Test
    void seedsFromApplicationYamlAreInsertedAtStartup()
    {
        List<ProfileEntity> profiles = repository.listAll();

        assertTrue(profiles.size() >= 2, "expected at least 2 seeded profiles, found " + profiles.size());
        assertTrue(profiles.stream().anyMatch(p -> "test-aws-c5".equals(p.id)));
        assertTrue(profiles.stream().anyMatch(p -> "test-gcp-n2".equals(p.id)));
    }

    @Test
    void seededAwsProfile_hasExpectedFields()
    {
        ProfileEntity profile = repository.findByIdOptional("test-aws-c5").orElse(null);
        assertNotNull(profile);
        assertNotNull(profile.uuid);
        assertEquals("Test AWS c5.large FRA", profile.description);
        assertEquals("FRA", profile.location);
        assertEquals(new ProfileData.InfrastructureData("c5.large", 43800), profile.data);
    }

    @Test
    @Transactional
    void onStart_addsMissingProfilesWithoutDuplicatingExistingOnes()
    {
        long initial = this.repository.count();
        this.repository.deleteById("test-aws-c5");
        assertEquals(initial - 1, this.repository.count());

        this.seeder.onStart(null);

        assertEquals(initial, this.repository.count(), "missing seed re-added, present seeds not duplicated");
        assertTrue(this.repository.findByIdOptional("test-aws-c5").isPresent());
    }
}
