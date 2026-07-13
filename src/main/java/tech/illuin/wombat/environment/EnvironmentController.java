package tech.illuin.wombat.environment;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import tech.illuin.wombat.asset.UnknownEnvironmentException;
import tech.illuin.wombat.environment.model.AssetCreationRequest;
import tech.illuin.wombat.environment.model.AssetInfo;
import tech.illuin.wombat.environment.model.EnvironmentCreationRequest;
import tech.illuin.wombat.environment.model.EnvironmentInfo;
import tech.illuin.wombat.environment.persistence.AssetData;
import tech.illuin.wombat.environment.persistence.AssetEntity;
import tech.illuin.wombat.environment.persistence.AssetRepository;
import tech.illuin.wombat.environment.persistence.EnvironmentEntity;
import tech.illuin.wombat.environment.persistence.EnvironmentRepository;
import tech.illuin.wombat.monitor.AssetType;
import tech.illuin.wombat.profile.model.ProfileType;
import tech.illuin.wombat.profile.persistence.ProfileEntity;
import tech.illuin.wombat.profile.persistence.ProfileRepository;

import java.util.List;

@Path("environments")
public class EnvironmentController
{

    private final EnvironmentRepository repository;
    private final AssetRepository assetRepository;
    private final ProfileRepository profileRepository;

    public EnvironmentController(EnvironmentRepository repository, AssetRepository assetRepository, ProfileRepository profileRepository)
    {
        this.repository = repository;
        this.assetRepository = assetRepository;
        this.profileRepository = profileRepository;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<EnvironmentInfo> list()
    {
        return this.repository.listAll().stream().map(this::toInfo).toList();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public EnvironmentInfo create(EnvironmentCreationRequest request)
    {
        if (request.id() == null || request.id().isBlank())
            throw new InvalidEnvironmentException("Environment id is required");
        if (request.name() == null || request.name().isBlank())
            throw new InvalidEnvironmentException("Environment name is required");
        if (this.repository.findByIdOptional(request.id()).isPresent())
            throw new EnvironmentAlreadyExistsException(request.id());

        EnvironmentEntity entity = new EnvironmentEntity();
        entity.id = request.id();
        entity.name = request.name();
        this.repository.persist(entity);
        return this.toInfo(entity);
    }

    @POST
    @Path("/{id}/disable")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public EnvironmentInfo disable(@PathParam("id") String id)
    {
        EnvironmentEntity entity = this.find(id);
        if (entity.disabledAt == null)
            entity.disabledAt = EnvironmentEntity.now();
        return this.toInfo(entity);
    }

    @POST
    @Path("/{id}/enable")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public EnvironmentInfo enable(@PathParam("id") String id)
    {
        EnvironmentEntity entity = this.find(id);
        entity.disabledAt = null;
        return this.toInfo(entity);
    }

    @POST
    @Path("/{id}/assets")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public AssetInfo addAsset(@PathParam("id") String id, AssetCreationRequest request)
    {
        EnvironmentEntity environment = this.find(id);
        this.validateAsset(request);

        AssetEntity entity = new AssetEntity();
        entity.id = request.id();
        entity.environmentId = environment.id;
        entity.name = request.name();
        entity.type = request.type();
        entity.profileId = request.profileId();
        entity.data = toData(request);
        this.assetRepository.persist(entity);
        return AssetInfo.from(entity);
    }

    @DELETE
    @Path("/{id}/assets/{assetId}")
    @Transactional
    public void removeAsset(@PathParam("id") String id, @PathParam("assetId") String assetId)
    {
        EnvironmentEntity environment = this.find(id);
        AssetEntity asset = this.assetRepository.findByIdOptional(assetId)
            .filter(candidate -> candidate.environmentId.equals(environment.id))
            .orElseThrow(() -> new UnknownAssetException(id, assetId));
        this.assetRepository.delete(asset);
    }

    private void validateAsset(AssetCreationRequest request)
    {
        if (request.id() == null || request.id().isBlank())
            throw new InvalidAssetException("Asset id is required");
        if (request.name() == null || request.name().isBlank())
            throw new InvalidAssetException("Asset name is required");
        if (request.type() == null)
            throw new InvalidAssetException("Asset type is required");
        if (request.profileId() == null || request.profileId().isBlank())
            throw new InvalidAssetException("Asset profile_id is required");
        if (this.assetRepository.findByIdOptional(request.id()).isPresent())
            throw new AssetAlreadyExistsException(request.id());

        ProfileEntity profile = this.profileRepository.findByIdOptional(request.profileId())
            .orElseThrow(() -> new InvalidAssetException("Unknown profile: " + request.profileId()));
        ProfileType expectedType = switch (request.type())
        {
            case KUBERNETES_API -> ProfileType.INFRASTRUCTURE;
            case LLM_STATIC -> ProfileType.LLM;
        };
        if (profile.type != expectedType)
            throw new InvalidAssetException("Profile " + request.profileId() + " has type " + profile.type + ", expected " + expectedType + " for a " + request.type() + " asset");

        if (request.type() == AssetType.KUBERNETES_API)
        {
            if (request.configPath() == null || request.configPath().isBlank())
                throw new InvalidAssetException("config_path is required for KUBERNETES_API assets");
            if (request.namespace() == null || request.namespace().isBlank())
                throw new InvalidAssetException("namespace is required for KUBERNETES_API assets");
        }
        else if (request.configPath() != null || request.namespace() != null || request.context() != null || request.readTimeout() != null)
            throw new InvalidAssetException("config_path, namespace, context and read_timeout only apply to KUBERNETES_API assets");
    }

    private static AssetData toData(AssetCreationRequest request)
    {
        return switch (request.type())
        {
            case KUBERNETES_API -> new AssetData.KubernetesData(request.configPath(), request.namespace(), request.context(), request.readTimeout());
            case LLM_STATIC -> new AssetData.LLMData();
        };
    }

    private EnvironmentInfo toInfo(EnvironmentEntity entity)
    {
        return EnvironmentInfo.from(entity, this.assetRepository.findByEnvironment(entity.id));
    }

    private EnvironmentEntity find(String id)
    {
        return this.repository.findByIdOptional(id)
            .orElseThrow(() -> new UnknownEnvironmentException(id));
    }
}
