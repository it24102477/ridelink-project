# RideLink — Backend Microservices (IT3130 Group Assignment)

Four independently deployable Spring Boot + MongoDB microservices for a fictional
ride-sharing platform. No frontend is included by design (see assignment brief,
section 6.5) — use Swagger UI or the Postman collection in `/postman`.

## Group / ownership

| # | Service | Primary owner | Port |
|---|---------|---------------|------|
| 1 | Account Service | IT24102477 | 8081 |
| 2 | Driver & Vehicle Service | IT24102666 | 8082 |
| 3 | Ride Management Service | IT24102711 | 8083 |
| 4 | Fare & Payment Service | IT24102651 | 8084 |

> Fill in real names before submission — the brief requires this to be recorded
> in both the report and this README (section 7).

## Architecture at a glance

- Each service owns its own MongoDB database (`ridelink_account`, `ridelink_driver`,
  `ridelink_ride`, `ridelink_fare`) — no service queries another's collections directly.
- **Authentication**: account-service issues a JWT (HMAC-SHA256) on login. All four
  services validate that JWT independently using a shared signing secret
  (`JWT_SECRET`), so a request only needs to hit account-service once, at login.
- **Authorization**: role-based checks (`hasRole("DRIVER")`, `hasRole("ADMIN")`, etc.)
  gate each endpoint, and each service additionally enforces *self-service
  ownership* in the service layer — e.g. a driver token can only create/update
  its own driver profile, a passenger can only request rides or view payments
  under their own account id, and only the ride's actual assigned driver (not
  just "any driver") can accept/start/complete/cancel it. A mismatch throws
  `AccessDeniedException` → `403 Forbidden` via each service's
  `GlobalExceptionHandler`.
- **Interservice communication**: synchronous REST (`RestTemplate`), because ride
  creation and completion are on the passenger/driver-facing request path and need
  an immediate response (see `docs/ARCHITECTURE.md` for the full comparison against
  asynchronous messaging, as required by the report).
  - ride-management-service → driver-vehicle-service: `GET /api/drivers/available`
  - ride-management-service → fare-payment-service: `POST /api/fares/estimate`,
    `POST /api/fares/final`, `POST /api/payments`

See `docs/ARCHITECTURE.md` for the sequence diagram and the monolith-vs-microservices
comparison required by the brief.

## Prerequisites

- Java 17+
- Maven 3.9+
- MongoDB 6/7 running locally on `27017` (or use `docker compose up mongodb`)
- (Optional) Docker, if you'd rather run everything in containers

## Configuration

Each service reads its config from environment variables with sane local defaults
(see each service's `src/main/resources/application.yml`). Copy `.env.example` to
`.env` and adjust if needed — **never commit a real `.env` file** (see `.gitignore`).

Key variables:

```
MONGODB_URI          # per-service Mongo connection string
JWT_SECRET            # shared HMAC secret — MUST be identical across all four services
DRIVER_SERVICE_URL    # ride-management-service only
FARE_SERVICE_URL      # ride-management-service only
```

## Running locally (no Docker)

Build the parent POM once (installs shared version info to your local `.m2`):

```bash
mvn -N install
```

Then, in **four separate terminals**, start the services **in this order** (driver
and fare services first, since ride-management calls them):

```bash
cd driver-vehicle-service && mvn spring-boot:run
cd fare-payment-service    && mvn spring-boot:run
cd account-service         && mvn spring-boot:run
cd ride-management-service && mvn spring-boot:run
```

Or use the helper script:

```bash
./scripts/start-all.sh
```

## Running with Docker Compose

```bash
docker compose up --build
```

## Swagger / OpenAPI

Each service exposes Swagger UI once running:

- http://localhost:8081/swagger-ui/index.html (Account)
- http://localhost:8082/swagger-ui/index.html (Driver & Vehicle)
- http://localhost:8083/swagger-ui/index.html (Ride Management)
- http://localhost:8084/swagger-ui/index.html (Fare & Payment)

## Testing

Run unit tests per service:

```bash
cd account-service && mvn test
```

Or all four, from the repo root:

```bash
for d in account-service driver-vehicle-service ride-management-service fare-payment-service; do
  (cd "$d" && mvn -q test)
done
```

Import `/postman/RideLink.postman_collection.json` and
`/postman/RideLink-Local.postman_environment.json` into Postman for an integrated,
repeatable end-to-end run covering the full ride lifecycle plus negative
scenarios (no available driver, invalid status transition, unauthorised access,
failed simulated payment, non-admin trying an admin action).

### Running folders separately in Postman

The collection is split into six numbered folders — Accounts, Driver
Preparation, Fare Estimate, Ride Lifecycle, Negative Scenarios, Admin — so you
can test each concern in isolation instead of always running the whole thing:

- **Run one folder**: right-click the folder in the sidebar → **Run folder**.
  This opens the Collection Runner scoped to just that folder's requests, in
  order.
- **Run the whole collection**: right-click the collection root → **Run
  collection**, or use the ▶ Run button at the top. Folders execute
  top-to-bottom (1 → 6), which matters because later folders depend on
  variables set by earlier ones (e.g. `{{passengerToken}}`, `{{rideId}}`).
- **Run requests manually one at a time**: just click into a request and hit
  **Send** — useful while you're still developing a service and want to check
  one endpoint without re-running everything before it.

**Folder 6 (Admin) needs a one-time manual step first** — there's no public
"register as admin" endpoint by design, so you must seed an admin user directly
into MongoDB before that folder will work. See `docs/ADMIN_SETUP.md` for the
ready-to-paste script.

## Sample test data (non-sensitive, for demo only)

| Role | Email | Password |
|------|-------|----------|
| Passenger | nimal.passenger@example.com | password123 |
| Driver | kasun.driver@example.com | password123 |

## What's implemented vs. what your group still needs to add

This scaffold gives you all four services with their minimum required functionality
(section 4 of the brief), the full ride lifecycle state machine, two documented
interservice REST interactions, JWT auth with role-based authorisation, a
documented fare rule, a documented simulated-payment failure rule, unit tests per
service, a CI pipeline, and a Postman collection with negative scenarios.

Still to do as a group (not something any AI tool should complete for you, since
these are exactly what the individual marks and viva assess):

- [ ] Fill in real member names/ownership above and in the report
- [ ] Architecture diagram + sequence diagram as polished images for the report
      (a Mermaid starting point is in `docs/ARCHITECTURE.md`)
- [ ] Technical report (8–12 pages): rationale, comparisons, limitations,
      individual contribution statement
- [ ] Expand unit test coverage (edge cases, boundary values) per service
- [ ] Decide and document your Git branching workflow, then actually use it —
      feature branches, PRs, reviews
- [ ] Tighten interservice auth: `/api/drivers/available`, `/api/fares/estimate`
      and `/api/fares/final` are currently `permitAll()` for simplicity so the
      ride flow works out of the box; decide as a group whether to add a
      service-to-service API key/shared secret and document the decision
- [ ] Decide whether any interaction should use asynchronous messaging instead of
      REST, implement it, and add it to the comparison section of the report
- [ ] Rehearse the demo and viva — every member must be able to run and explain
      the *whole* system, not just their own service
- [ ] Declare any AI-tool use in an appendix per the academic integrity policy
      (section 12 of the brief)

## Real location data (free, no API key)

- **Distance / travel time** (fare-payment-service): OSRM over OpenStreetMap data. Falls back to Haversine if OSRM is unreachable. Set `OSRM_URL` to change server, `ROUTING_ENABLED=false` to turn off.
- **Address -> coordinates** (ride-management-service): Nominatim. Pickup/destination may now be `{ "address": "Negombo Beach" }` with no lat/lng. Results are cached. Set `NOMINATIM_URL`, `GEOCODING_COUNTRY_CODES` (default `lk`), `GEOCODING_USER_AGENT`.

The public servers are free but have fair-use limits (Nominatim ~1 request/second). For **no limit**, self-host OSRM and/or Nominatim (both open source, Docker images available) and point the env vars above at them.

### Address-only endpoints

| Service | Endpoint | Body |
|---|---|---|
| ride (8083) | `POST /api/{userId}/rides` (PASSENGER, `userId` must be the caller) | `{"pickup","destination"}` - the service area (district) is resolved from the pickup address automatically |
| ride (8083) | `POST /api/rides/estimate` | `{"pickup","destination"}` |
| fare (8084) | `POST /api/fares/estimate` | `{"pickup","destination"}` |

Pickup and destination are plain address strings. Coordinates, road distance and estimated fare are calculated automatically. `POST /api/fares/estimate-coordinates` is the server-to-server variant the ride service uses after geocoding.

## Role-based APIs and deletion rules

Every service has separate `AdminController` (ADMIN only), `DriverController` (DRIVER only),
`PassengerController` (PASSENGER only) and `UserController` (PASSENGER, DRIVER and ADMIN) classes, and
`SecurityConfig` enforces the same split. The `{userId}` in a URL must always be the caller's own id.

**Deletion rules** (a ride is "finished" when it is `COMPLETED` or `CANCELLED`; any other status blocks deletion with a `409`):

| Who | Call | Rule / message |
|---|---|---|
| ADMIN | `DELETE /api/admin/accounts/{id}` (8081) | PASSENGER or DRIVER accounts only; all of that user's rides finished, else `Can not delete his ride not completed`. A DRIVER's driver profile is deleted with the account |
| PASSENGER / DRIVER | `DELETE /api/accounts/me` (8081) | same rule, message `Can not delete your ride not completed`. A DRIVER's profile is deleted with the account |
| ADMIN | `DELETE /api/admin/drivers/{id}` (8082) | driver profile only; all of the driver's rides finished, else `Can not delete his ride not completed` |
| ADMIN | `DELETE /api/admin/rides/{id}` (8083) | ride must be `COMPLETED` or `CANCELLED`, else `Ride not completed...` |
| ADMIN | `DELETE /api/admin/payments/{id}` (8084) | admin only |

Account and driver-profile deletion ask ride-management-service `GET /api/rides/users/{userId}/open-rides`
(forwarding the caller's token). If that check can not be made, nothing is deleted (`502`).

**Ride service**: `GET /api/admin/rides` (all rides, admin only), `PUT /api/admin/rides/{id}/cancel`
(REQUESTED / ASSIGNED / ACCEPTED / IN_PROGRESS -> CANCELLED), `GET /api/{userId}/rides` (my rides, passenger),
`GET /api/rides/driver/{userId}` (my rides, driver). Accepting a ride sets the driver to `BUSY`
in driver-vehicle-service automatically.

**Payments** (8084): `GET /api/admin/payments`, `GET /api/passenger/{userId}/payments` and
`.../payments/ride/{rideId}`, `GET /api/driver/{userId}/payments`, `.../payments/{id}` and `.../payments/ride/{rideId}`,
`GET /api/payments/{id}` (+ `/receipt`) for any of the three roles (own payments only, ADMIN any).

**Driver location** (8082): `PUT /api/{userId}/drivers/{id}/location` now takes `{ "address": "..." }`;
latitude/longitude are resolved from it with the same free Nominatim geocoder as the other services.


## Automatic service area, and driver availability

- **Service area is no longer a request field.** `POST /api/{userId}/rides` takes only `pickup`
  and `destination` addresses. The pickup address is geocoded (Nominatim, `addressdetails=1`) and
  its district is matched against Sri Lanka's 25 districts (`com.ridelink.ride.validation.SriLankaDistricts`)
  to become the ride's `serviceArea`. If no district can be determined, ride creation fails with
  `422` and a validation message naming the pickup address.
- **Driver availability now updates automatically at both ends of a ride:** accepting a ride sets
  the driver to `BUSY` (existing behaviour); completing a ride (`PUT /api/rides/{id}/complete`,
  either called directly by the driver or via fare-payment-service's driver-pay flow) sets them
  back to `AVAILABLE`. Either call is best-effort - if driver-vehicle-service can't be reached the
  ride still completes/accepts, it just leaves the driver's own availability for them to fix by hand.
  Cancelling a ride does **not** currently reset availability.
- **Validation messages in ride-management-service and fare-payment-service** are now consistent
  with account-service and driver-vehicle-service: malformed JSON and invalid enum values (e.g. an
  unknown `paymentMethod`) return a `400` naming the field and, for enums, the allowed values,
  instead of a generic `"An unexpected error occurred"`. Ride-management-service's exception
  handler also no longer leaks a debug stack-trace fragment on unexpected errors.
