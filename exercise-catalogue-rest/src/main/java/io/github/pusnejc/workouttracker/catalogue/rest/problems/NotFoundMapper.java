package io.github.pusnejc.workouttracker.catalogue.rest.problems;

import io.github.pusnejc.workouttracker.catalogue.rest.model.Problem;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class NotFoundMapper implements ExceptionMapper<NotFoundException> {

    @Override
    public Response toResponse(NotFoundException exception) {
        Response.Status status = Response.Status.NOT_FOUND;

        Problem problem = new Problem()
            .type(Problem.TypeEnum.ABOUT_BLANK)
            .title(status.getReasonPhrase())
            .status(status.getStatusCode())
            .detail("The requested resource does not exist.")
            .code("RESOURCE_NOT_FOUND");

        return Response.status(status)
            .type(ProblemMediaType.APPLICATION_PROBLEM_JSON)
            .entity(problem)
            .build();
    }
}
