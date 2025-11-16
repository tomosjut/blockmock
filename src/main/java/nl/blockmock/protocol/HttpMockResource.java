package nl.blockmock.protocol;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import nl.blockmock.service.HttpMockService;

import java.util.HashMap;
import java.util.Map;

@Path("/mock/http")
public class HttpMockResource {

    @Inject
    HttpMockService httpMockService;

    @Context
    UriInfo uriInfo;

    @Context
    HttpHeaders httpHeaders;

    @GET
    @Path("/{path:.*}")
    @Produces(MediaType.WILDCARD)
    public Response handleGet(@PathParam("path") String path,
                             @Context Request request) {
        return handleRequest("GET", "/" + path, request);
    }

    @POST
    @Path("/{path:.*}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.WILDCARD)
    public Response handlePost(@PathParam("path") String path,
                              String body,
                              @Context Request request) {
        return handleRequestWithBody("POST", "/" + path, body, request);
    }

    @PUT
    @Path("/{path:.*}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.WILDCARD)
    public Response handlePut(@PathParam("path") String path,
                             String body,
                             @Context Request request) {
        return handleRequestWithBody("PUT", "/" + path, body, request);
    }

    @DELETE
    @Path("/{path:.*}")
    @Produces(MediaType.WILDCARD)
    public Response handleDelete(@PathParam("path") String path,
                                 @Context Request request) {
        return handleRequest("DELETE", "/" + path, request);
    }

    @PATCH
    @Path("/{path:.*}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.WILDCARD)
    public Response handlePatch(@PathParam("path") String path,
                               String body,
                               @Context Request request) {
        return handleRequestWithBody("PATCH", "/" + path, body, request);
    }

    private Response handleRequest(String method, String path, Request request) {
        return handleRequestWithBody(method, path, null, request);
    }

    private Response handleRequestWithBody(String method, String path, String body, Request request) {
        // Extract headers
        Map<String, String> headers = new HashMap<>();
        for (String headerName : httpHeaders.getRequestHeaders().keySet()) {
            headers.put(headerName.toLowerCase(),
                       httpHeaders.getHeaderString(headerName));
        }

        // Extract query parameters
        Map<String, String> queryParams = new HashMap<>();
        for (Map.Entry<String, java.util.List<String>> entry : uriInfo.getQueryParameters().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                queryParams.put(entry.getKey(), entry.getValue().get(0));
            }
        }

        // Get client IP (simplified - in production you'd check X-Forwarded-For)
        String clientIp = "unknown";

        HttpMockService.HttpMockResponse mockResponse =
            httpMockService.handleRequest(method, path, headers, queryParams, body, clientIp);

        // Apply delay if specified
        if (mockResponse.delayMs() > 0) {
            try {
                Thread.sleep(mockResponse.delayMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Build response
        Response.ResponseBuilder responseBuilder = Response.status(mockResponse.statusCode());

        // Add headers
        for (Map.Entry<String, String> header : mockResponse.headers().entrySet()) {
            responseBuilder.header(header.getKey(), header.getValue());
        }

        // Add body
        if (mockResponse.body() != null) {
            responseBuilder.entity(mockResponse.body());
        }

        return responseBuilder.build();
    }
}
