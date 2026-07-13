package tech.illuin.wombat.handler;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import tech.illuin.wombat.asset.NoAssetsMatchedException;

@Provider
public class NoAssetsMatchedExceptionMapper implements ExceptionMapper<NoAssetsMatchedException>
{
    @Override
    public Response toResponse(NoAssetsMatchedException exception)
    {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(exception.getMessage())
            .build();
    }
}
