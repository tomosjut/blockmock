package nl.blockmock.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import nl.blockmock.domain.Scenario;
import nl.blockmock.service.ScenarioService;
import org.jboss.logging.Logger;

import java.util.List;

@Path("/api/scenarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ScenarioResource {

    private static final Logger LOG = Logger.getLogger(ScenarioResource.class);

    @Inject
    ScenarioService scenarioService;

    @GET
    public List<Scenario> getAllScenarios() {
        return scenarioService.findAll();
    }

    @GET
    @Path("/{id}")
    public Scenario getScenario(@PathParam("id") Long id) {
        Scenario scenario = scenarioService.findById(id);
        if (scenario == null) {
            throw new NotFoundException("Scenario not found");
        }
        return scenario;
    }

    @POST
    public Response createScenario(Scenario scenario) {
        Scenario created = scenarioService.create(scenario);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public Scenario updateScenario(@PathParam("id") Long id, Scenario scenario) {
        return scenarioService.update(id, scenario);
    }

    @DELETE
    @Path("/{id}")
    public Response deleteScenario(@PathParam("id") Long id) {
        scenarioService.delete(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/execute")
    @Consumes(MediaType.WILDCARD)
    public Response executeScenario(@PathParam("id") Long id) {
        Scenario scenario = scenarioService.findById(id);
        if (scenario == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Scenario not found\"}")
                    .build();
        }

        try {
            scenarioService.executeScenario(id);
            return Response.ok("{\"status\": \"Scenario executed successfully\"}").build();
        } catch (Exception e) {
            LOG.error("Error executing scenario", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }
}
