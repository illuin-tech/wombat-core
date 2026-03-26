import boavizta.BoaviztaService;
import boavizta.model.BoaviztaInstanceConfigResponse;
import boavizta.model.BoaviztaInstanceImpactRequest;
import boavizta.model.BoaviztaInstanceImpactResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import persistence.MemoryLoadTarget;
import persistence.MeterLoadTarget;

import java.util.List;

@Path("impact")
public class ImpactController {

    private static final Logger logger = LoggerFactory.getLogger(ImpactController.class);

    @Inject BoaviztaService boaviztaService;
    private final MemoryLoadTarget memoryLoadTarget;

    public ImpactController(MemoryLoadTarget memoryLoadTarget) {
        this.memoryLoadTarget = memoryLoadTarget;
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public BoaviztaInstanceImpactResponse getImpact(ImpactInputRequest input)
    {
        BoaviztaInstanceConfigResponse instanceConfig = this.boaviztaService.getInstanceConfig(input.provider(), input.instanceType());
        // TODO: check units
        double loadPercentage = (this.memoryLoadTarget.computeCpuUsage() / 1000 / 1000 / 1000) / instanceConfig.vcpu().def() * 100;
        logger.info("Current memory loadPercentage {}", loadPercentage);

        return this.boaviztaService.getInstanceImpact(
            new BoaviztaInstanceImpactRequest(
                input.provider(),
                input.instanceType(),
                new BoaviztaInstanceImpactRequest.Usage(input.location(), List.of(new BoaviztaInstanceImpactRequest.Usage.LoadSegment(100, loadPercentage)))
            ),
            input.lifespan()
        );
    }

    public record ImpactInputRequest(
        @JsonProperty("provider") BoaviztaInstanceImpactRequest.Provider provider,
        @JsonProperty("instance_type") String instanceType,
        @JsonProperty("location") String location,
        @JsonProperty("lifespan") int lifespan
    ) {}
}
