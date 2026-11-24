package io.github.pusnejc.workouttracker.catalogue.rest.problems;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.pusnejc.workouttracker.catalogue.rest.model.Problem;
import io.github.pusnejc.workouttracker.catalogue.rest.model.ProblemErrorsInner;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InvalidQueryParameterMapper implements ExceptionMapper<InvalidQueryParameterException> {

    @Context
    UriInfo uriInfo;

    @Context
    ResourceInfo resourceInfo;

    @Override
    public Response toResponse(InvalidQueryParameterException exception) {
        Response.Status status = Response.Status.BAD_REQUEST;

        // names and values of the query parameters in the request, but we don't know which ones are supposed to be integers
        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();

        // among all Integer query params of the called method,
        // find the ones that are present in the request and have a value that is not a number
        List<ProblemErrorsInner> errors = integerQueryParameterNames().stream()
            .filter(name -> queryParameters.getOrDefault(name, List.of()).stream().anyMatch(value -> !isInteger(value)))
            .sorted()
            .map(name -> new ProblemErrorsInner()
                .field(name)
                .message("must be an integer"))
            .toList();

        Problem problem = new Problem()
            .type(Problem.TypeEnum.ABOUT_BLANK)
            .title(status.getReasonPhrase())
            .status(status.getStatusCode())
            .detail("One or more query parameters are invalid.")
            .code("INVALID_QUERY_PARAMETERS")
            .errors(errors);

        return Response.status(status)
            .type(ProblemMediaType.APPLICATION_PROBLEM_JSON)
            .entity(problem)
            .build();
    }

    private Set<String> integerQueryParameterNames() {
        Method method = resourceInfo == null ? null : resourceInfo.getResourceMethod();

        // can't really happen, but let's be defensive
        if (method == null) {
            return Set.of();
        }

        // we check both the params of the method in the implementing class and the method in the interface, 
        // because the @QueryParam annotations are on the interface method, but the implementing class method
        // can also have @QueryParam annotations, then make sure we only take distinct names at the end
        return Stream.concat(Stream.of(method), interfaceDeclarations(method))
            .flatMap(declaration -> Arrays.stream(declaration.getParameters())) // all parameters of the method
            .filter(parameter -> parameter.getType() == Integer.class)          // only Integer parameters
            .map(parameter -> parameter.getAnnotation(QueryParam.class))    // get the @QueryParam annotation
            .filter(annotation -> annotation != null)   // only parameters that have the annotation
            .map(QueryParam::value)     // get the name of the query parameter
            .collect(Collectors.toSet());   // distinct names of the query parameters
    }

    private static Stream<Method> interfaceDeclarations(Method method) {
        // among the interfaces of the declaring class,
        // find the method with the same name and parameter types (if it exists)
        return Arrays.stream(method.getDeclaringClass().getInterfaces())
            .flatMap(type -> {
                try {
                    return Stream.of(type.getMethod(method.getName(), method.getParameterTypes()));
                } catch (NoSuchMethodException e) {
                    return Stream.empty();
                }
            });
    }

    private static boolean isInteger(String value) {
        try {
            Integer.valueOf(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
