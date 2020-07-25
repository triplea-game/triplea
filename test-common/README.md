# test-util

Common test utilities, meant to be included as a test dependency
in all other projects.

## Examples

### Custom Hamcrest Matcher

```java
  @Test
  void validateArgsValidCase() {
    assertThat(
        "With all args supplied we should see a valid object result: " + Arrays.toString(givenCompleteArgs()),
        HeadlessGameServerCliParam.validateArgs(givenCompleteArgs()),
        expectValid(true));
  }
   private static Matcher<ArgValidationResult> expectValid(final boolean valid) {
    return CustomMatcher.<ArgValidationResult>builder()
        .description("Expecting result to be valid? " + valid)
        .checkCondition(result -> result.isValid() == valid)
        .debug(result -> "Optional Custom debug info of actual value tested,"
        + " a 'result#toString' is default")
        .build();
   }
```
