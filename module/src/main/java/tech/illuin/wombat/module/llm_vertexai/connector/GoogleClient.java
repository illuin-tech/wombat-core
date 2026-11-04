package tech.illuin.wombat.module.llm_vertexai.connector;

import feign.Param;
import feign.RequestLine;
import tech.illuin.wombat.module.llm_vertexai.connector.model.GoogleQueryResponse;

public interface GoogleClient
{
    /**
     * Lists the time series matching {@code filter} over [start, end], aligned and summed per {@code period}.
     * A {@code null} {@code pageToken} requests the first page.
     */
    @RequestLine(
        "GET /v3/{resourceName}/timeSeries"
            + "?filter={filter}"
            + "&interval.startTime={start}"
            + "&interval.endTime={end}"
            + "&aggregation.alignmentPeriod={period}"
            + "&aggregation.perSeriesAligner=ALIGN_SUM"
            + "&aggregation.crossSeriesReducer=REDUCE_SUM"
            + "&aggregation.groupByFields=resource.labels.model_user_id"
            + "&aggregation.groupByFields=resource.labels.location"
            + "&view=FULL"
            + "&pageToken={pageToken}"
    )
    GoogleQueryResponse query(
        @Param("resourceName") String resourceName,
        @Param("filter") String filter,
        @Param("start") String start,
        @Param("end") String end,
        @Param("period") String period,
        @Param("pageToken") String pageToken
    );
}
