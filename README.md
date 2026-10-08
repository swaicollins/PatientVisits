# Patient Visits

Android app (Kotlin, Jetpack Compose) for registering patients, recording vitals, completing one of two assessment forms based on BMI, and listing patients with a visit-date filter. Data is saved locally first and synced to the backend in the background.

See [DESIGN.md](DESIGN.md) for the design decisions and what was simplified.

## Flow

1. **Sign in / Create account**: email and password. Signing up logs the user in.
2. **Patient listing**: shows name, age and last BMI status, with a visit-date filter and a Log out button. Tapping a patient starts a new visit; the register button adds a patient.
3. **Registration**: Patient ID (must be unique), registration date, first and last name, date of birth, gender.
4. **Vitals**: visit date, height (cm), weight (kg), BMI calculated automatically. One submission per patient per date.
5. **Assessment**: BMI below 25 opens form A (general health, ever on a diet, comments); BMI 25 or above opens form B (general health, currently on drugs, comments). One submission per form type per date.
6. Saving an assessment returns to the listing.

BMI status: below 18.5 is Underweight, 18.5 up to but not including 25 is Normal, 25 or above is Overweight.

## Requirements

- Android Studio (latest stable) with JDK 11 or newer
- compileSdk 37, minSdk 26
- An emulator or device running Android 8.0 or newer

## Run

1. Open the project folder in Android Studio and let Gradle sync.
2. Run the `app` configuration on an emulator or device.
3. Create an account on the first screen, then register a patient.

The backend base URL is set in `data/remote/ApiConfig.kt`: `https://patientvisitapis.intellisoftkenya.com/api/`.

## Tests

Unit tests (JVM):

```
./gradlew test
```

| Area | File |
| --- | --- |
| BMI calculation and status boundaries | `BmiCalculatorTest` |
| Patient, vitals and assessment validation (mandatory fields, duplicate Patient ID, one submission per date) | `PatientValidatorTest`, `VitalsValidatorTest`, `AssessmentValidatorTest` |
| Sign-in and sign-up validation | `AuthValidatorTest` |
| Repository against a faked API and DAO (sync order, retries, server ids, listing) | `PatientRepositoryImplTest` |
| Session handling (login, signup, offline, server errors, logout) | `SessionManagerTest` |

Room DAO test (needs an emulator or device):

```
./gradlew connectedAndroidTest
```

`PatientDaoTest` runs against an in-memory database and checks the unique indices, the foreign keys and the sync flags.

## Backend API

| Purpose | Request |
| --- | --- |
| Sign up | `POST user/signup` (`email`, `firstname`, `lastname`, `password`) |
| Sign in | `POST user/signin` (`email`, `password`), token in `data.access_token` |
| Register patient | `POST patients/register` (`firstname`, `lastname`, `unique`, `dob`, `gender`, `reg_date`) |
| List patients | `GET patients/view` |
| Add vitals | `POST vital/add` (`visit_date`, `height`, `weight`, `bmi`, `patient_id`), returns the vital `id` |
| Add visit | `POST visits/add` (`general_health`, `on_diet` or `on_drugs`, `comments`, `visit_date`, `patient_id`, `vital_id`) |

Vitals and visits are linked with the server's own ids. The app finds a patient's server id by matching `unique` in `patients/view`, and links a visit to the `id` returned by `vital/add`.

## Sync

- Saving a form writes to Room and marks the row unsynced.
- A WorkManager job (network required, exponential backoff) pushes patients first, then vitals, then assessments.
- A failure leaves the row unsynced and the job retries. Patients already on the server are reused, not registered again.
- If the server rejects the token (HTTP 401), the session is cleared and the user returns to sign in.

## Project layout

```
app/src/main/java/com/example/patientvisits/
  domain/   models, BMI calculation, validators, repository interfaces
  data/     Room (local), Retrofit (remote), repository, session, sync
  ui/       Compose screens and view models, navigation
  di/       AppContainer (manual dependency injection)
```

## Known limitations

- The first sign-in needs the server; there is no offline login.
- The auth token is stored unencrypted in DataStore.
- Sync is push-only; server data is not pulled into the app.
- Visit forms send only the question they ask (`on_diet` or `on_drugs`).
- The app was written without access to the live API (the backend returned 503 during development), so the request and response shapes follow the Postman collection but have not been exercised end to end.
