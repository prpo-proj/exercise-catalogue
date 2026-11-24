package io.github.pusnejc.workouttracker.catalogue.rest.problems;

import java.util.List;

import io.github.pusnejc.workouttracker.catalogue.rest.model.Problem;
import io.github.pusnejc.workouttracker.catalogue.rest.model.ProblemErrorsInner;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider 
public class ConstraintViolationMapper implements ExceptionMapper<ConstraintViolationException> {
    @Override
    public Response toResponse(ConstraintViolationException exception) {
        Response.Status status = Response.Status.BAD_REQUEST;

        List<ProblemErrorsInner> errors = exception.getConstraintViolations().stream()
            .map(violation -> new ProblemErrorsInner()
                .field(fieldName(violation))
                .message(violation.getMessage()))
            .toList();

        Problem problem = new Problem()
            .type(Problem.TypeEnum.ABOUT_BLANK)
            .title(status.getReasonPhrase())
            .status(status.getStatusCode())
            .detail("One or more query parameters are invalid.")
            .errors(errors)
            .code("INVALID_QUERY_PARAMETERS");

        return Response.status(status)
            .type(ProblemMediaType.APPLICATION_PROBLEM_JSON)
            .entity(problem)
            .build();
    }

    private static String fieldName(jakarta.validation.ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int separator = path.lastIndexOf('.');
        return separator >= 0 ? path.substring(separator + 1) : path;
    }
}
