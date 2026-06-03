package tech.illuin.wombat.ui;

import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import tech.illuin.wombat.handler.ImpactService;
import tech.illuin.wombat.handler.model.BoaviztaKubernetesConfig;
import tech.illuin.wombat.handler.model.ClusterInfo;
import tech.illuin.wombat.handler.model.ImpactRequest;
import tech.illuin.wombat.handler.model.ImpactResponse;
import tech.illuin.wombat.monitor.MonitorProperties;
import tech.illuin.wombat.persistence.NoCPUUsageException;
import tech.illuin.wombat.persistence.model.TimeRange;
import tech.illuin.wombat.profile.ProfileDto;
import tech.illuin.wombat.profile.ProfileSeedProperties;
import tech.illuin.wombat.profile.ServerProfileEntity;
import tech.illuin.wombat.profile.ServerProfileRepository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Path("ui")
public class UIController
{

    private final ImpactService impactService;
    private final ServerProfileRepository profileRepository;
    private final ProfileSeedProperties profileSeedProperties;
    private final MonitorProperties monitorProperties;

    public UIController(ImpactService impactService, ServerProfileRepository profileRepository, ProfileSeedProperties profileSeedProperties, MonitorProperties monitorProperties)
    {
        this.impactService = impactService;
        this.profileRepository = profileRepository;
        this.profileSeedProperties = profileSeedProperties;
        this.monitorProperties = monitorProperties;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get(
        @QueryParam("from") String from,
        @QueryParam("to") String to,
        @QueryParam("containers") List<String> containers,
        @QueryParam("clusters") List<String> clusters,
        @QueryParam("profileId") String profileId
    )
    {
        try
        {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm[:ss]").withZone(ZoneOffset.UTC);

            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
            Instant start = from != null && !from.isBlank()
                ? fmt.parse(from, Instant::from)
                : now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant();
            Instant end = to != null && !to.isBlank()
                ? fmt.parse(to, Instant::from)
                : now.plusMonths(1).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant();

            List<ClusterInfo> allClusters = this.monitorProperties.k8sConfigs().clusters().stream()
                .map(c -> new ClusterInfo(c.id(), c.namespace()))
                .toList();

            List<String> effectiveClusters = (clusters == null || clusters.isEmpty())
                ? allClusters.stream().findFirst().map(c -> List.of(c.id())).orElse(List.of())
                : clusters;

            List<ServerProfileEntity> allProfileEntities = this.profileRepository.listAll();
            ServerProfileEntity activeProfile = resolveProfile(profileId, allProfileEntities, this.profileSeedProperties.defaultId());

            ImpactRequest request = new ImpactRequest(
                new TimeRange(start, end),
                List.of(BoaviztaKubernetesConfig.fromProfileEntity(activeProfile, this.monitorProperties, effectiveClusters))
            );

            List<ImpactResponse> results = this.impactService.computeImpactResponse(request, containers, effectiveClusters);
            List<ProfileDto> allProfiles = allProfileEntities.stream().map(ProfileDto::from).toList();
            return Templates.impact(results.getFirst(), allClusters, effectiveClusters, allProfiles, activeProfile.id);
        }
        catch (NoCPUUsageException e) {
            return Templates.impactError(from, to);
        }
    }

    private static ServerProfileEntity resolveProfile(String profileId, List<ServerProfileEntity> profiles, String defaultId)
    {
        if (profiles.isEmpty())
            throw new IllegalStateException("No server profiles configured");
        if (profileId != null)
            return profiles.stream().filter(p -> p.id.equals(profileId)).findFirst()
                .orElseGet(() -> findByIdOrFirst(profiles, defaultId));
        return findByIdOrFirst(profiles, defaultId);
    }

    private static ServerProfileEntity findByIdOrFirst(List<ServerProfileEntity> profiles, String defaultId)
    {
        return profiles.stream().filter(p -> p.id.equals(defaultId)).findFirst()
            .orElse(profiles.getFirst());
    }
}
