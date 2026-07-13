package tech.illuin.wombat.handler;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import tech.illuin.wombat.asset.UnknownEnvironmentException;

@Provider
public class UnknownEnvironmentExceptionMapper implements ExceptionMapper<UnknownEnvironmentException>
{
    @Override
    public Response toResponse(UnknownEnvironmentException exception)
    {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(exception.getMessage())
            .build();
    }
}
