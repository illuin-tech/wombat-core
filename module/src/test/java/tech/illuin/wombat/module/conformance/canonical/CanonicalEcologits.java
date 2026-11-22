package tech.illuin.wombat.module.conformance.canonical;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import tech.illuin.wombat.core.activity.commons.ActivityData;
import tech.illuin.wombat.core.activity.commons.Country;
import tech.illuin.wombat.core.activity.llm.LLMActivityData;
import tech.illuin.wombat.core.activity.llm.LLMServiceActivity;
import tech.illuin.wombat.core.asset.Asset;
import tech.illuin.wombat.core.asset.type.ServiceFamily;
import tech.illuin.wombat.core.connector.ecologits.impact.EcologitsEvaluationResolver;
import tech.illuin.wombat.core.evaluation.WombatEvaluationResolver;
import tech.illuin.wombat.module.connector.ecologits.EcologitsClientConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ecologits called per request, as its own calculator does: each hour's requests times the impacts of one request of
 * that hour's tokens per request. A range is read as its midpoint, as Wombat reads it.
 */
public final class CanonicalEcologits implements CanonicalEngine
{
    private final URI uri;
    private final HttpClient http;
    private final ObjectMapper mapper;
    private final WombatEvaluationResolver resolver;
    private final Map<Request, CanonicalImpacts> requests = new HashMap<>();

    /** Wombat sends a whole number of tokens per request and keeps floats: within 0.1% from 500 tokens per request. */
    private static final Tolerance TOLERANCE = Tolerance.relative(0.001);
    private static final String WORLD_ZONE = "WOR";

    public CanonicalEcologits(URI uri)
    {
        this.uri = uri;
        /* No h2c upgrade: the sidecars' servers drop the body of an upgraded POST */
        this.http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        this.mapper = new ObjectMapper();
        this.resolver = new EcologitsEvaluationResolver(EcologitsClientConfig.create(uri.toString(), null));
    }

    @Override
    public ServiceFamily family()
    {
        return ServiceFamily.LLM;
    }

    @Override
    public WombatEvaluationResolver resolver()
    {
        return this.resolver;
    }

    @Override
    public CanonicalImpacts evaluate(Asset asset, List<ActivityData> activity)
    {
        CanonicalImpacts total = null;
        for (ActivityData slot : activity)
        {
            if (!(slot instanceof LLMActivityData llm))
                throw new IllegalArgumentException("only LLM activity is supported, got " + slot);
            for (Map.Entry<String, LLMServiceActivity> service : llm.services().entrySet())
            {
                if (service.getValue().requestCount() <= 0)
                    continue;
                CanonicalImpacts impacts = this.perRequest(service.getKey(), service.getValue()).multiply(service.getValue().requestCount());
                total = total == null ? impacts : total.plus(impacts);
            }
        }
        if (total == null)
            throw new IllegalArgumentException("asset " + asset.identity().id() + " has no request to evaluate");
        return total;
    }

    @Override
    public Tolerance tolerance()
    {
        return TOLERANCE;
    }

    /** Impacts of one request generating {@code outputTokens}, embodied and usage split. */
    public CanonicalImpacts request(String provider, String model, long outputTokens, String zone)
    {
        return this.requests.computeIfAbsent(new Request(provider, model, outputTokens, zone), this::call);
    }

    /* Ecologits takes whole tokens and is affine in them: a fractional count lies on the line between its neighbours */
    private CanonicalImpacts perRequest(String serviceId, LLMServiceActivity activity)
    {
        String provider = activity.provider().name();
        String model = activity.model() != null ? activity.model() : serviceId;
        String zone = activity.location() == Country.WORLD ? WORLD_ZONE : activity.location().alpha3();
        double tokens = activity.outputTokenCount() / activity.requestCount();

        long floor = (long) Math.floor(tokens);
        CanonicalImpacts lower = this.request(provider, model, floor, zone);
        if (tokens == floor)
            return lower;
        double weight = tokens - floor;
        return lower.multiply(1.0 - weight).plus(this.request(provider, model, floor + 1, zone).multiply(weight));
    }

    private CanonicalImpacts call(Request request)
    {
        ObjectNode body = this.mapper.createObjectNode()
            .put("provider", request.provider())
            .put("model_name", request.model())
            .put("output_token_count", request.outputTokens())
            .put("electricity_mix_zone", request.zone());

        JsonNode impacts = this.post(body).path("impacts");
        return new CanonicalImpacts(
            phases(impacts, "gwp"),
            phases(impacts, "pe"),
            phases(impacts, "adpe")
        );
    }

    private static CanonicalImpacts.Phases phases(JsonNode impacts, String criterion)
    {
        return new CanonicalImpacts.Phases(
            impacts.path(criterion).path("unit").asText(),
            midpoint(impacts.path("embodied").path(criterion).path("value")),
            midpoint(impacts.path("usage").path(criterion).path("value"))
        );
    }

    /* A value is either a number or a {min, max} range */
    private static double midpoint(JsonNode value)
    {
        if (value.isNumber())
            return value.asDouble();
        return (value.path("min").asDouble() + value.path("max").asDouble()) / 2.0;
    }

    private JsonNode post(JsonNode body)
    {
        try {
            HttpRequest request = HttpRequest.newBuilder(this.uri.resolve("v1beta/estimations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(this.mapper.writeValueAsString(body)))
                .build();
            HttpResponse<String> response = this.http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
                throw new IllegalStateException("ecologits answered " + response.statusCode() + ": " + response.body());
            return this.mapper.readTree(response.body());
        }
        catch (IOException e)
        {
            throw new IllegalStateException("ecologits could not be called", e);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("ecologits call interrupted", e);
        }
    }

    private record Request(String provider, String model, long outputTokens, String zone) {}
}
