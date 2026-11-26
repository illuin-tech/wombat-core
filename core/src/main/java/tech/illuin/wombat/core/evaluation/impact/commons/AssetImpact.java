package tech.illuin.wombat.core.evaluation.impact.commons;

import tech.illuin.wombat.core.activity.commons.TimeRange;
import tech.illuin.wombat.core.asset.AssetIdentity;
import tech.illuin.wombat.core.asset.profile.inference.ProfileOrigin;
import tech.illuin.wombat.core.evaluation.AssetEvaluation;

import java.util.List;

/**
 * @param profileOrigin where the profile the asset was evaluated with comes from
 * @param warnings      what the evaluation had to assume, e.g. to infer the asset's profile; empty when nothing
 */
public record AssetImpact(
    AssetIdentity assetIdentity,
    Footprint footprint,
    List<ServiceImpact> serviceImpacts,
    ImpactProvider provider,
    TimeRange range,
    ProfileOrigin profileOrigin,
    List<String> warnings
) implements AssetEvaluation
{
    public AssetImpact
    {
        profileOrigin = profileOrigin != null ? profileOrigin : ProfileOrigin.DECLARED;
        warnings = warnings != null ? List.copyOf(warnings) : List.of();
    }

    /**
     * The impact of an asset evaluated with the profile it declares.
     */
    public AssetImpact(AssetIdentity assetIdentity, Footprint footprint, List<ServiceImpact> serviceImpacts, ImpactProvider provider, TimeRange range)
    {
        this(assetIdentity, footprint, serviceImpacts, provider, range, ProfileOrigin.DECLARED, List.of());
    }

    /**
     * @return this impact, evaluated with a profile coming from {@code origin}, which had to assume {@code assumptions}
     */
    public AssetImpact withProfileOrigin(ProfileOrigin origin, List<String> assumptions)
    {
        return new AssetImpact(this.assetIdentity, this.footprint, this.serviceImpacts, this.provider, this.range, origin, assumptions);
    }

    @Override
    public EvaluationType type()
    {
        return EvaluationType.IMPACT;
    }
}
