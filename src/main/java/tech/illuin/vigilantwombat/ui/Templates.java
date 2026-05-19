package tech.illuin.vigilantwombat.ui;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import tech.illuin.vigilantwombat.handler.model.ImpactResponse;

@CheckedTemplate(requireTypeSafeExpressions = false)
public class Templates {
    public static native TemplateInstance impact(ImpactResponse impact);
    public static native TemplateInstance impactError(String from, String to);
}
