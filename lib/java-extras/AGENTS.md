# lib/java-extras

General-purpose Java utility library providing syntactic sugar and helpers used
across the entire TripleA project. Published as a Maven artifact — changes must
maintain backward compatibility.

## Package Layout

| Package | Purpose |
|---------|---------|
| `org.triplea.io` | File operations, HTTP downloads, zip extraction, stream utilities |
| `org.triplea.java` | String/date/color utilities, argument validation, retry logic |
| `org.triplea.java.cache.ttl` | Caffeine-backed TTL cache with expiry listeners |
| `org.triplea.java.collections` | `IntegerMap` (counting map) and collection helpers |
| `org.triplea.java.concurrency` | Thread-safe interrupt handling, async runners, latch management |
| `org.triplea.java.exception` | `UnhandledSwitchCaseException` for exhaustive switch defaults |
| `org.triplea.java.function` | `ThrowingFunction`, `ThrowingConsumer`, etc. — functional interfaces that permit checked exceptions |
| `org.triplea.java.timer` | Fluent builder for fixed-rate scheduled timers |
| `org.triplea.performance` | `PerfTimer` for lightweight timing measurements |
| `org.triplea.yaml` | YAML reading/writing via SnakeYAML engine |
| `games.strategy.engine.framework.system` | `SystemProperties` — typed wrapper around `System.getProperty` |

## Key Classes

### I/O and Network

- **`FileUtils`** — static helpers for reading (UTF-8, ISO-8859-1 fallback),
  writing, deleting, listing, and atomically replacing folders.
- **`ZipExtractor`** — secure zip extraction with Zip-Slip path traversal
  protection and a `MAX_DEPTH=10` limit to prevent zip-bomb recursion.
- **`ContentDownloader`** — HTTP GET with single retry on `IOException` and
  proxy support. Implements `CloseableDownloader` for try-with-resources.
- **`IoUtils`** — in-memory stream round-trips (write bytes to memory, read
  them back).

### Language Extensions

- **`Interruptibles`** — wraps `InterruptedException`-throwing operations and
  always re-interrupts the thread. Use `await()`, `awaitResult()`, `join()`,
  `sleep()`.
- **`Retriable<T>`** — fluent builder for retrying a task with fixed backoff:
  `Retriable.builder().withMaxAttempts(3).withFixedBackOff(Duration.ofSeconds(1)).withTask(task).buildAndExecute()`.
- **`ThrowingFunction` / `ThrowingConsumer` / `ThrowingSupplier` /
  `ThrowingRunnable` / `ThrowingBiFunction`** — functional interfaces that
  declare `throws Exception`, filling a gap in `java.util.function`.
- **`PredicateBuilder`** — compose predicates with fluent `and()`, `or()`,
  `andIf()`, `orIf()`.
- **`AlphanumComparator`** — natural sort order (`"foo9"` before `"foo10"`).

### Data Structures

- **`IntegerMap<T>`** — `Serializable` counting map used throughout game logic.
  `getInt(key)` returns `0` for missing keys (not `Optional`). Supports
  `add()`, `multiplyAllValuesBy()`, `totalValues()`, `greaterThanOrEqualTo()`.
- **`ExpiringAfterWriteTtlCache`** — Caffeine-backed cache implementing
  `TtlCache`. Fires removal listeners on expiry.

### Utilities

- **`StringUtils`** — capitalize, int parsing, truncation, null/blank checks.
- **`CollectionUtils`** — filtering, intersection, difference, counting
  matches. Thread-safe.
- **`DateTimeUtil` / `DateTimeFormatterUtil`** — localized time formatting,
  epoch-millis conversion.
- **`Sha512Hasher`** — hashes passwords with a `"TripleA"` salt prefix.
- **`ColorUtils`** — hex-to-Color parsing, seeded random color generation
  (HSB-based).
- **`Timers`** — factory for fixed-rate timers:
  `Timers.fixedRateTimer("name").period(5, TimeUnit.SECONDS).task(runnable)`.
- **`PerfTimer`** — lightweight timing with configurable log frequency.
  Supports try-with-resources and inline lambda usage.

## Conventions

- **Utility classes use Lombok `@UtilityClass`** — private constructor, all
  methods static. Most classes in this module follow this pattern.
- **Checked exceptions are wrapped** — I/O methods convert `IOException` to
  `RuntimeException` so callers don't need try/catch. Use the `Throwing*`
  functional interfaces when you need to pass checked-exception lambdas.
- **`Optional<T>` over null** — return types use `Optional` to model absence.
- **Thread-safety annotations** — classes like `CountDownLatchHandler` and
  `CollectionUtils` use `@ThreadSafe` and `@GuardedBy`.
- **Logging** — `@Slf4j` (Lombok). I/O failures generally log warn/error
  rather than throwing.

## Gotchas

- `FileUtils.readContents()` silently falls back from UTF-8 to ISO-8859-1 on
  `MalformedInputException` (logs a warning).
- `IntegerMap.getInt(key)` returns `0` for absent keys — not `null`, not
  `Optional`.
- `DateTimeFormatterUtil.setDefaultToUtc()` mutates static state; it exists
  for testing but can affect other code if called carelessly.
- `ContentDownloader` retries only once with a 1-second sleep — not suitable
  for unreliable networks without an outer retry wrapper.

## Testing

JUnit 5 with Hamcrest matchers. Parameterized tests are common
(`@ParameterizedTest` with `@ValueSource` / `@MethodSource`). Test doubles are
injected via constructor parameters (e.g., `UrlStreams` accepts a connection
factory, `Retriable` accepts a sleep consumer to avoid real delays in tests).

## Build

Published as a Maven artifact via `maven-publish` plugin. Version is set from
the `JAR_VERSION` environment variable. Test dependency on `:lib:test-common`.
