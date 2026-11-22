package tech.illuin.wombat.module.conformance.canonical;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.activity.kubernetes.KubernetesActivityData;
import tech.illuin.wombat.core.activity.kubernetes.RelativeCPUUsage;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.profile.ServerProfile;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.connector.boavizta.impact.BoaviztaEvaluationResolver;
import tech.illuin.wombat.core.evaluation.WombatEvaluationResolver;
import tech.illuin.wombat.module.connector.boavizta.BoaviztaClientConfig;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Boavizta called once over the whole range, the hourly loads as its {@code time_workload} and the profile's lifespan as
 * the hardware lifetime, times the node count.
 * <p>
 * It reads relative CPU usage, as static profiles declare it. Container shares are not split: the reference is the whole
 * asset.
 */
public final class CanonicalBoavizta implements CanonicalEngine
{
    private final URI uri;
    private final HttpClient http;
    private final ObjectMapper mapper;
    private final WombatEvaluationResolver resolver;

    /**
     * Boavizta rounds each figure to the precision it claims for it, down to 2 significant digits: the canonical figure
     * stands for an interval of half its last digit. Wombat sums slot figures that were rounded the same way, worth
     * another 0.5%.
     */
    private static final Tolerance TOLERANCE = Tolerance.relative(0.005);
    /** Boavizta's world average, ISO 3166 having no code for the world. */
    private static final String WORLD_LOCATION = "WOR";

    public CanonicalBoavizta(URI uri)
    {
        this.uri = uri;
        /* No h2c upgrade: the sidecars' servers drop the body of an upgraded POST */
        this.http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        /* Decimals as sent, so that each figure's last digit tells how Boavizta rounded it */
        this.mapper = new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        this.resolver = new BoaviztaEvaluationResolver(BoaviztaClientConfig.create(uri.toString(), null));
    }

    @Override
    public ServiceFamily family()
    {
        return ServiceFamily.KUBERNETES_CONTAINER;
    }

    @Override
    public WombatEvaluationResolver resolver()
    {
        return this.resolver;
    }

    @Override
    public CanonicalImpacts evaluate(Asset asset, List<ActivityData> activity)
    {
        if (!(asset.profile() instanceof ServerProfile profile))
            throw new IllegalArgumentException("asset " + asset.identity().id() + " has no server profile");

        double totalHours = activity.stream().mapToDouble(CanonicalBoavizta::hours).sum();
        List<Segment> workload = activity.stream()
            .map(slot -> new Segment(hours(slot) / totalHours * 100.0, load(slot) * 100.0))
            .toList();

        return this.instance(
            profile.provider().name(),
            profile.instanceType(),
            profile.location() == Country.WORLD ? WORLD_LOCATION : profile.location().alpha3(),
            (double) profile.lifespan().asHours(),
            totalHours,
            workload
        ).multiply(profile.nodeCount());
    }

    @Override
    public Tolerance tolerance()
    {
        return TOLERANCE;
    }

    /**
     * Impacts of one instance over {@code durationHours}.
     *
     * @param hoursLifeTime the hardware lifetime, null to leave Boavizta's default for the instance
     */
    public CanonicalImpacts instance(
        String provider,
        String instanceType,
        String location,
        Double hoursLifeTime,
        double durationHours,
        List<Segment> workload
    ) {
        ObjectNode body = this.mapper.createObjectNode()
            .put("provider", provider)
            .put("instance_type", instanceType);
        ObjectNode usage = body.putObject("usage").put("usage_location", location);
        if (hoursLifeTime != null)
            usage.put("hours_life_time", hoursLifeTime);
        ArrayNode timeWorkload = usage.putArray("time_workload");
        for (Segment segment : workload)
            timeWorkload.addObject()
                .put("time_percentage", segment.timePercentage())
                .put("load_percentage", segment.loadPercentage());

        JsonNode impacts = this.post("cloud/instance?verbose=false&duration=" + durationHours + "&criteria=gwp&criteria=pe&criteria=adp", body).path("impacts");
        return new CanonicalImpacts(phases(impacts.path("gwp")), phases(impacts.path("pe")), phases(impacts.path("adp")));
    }

    /** The instance's hardware lifetime when none is given, in hours. */
    public double defaultHoursLifeTime(String provider, String instanceType, String location)
    {
        ObjectNode body = this.mapper.createObjectNode()
            .put("provider", provider)
            .put("instance_type", instanceType);
        body.putObject("usage").put("usage_location", location);
        return this.post("cloud/instance?verbose=true&criteria=gwp", body).path("verbose").path("hours_life_time").path("value").asDouble();
    }

    private static double hours(ActivityData slot)
    {
        return Duration.between(slot.range().start(), slot.range().end()).toSeconds() / 3600.0;
    }

    private static double load(ActivityData slot)
    {
        if (!(slot instanceof KubernetesActivityData kubernetes) || !(kubernetes.cpuUsage() instanceof RelativeCPUUsage relative))
            throw new IllegalArgumentException("only relative CPU usage is supported, got " + slot);
        return relative.ratio();
    }

    private static CanonicalImpacts.Phases phases(JsonNode impact)
    {
        JsonNode embedded = impact.path("embedded").path("value");
        JsonNode use = impact.path("use").path("value");
        return new CanonicalImpacts.Phases(
            impact.path("unit").asText(),
            embedded.asDouble(),
            use.asDouble(),
            halfLastDigit(embedded),
            halfLastDigit(use)
        );
    }

    /* "0.25" stands for 0.245 to 0.255; "15.0" is sent for a figure known to 2 digits, hence the stripped zeros */
    private static double halfLastDigit(JsonNode value)
    {
        BigDecimal decimal = value.isBigDecimal() ? value.decimalValue() : BigDecimal.valueOf(value.asDouble());
        return decimal.stripTrailingZeros().ulp().doubleValue() / 2.0;
    }

    private JsonNode post(String path, JsonNode body)
    {
        try {
            HttpRequest request = HttpRequest.newBuilder(this.uri.resolve(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(this.mapper.writeValueAsString(body)))
                .build();
            HttpResponse<String> response = this.http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
                throw new IllegalStateException("boavizta answered " + response.statusCode() + ": " + response.body());
            return this.mapper.readTree(response.body());
        }
        catch (IOException e)
        {
            throw new IllegalStateException("boavizta could not be called", e);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("boavizta call interrupted", e);
        }
    }

    /** One share of the duration, in percent, at one load, in percent. */
    public record Segment(double timePercentage, double loadPercentage) {}
}
