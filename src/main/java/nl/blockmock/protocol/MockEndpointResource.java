package nl.blockmock.protocol;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import nl.blockmock.domain.MockEndpoint;
import nl.blockmock.domain.MockResponse;
import nl.blockmock.domain.ProtocolType;
import nl.blockmock.service.MockEndpointService;

import java.util.List;

@Path("/api/endpoints")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MockEndpointResource {

    @Inject
    MockEndpointService mockEndpointService;

    @GET
    public List<MockEndpoint> list() {
        return mockEndpointService.findAll();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") Long id) {
        return mockEndpointService.findById(id)
                .map(endpoint -> Response.ok(endpoint).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/protocol/{protocol}")
    public List<MockEndpoint> listByProtocol(@PathParam("protocol") ProtocolType protocol) {
        return mockEndpointService.findByProtocol(protocol);
    }

    @POST
    public Response create(MockEndpoint endpoint) {
        MockEndpoint created = mockEndpointService.create(endpoint);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, MockEndpoint endpoint) {
        if (!mockEndpointService.findById(id).isPresent()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        endpoint.id = id;
        MockEndpoint updated = mockEndpointService.update(endpoint);
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        if (!mockEndpointService.findById(id).isPresent()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        mockEndpointService.delete(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/toggle")
    @Consumes(MediaType.WILDCARD)
    public Response toggle(@PathParam("id") Long id) {
        if (!mockEndpointService.findById(id).isPresent()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        mockEndpointService.toggleEnabled(id);
        return Response.ok().build();
    }

    @POST
    @Path("/{id}/responses")
    public Response addResponse(@PathParam("id") Long id, MockResponse response) {
        try {
            MockResponse created = mockEndpointService.addResponse(id, response);
            return Response.status(Response.Status.CREATED).entity(created).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @DELETE
    @Path("/responses/{responseId}")
    public Response deleteResponse(@PathParam("responseId") Long responseId) {
        mockEndpointService.deleteResponse(responseId);
        return Response.noContent().build();
    }
}
