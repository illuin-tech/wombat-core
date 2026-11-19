package tech.illuin.wombat.core.secret;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecretKeyTest
{
    private final ObjectMapper mapper = JsonMapper.builder().build();

    @Test
    void isWrittenAndReadAsAPlainString() throws Exception
    {
        SecretKey key = SecretKey.of("API_KEY");

        assertEquals("\"API_KEY\"", this.mapper.writeValueAsString(key));
        assertEquals(key, this.mapper.readValue("\"API_KEY\"", SecretKey.class));
    }

    @Test
    void aBlankValueNamesNoKey() throws Exception
    {
        assertNull(SecretKey.of(null));
        assertNull(SecretKey.of("  "));
        assertNull(this.mapper.readValue("\"\"", SecretKey.class));
        assertNull(this.mapper.readValue("null", SecretKey.class));
    }

    @Test
    void cannotBeBuiltBlank()
    {
        assertThrows(NullPointerException.class, () -> new SecretKey(null));
        assertThrows(IllegalArgumentException.class, () -> new SecretKey(" "));
    }
}
