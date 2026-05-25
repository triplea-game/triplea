# .build/ — Static Analysis & Code Style Configs

This directory holds configuration files for static analysis tools (Checkstyle, PMD)
and IDE formatters (Eclipse). These are consumed by the root `build.gradle.kts`.

## Files

| File | Purpose | Applied via |
|------|---------|-------------|
| `checkstyle.xml` | Checkstyle rules (modified Google Java Style) | `build.gradle.kts` lines 76-91; zero warnings allowed |
| `pmd.xml` | PMD rules: unused params, locals, private fields | `build.gradle.kts` lines 101-107 |
| `eclipse/` | Eclipse IDE formatter and import order settings | Manual IDE import (not build-enforced) |

## code-convention-checks/

The `check-custom-style` bash script runs grep-based style checks beyond what
Checkstyle/PMD cover. It is **not wired into the Gradle build** — it must be run
manually.

### Active checks (enabled in the script)
- **Unused `@Slf4j` annotations** — flags files with `@Slf4j` but no `log.` usage
- **Static imports in tests** — flags `Mockito.when(...)` etc. that should be statically imported
- **`javax.annotation.Nonnull` over `lombok.NonNull`** — enforces consistent null annotation

### Disabled checks (commented out, have existing violations)
- Prefer `List.of()`/`Map.of()`/`Set.of()` over `Collections.*` methods
- Assign collections to interface types (e.g. `List` not `ArrayList`)
- Prefer `@Slf4j` over `@Log`/`java.util.logging`

### Style include files
- `style.include/concrete-collection-types` — grep patterns for concrete collection assignments
- `style.include/disallowed-collection-calls` — grep patterns for legacy `Collections.*` calls

## Key rules to follow when writing Java code

- **Naming**: camelCase members/params/locals; method names `^[a-z][a-z0-9][a-zA-Z0-9_]*$`
- **No star imports**, no finalizers, modifier order matters
- **Empty catch blocks** must name the variable `expected`
- **Switch statements** require a default case; fall-through is flagged
- **No unused** parameters, local variables, or private fields (PMD)
- **Use `javax.annotation.Nonnull`**, not `lombok.NonNull`
- **Static import** test utilities: `when(...)` not `Mockito.when(...)`
