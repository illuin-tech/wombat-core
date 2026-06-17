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

import java.util.List;

@Path("profiles")
public class ProfileController
{

    private final ServerProfileRepository repository;

    public ProfileController(ServerProfileRepository repository)
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
    public Profile create(Profile profile)
    {
        ServerProfileEntity entity = new ServerProfileEntity();
        entity.id = profile.id();
        entity.description = profile.description();
        entity.provider = profile.provider();
        entity.instanceType = profile.instanceType();
        entity.location = profile.location();
        entity.lifespan = profile.lifespan();
        this.repository.persist(entity);
        return Profile.from(entity);
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Profile update(@PathParam("id") String id, Profile dto)
    {
        ServerProfileEntity entity = this.repository.findByIdOptional(id)
            .orElseThrow(NotFoundException::new);
        entity.description = dto.description();
        entity.provider = dto.provider();
        entity.instanceType = dto.instanceType();
        entity.location = dto.location();
        entity.lifespan = dto.lifespan();
        return Profile.from(entity);
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
