package prometheus.model;

import java.util.List;

public record PrometheusResponse(
    PrometheusData data,
    String status
) {
    public record PrometheusData(
        List<Result> result,
        String resultType
    ) {
        public record Result(
            Metric metric,
            List<List<Object>> values
        ) {
            // TODO: put proper value
            public record Metric() {}
        }
    }
}
