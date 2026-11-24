package io.github.pusnejc.workouttracker.catalogue.rest;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;

import io.github.pusnejc.workouttracker.catalogue.rest.problems.InvalidQueryParameterException;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.MatrixParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

/**
 * Quarkus passes the annotations of the implementing method (ExercisesResource), not of the
 * generated interface, so @QueryParam is usually not visible here. We therefore handle every
 * Integer parameter except those explicitly marked as another parameter kind (path, header, ...).
 */
@Provider
public class IntegerQueryParamConverterProvider implements ParamConverterProvider {

    @Override
    @SuppressWarnings("unchecked")
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (rawType != Integer.class || isNonQueryParameter(annotations)) {
            return null; // JAX-RS default handling
        }

        return (ParamConverter<T>) new ParamConverter<Integer>() {
            @Override
            public Integer fromString(String value) {
                if (value == null) {
                    return null;
                }
                try {
                    return Integer.valueOf(value);
                } catch (NumberFormatException e) {
                    throw new InvalidQueryParameterException(value);
                }
            }

            @Override
            public String toString(Integer value) {
                return value == null ? null : value.toString();
            }
        };
    }

    private static boolean isNonQueryParameter(Annotation[] annotations) {
        return Arrays.stream(annotations).anyMatch(annotation ->
            annotation instanceof PathParam
                || annotation instanceof HeaderParam
                || annotation instanceof CookieParam
                || annotation instanceof MatrixParam
                || annotation instanceof FormParam);
    }
}
