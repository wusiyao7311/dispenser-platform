# Interview prep notes — VTEC Systems, Senior DevOps Software Developer Java

## A real Jenkins run, not just a written Jenkinsfile

The `Jenkinsfile` isn't just theoretical — it was actually run against a live
Jenkins controller (Docker, local), and it took two real fixes to get green.
This is worth mentioning if CI/CD debugging comes up, because it's a genuine
troubleshooting story rather than a rehearsed one:

1. **Build #1/#2 failed at checkout**: Jenkins' Git plugin refused to clone
   from a local filesystem path — "aborted because it references a local
   directory, which may be insecure." That's a real security guard (it stops
   a pipeline from trivially reading arbitrary local files via a crafted
   repo URL) that only needed bypassing because this was a throwaway local
   sandbox with no real remote host. Fixed by setting
   `-Dhudson.plugins.git.GitSCM.ALLOW_LOCAL_CHECKOUT=true` — and specifically
   via `JAVA_OPTS` at container *startup*, because the Git plugin reads that
   flag into a static field once at class-load time, so setting it later
   through the Script Console silently had no effect. Good concrete example
   of a JVM-static-init gotcha if asked about tricky bugs you've hit.
2. **Build #3 succeeded but the "Integration Test" stage was silently
   wrong**: it ran `mvn verify -Dsurefire.skip=true`, intending to skip
   Surefire's unit tests since they'd already run in the previous stage.
   `surefire.skip` isn't a property this `pom.xml` actually wires to
   anything, so it was quietly ignored and the unit tests ran a second time
   (harmless here since they passed, but wasteful, and it means the stage
   didn't do what its own comment claimed). Fixed by calling the Failsafe
   plugin's goals directly — `mvn failsafe:integration-test failsafe:verify`
   — which runs only the integration tests without walking back through the
   "test" phase at all. Verified locally before pushing the fix: build #4
   is clean, 6 unit tests run once, 4 integration tests run once, no
   duplication.

If asked "tell me about a bug you found and fixed" in the interview, this is
a legitimate, small, complete answer with a clear before/after.

## How to introduce this project

One version, roughly 30 seconds:

> "After reading your job posting I wanted something concrete to bring to
> this interview rather than just my CV, so I built a small dispenser-fleet
> management API and dashboard — it's the same shape as your merchandise
> dispensing systems: dispensers, products, stock levels, restocking. I used
> it to cover the parts of your stack I hadn't used professionally yet —
> Ansible, Prometheus/Grafana, a Jenkins pipeline — on top of the Java/Spring
> Boot work I do have production experience with at Citi. Happy to walk
> through any part of it."

This framing does two things: it's honest about what's new vs. proven, and
it turns the CV's biggest gap (no listed DevOps-tool experience) into a
demonstrated one instead of a denied one.

## Mapping your real experience onto this stack

| Your CITI CCAR experience | How it transfers |
|---|---|
| Java 17, Spring Boot, Hibernate/JPA, Oracle | Same core stack here, just PostgreSQL instead of Oracle — the JPA/Hibernate layer barely changes between the two; the differences are dialect-level (sequences vs. identity columns, `H2`'s Oracle-compatibility mode being one option if you ever need to bridge them). |
| Jenkins + Maven CI/CD, Docker | Directly reused in this project's `Jenkinsfile` and `Dockerfile` — same shape, new domain. |
| JUnit, Mockito, SonarQube, zero critical issues in prod | Same tools, `Surefire`/`Failsafe` split added on top (a step you can honestly say you hadn't needed at Citi but adopted here because the JD calls out "automated tests" as its own bullet). |
| XML/JSON integration pipelines | Generalises directly to the CSV import/export endpoints here — same "keep two systems' data in sync" problem, different serialisation. |
| Cross-border Agile/Scrum (US/India/China) | Speaks to communication and cross-team coordination, which the JD's "consulting... training for internal and external stakeholders" bullet is really asking about. |

## Where the CITI build artifact actually went (prepared answer)

You may get asked something like "walk me through what happened after Jenkins
built your project" — a natural follow-up once CI/CD comes up. It's easy to
half-remember this because the artifact itself isn't usually the part anyone
looks at day to day. Ground truth worth having straight before the
interview:

**Git almost never holds the built artifact itself.** Git is for source; a
built `.jar`/`.war` is a binary that changes every build and doesn't diff
meaningfully, so serious shops don't let it bloat repository history. If
Jenkins built a jar at Citi, that jar itself almost certainly did **not**
live in git long-term — this is the same tradeoff that came up building this
demo repo (I put a jar in `dist/` here for a portfolio-visible artifact, and
flagged it as *not* normal practice).

The artifact instead went one of two well-worn ways — worth mentally
matching to what CITI actually did:

1. **Artifact repository** (the most likely one at a bank this size): Jenkins
   runs `mvn deploy`, which pushes the versioned jar to an internal Maven
   repository — **Nexus or Artifactory**. Other services or later deployment
   stages pull that exact version from there. A `<distributionManagement>`
   block in `pom.xml`, or repo credentials in Jenkins' Maven `settings.xml`,
   is the tell.
2. **Docker image, not the raw jar**: given Jenkins + Maven + Docker were all
   in play, more likely the jar got wrapped into a Docker image which was
   then pushed to an internal registry, tagged with the build/version number
   — the image is what actually got deployed. This is exactly the shape of
   this repo's own `Jenkinsfile`: Build → Package → Docker Build → Docker
   Push.

**What might genuinely have touched git afterward** is probably what's
prompting the half-memory:
- A **git tag** cut per release (e.g. `v2.4.1`) so the source commit is
  traceable to the artifact version in Nexus/the registry — the artifact
  isn't *in* git, but its version is *linked through* git.
- A separate **deployment/config repo** (GitOps style), where Jenkins
  committed the new image tag into a Kubernetes manifest or Helm
  `values.yaml`, and a CD tool (ArgoCD, Spinnaker) watched that repo and
  deployed on commit — "the artifact reference lived in git," not the
  artifact.

**Safe phrasing if asked and genuinely unsure of the exact mechanism:**

> "Jenkins built and versioned the artifact, and it was published to our
> internal artifact or Docker registry rather than stored in git — git held
> the source and the release tag, not the binary itself. I'd want to
> double-check the exact deployment-repo pattern we used for the last mile
> to production before I state it as fact."

That's accurate to how virtually every serious Java shop runs this, and it
doesn't overclaim a specific tool name you're not 100% sure of.

## Where you're genuinely new — and how to say so

Don't oversell the demo project as equivalent to years of production
experience with these tools. A senior interviewer will probe, and getting
caught overstating is worse than a confident "I'm newer here, here's how I
closed the gap":

- **Ansible**: "I hadn't used it professionally before this; I read through
  it methodically for this project — a provisioning playbook and a rolling
  deploy playbook, using `become`, `serial: 1` for safety, and Jinja2
  templating for environment config. I'd want a week or two on a real
  fleet before I'd call myself fast with it, but the mental model — idempotent
  tasks, inventories, playbooks as the source of truth for host state — is
  one I already had from infrastructure-as-code thinking generally."
- **Prometheus/Grafana**: "New tools for me specifically, but the same
  concept as monitoring I've relied on before — Spring Boot Actuator plus
  Micrometer exposes metrics, Prometheus scrapes them, Grafana visualises.
  I built a small dashboard (request rate by status, p95 latency, JVM heap)
  to make sure I understood the whole chain, not just the config file."
- **Windows/IoT administration, PC hardware**: this is the one area the demo
  project can't really simulate. Be direct: "This is the area of the posting
  I have the least hands-on background in — my production experience has
  been Linux-hosted services. I wrote a PowerShell service-restart script to
  show I can write ops tooling for Windows, but I haven't managed touch-PC
  hardware or Windows IoT deployments day to day. I'd want to learn that
  from your team early on." Trying to fake expertise here is the fastest way
  to lose credibility on the rest of the conversation.

## Likely technical questions and how to answer from this repo

1. **"Walk me through your database schema / why H2 in dev and Postgres in
   prod?"** → `application.yml`'s profile blocks; explain `ddl-auto: update`
   in dev (fast iteration) vs. `validate` in prod (schema changes are
   deliberate, applied via `schema-postgres.sql` or a real migration tool —
   mention Flyway/Liquibase as "what I'd add next" if asked, since this repo
   doesn't actually wire one in).
2. **"How would you handle a schema migration safely?"** → Be honest this
   repo uses a plain DDL script, not Flyway/Liquibase — a good "if I had
   more time" answer, and a real senior-level distinction to raise yourself.
3. **"What happens if two requests restock the same dispenser
   concurrently?"** → `StockService.restock` isn't using optimistic
   locking (`@Version`) yet — a legitimate follow-up you can point out
   proactively: "under real concurrent load I'd add a `@Version` column to
   `StockLevel` to catch lost updates."
4. **"Why capped restock at capacity instead of rejecting the request?"** →
   Product decision worth defending either way — cap-and-succeed is more
   forgiving for a fleet-ops workflow than making a technician's phone app
   throw an error over an off-by-a-few count.
5. **"Unit vs. integration tests — why the split?"** → `Surefire` runs
   `*Test` (mocked, fast, every commit); `Failsafe` runs `*IT` (full Spring
   context + H2, slower, still every build but a separate stage) — mirrors
   the `Jenkinsfile`'s two test stages and is a real answer about keeping
   fast feedback loops fast.
6. **"How would you actually deploy this?"** → Walk through
   `Jenkinsfile` → Docker build → push → `ansible-playbook deploy.yml`
   `serial: 1` rolling restart → health check via `uri` module before
   moving to the next host.
7. **"What would you change before this went to production?"** → Have 2-3
   ready: (a) Flyway/Liquibase instead of the raw SQL file, (b) optimistic
   locking on stock updates, (c) the Ansible playbooks' DB credentials
   should come from `ansible-vault` or a secrets manager, not plain
   inventory vars (the playbook already routes them through
   `vault_db_*` variable names as a placeholder for this).

## Practical logistics from your CV worth having ready

- You're on a German Opportunity Card (Chancenkarte) with full work
  authorisation — the posting doesn't ask about visa sponsorship, but be
  ready to state this plainly and early if it doesn't come up naturally.
- German B1 (progressing to B2) — the JD only lists English as a
  requirement, but VTEC is a Espelkamp-based GmbH, so expect at least a
  courtesy question about German; your honest, factual answer ("B1, actively
  progressing, professional English") is a good one as-is — don't overstate.
- Career break framing: lead with what you did with it (relocation,
  language study, and this kind of self-directed project work), not with
  "I've been unemployed since September."
