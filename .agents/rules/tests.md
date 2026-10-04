---
paths:
  - "**/src/test/**"
  - "**/src/testFixtures/**"
---

# Test rules

- JUnit 5 only. Assertions MUST be AssertJ `assertThat`; NEVER add JUnit `assertEquals`/
  `assertTrue`/... or Hamcrest to new code *(lint)*. Existing tests may keep their style; a test
  you rewrite moves to AssertJ.
- Unless trivial, an assertion explains why with `.as("3 units added, one filtered")`.
- Every new test method gets `@DisplayName`.
- Structure tests as given / when / then; pull setup into `given...()` helper methods.
- Mockito and matchers are statically imported: `when(..)`, `verify(..)`, NEVER `Mockito.when`
  *(build, check-custom-style)*.
- HTTP is stubbed with WireMock, never real network calls.
- Game data comes from `TestMapGameData` + `TestMapGameDataLoader` (test fixtures of
  `:game-core`); NEVER hand-build a `GameData` when a test map fits.
- A behaviour change MUST come with a test that fails without it. Run the module's tests with
  the `build` skill before claiming they pass.
