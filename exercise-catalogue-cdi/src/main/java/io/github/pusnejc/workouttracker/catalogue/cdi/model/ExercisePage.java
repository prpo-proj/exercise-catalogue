package io.github.pusnejc.workouttracker.catalogue.cdi.model;

import java.util.List;

public record ExercisePage(
    List<Exercise> exercises,
    Integer limit,
    Integer offset,
    Integer total
) {
}
