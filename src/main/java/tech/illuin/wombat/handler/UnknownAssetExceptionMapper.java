package tech.illuin.wombat.handler;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import tech.illuin.wombat.environment.UnknownAssetException;

@Provider
public class UnknownAssetExceptionMapper implements ExceptionMapper<UnknownAssetException>
{
    @Override
    public Response toResponse(UnknownAssetException exception)
    {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(exception.getMessage())
            .build();
    }
}
