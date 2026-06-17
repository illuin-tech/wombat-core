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
import tech.illuin.wombat.persistence.KubernetesMetricRepository;
import tech.illuin.wombat.persistence.model.KubernetesMetricEntity;


import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;

@QuarkusTest
class ImpactControllerTest
{

    @InjectMock
    @RestClient
    BoaviztaClient boaviztaClient;

    @Inject
    KubernetesMetricRepository datapointRepository;

    @BeforeEach
    @Transactional
    void seedMetrics()
    {
        datapointRepository.deleteAll();
        Mockito.when(boaviztaClient.getInstanceConfig(any(), any())).thenReturn(BoaviztaTestData.fakeInstanceConfig(8));
        Mockito.when(boaviztaClient.getInstanceImpact(anyBoolean(), anyInt(), any(), any())).thenReturn(BoaviztaTestData.fakeImpactResponse());

        datapointRepository.save(metricRow(1_000L, "podA", "api", 100.0));
        datapointRepository.save(metricRow(1_000L, "podA", "worker", 300.0));
    }

    private static KubernetesMetricEntity metricRow(long instantMs, String pod, String container, double cpu)
    {
        KubernetesMetricEntity row = new KubernetesMetricEntity();
        row.instantMs = instantMs;
        row.cluster = "test-cluster";
        row.namespace = "test-ns";
        row.pod = pod;
        row.container = container;
        row.cpu = cpu;
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
            .body("payload.size()", is(1))
            .body("payload[0].global_impact", notNullValue())
            .body("payload[0].service_impacts.size()", greaterThan(0))
            .body("payload[0].parameters.provider_config.clusters[0].id", is("test-cluster"));
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
            .body("payload[0].parameters.provider_config.instance_type", notNullValue());
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

    @Transactional
    void emptyDatapoints()
    {
        datapointRepository.deleteAll();
    }
}
