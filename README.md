# Workout Planner and Log

## Purpose

The application helps a gym user create reusable workout routines, record performed sets during a workout, and review completed workouts. It has two backend microservices: Training Plans and Workout Sessions.

## Author and ownership

- Author: **pusnejc**
- The author owns both services, their tests, documentation, and deployments.

## MVP

1. Sign in and access only your own routines and workouts.
2. Browse a predefined text-only exercise catalogue.
3. Create a named routine, add/remove exercises, and specify target sets,
   minimum/maximum repetitions, and target repetitions in reserve (RIR).
4. Browse, edit, and delete routines.
5. Start a workout from a routine. Copy the routine name, exercises, order,
   and prescriptions into a session snapshot.
6. Save actual repetitions, weight in kilograms, and optional actual RIR per
   performed set. Save progress during the workout and support resuming it.
7. Finish or discard an active workout. Show completed workouts in history.
8. Display a routine's last-completed time.

One target prescription applies to all sets of an exercise in a routine. Exercise order is stored; an interactive reorder feature is optional.Workout-level notes are optional.

## Service ownership

| Service | Owns | Important rules |
| --- | --- | --- |
| Training Plans | Exercise catalogue, routines, ordered exercise prescriptions, derived last-completed summary | Only the owner edits a routine; minimum target reps cannot exceed maximum target reps; a routine must contain an exercise before starting a workout |
| Workout Sessions | Session snapshots, performed sets, session status, workout history | Only the owner accesses a workout; sets change only while active; finishing and set edits are coordinated; routine changes/deletion never rewrite history |

Each service owns a separate PostgreSQL database.

## Out of scope

AI coaching, automatic progression, nutrition, social features, wearables,
photos/videos, payments, timed/distance-based exercise models, and advanced
analytics.

## Demonstration scenario

Create a Push routine with bench press (3 sets, 8–12 reps, target 2 RIR).
Start a workout and record actual sets (for example 60 kg for 10, 9, and 8 reps).
Reload and resume to demonstrate saved progress. Finish and inspect history.
Edit or delete the original routine and show that the completed workout retains
its original snapshot and results. If the routine still exists, show its
last-completed summary updating through the message flow.

## Architecture documents

- [ADR-001: Service boundaries](docs/adr/ADR-001-service-boundaries.md)
- [C4 context and container views](docs/c4.md)
