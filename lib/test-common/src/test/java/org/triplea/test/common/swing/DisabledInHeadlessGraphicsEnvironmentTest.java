package org.triplea.test.common.swing;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.triplea.test.common.swing.DisabledInHeadlessGraphicsEnvironment.evaluateExecutionCondition;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;

final class DisabledInHeadlessGraphicsEnvironmentTest {
  @Nested
  final class EvaluateExecutionConditionTest {
    @Test
    void shouldReturnDisabledWhenGraphicsEnvironmentIsHeadless() {
      final ConditionEvaluationResult result = evaluateExecutionCondition(true);

      assertThat(result.isDisabled(), is(true));
      assertThat(
          result.getReason(), isPresentAndIs("Test disabled in headless graphics environment"));
    }

    @Test
    void shouldReturnEnabledWhenGraphicsEnvironmentIsHeaded() {
      final ConditionEvaluationResult result = evaluateExecutionCondition(false);

      assertThat(result.isDisabled(), is(false));
      assertThat(result.getReason(), isEmpty());
    }
  }
}
