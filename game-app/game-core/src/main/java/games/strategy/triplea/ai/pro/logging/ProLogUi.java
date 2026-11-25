package games.strategy.triplea.ai.pro.logging;

import games.strategy.engine.framework.GameShutdownRegistry;
import games.strategy.triplea.ui.menubar.debug.AiPlayerDebugAction;
import games.strategy.triplea.ui.menubar.debug.AiPlayerDebugOption;
import games.strategy.ui.Util;
import java.awt.Frame;
import java.util.List;
import javax.swing.SwingUtilities;
import lombok.experimental.UtilityClass;
import org.triplea.swing.key.binding.KeyCode;

/** Class to manage log window display. */
@UtilityClass
public final class ProLogUi {
  private static ProLogWindow settingsWindow = null;
  private static String currentName = "";
  private static int currentRound = 0;

  public static List<AiPlayerDebugOption> buildDebugOptions(final Frame frame) {
    Util.ensureOnEventDispatchThread();
    if (settingsWindow == null) {
      settingsWindow = new ProLogWindow(frame);
      GameShutdownRegistry.registerShutdownAction(ProLogUi::clearCachedInstances);
    }
    ProLogger.info("Initialized Hard AI");
    return List.of(
        AiPlayerDebugOption.builder()
            .title("Show Logs")
            .actionListener(ProLogUi::showSettingsWindow)
            .mnemonic(KeyCode.X.getInputEventCode())
            .build());
  }

  public static void clearCachedInstances() {
    if (settingsWindow != null) {
      settingsWindow.dispose();
    }
    settingsWindow = null;
  }

  public static void showSettingsWindow(AiPlayerDebugAction aiPlayerDebugAction) {
    if (settingsWindow == null) {
      return;
    }
    ProLogger.info("Showing Hard AI settings window");
    settingsWindow.setVisible(true);
  }

  static void notifyAiLogMessage(final String message) {
    SwingUtilities.invokeLater(
        () -> {
          if (settingsWindow != null) {
            settingsWindow.addMessage(message);
          }
        });
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
