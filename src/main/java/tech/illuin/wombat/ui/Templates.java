package tech.illuin.wombat.ui;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import tech.illuin.wombat.handler.model.ClusterInfo;
import tech.illuin.wombat.handler.model.ImpactResponse;
import tech.illuin.wombat.profile.ProfileDto;

import java.util.List;

@CheckedTemplate(requireTypeSafeExpressions = false)
public class Templates
{

    public static native TemplateInstance impact(
        ImpactResponse impact,
        List<ClusterInfo> allClusters,
        List<String> selectedClusterIds,
        List<ProfileDto> allProfiles,
        String selectedProfileId
    );

    public static native TemplateInstance impactError(String from, String to);
}
