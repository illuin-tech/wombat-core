package tech.illuin.wombat.handler;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import tech.illuin.wombat.environment.EnvironmentAlreadyExistsException;

@Provider
public class EnvironmentAlreadyExistsExceptionMapper implements ExceptionMapper<EnvironmentAlreadyExistsException>
{
    @Override
    public Response toResponse(EnvironmentAlreadyExistsException exception)
    {
        return Response.status(Response.Status.CONFLICT)
            .entity(exception.getMessage())
            .build();
    }
}
