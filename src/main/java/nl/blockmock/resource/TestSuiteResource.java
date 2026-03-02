package nl.blockmock.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import nl.blockmock.domain.TestRun;
import nl.blockmock.domain.TestSuite;
import nl.blockmock.service.TestSuiteService;
import org.jboss.logging.Logger;

import java.util.List;

@Path("/api/test-suites")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TestSuiteResource {

    private static final Logger LOG = Logger.getLogger(TestSuiteResource.class);

    @Inject
    TestSuiteService testSuiteService;

    @GET
    public List<TestSuite> getAllSuites() {
        return testSuiteService.findAll();
    }

    @GET
    @Path("/{id}")
    public TestSuite getSuite(@PathParam("id") Long id) {
        TestSuite suite = testSuiteService.findById(id);
        if (suite == null) {
            throw new NotFoundException("TestSuite not found");
        }
        return suite;
    }

    @POST
    public Response createSuite(TestSuite suite) {
        TestSuite created = testSuiteService.create(suite);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public TestSuite updateSuite(@PathParam("id") Long id, TestSuite suite) {
        return testSuiteService.update(id, suite);
    }

    @DELETE
    @Path("/{id}")
    public Response deleteSuite(@PathParam("id") Long id) {
        testSuiteService.delete(id);
        return Response.noContent().build();
    }

    // --- Run endpoints ---

    @POST
    @Path("/{id}/runs")
    @Consumes(MediaType.WILDCARD)
    public Response startRun(@PathParam("id") Long id) {
        TestSuite suite = testSuiteService.findById(id);
        if (suite == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\": \"TestSuite not found\"}")
                .build();
        }
        try {
            TestRun run = testSuiteService.startRun(id);
            return Response.status(Response.Status.CREATED).entity(run).build();
        } catch (Exception e) {
            LOG.error("Error starting test run", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }

    @GET
    @Path("/{id}/runs")
    public List<TestRun> getRuns(@PathParam("id") Long id) {
        return testSuiteService.findRunsForSuite(id);
    }

    @GET
    @Path("/{id}/runs/{runId}")
    public TestRun getRun(@PathParam("id") Long id, @PathParam("runId") Long runId) {
        TestRun run = testSuiteService.findRun(id, runId);
        if (run == null) {
            throw new NotFoundException("TestRun not found");
        }
        return run;
    }

    @POST
    @Path("/{id}/runs/{runId}/complete")
    @Consumes(MediaType.WILDCARD)
    public Response completeRun(@PathParam("id") Long id, @PathParam("runId") Long runId) {
        try {
            TestRun run = testSuiteService.completeRun(id, runId);
            return Response.ok(run).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.CONFLICT)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        } catch (Exception e) {
            LOG.error("Error completing test run", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }

    @DELETE
    @Path("/{id}/runs/{runId}")
    public Response cancelRun(@PathParam("id") Long id, @PathParam("runId") Long runId) {
        try {
            testSuiteService.cancelRun(id, runId);
            return Response.noContent().build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }

    @GET
    @Path("/{id}/runs/{runId}/junit")
    @Produces(MediaType.APPLICATION_XML)
    public Response getJUnitXml(@PathParam("id") Long id, @PathParam("runId") Long runId) {
        TestRun run = testSuiteService.findRun(id, runId);
        if (run == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("<error>TestRun not found</error>")
                .build();
        }
        String xml = testSuiteService.generateJUnitXml(runId);
        return Response.ok(xml, MediaType.APPLICATION_XML)
            .header("Content-Disposition", "attachment; filename=\"test-results-" + runId + ".xml\"")
            .build();
    }
}
