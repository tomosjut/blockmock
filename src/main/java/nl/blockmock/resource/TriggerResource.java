package nl.blockmock.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import nl.blockmock.domain.TriggerConfig;
import nl.blockmock.service.TriggerService;

import java.util.List;

@Path("/api/triggers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TriggerResource {

    @Inject
    TriggerService triggerService;

    @GET
    public List<TriggerConfig> list() {
        return triggerService.findAll();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") Long id) {
        TriggerConfig trigger = triggerService.findById(id);
        if (trigger == null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(trigger).build();
    }

    @POST
    public Response create(TriggerConfig trigger) {
        try {
            TriggerConfig created = triggerService.create(trigger);
            return Response.status(Response.Status.CREATED).entity(created).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage())).build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, TriggerConfig trigger) {
        try {
            TriggerConfig updated = triggerService.update(id, trigger);
            return Response.ok(updated).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage())).build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        triggerService.delete(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/fire")
    @Consumes(MediaType.WILDCARD)
    public Response fire(@PathParam("id") Long id) {
        try {
            TriggerService.TriggerFireResult result = triggerService.fire(id);
            return Response.ok(result).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage())).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse(e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse(e.getMessage())).build();
        }
    }

    public record ErrorResponse(String error) {}
}
