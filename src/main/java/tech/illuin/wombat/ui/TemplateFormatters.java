package tech.illuin.wombat.ui;

import io.quarkus.qute.TemplateExtension;
import tech.illuin.wombat.handler.model.BoaviztaKubernetesConfig;
import tech.illuin.wombat.handler.model.ClusterInfo;
import tech.illuin.wombat.handler.model.ProviderConfig;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@TemplateExtension
public class TemplateFormatters
{

    private static final DateTimeFormatter LOCAL_DT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm").withZone(ZoneOffset.UTC);

    public static String asLocalDateTime(Instant instant)
    {
        if (instant == null) return "";
        if (instant.equals(Instant.MIN) || instant.equals(Instant.MAX)) return "";
        return LOCAL_DT.format(instant);
    }

    public static String asIsoUtc(Instant instant)
    {
        if (instant == null || instant.equals(Instant.MIN) || instant.equals(Instant.MAX)) return "";
        return instant.toString();
    }

    public static String asPercent(Double value)
    {
        if (value == null) return "—";
        return String.format(Locale.US, "%.2f%%", value * 100);
    }

    public static String provider(ProviderConfig config)
    {
        if (config instanceof BoaviztaKubernetesConfig c) return c.provider().name();
        return "n/a";
    }

    public static String instanceType(ProviderConfig config)
    {
        if (config instanceof BoaviztaKubernetesConfig c) return c.instanceType();
        return "n/a";
    }

    public static String location(ProviderConfig config)
    {
        if (config instanceof BoaviztaKubernetesConfig c) return c.location();
        return "n/a";
    }

    public static int lifespan(ProviderConfig config)
    {
        if (config instanceof BoaviztaKubernetesConfig c) return c.lifespan();
        return 0;
    }

    public static List<ClusterInfo> clusters(ProviderConfig config)
    {
        if (config instanceof BoaviztaKubernetesConfig c && c.clusters() != null) return c.clusters();
        return List.of();
    }

    public static String asDecimal(Float value)
    {
        if (value == null) return "—";
        if (value != 0f && Math.abs(value) < 0.005f)
        {
            return String.format(Locale.US, "%.2E", value)
                .replaceAll("\\.?0+(E)", "$1")
                .replaceAll("E([+-])0*(\\d+)", "E$1$2");
        }
        return String.format(Locale.US, "%.2f", value).replaceAll("\\.?0+$", "");
    }
}
