# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Purpose

A learning project for Stubborn Contract (stubborn.sh), the community continuation of Spring Cloud Contract: consumer-driven contract testing with generated producer tests and WireMock stubs. The user is joining a team that uses it. The user writes the code themselves: suggest changes (with snippets) rather than applying them. Running builds and tests to check their work is fine. Favour clarity and short "why" comments.

Stubborn's Maven coordinates are `sh.stubborn:stubborn-*`, replacing `org.springframework.cloud:spring-cloud-contract-*`. Most Spring Cloud Contract documentation still applies once the names are changed; Stubborn's own docs are at docs.stubborn.sh (the Gradle plugin reference is `contract/reference/gradle-plugin.html`).

Producer setup (in `catalog-service/build.gradle`, version 0.1.2 from the Gradle Plugin Portal): plugin `sh.stubborn.contract`, BOM `sh.stubborn:stubborn-contract-dependencies` imported via `dependencyManagement`, and `testImplementation 'sh.stubborn:stubborn-contract-starter-verifier'`. Keep the plugin and BOM versions in step. Consumer side will use `sh.stubborn:stubborn-contract-starter-stub-runner`.

How the verifier works here:
- Contracts live in `src/contractTest/resources/contracts/<folder>/<name>.yml` (`failOnNoContracts` is on: no contracts fails the build).
- `generateContractTests` writes JUnit 5 tests to `build/generated-test-sources/contractTest/java/`, one class per contract folder (`book/` → `BookTest`), one method per file (`validate_<fileName>`), in the package of `baseClassForTests`. They use RestAssured MockMvc (`testMode` default `MockMvc`) and extend `baseClassForTests`.
- `contractTest` runs them and is hooked into `check`, so `build` fails if a contract breaks or the base class is missing.
- Naming convention: every contract has a sentence-like `name:` (the plugin lowercases it and replaces spaces with underscores to form the method name, overriding the file name). The generator can't emit `@DisplayName`, so `ContractTestBase` carries an inherited `@DisplayNameGeneration` that strips `validate_`, turns underscores into spaces and appends a full stop (e.g. `returns the book when it exists.`). `nameSuffixForTests` can change the class suffix (default `Test`); not used.
- After changing plugins or dependencies, IntelliJ needs a Gradle reload (e.g. to recognise `src/contractTest` as a test source set).

Consumer side (`loans-service`):
- The demo is self-contained: `catalog-service` exposes `verifierStubsJar` through a consumable `stubs` configuration, `loans-service` depends on it with `testRuntimeOnly project(path: ':catalog-service', configuration: 'stubs')`, and `CatalogClientContractTest` uses `StubsMode.CLASSPATH`. So `./gradlew check` on a fresh clone builds the stubs before the consumer tests, and contract changes rerun the consumer tests (the jar is a tracked input). This relies on one repository; across repositories, stubs travel through a Maven repository with `StubsMode.REMOTE`.
- The `publishing { }` block (`artifact verifierStubsJar`, `publishToMavenLocal` → `com.erasmos:catalog-service:<version>:stubs`) is kept to show that cross-repository route; nothing in the build depends on it. Each WireMock mapping in the jar is named after its contract's `name:`; the jar also carries the YAML contracts.
- Stubborn 0.1.2's BOM manages `spring-boot-restclient` and `spring-boot-http-client` at `${spring-boot.version}`, a property it never defines, so any module using `spring-boot-starter-restclient` fails to resolve. Fix: `mavenBom('sh.stubborn:stubborn-contract-dependencies:0.1.2') { bomProperty 'spring-boot.version', '4.1.1' }` (keep in step with the Boot plugin). Don't instead import Boot's BOM after Stubborn's: that lifts Jetty core to 12.1 while WireMock's `jetty-ee10-servlet` stays on 12.0, and Stub Runner fails with `NoSuchMethodError` in `ServletContextHandler`.
- Response templating: a response body value like `id: "{{{ request.path.[1] }}}"` makes the stub echo the request (WireMock's `response-template` transformer is already on every mapping). It needs a response matcher on the same path (here `by_regex` `number`), otherwise the producer test compares against the literal template. The quotes make the stub return the id as a JSON string while the real catalog returns a number; Jackson coerces it, but a strict client wouldn't. A raw string `body:` to avoid the quotes makes Stubborn 0.1.2 generate a producer test that doesn't compile.
- Stub Runner classes: `sh.stubborn.contract.stubrunner.spring.AutoConfigureStubRunner` / `StubRunnerPort`, and `sh.stubborn.contract.stubrunner.StubsMode`.
- WireMock answers unmatched requests with 404, so a consumer test of "not found" passes even without a 404 contract. Consumer tests guard against this with an `@AfterEach` that asserts `new WireMock(port).findAllUnmatchedRequests()` is empty and then calls `resetRequests()` (the request log is shared across the class).
- With `StubsMode.LOCAL`/`REMOTE`, Stub Runner fetches the stubs jar at runtime, which Gradle doesn't track: consumer tests then need `--rerun` (or `outputs.upToDateWhen { false }`) after new stubs are published.

Messaging (Kafka; the user's team uses Kafka), as built in steps 13–15:
- Producer (`loans-service` publishes `BookCheckedOut` to topic `book-checked-out`): contract has `label`, `input.triggeredBy: checkOutBook()` (a public method on the base class) and `outputMessage` (`sentTo`, `body`, `matchers`). Base class: `@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")`, `@EmbeddedKafka(topics = ...)`, `@AutoConfigureMessageVerifier` (`sh.stubborn.contract.verifier.messaging.boot`), `@MockitoBean CatalogClient`. No Docker needed.
- Needs `testImplementation 'sh.stubborn:stubborn-contract-messaging-kafka'` (and `spring-boot-starter-kafka-test`) on both sides. Without it the verifier silently falls back to Spring Integration and fails with `No bean named 'book-checked-out' available`.
- Stubborn 0.1.2 bug: for messaging contracts the generated test looks up its contract copy as `<name converted to method form>.yml`, but writes it as `<name as written>.yml`. So a messaging contract's `name:` must already be lowercase snake_case (e.g. `publishes_book_checked_out_when_a_book_is_checked_out`), or the test fails with `FileNotFoundException`.
- `predefined: iso_date_time` rejects a Java `Instant` (fractional seconds and `Z`); use `value: '[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(\.[0-9]+)?Z'`. A nested `@TestConfiguration` in the abstract base class is not picked up, so a fixed `Clock` that way doesn't work; the matcher is the better contract anyway.
- spring-kafka 4 JSON (de)serializers are `org.springframework.kafka.support.serializer.JacksonJsonSerializer` / `JacksonJsonDeserializer`. Consumer sets `spring.json.value.default.type` to its own `BookCheckedOut` and `spring.json.use.type.headers=false`.
- The stubs jar for a messaging contract holds only the YAML (no WireMock mapping). Consumer (`notifications-service`): `@EmbeddedKafka`, `@AutoConfigureStubRunner(ids = "com.erasmos:loans-service:+:stubs", stubsMode = CLASSPATH)` (no port), inject `StubTrigger` (`sh.stubborn.contract.stubrunner`), `trigger("<label>")`, then `verify(mock, timeout(...))`.

Stubborn Broker (tried in a scratch copy on 2026-10-16 with the released 0.3.0; deliberately not part of the demo until the team's answer to question 5):
- What it adds over a Maven repository: it stores contracts per application version, records verifications (producer version × consumer version) and deployments per environment, and answers `GET /api/v1/can-i-deploy?application=&version=&environment=` (e.g. "unsafe: loans-service 0.0.1 in staging has no successful verification against catalog-service 0.0.2"). REST API with basic auth; default users `admin`, `publisher`, `reader` (password = name).
- Running it: Docker image `mgrzejszczak/stubborn:0.3.0` (amd64 only; port 8642) needs PostgreSQL (`DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`), despite the docs' one-line `docker run`. The broker repo's `samples/compose.yaml` shows the pairing.
- Publishing contracts: the Gradle plugin `sh.stubborn.broker` is not released (not on the Plugin Portal or Central); the Maven plugin `sh.stubborn:stubborn-maven-plugin` is. From Gradle, `POST /api/v1/applications/{app}/versions/{version}/contracts` with `{contractName, content, contentType: "application/x-spring-cloud-contract+yaml"}` works; `contractName` may contain a folder (`book/shouldReturnBookWhenItExists`).
- Consuming: `testImplementation 'sh.stubborn:stubborn-stub-downloader:0.3.0'` plus `@AutoConfigureStubRunner(ids = "com.erasmos:catalog-service:0.0.1:stubs:8090", repositoryRoot = "stubborn://http://localhost:8642", stubsMode = StubsMode.REMOTE, properties = {"stubborn.contract.stubrunner.username=reader", "stubborn.contract.stubrunner.password=reader"})`. The artifactId is the broker application name; the downloader turns the YAML into WireMock mappings itself. `CatalogClientContractTest`'s tests passed unchanged this way.
- Pitfalls: the downloader is compiled for Java 25 (`UnsupportedClassVersionError` on this demo's Java 17); a `+` or `-SNAPSHOT` version makes it look up `latestVersion`, which the 0.3.0 broker doesn't return, so only exact release versions work; recording verifications and deployments is manual REST calls (no plugin support).



Contract evolution (worked out in a scratch copy on 2026-10-16, before guiding the user):
- Adding a field (`dueDate` on `BookCheckedOut`) is compatible: the producer test passes with the field in code only, and after adding it to the contract the consumer still passes (Jackson 3, as used by spring-kafka 4's `JacksonJsonDeserializer`, ignores unknown properties by default). `predefined: iso_date` works for a `LocalDate`.
- Renaming a field in code and contract (`memberId` → `borrowerId`) passes the producer but fails `notifications-service` (`send(null, ...)`): in one repository `./gradlew check` catches it; across repositories the consumer only finds out when it takes the new stubs.
- Back-compatibility check (built in steps 22–23, then removed from the demo at the user's request on 2026-10-16; these notes are kept so it can be rebuilt): publish the stubs jar of a release (here `maven-publish` to a file repository `releases/` at the root, version `0.0.1`), then run `contractTest` with `contracts { contractsMode = 'REMOTE'; contractRepository { repositoryUrl = rootProject.file('releases').toURI().toString() }; contractDependency { stringNotation = "com.erasmos:loans-service:<version>:stubs" }; contractsPath = "META-INF/com.erasmos/loans-service/<version>/contracts"; baseClassMappings { baseClassMapping('.*\\.http', '...HttpBase'); baseClassMapping('.*\\.messaging', '...MessagingBase') } }`, guarded by a `-PverifyAgainst=<version>` property. Without `contractsPath` the whole jar is scanned; the generated tests' package keeps the jar path (`...loans.META_INF.com_erasmos...contracts`), so `packageWithBaseClasses` would look for `ContractsHttpBase`, hence `baseClassMappings`. The flag also replaces the contracts in the stubs jar that consumers take, so `notifications-service` fails when run with it; guarding the block with `gradle.startParameter.taskNames.any { it.startsWith(':loans-service:') }` avoids that.
- Stubborn 0.1.2 Gradle plugin bug: `contractDependency` fails with `NoClassDefFoundError: org/apache/maven/settings/crypto/SettingsDecryptionRequest` (then `org/eclipse/aether/impl/DefaultServiceLocator$ErrorHandler`) because the Maven resolver is optional in stub-runner and missing from the plugin classpath. Workaround: a `buildscript { dependencies { classpath ... } }` block in the module with `org.apache.maven:maven-embedder:3.9.16`, `maven-resolver-provider:3.9.16` and `org.apache.maven.resolver:maven-resolver-{connector-basic,transport-file,transport-http}:1.9.27`.
- A Gradle-native alternative (resolving `com.erasmos:loans-service:0.0.1:stubs` in a configuration of `loans-service` itself) fails: Gradle substitutes the project for its own coordinates, even with `useGlobalDependencySubstitutionRules = false`.
- After a version change, `build/stubs/` keeps the old version's folder, so the stubs jar contains both until `clean`.
- Expand and contract works: the producer sends both names (record component `memberId` plus `public Long getBorrowerId()`, which Jackson serialises), the current contract lists both, and both the normal and the back-compatibility check pass; the consumer then switches to `borrowerId`; `memberId` is removed only once no consumer reads it.

## Commands

```bash
./gradlew build                      # compile + check
./gradlew check                      # all tests in all modules, contract tests included
./gradlew test                       # unit tests only (src/test); contract tests are the separate contractTest task
./gradlew :catalog-service:test --tests 'com.erasmos.stubborncontractdemo.catalog.SomeTests'            # one class
./gradlew :catalog-service:test --tests 'com.erasmos.stubborncontractdemo.catalog.SomeTests.someMethod' # one method
./gradlew :catalog-service:bootRun   # run one service
```

Each service is a Gradle module (listed in `settings.gradle`) whose code lives in its own subpackage of `com.erasmos.stubborncontractdemo` (`...catalog`, `...loans`, `...notifications`), with its `@SpringBootApplication` class in that subpackage so component scanning stays within the service.

Stack: Java 17 toolchain, Gradle 9 wrapper, Spring Boot 4.1.

## Status and planned architecture

The plan, built step by step (the user prefers one step at a time):

Done (steps 1–10): `catalog-service` (producer) with `GET /books/{id}` (`200 {"id": 100, "title": "Dune"}` or `404`, in-memory `BookCatalog`); two YAML contracts (book exists, book unknown) verified against a standalone-MockMvc `ContractTestBase` that mocks `BookCatalog` with exactly the data the contracts need (the usual convention: contract tests cover the API layer, not data or business logic); URL and body matchers, with `priority: 1` on the 404 contract because `/books/999` also matches the "exists" contract's regex; the producer broken on purpose (renaming `title`) to see the contract fail; stubs published to `~/.m2`, then consumed from the classpath instead; `loans-service` (consumer) with `CatalogClient` (`RestClient`, 404 → `Optional.empty()`) tested by `CatalogClientContractTest` via Stub Runner on port 8090, failing on any request that matches no stub.

Then (steps 11–12): the "exists" stub accepts any numeric id and echoes it back via response templating.

Then (steps 13–15): messaging. `LoansService.checkOut` (catalog check via `CatalogClient`, in-memory loan ids, injected `Clock`) publishes `BookCheckedOut` to `book-checked-out`; the contract (now `contracts/messaging/shouldPublishBookCheckedOut.yml`) is verified by the generated `LoansTest` against a `@SpringBootTest` + `@EmbeddedKafka` base class whose `checkOutBook()` sets up the scenario (member 7, book 100), so the YAML and `checkOutBook()` must agree. `notifications-service` consumes it with a `@KafkaListener` that emails the member via `EmailGateway`; `BookCheckedOutContractTest` triggers the stub by label and verifies the mocked gateway. Both sides were broken on purpose (code vs. YAML) to see the failures.

Then (steps 16–17): `POST /loans` on `loans-service` (`CheckOutRequest` → `201` + `Location` + `CheckOutResponse(loanId)`, or `422` on `UnknownBookException`; its own API types rather than the event). `loans-service` now uses `packageWithBaseClasses = 'com.erasmos.stubborncontractdemo.loans'`: the contract folder picks the base class (`contracts/http` → `HttpBase`, standalone MockMvc with a mocked `LoansService`; `contracts/messaging` → `MessagingBase`, the Kafka one) and names the generated test (`HttpTest`, `MessagingTest`). Both share a top-level `ContractDisplayNames`. A folder without a matching base class fails compilation (`cannot find symbol`). The loans stubs jar now holds WireMock mappings for the HTTP contracts too; nothing consumes them yet.

Then (steps 18–19): the `POST /loans` contracts were rewritten for external clients the team doesn't control. A `description:` lists the assumptions and invites changes (it travels in the stubs jar). Request matchers loosen only the stub (`Content-Type` regex `application/json.*` so a charset still matches; `bookId`/`memberId` `by_regex` `predefined: number`); the producer test still sends the example values. The unknown-book contract (`bookId: 999`) has `priority: 1` because it overlaps "any number". As an external client: WireMock standalone 3.13.2 (the version Stub Runner uses) with `--root-dir loans-service/build/stubs/META-INF/com.erasmos/loans-service/0.0.1-SNAPSHOT` serves the stubs without Spring or Gradle (it ignores the unknown `stubborn-contract` transformer); responses carry `Matched-Stub-Id`, an unmatched request gets a 404 with a closest-stub diff, and `GET /__admin/requests/unmatched` lists requests outside the contract.

Then (steps 20–24, done on 2026-10-16): contract evolution, as in the "Contract evolution" notes above. `BookCheckedOut` gained `dueDate` (compatible). A plain rename `memberId` → `borrowerId` broke notifications and was reverted. A back-compatibility check against a published release 0.0.1 was built and then removed again (no `maven-publish`, `releases/` or `-PverifyAgainst` in `loans-service` any more; version back to `0.0.1-SNAPSHOT`). The rename was then redone with expand and contract: loans sends both `memberId` and `borrowerId` (`getBorrowerId()`), the contract lists both, and notifications reads `borrowerId`. Removing `memberId` (the "contract" phase) is deliberately not done: it needs a team decision that no consumer reads it.

Next, roughly in order:
- Stubborn Broker was tried and deliberately left out (see the Broker notes above); revisit if the team uses it.

Keep Modulith, jMolecules and persistence out until the contract basics work, so contracts are the only new concept.

## The new team (context)

About ten microservices that the wider team controls, plus client services that call them which the team does not control. The team has recently wondered whether those external clients could write contracts themselves. The user's view: the producer team writes the initial contracts for external clients, shares the assumptions behind them, and invites the clients to propose changes.

Questions for the team, once the user starts:
1. Where do contracts live: in each producer's repository, a shared contracts repository, or Stubborn Broker?
2. How are stubs published: Nexus/Artifactory, git, or not at all?
3. Do the external clients use the stubs, or are the contracts only a producer-side safety net?
4. Do the team's own services call external services they don't control (which contracts can't verify), and how are those tested?
5. If Stubborn Broker is used or considered: which Java version do the services build on (its stub downloader needs Java 25), and is can-i-deploy (or something like it) part of the release pipeline?

## Domain

Reuses the library-lending domain of the user's `/Users/erasmos/work/spring-modulith-demo` (read its CLAUDE.md for the full design). Its modules become separate services here, and contracts replace the in-process checks that Modulith's `verify()` did there:

- `catalog` only answers "does this book exist?" (the remote version of `CatalogService.exists(BookId)`).
- `loans` owns availability. This is a deliberate DDD decision (copying availability into catalog would race and allow a double checkout), so the catalog's contract must not grow an `available` field.
- `BookCheckedOut(loanId, bookId, memberId, checkedOutAt)` belongs to `loans`.
- Ids are numeric (`BookId`, `MemberId`, `LoanId` in the modulith demo). Each service has its own types; the contract fixes their wire format.
