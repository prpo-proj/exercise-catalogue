package io.github.pusnejc.workouttracker.catalogue.rest.problems;

import jakarta.ws.rs.BadRequestException;

public class InvalidQueryParameterException extends BadRequestException {

    private final String invalidValue;

    public InvalidQueryParameterException(String invalidValue) {
        super("Invalid query parameter value: " + invalidValue);
        this.invalidValue = invalidValue;
    }

    public String invalidValue() {
        return invalidValue;
    }
}
