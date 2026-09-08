# JD requirement → where it's demonstrated

Line-by-line against the VTEC Systems "Senior DevOps Software Developer Java
(m/f/d)" posting.

## "These are your tasks"

- **Responsibility for... reliable operation of our software solutions for
  merchandise dispensing systems** → the whole project's domain model
  (`Dispenser`, `Product`, `StockLevel`) is a small dispenser-fleet system;
  `/actuator/health` + `scripts/health-check.sh` are the "keep it running"
  half of that.
- **Development of backend and frontend software solutions** →
  `src/main/java` (Spring Boot REST API) + `frontend/` (HTML/CSS/JS
  dashboard consuming it).
- **Support and further development of our CI/CD environment as well as
  automated tests** → `Jenkinsfile` (build → unit test → static analysis →
  integration test → package → containerise → deploy) and the
  Surefire/Failsafe split in `pom.xml` (`*Test` vs `*IT`).
- **Ensuring application operation under Linux and Windows, including
  automation of recurring processes** → `scripts/health-check.sh`,
  `scripts/backup-db.sh` (Linux/cron), `scripts/Restart-DispenserService.ps1`
  (Windows service restart + health poll).
- **Support and further development of import and export interfaces** →
  `DispenserImportExportService` + `ImportExportController`
  (`POST /api/import/dispensers`, `GET /api/export/dispensers`).
- **Hardware management for (touch) PC hardware including operating system
  adjustments** → not something a backend demo can meaningfully simulate;
  see `docs/INTERVIEW-NOTES.md` for how to speak to this from your own
  background instead of pretending the code covers it.
- **Analysis of technical requirements... development and implementation of
  suitable solutions** → the whole exercise: reading the job's actual bullet
  list and building something that answers it is itself the artifact.
- **Support in the continuous optimization of development, testing and
  operational processes** → the Surefire/Failsafe split and the multi-stage
  `Dockerfile` are both examples of "make the pipeline faster/leaner"
  thinking, worth mentioning explicitly.

## "This is what you bring with you"

- **Java, databases (SQL, H2, PostgreSQL)** → Spring Data JPA entities,
  `application.yml` profiles (`dev` → H2, `prod` → PostgreSQL),
  `schema-postgres.sql`.
- **Web technologies (HTML, CSS, JavaScript), REST APIs** →
  `frontend/index.html` + `app.js` + `styles.css`; `DispenserController`,
  `ProductController`.
- **CI/CD tools (Jenkins) and test frameworks** → `Jenkinsfile`; JUnit 5 +
  Mockito unit tests; Spring `MockMvc` integration test.
- **Automation and monitoring tools, especially Ansible, Prometheus and
  Grafana** → `ansible/provision.yml` + `ansible/deploy.yml`;
  `micrometer-registry-prometheus` + `/actuator/prometheus`;
  `monitoring/grafana/dashboards/dispenser-platform.json`.
- **Linux (Ubuntu, Debian), Windows administration (IoT), scripting (Bash,
  PowerShell)** → `ansible/provision.yml` targets Debian/Ubuntu via `apt`;
  `scripts/*.sh` (Bash); `scripts/Restart-DispenserService.ps1`
  (PowerShell).
- **PC hardware / OS interaction** → see the honest note above; this is the
  one bullet the repo can't demonstrate and shouldn't pretend to.
