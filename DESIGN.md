# Design note

## Decisions

1. **Offline-first, Room as the source of truth.** Every save writes to Room first and returns immediately; each row carries a `synced` flag. `SyncWorker` (WorkManager, network constraint, exponential backoff) pushes pending rows in dependency order: patients, then vitals and visits. Failures leave rows unsynced and are retried, so the UI never blocks on the network and nothing is lost offline. A 409 from the server counts as delivered, so a retry after a lost response cannot loop forever.
2. **Pure domain layer with validation outside the UI.** `BmiCalculator`, the validators and the models have no Android dependencies and are unit tested, including boundary values (18.49/18.5, 24.99/25.0). View models only orchestrate: validate, call the repository, emit a one-shot event that drives navigation (BMI < 25 goes to form A, otherwise form B).
3. **Rules enforced twice.** Unique patient ID, one vitals row per patient per date and one assessment per type per date are checked in the repository for friendly errors and backed by Room unique indices, so a race cannot create duplicates.
4. **Session as state.** Navigation observes a logged-in flow derived from the stored token, so signing out or an expired token moves the user to sign-in from anywhere without extra wiring.
5. **Stack.** Compose + Navigation (type-safe routes), Room, Retrofit + kotlinx.serialization, DataStore for the token, manual DI via `AppContainer`. Chosen to keep the dependency graph small and every piece explainable.

## KMP readiness

Could move to a shared module as-is: `domain` (models, `BmiCalculator`, validators, repository interface), view models once `java.time` becomes `kotlinx-datetime`, and the DTOs/Retrofit-free API contract if Ktor replaces Retrofit.
Cannot move: Compose UI as written (needs Compose Multiplatform), WorkManager sync, DataStore wiring, `AppContainer`, and the Room setup unless using Room KMP.

## Left out or simplified

- **Server ids are resolved by listing.** The API links vitals and visits by its own numeric ids and `patients/register` does not return one, so sync reads `patients/view`, matches on the unique Patient ID and stores the server id locally. Visits are linked to the vitals row for the same date (or the nearest one), using the id returned by `vital/add`.
- **Visit forms send only the question they ask** (`on_diet` for form A, `on_drugs` for form B); the sample sends both, so a backend that requires both would need a default.
- **Authentication is a plain email/password flow** (sign up, sign in, log out). The Sanctum token is kept in DataStore unencrypted; a real app would use encrypted storage. There is no token refresh, so a 401 during sync clears the session and returns the user to the sign-in screen.
- **Sync is push-only.** No pull of server data, conflict resolution or `visits/view` usage; the local database is authoritative.
- **Patients are never edited or deleted**, and the listing shows each patient's most recent vitals, as the brief requires.
- **No pagination** on the listing; fine for the expected data size.
