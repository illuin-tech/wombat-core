package tech.illuin.wombat.profile;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import tech.illuin.wombat.profile.model.InfrastructureProfile;
import tech.illuin.wombat.profile.model.Profile;
import tech.illuin.wombat.profile.model.ProfileType;
import tech.illuin.wombat.profile.persistence.ProfileData;
import tech.illuin.wombat.profile.persistence.ProfileEntity;
import tech.illuin.wombat.profile.persistence.ProfileRepository;

import java.util.List;

@Path("profiles")
public class ProfileController
{

    private final ProfileRepository repository;

    public ProfileController(ProfileRepository repository)
    {
        this.repository = repository;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Profile> list()
    {
        return this.repository.listAll().stream().map(Profile::from).toList();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Profile get(@PathParam("id") String id)
    {
        return this.repository.findByIdOptional(id)
            .map(Profile::from)
            .orElseThrow(NotFoundException::new);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public InfrastructureProfile create(InfrastructureProfile profile)
    {
        ProfileEntity entity = new ProfileEntity();
        entity.id = profile.id();
        entity.description = profile.description();
        entity.type = ProfileType.INFRASTRUCTURE;
        entity.provider = profile.provider().name();
        entity.location = profile.location();
        entity.data = new ProfileData.InfrastructureData(profile.instanceType(), profile.lifespan());
        this.repository.persist(entity);
        return InfrastructureProfile.from(entity);
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public InfrastructureProfile update(@PathParam("id") String id, InfrastructureProfile dto)
    {
        ProfileEntity entity = this.repository.findByIdOptional(id)
            .orElseThrow(NotFoundException::new);
        entity.description = dto.description();
        entity.provider = dto.provider().name();
        entity.location = dto.location();
        entity.data = new ProfileData.InfrastructureData(dto.instanceType(), dto.lifespan());
        return InfrastructureProfile.from(entity);
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public void delete(@PathParam("id") String id)
    {
        if (!this.repository.deleteById(id))
            throw new NotFoundException();
    }
}
