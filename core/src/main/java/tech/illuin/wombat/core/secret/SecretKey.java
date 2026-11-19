package tech.illuin.wombat.core.secret;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * The name under which a secret is bound in a {@link SecretResolver}, never its value: it is safe to store, log and
 * review. Assets declare their credentials with this type rather than a plain string, so tools reading their shape
 * (e.g. a JSON schema) can tell a secret reference apart from any other text.
 */
@JsonClassDescription("Name of a secret, resolved by the configured secret resolver")
public record SecretKey(@JsonValue String value)
{
    public SecretKey
    {
        Objects.requireNonNull(value, "value cannot be null");
        if (value.isBlank())
            throw new IllegalArgumentException("value cannot be null or blank");
    }

    /**
     * @return the key named {@code value}, {@code null} when it is null or blank: such a value names no secret, which
     *         lets a templated configuration leave an optional key empty
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static SecretKey of(String value)
    {
        if (value == null || value.isBlank())
            return null;
        return new SecretKey(value);
    }
}
