# Workout Tracker: Exercise Catalogue

Workout Tracker helps lifters log their strength training and review past workouts.

The Exercise Catalogue service provides the shared list of exercises. When logging a workout, a lifter can browse the catalogue or search by name to find an exercise such as bench press. The Workout Sessions service records the sets they perform.

## Planned MVP

- Browse a predefined exercise catalogue shared by all lifters.
- Search for exercises by name.
- View a selected exercise’s details.

## Build, run, and test

Requires Java 25. Maven is provided through the wrapper.

Build:

```powershell
.\mvnw.cmd clean verify
```

Run in development mode (the API is served at `http://localhost:8080/v1`):

```powershell
.\mvnw.cmd -pl exercise-catalogue-rest -am compile io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:dev
```

Automated tests will be added later.

## Related links

- [Project documentation](docs/)
- [Workout Sessions repository](https://github.com/prpo-proj/workout-sessions)
