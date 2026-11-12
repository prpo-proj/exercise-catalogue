# ADR-001: Separate the exercise catalogue from workout sessions

**Status:** Accepted
**Date**: 2026-11-12

## Context

Lifters choose from a shared list of exercises, but each lifter records their own workouts. An exercise such as bench press is defined once and can appear in many workouts. The weight and reps a lifter records belong to a particular set in a particular workout.

## Decision

Exercise Catalogue owns one predefined list shared by all lifters. Workout Sessions owns each lifter's workouts, the exercises added to each workout, and the sets logged for them. 

When a lifter selects an exercise, Workout Sessions looks it up in Exercise Catalogue and adds it to the workout. The entry stores the exercise’s catalogue ID and name. Workout Sessions then handles set logging and workout completion using its own data.

## Consequences

- Adding an exercise requires the catalogue to be available.
- Once the exercise is in a workout, the lifter can keep logging sets even if the catalogue goes offline.
- Workout history shows the exercise name as it was when the lifter added it.

## Alternatives

- Separate workout logging from workout history. We chose to keep them
  together because history is the collection of workouts the lifter has
  completed. The same service can save a workout and retrieve it later.

- Store only the catalogue exercise ID in a workout and look up the name
  when displaying it. This would avoid copying the name, but viewing
  workout history would depend on the catalogue being available. We chose
  to save the name when the lifter adds the exercise.
