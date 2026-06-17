package tech.illuin.wombat.ui;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import tech.illuin.wombat.asset.AssetConfig;
import tech.illuin.wombat.boavizta.model.BoaviztaInstanceConfigResponse;
import tech.illuin.wombat.handler.model.ImpactResponse;

import java.util.List;

@CheckedTemplate(requireTypeSafeExpressions = false)
public class Templates
{

    public static native TemplateInstance impact(
        ImpactResponse impact,
        List<AssetConfig> allAssets,
        List<String> selectedAssetIds,
        AssetConfig selectedAsset,
        BoaviztaInstanceConfigResponse instanceConfig,
        double loadPercent
    );

    public static native TemplateInstance impactError(String from, String to);
}
