## Favor adding `@DisplayName` to each test case [#5525](https://github.com/triplea-game/triplea/issues/5525)

## Favor AssertJ assertThat for new tests  [#5526](https://github.com/triplea-game/triplea/issues/5526)

Use AssertJ's fluent `assertThat` syntax.
- Keep existing tests consistent
- New tests should use AssertJ

### Example

Instead of:
```
assertEquals(6, mfb.getAttackingUnits().size());
```
Use:
```
assertThat(mfb.getAttackingUnits()).hasSize(6);
```

## Testing, favor using helper functions  for "given, when, then" like structures to tests [#5489](https://github.com/triplea-game/triplea/issues/5489)

IE: we want to try and have tests that are as much as possible like:
```
void test() {
  givenConditionsReturningValue("value");
  String result = someObject.doSomething();
  assertThat(result).isEqualTo("value");
}
```

## Use the `as` description to explain why we assert a condition [#5527](https://github.com/triplea-game/triplea/issues/5527)

Unless completely and absolutely trivial, favor the majority of the time to use AssertJ's `as` (or `describedAs`) description to explain why we expect an asserted condition to be true.

Example, instead of:
```
assertThat(collection).hasSize(2);
```

Prefer:
```
assertThat(collection).as("3 units added, one unit filtered, we expect to have 2").hasSize(2);
```
