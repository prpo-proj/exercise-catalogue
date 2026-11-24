package io.github.pusnejc.workouttracker.catalogue.rest;

import java.util.List;
import java.util.UUID;

import io.github.pusnejc.workouttracker.catalogue.rest.model.Exercise;
import io.github.pusnejc.workouttracker.catalogue.rest.model.ExercisePage;
import io.github.pusnejc.workouttracker.catalogue.cdi.ExerciseService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@ApplicationScoped 
public class ExercisesResource implements ExercisesApi {

    private final ExerciseService exerciseService;

    @Inject 
    public ExercisesResource(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    @Override
    public Exercise getExercise(UUID catalogueExerciseId) {
        return toDto(exerciseService.getExerciseById(catalogueExerciseId));
    }

    @Override
    public ExercisePage getExercises(String name, Integer limit, Integer offset) {
        return toDto(exerciseService.getExercises(name, limit, offset));
    }

    private static Exercise toDto(io.github.pusnejc.workouttracker.catalogue.cdi.model.Exercise exercise) {
        return new Exercise()
            .id(exercise.id())
            .name(exercise.name());
    }

    private static ExercisePage toDto(io.github.pusnejc.workouttracker.catalogue.cdi.model.ExercisePage exercisePage) {
        List<Exercise> exercises = exercisePage.exercises().stream()
            .map(ExercisesResource::toDto)
            .toList();

        return new ExercisePage()
            .items(exercises)
            .limit(exercisePage.limit())
            .offset(exercisePage.offset())
            .total(exercisePage.total());
    }
}
