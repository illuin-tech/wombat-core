package tech.illuin.wombat.core.secret;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyStoreSecretResolverTest
{
    @Test
    void resolvesSecretKeyEntry() throws Exception
    {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);

        SecretKey secretKey = new SecretKeySpec("super-secret-token".getBytes(StandardCharsets.UTF_8), "AES");
        KeyStore.SecretKeyEntry entry = new KeyStore.SecretKeyEntry(secretKey);
        KeyStore.PasswordProtection prot = new KeyStore.PasswordProtection("entry-pass".toCharArray());

        ks.setEntry("API_KEY", entry, prot);

        KeyStoreSecretResolver resolver = KeyStoreSecretResolver.builder()
            .keyStore(ks)
            .defaultProtection(prot)
            .build();

        assertEquals("super-secret-token", resolver.require("API_KEY"));
        assertEquals("super-secret-token", resolver.find("API_KEY").orElseThrow());
    }

    @Test
    void resolvesCaseInsensitiveAliasByDefault() throws Exception
    {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);

        SecretKey secretKey = new SecretKeySpec("db-pwd-123".getBytes(StandardCharsets.UTF_8), "AES");
        ks.setEntry("Database_Password", new KeyStore.SecretKeyEntry(secretKey), new KeyStore.PasswordProtection("pass".toCharArray()));

        KeyStoreSecretResolver resolver = KeyStoreSecretResolver.builder()
            .keyStore(ks)
            .defaultKeyPassword("pass")
            .build();

        assertEquals("db-pwd-123", resolver.require("database_password"));
        assertEquals("db-pwd-123", resolver.require("DATABASE_PASSWORD"));
        assertEquals("db-pwd-123", resolver.require("Database_Password"));
    }

    @Test
    void canConfigureCaseSensitivity() throws Exception
    {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);

        SecretKey secretKey = new SecretKeySpec("db-pwd-123".getBytes(StandardCharsets.UTF_8), "AES");
        ks.setEntry("database_password", new KeyStore.SecretKeyEntry(secretKey), new KeyStore.PasswordProtection("pass".toCharArray()));

        KeyStoreSecretResolver resolver = KeyStoreSecretResolver.builder()
            .keyStore(ks)
            .defaultKeyPassword("pass")
            .caseInsensitive(true)
            .build();

        assertEquals("db-pwd-123", resolver.require("DATABASE_PASSWORD"));
    }

    @Test
    void rejectsBlankKeysAndTreatsUnknownOnesAsAbsent() throws Exception
    {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);

        SecretKey secretKey = new SecretKeySpec("my-secret".getBytes(StandardCharsets.UTF_8), "AES");
        ks.setEntry("MY_SECRET", new KeyStore.SecretKeyEntry(secretKey), new KeyStore.PasswordProtection("pass".toCharArray()));

        KeyStoreSecretResolver resolver = KeyStoreSecretResolver.builder()
            .keyStore(ks)
            .defaultKeyPassword("pass")
            .build();

        // A key that is null or blank names nothing at all: that is a caller mistake, not a missing secret.
        assertThrows(IllegalArgumentException.class, () -> resolver.find(null));
        assertThrows(IllegalArgumentException.class, () -> resolver.find(""));
        assertThrows(IllegalArgumentException.class, () -> resolver.find("   "));
        assertThrows(IllegalArgumentException.class, () -> resolver.require(null));
        assertThrows(IllegalArgumentException.class, () -> resolver.require(""));
        assertThrows(IllegalArgumentException.class, () -> resolver.require("   "));

        // A well-formed key the keystore does not hold is simply absent.
        assertTrue(resolver.find("NON_EXISTENT").isEmpty());

        MissingSecretException e = assertThrows(MissingSecretException.class, () -> resolver.require("NON_EXISTENT"));
        assertEquals("NON_EXISTENT", e.key());
    }

    @Test
    void treatsBlankSecretValueAsAbsent() throws Exception
    {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);

        SecretKey blankKey = new SecretKeySpec("   ".getBytes(StandardCharsets.UTF_8), "AES");
        ks.setEntry("BLANK_KEY", new KeyStore.SecretKeyEntry(blankKey), new KeyStore.PasswordProtection("pass".toCharArray()));

        KeyStoreSecretResolver resolver = KeyStoreSecretResolver.builder()
            .keyStore(ks)
            .defaultKeyPassword("pass")
            .build();

        assertTrue(resolver.find("BLANK_KEY").isEmpty());
        assertThrows(MissingSecretException.class, () -> resolver.require("BLANK_KEY"));
    }

    @Test
    void loadsFromPathAndFile(@TempDir Path tempDir) throws Exception
    {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        char[] storePassword = "keystore-pass".toCharArray();
        char[] keyPassword = "secret-pass".toCharArray();
        ks.load(null, storePassword);

        SecretKey secretKey = new SecretKeySpec("file-secret".getBytes(StandardCharsets.UTF_8), "AES");
        ks.setEntry("FILE_ALIAS", new KeyStore.SecretKeyEntry(secretKey), new KeyStore.PasswordProtection(keyPassword));

        Path ksPath = tempDir.resolve("test-keystore.p12");
        try (var os = java.nio.file.Files.newOutputStream(ksPath))
        {
            ks.store(os, storePassword);
        }

        // Test load(Path, char[])
        KeyStoreSecretResolver fromPath = KeyStoreSecretResolver.builder()
            .load(ksPath, storePassword)
            .defaultKeyPassword(keyPassword)
            .build();

        assertEquals("file-secret", fromPath.require("FILE_ALIAS"));
        assertNotNull(fromPath.keyStore());

        // Test load(File, String)
        File ksFile = ksPath.toFile();
        KeyStoreSecretResolver fromFile = KeyStoreSecretResolver.builder()
            .load(ksFile, "keystore-pass")
            .defaultKeyPassword("secret-pass")
            .build();

        assertEquals("file-secret", fromFile.require("FILE_ALIAS"));
    }

    @Test
    void loadsFromInputStream() throws Exception
    {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        char[] password = "stream-pass".toCharArray();
        ks.load(null, password);

        SecretKey secretKey = new SecretKeySpec("stream-secret".getBytes(StandardCharsets.UTF_8), "AES");
        ks.setEntry("STREAM_KEY", new KeyStore.SecretKeyEntry(secretKey), new KeyStore.PasswordProtection(password));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ks.store(baos, password);

        KeyStoreSecretResolver resolver = KeyStoreSecretResolver.builder()
            .load(new ByteArrayInputStream(baos.toByteArray()), password)
            .defaultKeyPassword(password)
            .build();

        assertEquals("stream-secret", resolver.require("STREAM_KEY"));
    }

    @Test
    void builderFailsWhenKeyStoreNotConfigured()
    {
        KeyStoreSecretResolver.Builder builder = KeyStoreSecretResolver.builder();
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void builderFailsOnCorruptedStream()
    {
        byte[] corrupted = "not a valid keystore".getBytes(StandardCharsets.UTF_8);
        KeyStoreSecretResolver.Builder builder = KeyStoreSecretResolver.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.load(new ByteArrayInputStream(corrupted), "pass"));
    }

    @Test
    void builderFailsOnMissingResource()
    {
        KeyStoreSecretResolver.Builder builder = KeyStoreSecretResolver.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.loadResource("non-existent-file.p12", "pass"));
    }
}
