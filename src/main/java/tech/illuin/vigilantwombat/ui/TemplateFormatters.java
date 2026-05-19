package tech.illuin.vigilantwombat.ui;

import io.quarkus.qute.TemplateExtension;
import tech.illuin.vigilantwombat.boavizta.model.BoaviztaInstanceImpactResponse;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@TemplateExtension
public class TemplateFormatters {

    private static final DateTimeFormatter LOCAL_DT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm").withZone(ZoneOffset.UTC);

    public static String asLocalDateTime(Instant instant) {
        if (instant == null) return "";
        // Instant.MIN / Instant.MAX → chaîne vide plutôt qu'une date absurde
        if (instant.equals(Instant.MIN) || instant.equals(Instant.MAX)) return "";
        return LOCAL_DT.format(instant);
    }

    public static String asPercent(Double value) {
        if (value == null) return "—";
        return String.format(Locale.US, "%.2f%%", value * 100);
    }

    public static float totalValue(BoaviztaInstanceImpactResponse.Impact impact) {
        if (impact == null) return 0f;
        return impact.embedded().value() + impact.use().value();
    }

    public static String asDecimal(Float value) {
        if (value == null) return "—";
        if (value != 0f && Math.abs(value) < 0.005f) {
            return String.format(Locale.US, "%.2E", value)
                .replaceAll("\\.?0+(E)", "$1")
                .replaceAll("E([+-])0*(\\d+)", "E$1$2");
        }
        return String.format(Locale.US, "%.2f", value).replaceAll("\\.?0+$", "");
    }
}