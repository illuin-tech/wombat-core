package tech.illuin.wombat.ui;

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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;

@QuarkusTest
class UIControllerTest
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
    void seed()
    {
        datapointRepository.deleteAll();
        Mockito.when(boaviztaClient.getInstanceConfig(any(), any())).thenReturn(BoaviztaTestData.fakeInstanceConfig(8));
        Mockito.when(boaviztaClient.getInstanceImpact(anyBoolean(), anyInt(), any(), any())).thenReturn(BoaviztaTestData.fakeImpactResponse());
        Mockito.when(ecologitsClient.estimate(any())).thenReturn(EcologitsTestData.fakeEstimation());

        long now = System.currentTimeMillis();
        datapointRepository.save(metricRow(now, "podA", "api", 100.0));
        datapointRepository.save(metricRow(now, "podA", "worker", 300.0));
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
            .queryParam("services", "test-cluster")
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
    void get_withLLMAsset_rendersLLMBreakdown()
    {
        given()
            .when().get("/ui")
            .then()
            .statusCode(200)
            .body(containsString("Test LLM"))
            .body(containsString("mistral-large-latest"))
            .body(containsString("Output tokens / request"))
            .body(containsString("Requests / year"))
            .body(containsString("GWP / request"));
    }

    @Test
    void get_withLLMAsset_includesLLMServiceInBreakdownTableAndCounts()
    {
        given()
            .when().get("/ui")
            .then()
            .statusCode(200)
            .body(containsString("Test LLM / mistral-large-latest"))
            .body(containsString("Docker services"))
            .body(containsString("LLM services"))
            .body(containsString("icons/llm.svg"));
    }

    @Test
    void get_withLLMAsset_modelIsListedInServicePicker()
    {
        given()
            .when().get("/ui")
            .then()
            .statusCode(200)
            .body(containsString("value=\"mistral-large-latest\""));
    }

    @Test
    void get_withModelFilteredOut_excludesLLMFromAggregatesButKeepsAssetBlock()
    {
        given()
            .queryParam("services", "test-llm=api")
            .when().get("/ui")
            .then()
            .statusCode(200)
            .body(not(containsString("Test LLM / mistral-large-latest")))
            .body(containsString("Test LLM"));
    }

    @Test
    void get_withOnlyLLMAssetSelected_rendersWithoutKubernetesMetrics()
    {
        given()
            .queryParam("services", "test-llm")
            .when().get("/ui")
            .then()
            .statusCode(200)
            .body(containsString("Test LLM"))
            .body(containsString("mistral-large-latest"));
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
