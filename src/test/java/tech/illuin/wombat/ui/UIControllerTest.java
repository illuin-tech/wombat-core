package tech.illuin.wombat.ui;

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
import tech.illuin.wombat.persistence.model.DatapointEntity;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;

@QuarkusTest
class UIControllerTest
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
    void seed() throws Exception
    {
        datapointRepository.deleteAll();
        Mockito.when(boaviztaClient.getInstanceConfig(any(), any())).thenReturn(BoaviztaTestData.fakeInstanceConfig(8));
        Mockito.when(boaviztaClient.getInstanceImpact(anyBoolean(), anyInt(), any(), any())).thenReturn(BoaviztaTestData.fakeImpactResponse());

        DatapointEntity row = new DatapointEntity();
        row.instantMs = System.currentTimeMillis();
        row.type = "KUBERNETES";
        row.cluster = "test-cluster";
        row.namespace = "test-ns";
        row.payload = mapper.writeValueAsString(new tech.illuin.wombat.persistence.model.PodMetrics(
            Map.of("podA", new tech.illuin.wombat.persistence.model.PodMetrics.ContainerMetrics(Map.of("api", "100", "worker", "300")))));
        datapointRepository.save(row);
    }

    @Test
    void get_withSeededMetrics_rendersImpactPage()
    {
        given()
            .when().get("/ui")
            .then()
            .statusCode(200)
            .body(containsString("Environmental Impact"))
            .body(containsString("api"))
            .body(containsString("worker"));
    }

    @Test
    void get_withAssetParam_selectsThatAsset()
    {
        given()
            .queryParam("assets", "test-cluster")
            .when().get("/ui")
            .then()
            .statusCode(200)
            .body(containsString("c5.large"))
            .body(containsString("Test Cluster"))
            .body(containsString("vCPU"))
            .body(containsString("Memory"))
            .body(containsString("CPU used"))
            .body(containsString("Load factor"));
    }

    @Test
    void get_withCustomTimeRange_acceptsAndRenders()
    {
        given()
            .queryParam("from", "1970-01-01T00:00")
            .queryParam("to", "2099-01-01T00:00")
            .when().get("/ui")
            .then()
            .statusCode(200);
    }

    @Test
    void get_noMetrics_returnsErrorPage()
    {
        emptyDatapoints();

        given()
            .when().get("/ui")
            .then()
            .statusCode(200)
            .body(containsString("No CPU usage found"));
    }

    @Transactional
    void emptyDatapoints()
    {
        datapointRepository.deleteAll();
    }
}
