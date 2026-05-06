import boavizta.BoaviztaService;
import boavizta.model.BoaviztaInstanceConfigResponse;
import boavizta.model.BoaviztaInstanceImpactRequest;
import boavizta.model.BoaviztaInstanceImpactResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import persistence.MemoryLoadTarget;
import persistence.NoCPUUsageException;
import profile.ServerConfig;
import profile.ServerProfileProperties;
import response.Response;

import java.util.List;

@Path("impact")
public class ImpactController {

    private static final Logger logger = LoggerFactory.getLogger(ImpactController.class);

    @Inject BoaviztaService boaviztaService;
    private final MemoryLoadTarget memoryLoadTarget;
    private final ServerProfileProperties serverProfileProperties;

    public ImpactController(MemoryLoadTarget memoryLoadTarget, ServerProfileProperties serverProfileProperties) {
        this.memoryLoadTarget = memoryLoadTarget;
        this.serverProfileProperties = serverProfileProperties;
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response<BoaviztaInstanceImpactResponse> getImpact(ImpactInputRequest input)
    {
        try {
            if ((input == null || input.serverConfig() == null) && !this.serverProfileProperties.enable())
                throw new BadRequestException("Required server not provided");
            ServerConfig serverConfig = (input != null && input.serverConfig() != null) ? input.serverConfig() : ServerConfig.fromServerProfile(this.serverProfileProperties);

            BoaviztaInstanceConfigResponse instanceConfig = this.boaviztaService.getInstanceConfig(serverConfig.provider(), serverConfig.instanceType());
            // TODO: check units
            double loadPercentage = (this.memoryLoadTarget.computeCpuUsage() / 1000 / 1000 / 1000) / instanceConfig.vcpu().def() * 100;
            logger.info("Current memory loadPercentage {}", loadPercentage);

            return Response.success(
                this.boaviztaService.getInstanceImpact(
                    new BoaviztaInstanceImpactRequest(
                        serverConfig.provider(),
                        serverConfig.instanceType(),
                        new BoaviztaInstanceImpactRequest.Usage(serverConfig.location(), List.of(new BoaviztaInstanceImpactRequest.Usage.LoadSegment(100, loadPercentage)))
                    ),
                    serverConfig.lifespan()
                )
            );
        } catch (NoCPUUsageException e)
        {
            throw new BadRequestException("No CPU Usage Could be found");
        }
    }

    public record ImpactInputRequest(@JsonProperty("server_config") ServerConfig serverConfig) {}
}
