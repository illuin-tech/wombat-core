package tech.illuin.wombat.ui;

import tech.illuin.wombat.ecologits.model.EcologitsEstimationResponse;
import tech.illuin.wombat.profile.model.LLMProfile;
import tech.illuin.wombat.profile.model.MeasureType;

public record LLMAssetBreakdown(
    String name,
    MeasureType measureType,
    String provider,
    String model,
    String location,
    int outputTokenCount,
    int requestPerYear,
    double requestCount,
    Amount gwpPerRequest,
    Amount gwpTotal,
    Amount energyTotal,
    Amount peTotal,
    Amount adpeTotal
)
{
    public static LLMAssetBreakdown from(LLMAssetImpact impact)
    {
        LLMProfile profile = impact.profile();
        LLMProfile.RequestProfile requestProfile = profile.requestProfile();
        EcologitsEstimationResponse.Impacts impacts = impact.estimation().impacts();
        return new LLMAssetBreakdown(
            impact.name(),
            profile.measureType(),
            profile.provider().name(),
            profile.model(),
            profile.location(),
            requestProfile.outputTokenCount(),
            requestProfile.requestPerYear(),
            impact.requestCount(),
            perRequest(impacts.gwp()),
            total(impacts.gwp(), impact.requestCount()),
            total(impacts.energy(), impact.requestCount()),
            total(impacts.pe(), impact.requestCount()),
            total(impacts.adpe(), impact.requestCount())
        );
    }

    private static Amount perRequest(EcologitsEstimationResponse.Metric metric)
    {
        return new Amount(mean(metric), metric.unit());
    }

    private static Amount total(EcologitsEstimationResponse.Metric metric, double requestCount)
    {
        return new Amount(mean(metric) * requestCount, metric.unit());
    }

    private static double mean(EcologitsEstimationResponse.Metric metric)
    {
        return (metric.value().min() + metric.value().max()) / 2.0;
    }

    public record Amount(double value, String unit) {}
}
