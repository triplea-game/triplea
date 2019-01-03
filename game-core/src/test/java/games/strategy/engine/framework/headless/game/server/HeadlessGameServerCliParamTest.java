package games.strategy.engine.framework.headless.game.server;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.triplea.test.common.CustomMatcher;

class HeadlessGameServerCliParamTest {

  @Test
  void validateArgsTrivialFalseCase() {
    assertThat(
        "With all args missing, result should not be valid",
        HeadlessGameServerCliParam.validateArgs(),
        expectValid(false));
  }

  @Test
  void validateArgsValidCase() {
    assertThat(
        "With all args supplied we should see a valid object result: "
            + Arrays.toString(givenCompleteArgs()),
        HeadlessGameServerCliParam.validateArgs(givenCompleteArgs()),
        expectValid(true));
  }

  private static Matcher<ArgValidationResult> expectValid(final boolean valid) {
    return CustomMatcher.<ArgValidationResult>builder()
        .description("Expecting result to be valid? " + valid)
        .checkCondition(result -> result.isValid() == valid)
        .build();
  }

  private static String[] givenCompleteArgs() {
    return Arrays.stream(HeadlessGameServerCliParam.values())
        .map(value -> "-P" + value + "=value")
        .toArray(String[]::new);
  }
}
