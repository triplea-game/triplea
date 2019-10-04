package org.triplea.test.common.swing;

import com.google.common.annotations.VisibleForTesting;
import java.awt.GraphicsEnvironment;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/** An extension that disables tests when run in a headless graphics environment. */
public final class DisabledInHeadlessGraphicsEnvironment implements ExecutionCondition {
  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(final ExtensionContext context) {
    return evaluateExecutionCondition(GraphicsEnvironment.isHeadless());
  }

  @VisibleForTesting
  static ConditionEvaluationResult evaluateExecutionCondition(final boolean headless) {
    return headless
        ? ConditionEvaluationResult.disabled("Test disabled in headless graphics environment")
        : ConditionEvaluationResult.enabled(null);
  }
}
