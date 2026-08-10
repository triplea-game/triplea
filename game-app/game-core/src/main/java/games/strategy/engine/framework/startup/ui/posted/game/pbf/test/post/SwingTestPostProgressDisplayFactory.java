package games.strategy.engine.framework.startup.ui.posted.game.pbf.test.post;

import java.util.function.Supplier;
import org.triplea.swing.ProgressWindow;
import org.triplea.swing.SwingComponents;

public class SwingTestPostProgressDisplayFactory implements Supplier<TestPostProgressDisplay> {

  @Override
  public TestPostProgressDisplay get() {
    final ProgressWindow progressWindow =
        new ProgressWindow(null, "Testing... This may take a while");

    progressWindow.setVisible(true);

    return new TestPostProgressDisplay() {
      @Override
      public void showSuccess(final String message) {
        SwingComponents.showDialog(null, message, "Test Turn Summary Post");
      }

      @Override
      public void showFailure(final Throwable throwable) {
        SwingComponents.showWarning(null, throwable.getMessage(), "Test Turn Summary Post");
      }

      @Override
      public void close() {
        progressWindow.setVisible(false);
      }
    };
  }
}
