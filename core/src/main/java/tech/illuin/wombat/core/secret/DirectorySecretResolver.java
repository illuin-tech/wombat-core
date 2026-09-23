package tech.illuin.wombat.core.secret;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Resolves secrets from files in a directory, such as a Kubernetes Secret volume mount.
 * <p>
 * Each file in the directory corresponds to a secret, where the filename is the secret key and the file content is the
 * secret value. Trailing newlines and whitespace are stripped from the secret value.
 */
public class DirectorySecretResolver implements SecretResolver
{
    private final Path directory;

    public DirectorySecretResolver(Path directory)
    {
        if (directory == null)
            throw new IllegalArgumentException("directory must not be null");

        this.directory = directory;
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
            throw new IllegalArgumentException("Secret key must not be null or blank");
        if (!Files.isDirectory(this.directory))
            return Optional.empty();

        try {
            Path exactFile = this.directory.resolve(key).normalize();
            if (!Files.exists(exactFile))
                return Optional.empty();
            if (!Files.isRegularFile(exactFile))
                throw new IllegalArgumentException("Secret file in directory " + this.directory + " cannot be read (not a regular file)");

            if (exactFile.toRealPath().getFileName().toString().equals(key))
                return readFileValue(exactFile);

            return Optional.empty();
        }
        catch (IOException e) {
            return Optional.empty();
        }
    }

    private static Optional<String> readFileValue(Path path)
    {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            String trimmed = content.stripTrailing();
            if (trimmed.isBlank())
                return Optional.empty();
            return Optional.of(trimmed);
        }
        catch (IOException e) {
            return Optional.empty();
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private Path directory;

        public Builder directory(Path directory)
        {
            this.directory = directory;
            return this;
        }

        public Builder directory(File directory)
        {
            this.directory = directory != null ? directory.toPath() : null;
            return this;
        }

        public Builder directory(String directoryPath)
        {
            this.directory = directoryPath != null ? Path.of(directoryPath) : null;
            return this;
        }

        public DirectorySecretResolver build()
        {
            if (this.directory == null)
                throw new IllegalStateException("Directory must be configured before building DirectorySecretResolver");
            return new DirectorySecretResolver(this.directory);
        }
    }
}
