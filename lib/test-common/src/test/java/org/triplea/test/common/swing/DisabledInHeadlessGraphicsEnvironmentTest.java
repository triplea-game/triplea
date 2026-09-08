package org.triplea.test.common.swing;

import static org.assertj.core.api.Assertions.assertThat;
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

      assertThat(result.isDisabled()).isTrue();
      assertThat(result.getReason()).contains("Test disabled in headless graphics environment");
    }

    @Test
    void shouldReturnEnabledWhenGraphicsEnvironmentIsHeaded() {
      final ConditionEvaluationResult result = evaluateExecutionCondition(false);

      assertThat(result.isDisabled()).isFalse();
      assertThat(result.getReason()).isEmpty();
    }
  }
}
