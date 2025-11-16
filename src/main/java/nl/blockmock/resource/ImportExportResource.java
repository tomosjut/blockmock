package nl.blockmock.resource;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import nl.blockmock.domain.MockEndpoint;
import nl.blockmock.service.MockEndpointService;
import org.jboss.logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Path("/api/import-export")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ImportExportResource {

    private static final Logger LOG = Logger.getLogger(ImportExportResource.class);

    @Inject
    MockEndpointService mockEndpointService;

    @GET
    @Path("/export")
    @Produces(MediaType.APPLICATION_JSON)
    public Response exportAll() {
        try {
            List<MockEndpoint> endpoints = mockEndpointService.findAll();
            return Response.ok(endpoints)
                    .header("Content-Disposition", "attachment; filename=\"blockmock-export.json\"")
                    .build();
        } catch (Exception e) {
            LOG.error("Error exporting endpoints", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error exporting endpoints: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/export/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response exportSingle(@PathParam("id") Long id) {
        try {
            MockEndpoint endpoint = mockEndpointService.findById(id)
                    .orElse(null);
            if (endpoint == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return Response.ok(endpoint)
                    .header("Content-Disposition", "attachment; filename=\"endpoint-" + id + ".json\"")
                    .build();
        } catch (Exception e) {
            LOG.error("Error exporting endpoint", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error exporting endpoint: " + e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("/import")
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    public Response importEndpoints(List<MockEndpoint> endpoints) {
        try {
            int imported = 0;
            for (MockEndpoint endpoint : endpoints) {
                // Remove ID to create new endpoints
                endpoint.id = null;

                // Clear IDs from nested entities
                if (endpoint.getHttpConfig() != null) {
                    endpoint.getHttpConfig().id = null;
                }
                if (endpoint.getSftpConfig() != null) {
                    endpoint.getSftpConfig().id = null;
                }
                if (endpoint.getAmqpConfig() != null) {
                    endpoint.getAmqpConfig().id = null;
                }
                if (endpoint.getSqlConfig() != null) {
                    endpoint.getSqlConfig().id = null;
                    if (endpoint.getSqlConfig().getQueryMocks() != null) {
                        endpoint.getSqlConfig().getQueryMocks().forEach(qm -> qm.id = null);
                    }
                }
                if (endpoint.getResponses() != null) {
                    endpoint.getResponses().forEach(r -> r.id = null);
                }

                mockEndpointService.create(endpoint);
                imported++;
            }

            LOG.info("Successfully imported " + imported + " endpoints");
            return Response.ok("{\"imported\": " + imported + "}").build();
        } catch (Exception e) {
            LOG.error("Error importing endpoints", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error importing endpoints: " + e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("/import-single")
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    public Response importSingleEndpoint(MockEndpoint endpoint) {
        try {
            // Remove ID to create new endpoint
            endpoint.id = null;

            // Clear IDs from nested entities
            if (endpoint.getHttpConfig() != null) {
                endpoint.getHttpConfig().id = null;
            }
            if (endpoint.getSftpConfig() != null) {
                endpoint.getSftpConfig().id = null;
            }
            if (endpoint.getAmqpConfig() != null) {
                endpoint.getAmqpConfig().id = null;
            }
            if (endpoint.getSqlConfig() != null) {
                endpoint.getSqlConfig().id = null;
                if (endpoint.getSqlConfig().getQueryMocks() != null) {
                    endpoint.getSqlConfig().getQueryMocks().forEach(qm -> qm.id = null);
                }
            }
            if (endpoint.getResponses() != null) {
                endpoint.getResponses().forEach(r -> r.id = null);
            }

            MockEndpoint created = mockEndpointService.create(endpoint);
            LOG.info("Successfully imported endpoint: " + created.getName());
            return Response.ok(created).build();
        } catch (Exception e) {
            LOG.error("Error importing endpoint", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error importing endpoint: " + e.getMessage())
                    .build();
        }
    }
}
