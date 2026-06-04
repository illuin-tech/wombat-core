package tech.illuin.wombat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import tech.illuin.wombat.persistence.DatapointRepository;
import tech.illuin.wombat.persistence.model.KubernetesPayload;

import java.util.List;
import java.util.Map;

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
    DatapointRepository datapointRepository;

    @Inject
    ObjectMapper mapper;

    @BeforeEach
    @Transactional
    void seedMetrics() throws Exception
    {
        datapointRepository.deleteAll();
        Mockito.when(boaviztaClient.getInstanceConfig(any(), any())).thenReturn(BoaviztaTestData.fakeInstanceConfig(8));
        Mockito.when(boaviztaClient.getInstanceImpact(anyBoolean(), anyInt(), any(), any())).thenReturn(BoaviztaTestData.fakeImpactResponse());

        KubernetesPayload payload = new KubernetesPayload("test-cluster", "test-ns",
            Map.of("podA", Map.of("api", "100", "worker", "300")));
        String json = mapper.writeValueAsString(List.of(payload));
        datapointRepository.upsert(1_000L, "KUBERNETES", existing -> json);
    }

    @Test
    void getImpact_withExplicitConfig_returnsFootprintForFirstResponse()
    {
        String body = """
            {
              "source_time_range": { "start": "1970-01-01T00:00:00Z", "end": "1970-01-01T00:00:10Z" },
              "configs": [{
                "type": "BOAVIZTA_KUBERNETES",
                "provider": "aws",
                "instance_type": "c5.large",
                "location": "FRA",
                "lifespan": 43800,
                "clusters": [{ "id": "test-cluster", "namespace": "test-ns" }]
              }]
            }
            """;

        given()
            .contentType("application/json").body(body)
            .when().post("/impact")
            .then()
            .statusCode(200)
            .body("payload.size()", is(1))
            .body("payload[0].global_impact", notNullValue())
            .body("payload[0].service_impacts.size()", greaterThan(0));
    }

    @Test
    void getImpact_withoutConfig_fallsBackToDefaultProfile()
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
