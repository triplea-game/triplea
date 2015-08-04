package games.strategy.triplea.ai.Dynamix_AI.UI;

import java.util.logging.Level;

import javax.swing.SwingUtilities;

import games.strategy.triplea.ui.TripleAFrame;


public class UI {
  public static void Initialize(final TripleAFrame frame) {
    if (!SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Wrong thread, should be running on AWT thread.");
    }
    s_frame = frame;
    s_settingsWindow = new SettingsWindow(frame);
  }

  public static void clearCachedInstances() {
    s_frame = null;
    if (s_settingsWindow != null) {
      s_settingsWindow.clear();
    }
    s_settingsWindow = null;
  }

  private static TripleAFrame s_frame = null;
  private static SettingsWindow s_settingsWindow = null;

  public static void ShowSettingsWindow() {
    if (s_settingsWindow == null) {
      return;
    }
    s_settingsWindow.setVisible(true);
    s_settingsWindow.setLocationRelativeTo(s_frame);
  }

  public static void NotifyAILogMessage(final Level level, final String message) {
    if (s_settingsWindow == null) {
      return;
    }
    s_settingsWindow.addMessage(level, message);
  }

  public static void NotifyStartOfRound(final int round) {
    if (s_settingsWindow == null) {
      return;
    }
    s_settingsWindow.notifyNewRound(round);
  }
}
