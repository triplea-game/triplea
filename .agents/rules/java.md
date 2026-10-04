---
paths:
  - "**/*.java"
---

# Java rules

These are requirements, not suggestions. Items marked *(lint)* are checked on every added line
by `.agents/scripts/convention_lint.py` (part of `check_changes.py`); items marked *(build)* fail
`./gradlew check` or check-custom-style. The rest are checked in `code-review`.

## Style and tooling

- MUST be Google Java Style; formatting is whatever `spotlessApply` produces *(build)*.
- NEVER use star imports; import inner classes instead of qualifying them (`Inner.call()`, not
  `Outer.Inner.call()`) *(build)*.
- NEVER leave unused parameters, locals or private fields *(build, PMD)*.
- Switch statements MUST have a `default`; no fall-through *(build)*.
- An empty catch block MUST name its variable `expected` *(build)*.
- Null annotations: `javax.annotation.Nonnull`, NEVER `lombok.NonNull` *(build)*.
- Logging: Lombok `@Slf4j` only, NEVER `@Log` or `java.util.logging` *(lint)*; no `@Slf4j`
  without a `log.` use *(build)*.

## API design

- NEVER return `null`; return `Optional`. Where `null` is unavoidable, annotate it `@Nullable`.
- NEVER take `Optional` parameters; add an overload without the parameter *(lint)*.
- NEVER add `boolean` parameters to public or package APIs; use overloads, an enum, or factory
  methods (`MyObject.newValidating()`). Exception: an `@Override` of an API we do not own.
- Factory methods are named `newFoo()`, NEVER `createFoo()`.
- Inject dependencies through the constructor; NEVER add setters for them. Fields `final`.
- Use the most restrictive visibility that compiles (private > package > protected > public).
- Methods that use no instance state MUST be `static`.
- Avoid static coupling: pass a `Supplier`/interface instead of calling a static service.

## Code body

- NEVER mutate parameters or use out/in-out parameters; copy and return a new value.
- Prefer immutable locals: `int value = condition ? 3 : 2;`, not reassigning after `if`.
- Declare variables right before first use.
- Step-down ordering: a method sits directly below its first caller; overloads stay together.
- Collections: `List.of()` / `Set.of()` / `Map.of()`, NEVER `Collections.empty*`,
  `Collections.singleton*`, `Collections.unmodifiable*` or `Arrays.asList` *(lint)*.
- Declare by interface: `List<String> names = new ArrayList<>()`, NEVER `ArrayList<String> names`
  *(lint)*.
- `isEmpty()` instead of comparing `size()` with 0 or 1 *(lint)*.
- `var` only when the name or initializer makes the type obvious (`var diceRoll = new DiceRoll(..)`),
  NEVER for `var hits = new DiceRoll(..)`.
- Every TODO MUST carry a tracking token: `TODO: Issue#1234`, `TODO: Project#12`,
  `TODO: Topic#1183` *(lint)*.
- Lombok (`@Getter`, `@Builder`, `@Value`, `@AllArgsConstructor`, ...) over handwritten boilerplate.
