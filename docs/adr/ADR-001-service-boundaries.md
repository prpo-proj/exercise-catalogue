# ADR-001: Separate training plans from workout execution

Status: Proposed

## Context

This project will implement two backend microservices. The MVP lets a
user define routines, perform workouts, and inspect history. A routine is a
reusable prescription; a workout is a record of what happened at a particular
time. Editing or deleting the prescription must not change workout history.

## Decision

Separate the backend into Training Plans and Workout Sessions. Training Plans
owns the catalogue and routine prescriptions. Workout Sessions owns session
snapshots, performed sets, lifecycle rules, and history. Each service has its own
API, logical database, build, and deployment, with no cross-service table access.

On start, Workout Sessions retrieves an authorised routine through REST and
stores a snapshot. Subsequent set logging and completion use local data.

## Consequences

- Routine changes do not alter historical results; an active workout can continue
  during a Training Plans outage.
- The services can evolve and deploy separately while preserving their contracts.
- Snapshots intentionally duplicate descriptive data.
- Two services increase testing, configuration, and deployment work compared
  with one backend.
