package tech.illuin.wombat.handler;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import tech.illuin.wombat.environment.InvalidAssetException;

@Provider
public class InvalidAssetExceptionMapper implements ExceptionMapper<InvalidAssetException>
{
    @Override
    public Response toResponse(InvalidAssetException exception)
    {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(exception.getMessage())
            .build();
    }
}
