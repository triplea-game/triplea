package games.strategy.triplea.ai.proAI.logging;

import java.util.logging.Level;

import javax.swing.SwingUtilities;

import games.strategy.triplea.ui.TripleAFrame;

/**
 * Class to manage log window display.
 */
public class ProLogUI {
  private static ProLogWindow s_settingsWindow = null;
  private static String currentName = "";
  private static int currentRound = 0;

  public static void initialize(final TripleAFrame frame) {
    if (!SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Wrong thread, should be running on AWT thread.");
    }
    s_settingsWindow = new ProLogWindow(frame);
  }

  public static void clearCachedInstances() {
    if (s_settingsWindow != null) {
      s_settingsWindow.clear();
    }
    s_settingsWindow = null;
  }

  public static void showSettingsWindow() {
    if (s_settingsWindow == null) {
      return;
    }
    s_settingsWindow.setVisible(true);
  }

  static void notifyAILogMessage(final Level level, final String message) {
    if (s_settingsWindow == null) {
      return;
    }
    s_settingsWindow.addMessage(level, message);
  }

  public static void notifyStartOfRound(final int round, final String name) {
    if (s_settingsWindow == null) {
      return;
    }
    if (round != currentRound || !name.equals(currentName)) {
      currentRound = round;
      currentName = name;
      s_settingsWindow.notifyNewRound(round, name);
    }
  }
}
