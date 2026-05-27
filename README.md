# Mule SonarQube Plugin

A SonarQube plugin for static code analysis of **Mule 4** applications. The plugin validates Mule XML configuration files using configurable XPath-based rules, calculates quality metrics, integrates with MUnit test coverage, and detects copy-paste in both XML and DataWeave files.

**Plugin coordinates**

| Field | Value |
|---|---|
| Group ID | `com.mulesoft.services` |
| Artifact ID | `sonar-mulesoft-plugin` |
| Plugin key | `sonarqube-mule` |
| Plugin name | `Mulesoft Analyzer` |
| Entry class | `com.sonar.plugins.mulesoft.MulePlugin` |

**Key features**
- XPath-based validation rules for Mule 4 XML (40+ rules out of the box)
- External rule configuration — customize rules without rebuilding the plugin (YAML preferred, XML legacy)
- 4-tier rule loading: external YAML → external XML → bundled YAML → bundled XML
- Custom metrics: flow count, subflow count, DataWeave transformations, configuration complexity
- Cyclomatic complexity, cognitive complexity, and nesting depth checks per flow
- MUnit test counting and coverage import from JSON report
- Copy-paste detection (CPD) for both XML and DataWeave (`.dwl`) files
- DataWeave sensor — detects inline code, commented-out code, and duplicates
- Three quality profiles: Sanity, Recommended (default), Strict
- Rule templates for user-defined validations
- 6 custom XPath functions for advanced validation logic
- 50+ Mule namespace mappings built in

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Build Commands](#build-commands)
- [Local Development with Docker](#local-development-with-docker)
- [CI/CD — GitHub Actions](#cicd--github-actions)
- [Architecture](#architecture)
  - [Plugin Components](#plugin-components)
  - [Rule Loading Strategy](#rule-loading-strategy)
  - [Sensors](#sensors)
  - [XPath Processor & Custom Functions](#xpath-processor--custom-functions)
  - [Quality Profiles](#quality-profiles)
  - [Metrics](#metrics)
- [Validation Rules Reference](#validation-rules-reference)
  - [Application Rules](#application-rules)
  - [Flow Rules](#flow-rules)
  - [Configuration Rules](#configuration-rules)
  - [Security Rules](#security-rules)
  - [Observability Rules](#observability-rules)
- [Ruleset Configuration](#ruleset-configuration)
  - [Rule File Format (YAML)](#rule-file-format-yaml)
  - [Rule File Format (XML — legacy)](#rule-file-format-xml--legacy)
- [SonarQube Configuration](#sonarqube-configuration)
  - [Server Setup](#server-setup)
  - [Project Analysis](#project-analysis)
- [Quality Gates](#quality-gates)
- [Final Notes](#final-notes)

---

## Prerequisites

| Requirement | Version |
|---|---|
| Java (JDK) | 21+ |
| Maven | 3.8+ |
| SonarQube | 9.9+ (LTS) or 10.x |
| Mule | 4.x |

---

## Quick Start

### 1. Build the plugin

```bash
mvn clean package
```

The built JAR is placed in `target/sonar-mulesoft-plugin-<version>.jar`.

### 2. Deploy to SonarQube

```bash
cp target/sonar-mulesoft-plugin-*.jar <SONARQUBE_HOME>/extensions/plugins/
```

### 3. (Optional) Override bundled rules

```bash
# YAML format (preferred)
cp ruleset/mulesoft-ruleset.yaml <SONARQUBE_HOME>/extensions/plugins/

# or XML format (legacy)
cp src/main/resources/mulesoft-ruleset.xml <SONARQUBE_HOME>/extensions/plugins/
```

If neither file is present, the plugin uses the bundled defaults.

### 4. Restart SonarQube

```bash
<SONARQUBE_HOME>/bin/<OS>/sonar.sh restart
```

### 5. Remove the XML file suffix from the SonarQube XML plugin

SonarQube ships with a built-in XML plugin that would also try to analyze Mule XML files. To prevent conflicts, remove `.xml` from its file suffix list:

`Administration → Configuration → General Settings → XML → File suffixes` — clear the `.xml` entry.

### 6. Verify installation

Navigate to `Administration → Quality Profiles` and confirm **Mulesoft Analyzer & Validator** (Recommended) is listed under the Mule language.

### 7. Analyze a Mule project

```bash
mvn sonar:sonar \
  -Dsonar.projectKey=my-mule-app \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=<your-token> \
  -Dsonar.sources=src/main/mule
```

---

## Build Commands

The `Makefile` provides convenience targets for the full development lifecycle:

| Target | Command | Description |
|---|---|---|
| `make build` | `mvn clean package -DskipTests` | Build JAR without running tests |
| `make test` | `mvn clean package` | Full build including all tests |
| `make stage` | Copy JAR + YAML → `plugins/` | Stage artifacts for Docker mount |
| `make deploy` | `build + stage + docker-restart` | Full cycle: build, stage, reload |
| `make docker-build` | `docker build -t sonarqube-mule .` | Build Docker image with plugin baked in |
| `make docker-up` | `docker compose up -d` | Start local SonarQube container |
| `make docker-restart` | `docker compose restart sonarqube` | Reload plugins from mounted directory |
| `make docker-logs` | `docker compose logs -f sonarqube` | Tail live SonarQube logs |
| `make clean-stage` | `rm -rf plugins/` | Remove staged artifacts |
| `make help` | — | List all available targets |

---

## Local Development with Docker

The `local/` directory contains a `docker-compose.yml` for spinning up a local SonarQube instance with the plugin mounted for live development.

```bash
# Stage the plugin artifacts first
make stage

# Start SonarQube (mounts ./plugins/ into the container)
make docker-up

# After rebuilding, reload without full restart
make deploy
```

**What the stack configures:**

- Container name: `sonarqubeMule`
- Port: `9000` (UI and API)
- Volume mount: `./plugins/` → `/opt/sonarqube/extensions/plugins/` (hot-reload)
- Persistent volumes for `data/` and `logs/`
- Elasticsearch bootstrap checks disabled (dev mode: `SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true`)
- Health check: `curl http://localhost:9000/api/system/status` every 30 s, up to 5 m start period

The `local/Dockerfile` bakes the plugin JAR and ruleset YAML directly into the image for static deployments:

```dockerfile
FROM sonarqube:latest
COPY target/sonar-mulesoft-plugin-*.jar /opt/sonarqube/extensions/plugins/
COPY ruleset/mulesoft-ruleset.yaml      /opt/sonarqube/extensions/plugins/
```

---

## CI/CD — GitHub Actions

The workflow at [.github/workflows/release.yml](.github/workflows/release.yml) triggers on every push to `main` and automates the full release cycle:

1. Extracts the version from `pom.xml` (strips `-SNAPSHOT`)
2. Skips if that version tag already exists (idempotent)
3. Builds with `mvn --batch-mode clean package` (tests included)
4. Creates and pushes a semver git tag (e.g. `v0.0.1`)
5. Creates a GitHub Release and attaches the built JAR

To publish a new release, bump `<version>` in `pom.xml`, commit, and push to `main`.

---

## Architecture

### Plugin Components

```
MulePlugin (entry point)
├── MuleLanguage              — registers "mule" / "mule4" language, .xml file suffix
├── MuleRulesDefinition       — registers 40+ XPath rules + rule templates
├── DataWeaveRulesDefinition  — registers DataWeave-specific rules
├── MuleCheckList             — programmatic checks (complexity, nesting depth)
├── Sensors (9 total)         — see Sensors section
├── Metrics (4 custom)        — see Metrics section
├── Aggregators (3)           — roll up file-level metrics to project level
└── Quality Profiles (3)      — Sanity, Recommended, Strict
```

### Rule Loading Strategy

The plugin uses an **external-first** 4-tier loading strategy to allow runtime customization without rebuilding:

```
1. <SONARQUBE_HOME>/extensions/plugins/mulesoft-ruleset.yaml   (external YAML — preferred)
2. <SONARQUBE_HOME>/extensions/plugins/mulesoft-ruleset.xml    (external XML  — legacy)
3. Bundled mulesoft-ruleset.yaml                               (JAR resource  — default)
4. Bundled mulesoft-ruleset.xml                                (JAR resource  — last resort)
```

The loader (`RuleLoader`) handles field-name compatibility between formats (`key`/`ruleKey`, `scope`/`applies`) so both old and new rule file schemas work transparently.

### Sensors

| Sensor | Purpose |
|---|---|
| `MuleValidationSensor` | Executes all XPath rules; reports issues with precise line numbers via `locationHint` XPath |
| `MuleStructuralMetricsSensor` | Single DOM walk — counts flows, subflows, HTTP listeners, MUnit tests, DataWeave transforms; computes cyclomatic/cognitive complexity and nesting depth |
| `DataWeaveSensor` | Analyzes `.dwl` files — detects inline code, commented-out code, duplicates |
| `CpdTokensSensor` | Coordinates copy-paste detection across XML and DataWeave files |
| `CpdXmlTokenizer` | Tokenizes Mule XML for CPD |
| `CpdDwlTokenizer` | Tokenizes DataWeave for CPD |
| `ConfigurationMetricsSensor` | Collects configuration-file-level metrics |
| `MUnitMetricsSensor` | Counts MUnit test cases |
| `CoverageImportSensor` | Imports MUnit flow coverage from JSON report |

`ParsedDocumentCache` ensures each XML file is parsed only once per analysis, even when multiple sensors reference the same file.

### XPath Processor & Custom Functions

`XPathProcessor` is thread-safe and immutable. It is constructed with:
- Default Mule namespace bindings (from `mulesoft-namespace.properties` — 50+ namespaces)
- Optional custom namespace list
- Optional properties file for additional bindings

**Custom XPath functions available in rule expressions:**

| Function | Signature | Description |
|---|---|---|
| `matches` | `matches(nodes, regex)` | Returns `true` if any node value matches the given regular expression |
| `is-configurable` | `is-configurable(attribute)` | Returns `true` if the attribute value is a property placeholder (`${...}`) |
| `starts-with-any` | `starts-with-any(value, prefix1, prefix2, ...)` | Returns `true` if value starts with any of the supplied prefixes |
| `child-count` | `child-count(node)` | Returns the number of child elements of a node |
| `distinct-count` | `distinct-count(nodes)` | Returns the count of distinct values across a node set |
| `prop-placeholder` | `prop-placeholder(attribute)` | Validates strict property placeholder format |

### Quality Profiles

| Profile | Description | Default |
|---|---|---|
| `Mule Sanity` | Minimal set of critical/blocker rules only | |
| `Mule Recommended` | All 40+ rules enabled — balanced for most teams | ✅ |
| `Mule Strict` | Maximum ruleset with tightest thresholds | |

### Metrics

**Custom metrics** (defined in `MuleMetrics`):

| Metric key | Display name | Type |
|---|---|---|
| `mule_flows` | Number of Flows | Integer |
| `mule_subflows` | Number of SubFlows | Integer |
| `mule_transformations` | Number of DW Transformations | Integer |
| `mule_config_complexity_rating` | Configuration Complexity Rating | Rating |

**Complexity rating** is based on flow count per file:
- **Simple** — ≤ 7 flows
- **Medium** — 8–14 flows
- **Complex** — ≥ 15 flows

**Coverage** requires MUnit configured to emit JSON reports. See the [MUnit coverage documentation](https://docs.mulesoft.com/munit/2.2/coverage-maven-concept).

File-level metrics are rolled up to the project level by three aggregators: `FlowCountAggregator`, `SubFlowCountAggregator`, and `TransformationCountAggregator`.

---

## Validation Rules Reference

All rules are defined in `mulesoft-ruleset.yaml`. Severity and remediation effort guidance:

| Severity | Remediation effort |
|---|---|
| CRITICAL | 90 min |
| MAJOR | 60 min |
| MINOR | 15 min |
| INFO | 5 min |

### Application Rules

| Key | Severity | Type | Description |
|---|---|---|---|
| `apikit-config-required` | MAJOR | CODE_SMELL | Application must declare an APIKit configuration |
| `apikit-exception-strategy` | MAJOR | CODE_SMELL | Application must configure an APIKit exception strategy |

### Flow Rules

| Key | Severity | Type | Description |
|---|---|---|---|
| `count-limit` | MAJOR | CODE_SMELL | Configuration file must not contain ≥ 10 flows |
| `subflow-count-limit` | MAJOR | CODE_SMELL | Configuration file must not contain ≥ 5 subflows |
| `naming-convention` | MINOR | CODE_SMELL | Flow names must follow kebab-case naming convention |
| `subflow-naming-convention` | MINOR | CODE_SMELL | Subflow names must follow kebab-case naming convention |
| `encryption-key-logging-forbidden` | MAJOR | VULNERABILITY | Logger message must not reference `mule.key` |
| `set-variable-doc-name` | MINOR | CODE_SMELL | `doc:name` on Set Variable must match the variable name |
| `healthcheck-flow-required` | MINOR | CODE_SMELL | Application must define a dedicated health check flow |

### Configuration Rules

| Key | Severity | Type | Description |
|---|---|---|---|
| `config-properties-required` | MAJOR | CODE_SMELL | Application must use configuration properties files |
| `credentials-vault-no-hardcoded-key` | MAJOR | VULNERABILITY | Secure Properties config key must use a property placeholder |
| `autodiscovery-required` | MAJOR | CODE_SMELL | API autodiscovery element must be present |
| `apikit-http-status-codes` | MAJOR | CODE_SMELL | Standard HTTP error status codes must be configured in the APIKit router |
| `dwl-external-payload` | MINOR | CODE_SMELL | DataWeave `set-payload` must reference an external `.dwl` file |
| `dwl-external-variable` | MINOR | CODE_SMELL | DataWeave `set-variable` must reference an external `.dwl` file |
| `http-listener-https-protocol` | MAJOR | VULNERABILITY | HTTP listener connection must use HTTPS protocol |
| `http-listener-port-property` | MAJOR | VULNERABILITY | HTTP listener port must use a property placeholder |
| `http-listener-tls-context` | CRITICAL | VULNERABILITY | HTTP listener must configure a TLS context |
| `autodiscovery-api-id-no-hardcode` | MAJOR | VULNERABILITY | API autodiscovery `apiId` must use a property placeholder |
| `mssql-host-no-hardcode` | MAJOR | VULNERABILITY | Database (MSSQL) host must use a property placeholder |
| `db-generic-url-no-hardcode` | MAJOR | VULNERABILITY | Database generic URL must use a property placeholder |
| `jms-credentials-no-hardcode` | MAJOR | VULNERABILITY | JMS username and password must use property placeholders |
| `email-smtp-no-hardcode` | MAJOR | VULNERABILITY | SMTP host, user, and password must use property placeholders |
| `http-requester-https-protocol` | MAJOR | VULNERABILITY | HTTP requester connection must use HTTPS protocol |
| `http-requester-port-property` | MAJOR | VULNERABILITY | HTTP requester port must use a property placeholder |
| `http-requester-basic-auth` | MAJOR | VULNERABILITY | HTTP requester must not use basic authentication configuration |
| `http-requester-basic-auth-no-hardcode` | MAJOR | VULNERABILITY | HTTP requester basic auth credentials must use property placeholders |
| `http-requester-tls-context` | MAJOR | VULNERABILITY | HTTP requester must configure a TLS context |
| `munit-test-required` | MAJOR | CODE_SMELL | At least one MUnit test file must exist in the project |

### Security Rules

| Key | Severity | Type | Description |
|---|---|---|---|
| `set-payload-no-inline-secret` | CRITICAL | VULNERABILITY | `set-payload` expression must not contain password, secret, or key literals |
| `logger-no-payload-at-info-level` | MAJOR | VULNERABILITY | Logger at INFO level must not log `#[payload]` |
| `logger-no-authorization-header` | CRITICAL | VULNERABILITY | Logger message must not reference the Authorization header |
| `tls-context-revocation-check` | MAJOR | VULNERABILITY | TLS context must configure certificate revocation checking |
| `http-listener-no-wildcard-path` | MAJOR | VULNERABILITY | HTTP listener path must not be a wildcard (`/*`) |

### Observability Rules

| Key | Severity | Type | Description |
|---|---|---|---|
| `logger-category-required` | MINOR | CODE_SMELL | Logger component must specify a category |
| `correlation-id-in-logger` | MINOR | CODE_SMELL | Logger message must include the correlation ID |
| `doc-name-non-empty` | MINOR | CODE_SMELL | All components must have a non-empty `doc:name` attribute |

---

## Ruleset Configuration

Rules can be customized at runtime by placing a file in `<SONARQUBE_HOME>/extensions/plugins/`. The plugin picks it up on next restart without requiring a plugin rebuild.

See the full reference in [docs/RULESET_CONFIGURATION.md](docs/RULESET_CONFIGURATION.md).

### Rule File Format (YAML)

```yaml
type: mulesoft-sonar-rules
version: "1.0"

rulesets:
  - category: security
    rules:
      - ruleKey: http-listener-https-protocol
        name: HTTP Listener must use HTTPS protocol
        description: |
          HTTP Listener configuration must specify protocol='HTTPS'.
          Using HTTP exposes traffic to interception.
        severity: MAJOR
        type: VULNERABILITY
        applies: file                          # file | application
        tags: [security, cwe-319, https]
        remediationEffort: 60                  # minutes
        locationHint: //http:listener-config   # XPath to highlight the issue location
        xpath: |
          count(//mule:mule/http:listener-config)=0 or
          //mule:mule/http:listener-config/http:listener-connection/@protocol='HTTPS'
```

**Field reference:**

| Field | Required | Description |
|---|---|---|
| `ruleKey` / `key` | ✅ | Unique rule identifier |
| `name` | ✅ | Display name in SonarQube UI |
| `description` | ✅ | Markdown description (supports HTML rule files in `rules/`) |
| `severity` | ✅ | `BLOCKER`, `CRITICAL`, `MAJOR`, `MINOR`, `INFO` |
| `type` | ✅ | `BUG`, `VULNERABILITY`, `CODE_SMELL`, `SECURITY_HOTSPOT` |
| `applies` / `scope` | ✅ | `file` — evaluated per file; `application` — evaluated once across the whole project |
| `xpath` | ✅ | XPath expression; must evaluate to boolean `true` for a **passing** file |
| `locationHint` | ❌ | XPath to the element where the issue line number should be reported |
| `tags` | ❌ | Array of categorization tags |
| `remediationEffort` | ❌ | Effort in minutes |

### Rule File Format (XML — legacy)

```xml
<rules>
  <rule>
    <key>http-listener-https-protocol</key>
    <name>HTTP Listener must use HTTPS protocol</name>
    <severity>MAJOR</severity>
    <type>VULNERABILITY</type>
    <scope>FILE</scope>
    <xpath>
      count(//mule:mule/http:listener-config)=0 or
      //mule:mule/http:listener-config/http:listener-connection/@protocol='HTTPS'
    </xpath>
  </rule>
</rules>
```

---

## SonarQube Configuration

### Server Setup

**Remove XML suffix from the built-in XML plugin** so it does not conflict with the Mule plugin's analysis of `.xml` files:

`Administration → Configuration → General Settings → XML → File suffixes` — remove `.xml`.

### Project Analysis

**Option 1 — Pass parameters on the command line:**

```bash
mvn sonar:sonar \
  -Dsonar.projectKey=my-mule-app \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=<your-token> \
  -Dsonar.sources=src/main/mule
```

**Option 2 — Add to the project `pom.xml`:**

```xml
<properties>
  <sonar.sources>src/main/mule</sonar.sources>
  <sonar.projectKey>my-mule-app</sonar.projectKey>
</properties>
```

**Option 3 — Configure the server in Maven `settings.xml`:**

```xml
<profile>
  <id>sonar</id>
  <activation>
    <activeByDefault>true</activeByDefault>
  </activation>
  <properties>
    <sonar.host.url>http://localhost:9000</sonar.host.url>
  </properties>
</profile>
```

For more analysis parameters see: https://docs.sonarqube.org/latest/analysis/analysis-parameters/

### Results

After a successful analysis the SonarQube UI shows:

| Section | Content |
|---|---|
| **Overview** | Project health, quality gate status, aggregated Mule metrics |
| **Issues** | Rule violations raised by the validation sensor with precise line numbers |
| **Measures** | Flow counts, subflow counts, DataWeave transformation counts, complexity rating, MUnit coverage |
| **Duplications** | Copy-paste detected in XML and DataWeave files |

---

## Quality Gates

Quality gates let you enforce thresholds as a CI/CD pass-or-fail step. Examples:

- Do not release if MUnit coverage < 60 %
- Block deployment if any CRITICAL or BLOCKER issue is open
- Fail build if more than 3 new Code Smell issues are introduced

**Assign a quality gate to a project:** `Project → Administration → Quality Gates`

For more information see: https://docs.sonarqube.org/latest/user-guide/quality-gates/

---

## Final Notes

This plugin is licensed under the [Apache 2.0 License](LICENSE).

For more information about SonarQube see: https://www.sonarqube.org/

Contributions, feedback, and bug reports are welcome. If you need assistance extending the plugin, contact MuleSoft Professional Services.
