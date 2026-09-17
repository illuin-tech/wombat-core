package tech.illuin.wombat.core.secret;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Optional;

public class KeyStoreSecretResolver implements SecretResolver
{
    private final KeyStore keyStore;
    private final KeyStore.ProtectionParameter defaultProtection;
    private final boolean caseInsensitive;

    public static final String DEFAULT_TYPE = "PKCS12";

    public KeyStoreSecretResolver(KeyStore keyStore)
    {
        this(keyStore, null, true);
    }

    public KeyStoreSecretResolver(KeyStore keyStore, KeyStore.ProtectionParameter defaultProtection)
    {
        this(keyStore, defaultProtection, true);
    }

    public KeyStoreSecretResolver(KeyStore keyStore, KeyStore.ProtectionParameter defaultProtection, boolean caseInsensitive)
    {
        this.keyStore = Objects.requireNonNull(keyStore, "keyStore cannot be null");
        this.defaultProtection = defaultProtection;
        this.caseInsensitive = caseInsensitive;
    }

    @Override
    public String require(String key)
    {
        return this.find(key).orElseThrow(() -> new MissingSecretException(key));
    }

    @Override
    public Optional<String> find(String key)
    {
        if (key == null || key.isBlank())
            throw new IllegalArgumentException("key cannot be null or blank");

        try {
            Optional<String> findKey = this.resolveAlias(key);
            if (findKey.isEmpty())
                return Optional.empty();

            String alias = findKey.get();
            if (!this.keyStore.isKeyEntry(alias))
                return Optional.empty();

            KeyStore.Entry entry = this.keyStore.getEntry(alias, this.defaultProtection);
            if (entry instanceof KeyStore.SecretKeyEntry secretKeyEntry)
            {
                byte[] encoded = secretKeyEntry.getSecretKey().getEncoded();
                if (encoded == null)
                    return Optional.empty();

                String secret = new String(encoded, StandardCharsets.UTF_8);
                return Optional.of(secret).filter(value -> !value.isBlank());
            }

            char[] password = this.defaultProtection instanceof KeyStore.PasswordProtection passwordProtection
                ? passwordProtection.getPassword()
                : null;
            Key keyObj = this.keyStore.getKey(alias, password);
            if (keyObj != null && keyObj.getEncoded() != null)
            {
                String secret = new String(keyObj.getEncoded(), StandardCharsets.UTF_8);
                return Optional.of(secret).filter(value -> !value.isBlank());
            }

            return Optional.empty();
        }
        catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableEntryException e) {
            throw new IllegalStateException("Failed to retrieve secret '" + key + "' from KeyStore", e);
        }
    }

    public KeyStore keyStore()
    {
        return this.keyStore;
    }

    private Optional<String> resolveAlias(String key) throws KeyStoreException
    {
        if (this.keyStore.containsAlias(key))
            return Optional.of(key);

        if (this.caseInsensitive)
        {
            Enumeration<String> aliases = this.keyStore.aliases();
            while (aliases.hasMoreElements())
            {
                String alias = aliases.nextElement();
                if (alias.equalsIgnoreCase(key))
                    return Optional.of(alias);
            }
        }

        return Optional.empty();
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private KeyStore keyStore;
        private String type = DEFAULT_TYPE;
        private Provider provider;
        private KeyStore.ProtectionParameter defaultProtection;
        private boolean caseInsensitive = true;

        public Builder keyStore(KeyStore keyStore)
        {
            this.keyStore = keyStore;
            return this;
        }

        public Builder type(String type)
        {
            this.type = Objects.requireNonNull(type, "type cannot be null");
            return this;
        }

        public Builder provider(Provider provider)
        {
            this.provider = provider;
            return this;
        }

        public Builder caseInsensitive(boolean caseInsensitive)
        {
            this.caseInsensitive = caseInsensitive;
            return this;
        }

        public Builder defaultProtection(KeyStore.ProtectionParameter defaultProtection)
        {
            this.defaultProtection = defaultProtection;
            return this;
        }

        public Builder defaultKeyPassword(char[] password)
        {
            this.defaultProtection = password != null ? new KeyStore.PasswordProtection(password) : null;
            return this;
        }

        public Builder defaultKeyPassword(String password)
        {
            return this.defaultKeyPassword(password != null ? password.toCharArray() : null);
        }

        public Builder load(InputStream input, char[] password)
        {
            if (input == null)
                throw new IllegalArgumentException("input stream cannot be null");
            try {
                KeyStore ks = this.provider != null
                    ? KeyStore.getInstance(this.type, this.provider)
                    : KeyStore.getInstance(this.type);
                ks.load(input, password);
                this.keyStore = ks;
                return this;
            }
            catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
                throw new IllegalArgumentException("Failed to load KeyStore of type " + this.type, e);
            }
        }

        public Builder load(InputStream input, String password)
        {
            return this.load(input, password != null ? password.toCharArray() : null);
        }

        public Builder load(Path path, char[] password)
        {
            if (path == null)
                throw new IllegalArgumentException("path cannot be null");
            try (InputStream is = Files.newInputStream(path)) {
                return this.load(is, password);
            }
            catch (IOException e) {
                throw new IllegalArgumentException("Failed to read KeyStore file at " + path, e);
            }
        }

        public Builder load(Path path, String password)
        {
            return this.load(path, password != null ? password.toCharArray() : null);
        }

        public Builder load(File file, char[] password)
        {
            Objects.requireNonNull(file, "file cannot be null");
            return this.load(file.toPath(), password);
        }

        public Builder load(File file, String password)
        {
            return this.load(file, password != null ? password.toCharArray() : null);
        }

        public Builder loadResource(String resourceName, char[] password)
        {
            Objects.requireNonNull(resourceName, "resourceName cannot be null");
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            InputStream rawStream = cl != null ? cl.getResourceAsStream(resourceName) : null;
            if (rawStream == null)
                rawStream = KeyStoreSecretResolver.class.getResourceAsStream(resourceName.startsWith("/") ? resourceName : "/" + resourceName);
            if (rawStream == null)
                throw new IllegalArgumentException("KeyStore resource not found: " + resourceName);
            try (InputStream is = rawStream) {
                return this.load(is, password);
            }
            catch (IOException e) {
                throw new IllegalArgumentException("Failed to read KeyStore resource " + resourceName, e);
            }
        }

        public Builder loadResource(String resourceName, String password)
        {
            return this.loadResource(resourceName, password != null ? password.toCharArray() : null);
        }

        public KeyStoreSecretResolver build()
        {
            if (this.keyStore == null)
                throw new IllegalStateException("KeyStore must be configured before building KeyStoreSecretResolver");
            return new KeyStoreSecretResolver(this.keyStore, this.defaultProtection, this.caseInsensitive);
        }
    }
}
