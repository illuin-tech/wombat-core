package tech.illuin.wombat.module.llm_anthropic.connector;

import feign.Param;
import feign.RequestLine;
import tech.illuin.wombat.module.llm_anthropic.connector.model.AnthropicUsageReport;

import java.util.Collection;

public interface AnthropicAdminClient
{
    /**
     * Usage is always grouped by model. Any {@code null} argument is left out of the query; collections are expanded
     * into one parameter per element (e.g. {@code models[]=a&models[]=b}).
     *
     * @param startingAt   RFC 3339 timestamp, buckets starting on or after it are returned
     * @param endingAt     RFC 3339 timestamp, buckets ending before it are returned
     * @param bucketWidth  {@code 1m}, {@code 1h} or {@code 1d}
     * @param limit        maximum number of buckets per page (1440, 168 and 31 respectively at most)
     * @param models       optional filter on model ids
     * @param workspaceIds optional filter on workspace ids
     * @param apiKeyIds    optional filter on API key ids
     * @param page         optional {@code next_page} cursor from the previous response
     */
    @RequestLine(
        "GET /v1/organizations/usage_report/messages" +
        "?starting_at={startingAt}" +
        "&ending_at={endingAt}" +
        "&bucket_width={bucketWidth}" +
        "&limit={limit}" +
        "&group_by[]=model" +
        "&models[]={models}" +
        "&workspace_ids[]={workspaceIds}" +
        "&api_key_ids[]={apiKeyIds}" +
        "&page={page}"
    )
    AnthropicUsageReport usageReport(
        @Param("startingAt") String startingAt,
        @Param("endingAt") String endingAt,
        @Param("bucketWidth") String bucketWidth,
        @Param("limit") int limit,
        @Param("models") Collection<String> models,
        @Param("workspaceIds") Collection<String> workspaceIds,
        @Param("apiKeyIds") Collection<String> apiKeyIds,
        @Param("page") String page
    );
}
