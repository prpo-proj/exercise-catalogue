package io.github.pusnejc.workouttracker.catalogue.cdi;

import java.util.UUID;

import io.github.pusnejc.workouttracker.catalogue.cdi.model.Exercise;
import io.github.pusnejc.workouttracker.catalogue.cdi.model.ExercisePage;

public interface ExerciseService {
    Exercise getExerciseById(UUID id);
    ExercisePage getExercises(String nameFilter, int limit, int offset);
}
