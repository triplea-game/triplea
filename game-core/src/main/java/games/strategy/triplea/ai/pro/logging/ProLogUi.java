package games.strategy.triplea.ai.pro.logging;

import games.strategy.engine.framework.GameShutdownRegistry;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.menubar.DebugMenu;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.List;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import org.triplea.swing.SwingAction;

/** Class to manage log window display. */
public final class ProLogUi {
  private static boolean registered = false;
  private static ProLogWindow settingsWindow = null;
  private static String currentName = "";
  private static int currentRound = 0;

  private ProLogUi() {}

  public static void registerDebugMenu() {
    if (!registered) {
      DebugMenu.registerMenuCallback("Hard AI", ProLogUi::initialize);
      registered = true;
    }
  }

  private static Collection<JMenuItem> initialize(final TripleAFrame frame) {
    if (!SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Wrong thread, should be running on AWT thread.");
    }
    if (settingsWindow == null) {
      settingsWindow = new ProLogWindow(frame);
      GameShutdownRegistry.registerShutdownAction(ProLogUi::clearCachedInstances);
    }

    ProLogger.info("Initialized Hard AI");
    final JMenuItem logMenuItem =
        new JMenuItem(SwingAction.of("Show Logs", ProLogUi::showSettingsWindow));
    logMenuItem.setMnemonic(KeyEvent.VK_X);
    return List.of(logMenuItem);
  }

  public static void clearCachedInstances() {
    if (settingsWindow != null) {
      settingsWindow.dispose();
    }
    registered = false;
    settingsWindow = null;
  }

  public static void showSettingsWindow() {
    if (settingsWindow == null) {
      return;
    }
    ProLogger.info("Showing Hard AI settings window");
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
