package tech.illuin.wombat.handler;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import tech.illuin.wombat.environment.InvalidEnvironmentException;

@Provider
public class InvalidEnvironmentExceptionMapper implements ExceptionMapper<InvalidEnvironmentException>
{
    @Override
    public Response toResponse(InvalidEnvironmentException exception)
    {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(exception.getMessage())
            .build();
    }
}
