package tech.illuin.wombat.ui;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import tech.illuin.wombat.asset.model.Asset;

import java.util.List;

@CheckedTemplate(requireTypeSafeExpressions = false)
public class Templates
{

    public static native TemplateInstance impact(
        EnvironmentImpact env,
        AssetSelection assets,
        EnvironmentSelection environments,
        MaxSpan maxSpan
    );

    public static native TemplateInstance impactError(String from, String to);

    public record MaxSpan(long millis, String label) {}

    public record AssetSelection(List<Asset> all, List<String> selectedIds) {}

    public record EnvironmentView(String id, String name) {}

    public record EnvironmentSelection(List<EnvironmentView> options, String selectedId) {}
}
