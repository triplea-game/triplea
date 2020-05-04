package games.strategy.triplea.ai.pro.logging;

import games.strategy.triplea.ui.TripleAFrame;
import java.util.logging.Level;
import javax.swing.SwingUtilities;

/** Class to manage log window display. */
public final class ProLogUi {
  private static ProLogWindow settingsWindow = null;
  private static String currentName = "";
  private static int currentRound = 0;

  private ProLogUi() {}

  public static void initialize(final TripleAFrame frame) {
    if (!SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Wrong thread, should be running on AWT thread.");
    }
    settingsWindow = new ProLogWindow(frame);
  }

  public static void clearCachedInstances() {
    if (settingsWindow != null) {
      settingsWindow.dispose();
    }
    settingsWindow = null;
  }

  public static void showSettingsWindow() {
    if (settingsWindow == null) {
      return;
    }
    settingsWindow.setVisible(true);
  }

  static void notifyAiLogMessage(final String message) {
    if (settingsWindow == null) {
      return;
    }
    settingsWindow.addMessage(message);
  }

  public static void notifyStartOfRound(final int round, final String name) {
    if (settingsWindow == null) {
      return;
    }
    if (round != currentRound || !name.equals(currentName)) {
      currentRound = round;
      currentName = name;
      settingsWindow.notifyNewRound(round, name);
    }
  }
}
