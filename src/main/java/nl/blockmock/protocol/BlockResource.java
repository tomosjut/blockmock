package nl.blockmock.protocol;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import nl.blockmock.domain.Block;
import nl.blockmock.domain.MockEndpoint;
import nl.blockmock.service.BlockService;

import java.util.List;
import java.util.Set;

@Path("/api/blocks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BlockResource {

    @Inject
    BlockService blockService;

    @GET
    public List<Block> list() {
        return blockService.findAll();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") Long id) {
        return blockService.findById(id)
                .map(block -> Response.ok(block).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    public Response create(Block block) {
        try {
            Block created = blockService.create(block);
            return Response.status(Response.Status.CREATED).entity(created).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, Block block) {
        if (!blockService.findById(id).isPresent()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        block.id = id;
        Block updated = blockService.update(block);
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        if (!blockService.findById(id).isPresent()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        blockService.delete(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}/endpoints")
    public Response getEndpoints(@PathParam("id") Long id) {
        try {
            Set<MockEndpoint> endpoints = blockService.getBlockEndpoints(id);
            return Response.ok(endpoints).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/{id}/endpoints/{endpointId}")
    public Response addEndpoint(@PathParam("id") Long id, @PathParam("endpointId") Long endpointId) {
        try {
            blockService.addEndpointToBlock(id, endpointId);
            return Response.ok().build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/{id}/endpoints/{endpointId}")
    public Response removeEndpoint(@PathParam("id") Long id, @PathParam("endpointId") Long endpointId) {
        try {
            blockService.removeEndpointFromBlock(id, endpointId);
            return Response.noContent().build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/{id}/start")
    public Response startBlock(@PathParam("id") Long id) {
        try {
            blockService.startBlock(id);
            return Response.ok(new SuccessResponse("Block started successfully")).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/{id}/stop")
    public Response stopBlock(@PathParam("id") Long id) {
        try {
            blockService.stopBlock(id);
            return Response.ok(new SuccessResponse("Block stopped successfully")).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/endpoint/{endpointId}")
    public List<Block> getBlocksForEndpoint(@PathParam("endpointId") Long endpointId) {
        return blockService.findBlocksForEndpoint(endpointId);
    }

    public record ErrorResponse(String error) {}
    public record SuccessResponse(String message) {}
}
