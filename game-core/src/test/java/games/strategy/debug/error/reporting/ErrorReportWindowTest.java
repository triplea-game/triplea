package games.strategy.debug.error.reporting;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.test.common.swing.DisabledInHeadlessGraphicsEnvironment;
import org.triplea.test.common.swing.SwingComponentWrapper;

@ExtendWith(DisabledInHeadlessGraphicsEnvironment.class)
class ErrorReportWindowTest {
  /** Verify that all expected components have been added. */
  @Test
  void errorReportWindowContainsExpectedComponents() {
    final ErrorReportWindow errorReportWindow = new ErrorReportWindow((frame, data) -> {});
    final SwingComponentWrapper helper = SwingComponentWrapper.of(errorReportWindow.buildWindow());

    Arrays.stream(ErrorReportComponents.Names.values())
        .forEach(componentName -> helper.assertHasComponentByName(componentName.toString()));
  }
}
