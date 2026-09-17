package tech.illuin.wombat.core.secret;

import java.util.Set;

/**
 * Implemented by assets that need credentials, so the application can check every one of them is available before it
 * starts rather than discovering a missing variable on the first scrape.
 * <p>
 * The returned values are variable <em>names</em>, resolved through a {@link SecretResolver}.
 */
public interface SecretAware
{
    Set<String> requiredSecretKeys();
}
