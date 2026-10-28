package tech.illuin.wombat.module.llm_google.connector.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GoogleQueryResponseTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String SAMPLE = """
        {
          "timeSeries": [
            {
              "metric": {
                "labels": {
                  "request_type": "shared",
                  "source": "us-central1",
                  "type": "output",
                  "shared_request_type": "standard"
                },
                "type": "aiplatform.googleapis.com/publisher/online_serving/token_count"
              },
              "resource": {
                "type": "aiplatform.googleapis.com/PublisherModel",
                "labels": {
                  "location": "us-central1",
                  "publisher": "google",
                  "model_version_id": "",
                  "model_user_id": "gemini-2.5-flash",
                  "project_id": "macro-theater-402700"
                }
              },
              "metricKind": "DELTA",
              "valueType": "INT64",
              "points": [
                {
                  "interval": {
                    "startTime": "2026-10-06T20:58:03.001Z",
                    "endTime": "2026-10-06T20:59:03Z"
                  },
                  "value": { "int64Value": "1576" }
                }
              ]
            }
          ],
          "unit": "1"
        }
        """;

    @Test
    void parsesARealCloudMonitoringPayload() throws Exception
    {
        GoogleQueryResponse response = MAPPER.readValue(SAMPLE, GoogleQueryResponse.class);

        assertEquals(1, response.timeSeries().size());
        GoogleQueryResponse.TimeSeries series = response.timeSeries().getFirst();
        assertEquals("DELTA", series.metricKind());
        assertEquals("INT64", series.valueType());
        assertEquals("gemini-2.5-flash", series.resource().get("labels").get("model_user_id").asText());
        assertEquals(1576.0d, response.sum());
    }

    @Test
    void sumsEveryPointOfEverySeries() throws Exception
    {
        GoogleQueryResponse response = MAPPER.readValue("""
            {"timeSeries":[
              {"points":[{"value":{"int64Value":"100"}},{"value":{"int64Value":"50"}}]},
              {"points":[{"value":{"doubleValue":0.5}},{"value":{"int64Value":"25"}}]}
            ]}
            """, GoogleQueryResponse.class);

        assertEquals(175.5d, response.sum());
    }

    @Test
    void anEmptyResponseSumsToZero() throws Exception
    {
        GoogleQueryResponse response = MAPPER.readValue("{}", GoogleQueryResponse.class);

        assertNull(response.timeSeries());
        assertEquals(0.0d, response.sum());
        assertEquals(0.0d, new GoogleQueryResponse(List.of(), null).sum());
    }

    @Test
    void aSeriesWithoutPointsOrValuesContributesNothing() throws Exception
    {
        GoogleQueryResponse response = MAPPER.readValue(
            "{\"timeSeries\":[{},{\"points\":[{},{\"value\":{}}]}]}", GoogleQueryResponse.class);

        assertEquals(0.0d, response.sum());
    }

    @Test
    void unknownFieldsAreIgnored() throws Exception
    {
        GoogleQueryResponse response = MAPPER.readValue(
            "{\"timeSeries\":[],\"unit\":\"1\",\"nextPageToken\":\"abc\",\"executionErrors\":[]}", GoogleQueryResponse.class);

        assertEquals("abc", response.nextPageToken());
    }
}
