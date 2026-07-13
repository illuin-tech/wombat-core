package tech.illuin.wombat.handler;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tech.illuin.wombat.boavizta.BoaviztaClient;
import tech.illuin.wombat.boavizta.BoaviztaTestData;
import tech.illuin.wombat.ecologits.EcologitsClient;
import tech.illuin.wombat.ecologits.EcologitsTestData;
import tech.illuin.wombat.persistence.ServerMetricRepository;
import tech.illuin.wombat.persistence.model.MetricData;
import tech.illuin.wombat.persistence.model.ServerMetricEntity;


import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;

@QuarkusTest
class ImpactControllerTest
{

    @InjectMock
    @RestClient
    BoaviztaClient boaviztaClient;

    @InjectMock
    @RestClient
    EcologitsClient ecologitsClient;

    @Inject
    ServerMetricRepository datapointRepository;

    @BeforeEach
    @Transactional
    void seedMetrics()
    {
        datapointRepository.deleteAll();
        Mockito.when(boaviztaClient.getInstanceConfig(any(), any())).thenReturn(BoaviztaTestData.fakeInstanceConfig(8));
        Mockito.when(boaviztaClient.getInstanceImpact(anyBoolean(), anyInt(), any(), any())).thenReturn(BoaviztaTestData.fakeImpactResponse());
        Mockito.when(ecologitsClient.estimate(any())).thenReturn(EcologitsTestData.fakeEstimation());

        datapointRepository.save(metricRow(1_000L, "podA", "api", 100.0));
        datapointRepository.save(metricRow(1_000L, "podA", "worker", 300.0));
    }

    private static ServerMetricEntity metricRow(long instantMs, String pod, String container, double cpu)
    {
        ServerMetricEntity row = new ServerMetricEntity();
        row.instantMs = instantMs;
        row.data = new MetricData.KubernetesData("test-cluster", "test-ns", pod, container);
        row.cpuNanocores = cpu;
        return row;
    }

    @Test
    void getImpact_withExplicitAssetId_returnsFootprintForThatAsset()
    {
        String body = """
            {
              "source_time_range": { "start": "1970-01-01T00:00:00Z", "end": "1970-01-01T00:00:10Z" },
              "asset_ids": ["test-cluster"]
            }
            """;

        given()
            .contentType("application/json").body(body)
            .when().post("/impact")
            .then()
            .statusCode(200)
            .body("payload.asset_impacts.size()", is(1))
            .body("payload.asset_impacts.'test-cluster'.type", is("KUBERNETES_API"))
            .body("payload.asset_impacts.'test-cluster'.name", is("Test Cluster"))
            .body("payload.asset_impacts.'test-cluster'.measure_type", is("DYNAMIC"))
            .body("payload.asset_impacts.'test-cluster'.response.global_impact", notNullValue())
            .body("payload.asset_impacts.'test-cluster'.response.service_impacts.size()", greaterThan(0))
            .body("payload.asset_impacts.'test-cluster'.response.provider_config.clusters[0].id", is("test-cluster"))
            .body("payload.global.gwp", notNullValue())
            .body("payload.global.gwp.total", notNullValue())
            .body("payload.config", nullValue());
    }

    @Test
    void getImpact_withoutAssetIds_defaultsToAllConfiguredAssets()
    {
        String body = """
            {
              "source_time_range": { "start": "1970-01-01T00:00:00Z", "end": "1970-01-01T00:00:10Z" }
            }
            """;

        given()
            .contentType("application/json").body(body)
            .when().post("/impact")
            .then()
            .statusCode(200)
            .body("payload.asset_impacts.size()", is(2))
            .body("payload.asset_impacts.collect { it.value.type }", containsInAnyOrder("KUBERNETES_API", "LLM_STATIC"));
    }

    @Test
    void getImpact_withEnvironmentId_returnsAllAssetsOfThatEnvironment()
    {
        String body = """
            {
              "source_time_range": { "start": "1970-01-01T00:00:00Z", "end": "1970-01-01T00:00:10Z" },
              "environment_id": "test"
            }
            """;

        given()
            .contentType("application/json").body(body)
            .when().post("/impact")
            .then()
            .statusCode(200)
            .body("payload.asset_impacts.size()", is(2))
            .body("payload.asset_impacts.'test-llm'.type", is("LLM_STATIC"))
            .body("payload.asset_impacts.'test-llm'.name", is("Test LLM"))
            .body("payload.asset_impacts.'test-llm'.measure_type", is("STATIC"))
            .body("payload.asset_impacts.'test-llm'.profile.model", is("mistral-large-latest"))
            .body("payload.asset_impacts.'test-llm'.profile.request_profile.output_token_count", is(500))
            .body("payload.asset_impacts.'test-llm'.request_count", greaterThan(0.0f))
            .body("payload.asset_impacts.'test-llm'.estimation.impacts.gwp", notNullValue())
            .body("payload.global.gwp.total", notNullValue())
            .body("payload.config.id", is("test"))
            .body("payload.config.name", is("Test"))
            .body("payload.config.assets.size()", is(2))
            .body("payload.config.time_range.start", notNullValue());
    }

    @Test
    void getImpact_withUnknownEnvironmentId_returns400()
    {
        String body = """
            {
              "source_time_range": { "start": "1970-01-01T00:00:00Z", "end": "1970-01-01T00:00:10Z" },
              "environment_id": "unknown-env"
            }
            """;

        given()
            .contentType("application/json").body(body)
            .when().post("/impact")
            .then()
            .statusCode(400);
    }

    @Test
    void getImpact_noCpuData_returns400()
    {
        // Wipe out the datapoints seeded in @BeforeEach via a separate transaction
        emptyDatapoints();

        String body = """
            {
              "source_time_range": { "start": "1970-01-01T00:00:00Z", "end": "1970-01-01T00:00:10Z" }
            }
            """;

        given()
            .contentType("application/json").body(body)
            .when().post("/impact")
            .then()
            .statusCode(400);
    }

    @Test
    void getEnvironments_listsConfiguredEnvironments()
    {
        given()
            .when().get("/impact/environments")
            .then()
            .statusCode(200)
            .body("payload.size()", is(1))
            .body("payload[0].id", is("test"))
            .body("payload[0].name", is("Test"))
            .body("payload[0].assets.size()", is(2))
            .body("payload[0].assets.id", containsInAnyOrder("test-cluster", "test-llm"))
            .body("payload[0].time_range", nullValue());
    }

    @Test
    void getAssets_returnsAssetSummariesOfEnvironment()
    {
        given()
            .when().get("/impact/environments/test/assets")
            .then()
            .statusCode(200)
            .body("payload.size()", is(2))
            .body("payload.type", containsInAnyOrder("KUBERNETES_API", "LLM_STATIC"))
            .body("payload.find { it.id == 'test-cluster' }.name", is("Test Cluster"));
    }

    @Test
    void getAssets_unknownEnvironment_returns404()
    {
        given()
            .when().get("/impact/environments/unknown-env/assets")
            .then()
            .statusCode(404);
    }

    @Transactional
    void emptyDatapoints()
    {
        datapointRepository.deleteAll();
    }
}
