package games.strategy.triplea.ai.proAI.logging;

import java.util.logging.Level;

import javax.swing.SwingUtilities;

import games.strategy.triplea.ui.TripleAFrame;

/**
 * Class to manage log window display.
 */
public class ProLogUi {
  private static ProLogWindow settingsWindow = null;
  private static String currentName = "";
  private static int currentRound = 0;

  public static void initialize(final TripleAFrame frame) {
    if (!SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Wrong thread, should be running on AWT thread.");
    }
    settingsWindow = new ProLogWindow(frame);
  }

  public static void clearCachedInstances() {
    if (settingsWindow != null) {
      settingsWindow.clear();
    }
    settingsWindow = null;
  }

  public static void showSettingsWindow() {
    if (settingsWindow == null) {
      return;
    }
    settingsWindow.setVisible(true);
  }

  static void notifyAiLogMessage(final Level level, final String message) {
    if (settingsWindow == null) {
      return;
    }
    settingsWindow.addMessage(level, message);
  }

  public static void notifyStartOfRound(final int round, final String name) {
    if (settingsWindow == null) {
      return;
    }
    if ((round != currentRound) || !name.equals(currentName)) {
      currentRound = round;
      currentName = name;
      settingsWindow.notifyNewRound(round, name);
    }
  }
}
