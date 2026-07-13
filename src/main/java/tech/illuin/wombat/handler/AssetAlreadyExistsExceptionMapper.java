package tech.illuin.wombat.handler;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import tech.illuin.wombat.environment.AssetAlreadyExistsException;

@Provider
public class AssetAlreadyExistsExceptionMapper implements ExceptionMapper<AssetAlreadyExistsException>
{
    @Override
    public Response toResponse(AssetAlreadyExistsException exception)
    {
        return Response.status(Response.Status.CONFLICT)
            .entity(exception.getMessage())
            .build();
    }
}
