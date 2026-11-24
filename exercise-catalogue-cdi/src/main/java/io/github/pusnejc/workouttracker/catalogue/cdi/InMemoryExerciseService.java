package io.github.pusnejc.workouttracker.catalogue.cdi;

import io.github.pusnejc.workouttracker.catalogue.cdi.exceptions.ExerciseNotFoundException;
import io.github.pusnejc.workouttracker.catalogue.cdi.model.Exercise;
import io.github.pusnejc.workouttracker.catalogue.cdi.model.ExercisePage;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped 
class InMemoryExerciseService implements ExerciseService {

    private final Map<UUID, Exercise> exercisesById = Stream.of(
            exercise("65dcd12e-08f0-4b53-b757-5b89a13816df", "Barbell Bench Press"),
            exercise("2a73c8c3-525d-4e01-af2c-1a3b7e182961", "Dumbbell Bench Press"),
            exercise("346f8b36-650b-4fc3-8c85-a89984773ca5", "Barbell Romanian Deadlift"),
            exercise("87d854e4-fc7e-4ea4-a9b6-295f88e193b0", "Hack Squat"))
        .collect(Collectors.toUnmodifiableMap(Exercise::id, e -> e));

    private static Exercise exercise(String id, String name) {
        return new Exercise(UUID.fromString(id), name);
    }

    @Override
    public Exercise getExerciseById(UUID id) {
        Exercise exercise = exercisesById.get(id);
        if (exercise == null) {
            throw new ExerciseNotFoundException(id);
        }
        return exercise;
    }

    @Override
    public ExercisePage getExercises(String nameFilter, int limit, int offset) {
        var filteredExercises = exercisesById.values().stream()
            .filter(exercise -> nameFilter == null || exercise.name().toLowerCase().contains(nameFilter.toLowerCase()))
            .sorted(Comparator.comparing(Exercise::name, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(exercise -> exercise.id().toString()))
            .toList();

        var paginatedExercises = filteredExercises.stream()
            .skip(offset)
            .limit(limit)
            .toList();

        return new ExercisePage(
            paginatedExercises,
            limit,
            offset,
            filteredExercises.size()
        );
    }
}
