package nl.blockmock.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import nl.blockmock.service.TestSuiteExportService;
import nl.blockmock.service.TestSuiteExportService.TestSuiteExport;
import nl.blockmock.service.TestSuiteExportService.ImportResult;
import org.jboss.logging.Logger;

@Path("/api/import-export")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ImportExportResource {

    private static final Logger LOG = Logger.getLogger(ImportExportResource.class);

    @Inject
    TestSuiteExportService exportService;

    @GET
    @Path("/suites/{id}")
    public Response exportSuite(@PathParam("id") Long id) {
        try {
            TestSuiteExport export = exportService.export(id);
            String filename = "suite-" + export.testSuite().name().replaceAll("[^a-zA-Z0-9_-]", "_") + ".json";
            return Response.ok(export)
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (Exception e) {
            LOG.error("Error exporting suite", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/suites")
    public Response importSuite(TestSuiteExport export) {
        try {
            ImportResult result = exportService.importSuite(export);
            return Response.ok(result).build();
        } catch (Exception e) {
            LOG.error("Error importing suite", e);
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
}
