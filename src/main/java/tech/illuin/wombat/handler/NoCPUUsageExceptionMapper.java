package tech.illuin.wombat.handler;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import tech.illuin.wombat.persistence.NoCPUUsageException;

@Provider
public class NoCPUUsageExceptionMapper implements ExceptionMapper<NoCPUUsageException>
{
    @Override
    public Response toResponse(NoCPUUsageException exception)
    {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity("No CPU Usage Could be found")
            .build();
    }
}
