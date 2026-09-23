package tech.illuin.wombat.core.secret;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectorySecretResolverTest
{
    @Test
    void resolvesSecretFileWithTrailingNewlineStripped(@TempDir Path tempDir) throws IOException
    {
        Files.writeString(tempDir.resolve("API_TOKEN"), "my-super-secret-token\r\n");

        SecretResolver resolver = SecretResolver.directory(tempDir);

        assertEquals("my-super-secret-token", resolver.require("API_TOKEN"));
        assertEquals("my-super-secret-token", resolver.find("API_TOKEN").orElseThrow());
    }

    @Test
    void treatsBlankSecretValueAsAbsent(@TempDir Path tempDir) throws IOException
    {
        Files.writeString(tempDir.resolve("BLANK_KEY"), "   \n\r\n");

        SecretResolver resolver = SecretResolver.directory(tempDir);

        assertTrue(resolver.find("BLANK_KEY").isEmpty());
        assertThrows(MissingSecretException.class, () -> resolver.require("BLANK_KEY"));
    }

    @Test
    void rejectsBlankKeysAndTreatsUnknownOnesAsAbsent(@TempDir Path tempDir) throws IOException
    {
        Files.writeString(tempDir.resolve("EXISTING"), "secret");

        SecretResolver resolver = SecretResolver.directory(tempDir);

        assertThrows(IllegalArgumentException.class, () -> resolver.find(null));
        assertThrows(IllegalArgumentException.class, () -> resolver.find(""));
        assertThrows(IllegalArgumentException.class, () -> resolver.find("   "));
        assertThrows(IllegalArgumentException.class, () -> resolver.require(null));
        assertThrows(IllegalArgumentException.class, () -> resolver.require(""));
        assertThrows(IllegalArgumentException.class, () -> resolver.require("   "));

        assertTrue(resolver.find("NON_EXISTENT").isEmpty());
        MissingSecretException e = assertThrows(MissingSecretException.class, () -> resolver.require("NON_EXISTENT"));
        assertEquals("NON_EXISTENT", e.key());
    }

    @Test
    void handlesNonExistentDirectoryGracefully(@TempDir Path tempDir)
    {
        Path nonExistent = tempDir.resolve("does-not-exist");
        SecretResolver resolver = SecretResolver.directory(nonExistent);

        assertTrue(resolver.find("ANY_KEY").isEmpty());
        assertThrows(MissingSecretException.class, () -> resolver.require("ANY_KEY"));
    }

    @Test
    void handlesPathTraversalAttempts(@TempDir Path tempDir) throws IOException
    {
        Path subDir = Files.createDirectory(tempDir.resolve("secrets"));
        Files.writeString(tempDir.resolve("OUTSIDE"), "outside-value");

        SecretResolver resolver = SecretResolver.directory(subDir);

        assertTrue(resolver.find("../OUTSIDE").isEmpty());
    }

    @Test
    void builderFailsWhenDirectoryNotConfigured()
    {
        DirectorySecretResolver.Builder builder = DirectorySecretResolver.builder();
        assertThrows(IllegalStateException.class, builder::build);
    }
}
