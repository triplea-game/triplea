package games.strategy.debug.error.reporting;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.test.common.swing.SwingComponentWrapper;

@ExtendWith(MockitoExtension.class)
class ErrorReportWindowTest {

  @BeforeAll
  static void skipIfHeadless() {
    assumeFalse(GraphicsEnvironment.isHeadless());
    assumeFalse(Boolean.valueOf(System.getProperty("java.awt.headless", "false")));
  }

  /**
   * Verify that all expected components have been added.
   */
  @Test
  void errorReportWindowContainsExpectedComponents() {
    final ErrorReportWindow errorReportWindow = new ErrorReportWindow(data -> {
    });
    final SwingComponentWrapper helper = SwingComponentWrapper.of(errorReportWindow.buildWindow());

    Arrays.stream(ErrorReportComponents.Names.values())
        .forEach(componentName -> helper.assertHasComponentByName(componentName.toString()));
  }
}
