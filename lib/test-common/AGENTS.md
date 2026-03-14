# lib/test-common

Shared test utility library providing custom Hamcrest matchers, test data loading,
and specialized helpers for Swing and security testing. Used as a `testImplementation`
dependency by most modules in the project.

## Package Layout

| Package | Purpose |
|---------|---------|
| `org.triplea.test.common` | Core utilities — custom matcher builder, JSON serialization, test data file reading, stream conversion, temporal assertions |
| `org.triplea.test.common.matchers` | Collection-level Hamcrest matchers with mapping/transformation support |
| `org.triplea.test.common.security` | RSA key pair loading for security tests |
| `org.triplea.test.common.swing` | Swing test helpers — headless detection and component lookup |

## Key Classes

### Custom Assertions

- **`CustomMatcher<T>`** — fluent builder for type-safe Hamcrest matchers.
  Extends `TypeSafeMatcher<T>`. Supports an optional `debug` lambda for
  richer failure messages. Usage:
  ```java
  CustomMatcher.builder(MyType.class)
      .description("has expected name")
      .checkCondition(obj -> obj.getName().equals("foo"))
      .debug(obj -> obj.toString())
      .build();
  ```
- **`IsInstant`** — matcher for `java.time.Instant` values by component.
  `isInstant(2024, 1, 15, 10, 30, 0)` matches an Instant at that UTC time.
- **`CollectionMatchers`** — matchers that apply a mapping function before
  comparison: `containsMappedItem(MyObj::getName, "foo")` and
  `doesNotContainMappedItem(MyObj::getName, "foo")`.

### Test Data and I/O

- **`TestDataFileReader`** — reads test fixture files with fallback path
  resolution: tries project root, walks parent directories, then falls back
  to classpath resources (`src/test/resources`). Throws
  `TestDataFileNotFound` if all lookups fail.
- **`StringToInputStream`** — converts a `String` to an `InputStream` for
  testing I/O-consuming code.
- **`JsonUtil`** — Jackson-based `toJson(Object)` with `JavaTimeModule`
  registered for `Instant` serialization. Excludes null values.

### Security

- **`TestSecurityUtils`** — loads RSA key pairs from classpath resources.
  Key files follow the naming convention `{ClassName}-public.key` /
  `{ClassName}-private.key`. Bundled test keys are in
  `src/main/resources/`.

### Swing Testing

- **`DisabledInHeadlessGraphicsEnvironment`** — JUnit 5
  `ExecutionCondition` that disables tests when `GraphicsEnvironment.isHeadless()`
  is true. Apply with `@ExtendWith(DisabledInHeadlessGraphicsEnvironment.class)`.
- **`SwingComponentWrapper`** — wraps a Swing `Component` and provides
  recursive depth-first child lookup by name (`findChildByName`), with
  typed casting and assertion helpers (`assertHasComponentByName`).

## Conventions

- **Lombok `@UtilityClass`** on all stateless helper classes (`JsonUtil`,
  `StringToInputStream`, `TestSecurityUtils`, `CollectionMatchers`).
- **Hamcrest over AssertJ** — all custom matchers extend
  `TypeSafeMatcher<T>` and integrate with `assertThat()`.
- **Published as `main` source** — despite being test utilities, sources
  live under `src/main/java` so consuming modules can declare this as a
  `testImplementation` dependency.

## Consuming Modules

This library is a `testImplementation` dependency in: `game-core`,
`game-headed`, `game-relay-server`, `smoke-testing`, `lobby-client`,
`swing-lib`, `java-extras`, `feign-common`, and `websocket-server`.
Additionally, `swing-lib-test-support` uses it as an `implementation`
dependency to re-export `CustomMatcher`.

## Build

Dependencies: Hamcrest, JUnit 5 API, and Jackson Datatype JSR-310.
No transitive runtime dependencies — this module is test-only.
