package tech.illuin.wombat.module.llm_google.connector.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

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
        assertEquals("gemini-2.5-flash", series.resource().labels().model());
        assertEquals(1576.0d, series.points().getFirst().numericValue());
        assertEquals(Optional.of(Instant.parse("2026-10-06T20:59:03Z")), series.points().getFirst().endTime());
    }

    @Test
    void aPointWithoutIntervalHasNoEndTime() throws Exception
    {
        GoogleQueryResponse response = MAPPER.readValue(
            "{\"timeSeries\":[{\"points\":[{},{\"interval\":{}}]}]}", GoogleQueryResponse.class);

        response.timeSeries().getFirst().points().forEach(point -> assertEquals(Optional.empty(), point.endTime()));
    }

    @Test
    void parsesIntegerAndDoublePointValues() throws Exception
    {
        GoogleQueryResponse response = MAPPER.readValue("""
            {"timeSeries":[
              {"points":[{"value":{"int64Value":"100"}},{"value":{"int64Value":"50"}}]},
              {"points":[{"value":{"doubleValue":0.5}},{"value":{"int64Value":"25"}}]}
            ]}
            """, GoogleQueryResponse.class);

        assertEquals(100.0d, response.timeSeries().getFirst().points().getFirst().numericValue());
        assertEquals(50.0d, response.timeSeries().getFirst().points().get(1).numericValue());
        assertEquals(0.5d, response.timeSeries().get(1).points().getFirst().numericValue());
        assertEquals(25.0d, response.timeSeries().get(1).points().get(1).numericValue());
    }

    @Test
    void anEmptyResponseHasNoTimeSeries() throws Exception
    {
        GoogleQueryResponse response = MAPPER.readValue("{}", GoogleQueryResponse.class);

        assertNull(response.timeSeries());
    }

    @Test
    void aSeriesWithoutPointsOrValuesContributesNothing() throws Exception
    {
        GoogleQueryResponse response = MAPPER.readValue(
            "{\"timeSeries\":[{},{\"points\":[{},{\"value\":{}}]}]}", GoogleQueryResponse.class);

        assertNull(response.timeSeries().getFirst().points());
        response.timeSeries().get(1).points().forEach(point -> assertEquals(0.0d, point.numericValue()));
    }

    @Test
    void unknownFieldsAreIgnored() throws Exception
    {
        GoogleQueryResponse response = MAPPER.readValue(
            "{\"timeSeries\":[],\"unit\":\"1\",\"nextPageToken\":\"abc\",\"executionErrors\":[]}", GoogleQueryResponse.class);

        assertEquals("abc", response.nextPageToken());
    }
}
