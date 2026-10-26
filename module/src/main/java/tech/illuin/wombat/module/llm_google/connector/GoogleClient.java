package tech.illuin.wombat.module.llm_google.connector;

import feign.Param;
import feign.RequestLine;
import tech.illuin.wombat.module.llm_google.connector.model.GoogleQueryResponse;

public interface GoogleClient
{
    @RequestLine(
        "GET /v3/projects/{projectId}/timeSeries"
            + "?filter={filter}"
            + "&interval.startTime={start}"
            + "&interval.endTime={end}"
            + "&aggregation.alignmentPeriod={period}"
            + "&aggregation.perSeriesAligner=ALIGN_SUM"
            + "&aggregation.crossSeriesReducer=REDUCE_SUM"
    )
    GoogleQueryResponse query(
        @Param("projectId") String projectId,
        @Param("filter") String filter,
        @Param("start") String start,
        @Param("end") String end,
        @Param("period") String period
    );
}
