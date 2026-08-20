Finalize Java upgrade, add Jolokia dashboard, secure endpoint, and UI/footer polish

Summary
- Upgrade target and build: set `java.version=17` and configure `maven-compiler-plugin`.
- Jolokia: add Jolokia servlet + static dashboard; secure endpoint with `ACTUATOR_ADMIN` and IP whitelist; add `POST /jolokia/glossary/save` to persist dashboard glossary.
- UI: Details Explorer, contextual explanations, glossary export/copy/print, shared footer with tech logos and build-time version labels.
- Tests: Baseline tests executed under Java 17; full JDK 25 validation deferred (recommended CI update).

Commits of note
- 7e3570e - Persist glossary to static
- 56d1403 - Add powered-by footer and logos
- 0b7d7cb - Footer links + accessibility
- cc80543 - Consolidate footer into include fragment
- 1ccb9cb - Footer tech versions display

Verification steps (locally)
1. Run tests:
   ```bash
   set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot"
   set "PATH=%JAVA_HOME%\bin;%PATH%"
   mvnw.cmd test
   ```
2. Run app and test dashboard/save:
   ```bash
   mvnw.cmd -DskipTests=true spring-boot:run
   # open http://localhost:8080/jolokia-dashboard.html (login admin/admin)
   # click "Save Glossary to Server" or POST src/main/resources/static/jolokia-glossary.json to /jolokia/glossary/save
   ```

Notes
- Versions shown in footer are build-time values (Spring Boot 4.0.5, Jolokia 2.2.1, Java 17). I can wire runtime-driven labels via Actuator info if desired.
- Recommend updating CI to run on JDK 17 (or JDK 25 when ready) and to add a job that runs the integration smoke tests against the dashboard.

Reviewers
- @your-team-lead
- @backend

Labels
- upgrade
- feature
- security

