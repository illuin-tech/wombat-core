package tech.illuin.wombat.module.boavizta.connector;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import tech.illuin.wombat.module.boavizta.connector.model.BoaviztaInstanceConfigResponse;
import tech.illuin.wombat.module.boavizta.connector.model.BoaviztaCloudInstanceImpactRequest;
import tech.illuin.wombat.module.boavizta.connector.model.BoaviztaImpactResponse;
import tech.illuin.wombat.module.boavizta.connector.model.BoaviztaServerImpactRequest;
import tech.illuin.wombat.module.boavizta.connector.model.BoaviztaServerProvider;

import java.util.Set;

public interface BoaviztaClient
{
    @RequestLine("POST /cloud/instance?verbose={verbose}&duration={duration}&criteria={criteria}")
    @Headers("Content-Type: application/json")
    BoaviztaImpactResponse getInstanceImpact(
        @Param("verbose") boolean verbose,
        @Param("duration") int duration,
        @Param("criteria") Set<String> criteria,
        BoaviztaCloudInstanceImpactRequest request
    );

    @RequestLine("POST /server/?verbose={verbose}&duration={duration}&criteria={criteria}")
    @Headers("Content-Type: application/json")
    BoaviztaImpactResponse getServerImpact(
        @Param("verbose") boolean verbose,
        @Param("duration") int duration,
        @Param("criteria") Set<String> criteria,
        BoaviztaServerImpactRequest request
    );

    @RequestLine("GET /cloud/instance/instance_config?provider={provider}&instance_type={instanceType}")
    BoaviztaInstanceConfigResponse getInstanceConfig(
        @Param("provider") BoaviztaServerProvider provider,
        @Param("instanceType") String instanceType
    );
}
