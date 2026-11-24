package io.github.pusnejc.workouttracker.catalogue.cdi.exceptions;

import java.util.UUID;

public class ExerciseNotFoundException extends RuntimeException {
    public ExerciseNotFoundException(UUID id) {
        super("No exercise found with ID " + id);
    }
}
