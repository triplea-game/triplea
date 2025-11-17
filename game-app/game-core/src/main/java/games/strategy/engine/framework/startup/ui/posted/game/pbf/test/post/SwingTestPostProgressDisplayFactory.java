package games.strategy.engine.framework.startup.ui.posted.game.pbf.test.post;

import java.util.function.Supplier;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.triplea.swing.ProgressWindow;

public class SwingTestPostProgressDisplayFactory implements Supplier<TestPostProgressDisplay> {

  @Override
  public TestPostProgressDisplay get() {
    final ProgressWindow progressWindow =
        new ProgressWindow(null, "Testing... This may take a while");

    progressWindow.setVisible(true);

    return new TestPostProgressDisplay() {
      @Override
      public void showSuccess(final String message) {
        SwingUtilities.invokeLater(
            () ->
                JOptionPane.showMessageDialog(
                    progressWindow,
                    message,
                    "Test Turn Summary Post",
                    JOptionPane.INFORMATION_MESSAGE));
      }

      @Override
      public void showFailure(final Throwable throwable) {
        SwingUtilities.invokeLater(
            () ->
                JOptionPane.showMessageDialog(
                    progressWindow,
                    throwable.getMessage(),
                    "Test Turn Summary Post",
                    JOptionPane.WARNING_MESSAGE));
      }

      @Override
      public void close() {
        progressWindow.setVisible(false);
      }
    };
  }
}
