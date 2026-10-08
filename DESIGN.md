# Design note

## Decisions

1. **Offline-first, with Room as the source of truth (architecture and data model).** Every save writes to Room and returns at once, so the UI never waits on the network and nothing is lost offline. A `synced` flag marks pending rows, and `SyncWorker` (WorkManager, network constraint, exponential backoff) pushes them in dependency order: patients, then vitals, then assessments. Data model: a patient (the unique Patient ID is the primary key) has many vitals rows and many assessment rows; each assessment row carries its form type (A or B), and synced rows store the server's id.
2. **A pure domain layer.** `BmiCalculator`, the validators and the models have no Android dependencies and are unit tested, including the boundaries (18.49/18.5 and 24.99/25.0). View models only orchestrate: validate, save, emit a one-shot navigation event (BMI below 25 opens form A, otherwise form B).
3. **Business rules enforced twice.** Unique Patient ID, one vitals row per patient per date and one assessment per type per date are checked in the repository (for friendly errors) and backed by Room unique indices, so a race cannot create duplicates.

Stack: Compose with type-safe Navigation, Room, Retrofit with kotlinx.serialization, DataStore for the token, manual DI. Chosen to keep the dependency graph small and explainable.

## KMP

Could move: `domain` (models, `BmiCalculator`, validators, repository interfaces) and the view models once `java.time` becomes `kotlinx-datetime`. Could move with a library swap: networking (Ktor) and storage (Room KMP). Could not: the Compose UI, WorkManager sync, DataStore wiring and `AppContainer`.

## Left out or simplified, and why

- **Server ids are found by listing.** The Postman collection shows no patient id in the `patients/register` response, yet vitals and visits link by the server's ids. So sync reads `patients/view`, matches on Patient ID and stores the server id. Based on the collection; not yet confirmed against the live API.
- **Each visit form sends only its own question** (`on_diet` for A, `on_drugs` for B), because that is all the form asks. A backend requiring both would need a default.
- **Plain email/password auth**, because the API needs a token. The token is unencrypted and never refreshed; a 401 returns the user to sign-in. Kept simple to stay in scope.
- **Sync is push-only.** The brief asks only for submission, so there is no pull, conflict handling or `visits/view` use.
- **No edit, delete or pagination.** Not required by the brief, and the expected data is small.