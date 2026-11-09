package tech.illuin.wombat.core.secret;

import java.util.List;

public interface SecretWriter
{
    List<SecretMetadata> listSecrets();

    void putSecret(String key, String value);

    boolean deleteSecret(String key);
}
