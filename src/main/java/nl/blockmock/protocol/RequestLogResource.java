package nl.blockmock.protocol;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import nl.blockmock.domain.ProtocolType;
import nl.blockmock.domain.RequestLog;
import nl.blockmock.service.RequestLogService;

import java.util.List;

@Path("/api/logs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RequestLogResource {

    @Inject
    RequestLogService requestLogService;

    @GET
    public List<RequestLog> list() {
        return requestLogService.findAll();
    }

    @GET
    @Path("/recent")
    public List<RequestLog> recent(@QueryParam("limit") @DefaultValue("100") int limit) {
        return requestLogService.findRecent(limit);
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") Long id) {
        return requestLogService.findById(id)
                .map(log -> Response.ok(log).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/endpoint/{endpointId}")
    public List<RequestLog> byEndpoint(@PathParam("endpointId") Long endpointId) {
        return requestLogService.findByEndpoint(endpointId);
    }

    @GET
    @Path("/protocol/{protocol}")
    public List<RequestLog> byProtocol(@PathParam("protocol") ProtocolType protocol) {
        return requestLogService.findByProtocol(protocol);
    }

    @GET
    @Path("/matched/{matched}")
    public List<RequestLog> byMatched(@PathParam("matched") boolean matched) {
        return requestLogService.findMatched(matched);
    }

    @GET
    @Path("/stats")
    @Produces(MediaType.APPLICATION_JSON)
    public Response stats() {
        return Response.ok(new Stats(
                requestLogService.countMatched(),
                requestLogService.countUnmatched()
        )).build();
    }

    @DELETE
    public Response deleteAll() {
        requestLogService.deleteAll();
        return Response.noContent().build();
    }

    public record Stats(long matched, long unmatched) {}
}
