# .build/ — Static Analysis & Code Style Configs

This directory holds the PMD ruleset and IDE formatter settings (Eclipse).

## Files

| File | Purpose | Applied via |
|------|---------|-------------|
| `pmd.xml` | PMD rules: naming, switches, empty blocks, unused code | `triplea-base-project` convention plugin; any violation fails the build |
| `eclipse/` | Eclipse IDE formatter and import order settings | Manual IDE import (not build-enforced) |

## code-convention-checks/

The `check-custom-style` bash script runs grep-based style checks beyond what
PMD covers. It is **not wired into the Gradle build** — the root
`verify` script (and so CI) runs it after Gradle.

### Active checks (enabled in the script)
- **Unused `@Slf4j` annotations** — flags files with `@Slf4j` but no `log.` usage
- **Static imports in tests** — flags `Mockito.when(...)` etc. that should be statically imported
- **`javax.annotation.Nonnull` over `lombok.NonNull`** — enforces consistent null annotation
- **LF line endings in git** — flags text files committed with CRLF (fix with `git add --renormalize <file>`)

### Disabled checks (commented out, have existing violations)
- Prefer `List.of()`/`Map.of()`/`Set.of()` over `Collections.*` methods
- Assign collections to interface types (e.g. `List` not `ArrayList`)
- Prefer `@Slf4j` over `@Log`/`java.util.logging`

### Style include files
- `style.include/concrete-collection-types` — grep patterns for concrete collection assignments
- `style.include/disallowed-collection-calls` — grep patterns for legacy `Collections.*` calls

## Key rules to follow when writing Java code

- **Naming**: camelCase members/params/locals; type parameters are one capital letter or end in `T` (eg: `ViewDataT`)
- **Switch statements** must be exhaustive or have a default case; fall-through is flagged
- **Empty catch blocks** must name the variable `expected` or `ignored`, or hold a comment
- **No unused** parameters, local variables, or private fields
- **Use `javax.annotation.Nonnull`**, not `lombok.NonNull`
- **Static import** test utilities: `when(...)` not `Mockito.when(...)`
- Star imports and modifier order are fixed by `./gradlew spotlessApply`
