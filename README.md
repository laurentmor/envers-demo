# envers-demo

Spring Boot CRUD REST API for a `Product` entity, backed by an embedded H2
in-memory database, with **Hibernate Envers** entity auditing turned on via
`@Audited`.

Stack: **Spring Boot 4.0.5** (Spring Framework 7) / **Hibernate ORM 7.4.1.Final**
(pinned explicitly — Boot 4.0.x ships 7.1 by default) / **H2** / Java 21.

## What's in this build

- **Bootstrap UI** at `http://localhost:8080/` &mdash; behind login (see "Login"
  below) &mdash; list/create/edit/delete products, and a "history" button per row
  that opens each product's Envers revision trail: revision #, type badge,
  timestamp, **who made the change**, and field values at that point.
  Plain HTML/CSS/JS in `src/main/resources/static/`, no build step.
- **Who did it**: every Envers revision now carries the logged-in username,
  via a custom revision entity + `RevisionListener` — see "What it
  demonstrates" below.
- **Actuator dashboard** at `http://localhost:8080/actuator.html` &mdash; behind
  login, and additionally requires the ACTUATOR_ADMIN role &mdash; a Bootstrap
  front end over the raw `/actuator/*` JSON: Health (overall status + each
  component as a card), Info, Metrics (quick-stat cards for JVM
  memory/CPU/HTTP requests, plus a searchable full metric list with
  measurements and tags), Mappings (every `@RequestMapping`, filterable),
  Environment (property-source browser + a live search box hitting
  `/actuator/env/{name}`), and Beans (filterable name/scope/type table).
  Nothing is cached client-side; hit Refresh to re-poll everything.
- **Swagger UI** at `http://localhost:8080/swagger-ui/index.html` (raw spec at
  `/v3/api-docs`, both behind login), via springdoc-openapi 3.0.3 &mdash; the
  springdoc line built against Spring Boot 4 / Spring Framework 7.
- **Actuator** at `http://localhost:8080/actuator` (raw JSON, same login +
  role requirement as the dashboard) &mdash; `health`, `info`, `env`, `beans`,
  `mappings`, `metrics`, etc. all exposed for local poking around.
  `management.endpoints.web.exposure.include=*` is set wide open in
  `application.properties`; tighten that before this ever runs anywhere
  besides your own machine.
- **Hot reload** via `spring-boot-devtools`: saving a `.java` file triggers a
  fast in-JVM restart (dual classloader, only your own classes reload - not
  the whole dependency graph), and editing anything under `static/`
  (html/css/js) triggers a LiveReload browser refresh with no restart at
  all. Devtools injects the LiveReload script into local HTML responses
  automatically - no browser extension needed. See "Hot reload" below for
  the one thing each IDE needs enabled to actually recompile on save.

## What it demonstrates

- `@Audited` on the `Product` entity → Envers auto-generates a `product_aud`
  shadow table plus a `REVINFO` table, and writes a row to both on every
  insert/update/delete, inside the same transaction.
- `@NotAudited` on `Product.lastViewedAt` → one field deliberately excluded
  from history.
- **Who made each change**: `AuditRevisionInfo` (`audit/`) replaces Envers'
  built-in `DefaultRevisionEntity` with one extra `username` column, and
  `AuditRevisionListener` fills it in from `SecurityContextHolder` right
  before each revision commits. No changes needed per-entity or
  per-endpoint — every `@Audited` entity in the app gets this for free.
  Falls back to `"system"` for changes made outside an HTTP request (e.g.
  `DataSeeder` at startup, before anyone's logged in).
- `AuditReader` (`ProductAuditService`) reading that history back:
  - full revision list for an entity
  - entity state as of one specific revision number
  - an `AuditQuery` filtered to only the revisions where `price` changed
    (needs `global_with_modified_flag=true`, see `application.properties`)
- A `DataSeeder` that inserts a product and mutates it twice on startup, so
  there's already a 3-revision trail (ADD → MOD → MOD) to query immediately
  (attributed to `"system"`, since it runs before login exists).

## Run it

```bash
mvn spring-boot:run
```

> Built in a sandboxed environment without Maven Central access, so this
> hasn't been compiled here — review it before running, though the API
> surface (Envers 5–7, Spring Data JPA) has been stable across recent
> versions. If `mvn` errors resolving `org.hibernate.orm:hibernate-envers`,
> double check your local `~/.m2/settings.xml` isn't pointing somewhere
> that lacks the artifact, and that you're on Maven ≥ 3.9 / JDK 21.

The app starts on `http://localhost:8080`. Everything below requires signing
in first (see "Login").

- UI: `http://localhost:8080/`
- Actuator dashboard (ACTUATOR_ADMIN role required): `http://localhost:8080/actuator.html`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Actuator (raw JSON, same login+role): `http://localhost:8080/actuator`
- H2 console: `http://localhost:8080/h2-console`
  (JDBC URL `jdbc:h2:mem:enversdemo`, user `sa`, empty password) &mdash; useful for
  looking directly at `PRODUCT`, `PRODUCT_AUD`, and `REVINFO`.

## Login

The whole app requires signing in now (not just Actuator) — visiting any
page redirects to a Bootstrap login page (`login.html`) if you're not
authenticated, and lands you back where you were headed on success.
"Sign out" is in both navbars.

Three demo users, defined as an `InMemoryUserDetailsManager` bean in
`SecurityConfig` (not `application.properties` — having more than one lets
the audit trail actually show different names):

| Username | Password | Roles |
|---|---|---|
| `admin` | `admin` | USER, ACTUATOR_ADMIN |
| `alice` | `alice` | USER |
| `bob`   | `bob`   | USER |

Only `admin` can reach `/actuator.html` or `/actuator/**` (403 otherwise —
the Actuator nav link also just hides itself for non-admins, via `/api/me`).
Everyone can use the product UI/API/Swagger/H2 console. Try logging in as
`alice`, editing a product, then `bob`, editing it again, then check its
history — each revision will show who made it.

Passwords are plaintext (`{noop}`) in `SecurityConfig` for demo readability.
Before this runs anywhere but your own machine: switch to hashed passwords
(swap `PasswordEncoderFactories.createDelegatingPasswordEncoder()`'s output
in for `{noop}`, e.g. `{bcrypt}$2a$...`) and a real user store instead of an
in-memory list.

CSRF protection is disabled app-wide. That's intentional for a local dev
tool with a static (non-templated) login form — there's no server-rendered
page to stamp a CSRF token into. Before exposing this beyond localhost:
re-enable CSRF and either switch the login form to `fetch()` with
`CookieCsrfTokenRepository.withHttpOnlyFalse()`, or template `login.html`
server-side (e.g. Thymeleaf) so a token can be embedded.

H2 console needs one more Security tweak worth knowing about: it renders
itself inside an iframe, which Spring Security's default
`X-Frame-Options: DENY` would otherwise blank out — `SecurityConfig` relaxes
that to `sameOrigin()` specifically so `/h2-console` keeps working now that
everything sits behind one filter chain.

## Hot reload

`spring-boot-devtools` is already wired in, but each dev environment needs one
thing enabled so `.class` files actually get recompiled while the app is
running — devtools only *reacts* to changed `.class` files, it doesn't
compile them itself:

- **IntelliJ IDEA**: Settings → Build, Execution, Deployment → Compiler →
  check "Build project automatically". Then, since IntelliJ won't build
  while focus is on the editor: Settings → Advanced Settings → check
  "Allow auto-make to start even if developed application is currently
  running" (or press <kbd>Ctrl+F9</kbd> / <kbd>Cmd+F9</kbd> to build manually
  after each save).
- **VS Code (Spring Boot Dashboard / Java extensions)**: builds on save by
  default — no change needed.
- **Eclipse / Spring Tool Suite**: Project → check "Build Automatically" —
  on by default.
- **Plain `mvn spring-boot:run` from a terminal, no IDE**: nothing rebuilds
  your `.java` files for you. Either run from an IDE as above, or run
  `mvn compile` again in a second terminal after each change (devtools
  will pick up the refreshed `target/classes` and restart automatically).

Once that's set, saving a `.java` file triggers a restart (watch the console
— it logs `Restarting due to...`), and saving anything under
`src/main/resources/static/` (the UI's html/css/js) LiveReload-refreshes the
browser tab with no restart at all.

## Endpoints

CRUD:

```
GET    /api/products
GET    /api/products/{id}
POST   /api/products
PUT    /api/products/{id}
DELETE /api/products/{id}
```

Audit history (this is the Envers part):

```
GET /api/products/{id}/history                 -> every revision, oldest first
GET /api/products/{id}/history/{revisionNumber} -> entity state at that revision
GET /api/products/{id}/history/price-changes    -> only revisions where price changed
```

## Try it

The seeder already creates product id `1` with a 3-revision history
(created at $29.99/qty 100 → price cut to $24.99 → quantity sold down to 85,
both attributed to `"system"` since the seeder runs at startup before any
login exists).

The API is behind login now too, so `curl` needs to authenticate first and
carry the session cookie along on every call:

```bash
# log in once, keep the session in a cookie jar
curl -s -c cookies.txt -X POST http://localhost:8080/login \
  -d 'username=alice&password=alice' > /dev/null

# see the full trail
curl -s -b cookies.txt http://localhost:8080/api/products/1/history | jq

# state as it was at revision 1 (right after creation)
curl -s -b cookies.txt http://localhost:8080/api/products/1/history/1 | jq

# just the price-change revisions
curl -s -b cookies.txt http://localhost:8080/api/products/1/history/price-changes | jq

# make a change as alice, then re-check history - the new revision is hers
curl -s -b cookies.txt -X PUT http://localhost:8080/api/products/1 \
  -H 'Content-Type: application/json' \
  -d '{"name":"Wireless Mouse","description":"Ergonomic wireless mouse","price":19.99,"quantity":85}'

curl -s -b cookies.txt http://localhost:8080/api/products/1/history | jq

# delete it, then note the DEL revision - state is still populated because
# store_data_at_delete=true, showing what the row looked like right before deletion
curl -s -b cookies.txt -X DELETE http://localhost:8080/api/products/1 -w '%{http_code}\n'
curl -s -b cookies.txt http://localhost:8080/api/products/1/history | jq
```

Each `history` entry looks like:

```json
{
  "revisionNumber": 2,
  "revisionTimestamp": "2026-07-25T14:03:11.123Z",
  "revisionType": "MOD",
  "username": "alice",
  "state": {
    "id": 1,
    "name": "Wireless Mouse",
    "description": "Ergonomic wireless mouse",
    "price": 24.99,
    "quantity": 100,
    "lastViewedAt": null
  }
}
```

`revisionType` is `ADD`, `MOD`, or `DEL`. `lastViewedAt` stays `null` in every
revision because it's `@NotAudited` — Envers never tracks it. `username` is
whoever was logged in at commit time, or `"system"` for changes made outside
an HTTP request (like the seeder).

## Project layout

```
src/main/java/com/example/enversdemo/
  EnversDemoApplication.java   entry point
  DataSeeder.java              seeds 3 revisions on startup
  audit/AuditRevisionInfo.java     custom REVINFO entity (+ username column)
  audit/AuditRevisionListener.java fills in username from SecurityContextHolder
  config/SecurityConfig.java   login for the whole app + ACTUATOR_ADMIN role gate
  config/OpenApiConfig.java    Swagger UI title/description
  entity/Product.java          @Audited entity
  repository/ProductRepository.java
  service/ProductService.java       plain CRUD
  service/ProductAuditService.java  Envers AuditReader queries
  controller/ProductController.java CRUD + history endpoints
  controller/MeController.java      GET /api/me - who's logged in, which roles
  dto/ProductRevisionDto.java
  exception/ResourceNotFoundException.java
src/main/resources/
  application.properties
  static/                      index.html, actuator.html, login.html + css/js
```
