# Dispenser Fleet Platform

A small, fully-runnable demo built to prepare for the **Senior DevOps Software
Developer Java (m/f/d)** interview at VTEC Systems GmbH. The domain
("dispenser fleet + stock levels") mirrors VTEC's actual business
(merchandise dispensing systems); the stack mirrors the job description's
requirements point-for-point so there's something concrete to open and walk
through in the interview, not just talk about.

Everything in here has been built and actually run — compiled, unit +
integration tested, booted, and exercised through both the REST API and the
browser UI. See [`docs/JD-MAPPING.md`](docs/JD-MAPPING.md) for exactly which
JD line maps to which file, and [`docs/INTERVIEW-NOTES.md`](docs/INTERVIEW-NOTES.md)
for anticipated questions and how to answer them.

## Stack

| Layer | Choice | Matches JD |
|---|---|---|
| Language / runtime | Java 17, Spring Boot 3.3 | "Java", "Several years of professional experience... Java" |
| Persistence | Spring Data JPA / Hibernate, PostgreSQL (prod), H2 (dev/test) | "databases (SQL, H2, PostgreSQL)" |
| Frontend | Plain HTML / CSS / JavaScript (no framework) | "web technologies (HTML, CSS, JavaScript)" |
| API | REST (Spring MVC) | "REST APIs" |
| Testing | JUnit 5, Mockito, Spring MockMvc | "CI/CD tools... and test frameworks" |
| CI/CD | Jenkinsfile (declarative pipeline) | "CI/CD tools (Jenkins)" |
| Containerisation | Docker, docker-compose | implied by "reliable operation", used by Ansible/Jenkins stages |
| Config/deploy automation | Ansible (provisioning + rolling deploy playbooks) | "automation and monitoring tools, especially Ansible" |
| Monitoring | Prometheus (via Micrometer/Actuator) + Grafana dashboard | "Prometheus and Grafana" |
| Linux ops | Bash scripts (health-check, backup) | "Linux (Ubuntu, Debian)... scripting (Bash, PowerShell)" |
| Windows/IoT ops | PowerShell service-restart script | "Windows administration (IoT)" |
| Import/export | CSV import + export REST endpoints | "import and export interfaces" |
| Artifact registry | GitLab Maven Package Registry | matches the actual CITI Jenkins→artifact-registry pattern (see interview notes) |

## Project layout

```
src/main/java/...        Spring Boot application (controllers/services/repositories/model)
src/test/java/...        JUnit + Mockito unit tests, MockMvc integration test (*IT)
frontend/                 Vanilla HTML/CSS/JS dashboard, talks to the REST API
Dockerfile                 Multi-stage build -> small runtime image
docker-compose.yml         app + postgres + prometheus + grafana, one command
ansible/                   provision.yml (host setup) + deploy.yml (rolling deploy)
monitoring/                 Prometheus scrape config + Grafana datasource/dashboard provisioning
scripts/                    health-check.sh, backup-db.sh, Restart-DispenserService.ps1
Jenkinsfile                 Declarative pipeline: build -> unit test -> sonar -> integration test -> package -> docker -> ansible deploy
docs/                       JD-to-code mapping + interview talking points
```

## Running it

### Option A — everything in Docker (closest to how it'd run in prod)

```bash
docker compose up --build
```

- App: http://localhost:8080
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (anonymous viewer access enabled; admin/admin)

### Option B — backend locally, frontend as static files (fastest local loop)

```bash
# backend (H2 in-memory, auto-seeded with sample data)
mvn spring-boot:run

# in another terminal — serve the dashboard
cd frontend && python3 -m http.server 8090
```

Open http://localhost:8090.

### Tests

```bash
mvn test      # fast unit tests (Surefire): StockServiceTest, DispenserServiceTest
mvn verify    # + integration tests (Failsafe): DispenserControllerIT, full Spring context + H2
```

### API quick reference

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/dispensers` | list dispensers |
| POST | `/api/dispensers` | create a dispenser |
| GET | `/api/dispensers/{id}/stock` | stock levels for one dispenser |
| POST | `/api/dispensers/{id}/restock` | restock a product (capped at capacity) |
| GET | `/api/dispensers/low-stock` | dispensers at or below 20% capacity |
| POST | `/api/import/dispensers` | bulk import from CSV (multipart `file`) |
| GET | `/api/export/dispensers` | export all dispensers as CSV |
| GET | `/actuator/health`, `/actuator/prometheus` | ops endpoints |

## What's real vs. illustrative

To be upfront about this for the interview itself:

- **Real and runnable:** the Spring Boot app, its tests (10/10 passing), the
  Docker build, the frontend, the Prometheus/Actuator wiring.
- **Actually run, not just written:** the `Jenkinsfile` was executed against
  a real Jenkins controller (Docker, local) pulling from the live GitHub repo
  — Checkout, Build & Unit Test, Integration Test, Package, and Docker Build
  all genuinely ran and passed (build #9). The "Publish to GitLab" stage
  really uploaded the built jar into GitLab's Maven Package Registry
  (`com.vtecdemo:dispenser-platform:1.0.0`, verified via the GitLab UI and
  API) — this is the actual CITI Jenkins-artifact pattern reproduced end to
  end, not a guess (see `docs/INTERVIEW-NOTES.md`).
- **Configured but not verified live:** **Docker Push** (to GitLab's
  Container Registry) was wired up the same way as the Maven publish —
  correct scopes, correct credentials, correct endpoint — but authentication
  was rejected with a generic 401 that didn't trace back to any visible
  token, project, or group setting after checking all three. Rather than
  keep guessing against an opaque failure, the call was made to stop and
  document it honestly here instead of chasing it further. The **Ansible
  deploy** and **SonarQube** stages remain genuinely illustrative — they need
  real target hosts / a real Sonar server this sandbox doesn't have — and are
  written the way they'd actually be written for this stack, worth walking
  through even though unexecuted.
