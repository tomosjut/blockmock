package nl.blockmock.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import nl.blockmock.domain.TestRun;
import nl.blockmock.domain.TestScenario;
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

    // --- Suite CRUD ---

    @GET
    public List<TestSuite> getAllSuites() {
        return testSuiteService.findAll();
    }

    @GET
    @Path("/{id}")
    public TestSuite getSuite(@PathParam("id") Long id) {
        TestSuite suite = testSuiteService.findById(id);
        if (suite == null) throw new NotFoundException("TestSuite not found");
        return suite;
    }

    @POST
    public Response createSuite(TestSuite suite) {
        return Response.status(Response.Status.CREATED).entity(testSuiteService.create(suite)).build();
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

    // --- Scenario CRUD ---

    @GET
    @Path("/{id}/scenarios")
    public List<TestScenario> getScenarios(@PathParam("id") Long id) {
        return testSuiteService.findScenarios(id);
    }

    @POST
    @Path("/{id}/scenarios")
    public Response createScenario(@PathParam("id") Long id, TestScenario scenario) {
        return Response.status(Response.Status.CREATED)
                .entity(testSuiteService.createScenario(id, scenario)).build();
    }

    @PUT
    @Path("/{id}/scenarios/{scenarioId}")
    public TestScenario updateScenario(@PathParam("id") Long id,
                                       @PathParam("scenarioId") Long scenarioId,
                                       TestScenario scenario) {
        return testSuiteService.updateScenario(id, scenarioId, scenario);
    }

    @DELETE
    @Path("/{id}/scenarios/{scenarioId}")
    public Response deleteScenario(@PathParam("id") Long id, @PathParam("scenarioId") Long scenarioId) {
        testSuiteService.deleteScenario(id, scenarioId);
        return Response.noContent().build();
    }

    // --- Runs ---

    @POST
    @Path("/{id}/scenarios/{scenarioId}/runs")
    @Consumes(MediaType.WILDCARD)
    public Response startRun(@PathParam("id") Long id, @PathParam("scenarioId") Long scenarioId) {
        try {
            TestRun run = testSuiteService.startRun(id, scenarioId);
            return Response.status(Response.Status.CREATED).entity(run).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (Exception e) {
            LOG.error("Error starting run", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/{id}/scenarios/{scenarioId}/runs")
    public List<TestRun> getRuns(@PathParam("id") Long id, @PathParam("scenarioId") Long scenarioId) {
        return testSuiteService.findRuns(id, scenarioId);
    }

    @POST
    @Path("/{id}/scenarios/{scenarioId}/runs/{runId}/complete")
    @Consumes(MediaType.WILDCARD)
    public Response completeRun(@PathParam("id") Long id,
                                @PathParam("scenarioId") Long scenarioId,
                                @PathParam("runId") Long runId) {
        try {
            return Response.ok(testSuiteService.completeRun(id, scenarioId, runId)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        } catch (Exception e) {
            LOG.error("Error completing run", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @DELETE
    @Path("/{id}/scenarios/{scenarioId}/runs")
    public Response clearRuns(@PathParam("id") Long id, @PathParam("scenarioId") Long scenarioId) {
        int count = testSuiteService.clearCompletedRuns(id, scenarioId);
        return Response.ok("{\"deleted\": " + count + "}").build();
    }

    @DELETE
    @Path("/{id}/scenarios/{scenarioId}/runs/{runId}")
    public Response cancelRun(@PathParam("id") Long id,
                              @PathParam("scenarioId") Long scenarioId,
                              @PathParam("runId") Long runId) {
        try {
            testSuiteService.cancelRun(id, scenarioId, runId);
            return Response.noContent().build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/{id}/scenarios/{scenarioId}/runs/{runId}/junit")
    @Produces(MediaType.APPLICATION_XML)
    public Response getJUnitXml(@PathParam("id") Long id,
                                @PathParam("scenarioId") Long scenarioId,
                                @PathParam("runId") Long runId) {
        TestRun run = testSuiteService.findRun(id, scenarioId, runId);
        if (run == null) return Response.status(Response.Status.NOT_FOUND).build();
        String xml = testSuiteService.generateJUnitXml(runId);
        return Response.ok(xml, MediaType.APPLICATION_XML)
                .header("Content-Disposition", "attachment; filename=\"test-results-" + runId + ".xml\"")
                .build();
    }
}
