package games.strategy.triplea.ai.pro.logging;

import games.strategy.engine.framework.GameShutdownRegistry;
import games.strategy.triplea.ui.menubar.debug.AiPlayerDebugOption;
import games.strategy.ui.Util;
import java.awt.Frame;
import java.util.List;
import javax.swing.SwingUtilities;
import lombok.experimental.UtilityClass;
import org.triplea.swing.key.binding.KeyCode;

/** Builds the Hard AI debug menu and manages the AI log window it shows. */
@UtilityClass
public final class ProLogDebugMenu {
  private static ProLogWindow settingsWindow = null;

  private static final ProLogDisplay windowDisplay =
      new ProLogDisplay() {
        @Override
        public void addMessage(final String message) {
          SwingUtilities.invokeLater(
              () -> {
                if (settingsWindow != null) {
                  settingsWindow.addMessage(message);
                }
              });
        }

        @Override
        public void notifyNewRound(final int round, final String playerName) {
          final ProLogWindow window = settingsWindow;
          if (window != null) {
            window.notifyNewRound(round, playerName);
          }
        }
      };

  public static List<AiPlayerDebugOption> buildDebugOptions(final Frame frame) {
    Util.ensureOnEventDispatchThread();
    if (settingsWindow == null) {
      settingsWindow = new ProLogWindow(frame);
      ProLogUi.setLogDisplay(windowDisplay);
      GameShutdownRegistry.registerShutdownAction(ProLogDebugMenu::clearCachedInstances);
    }
    ProLogger.info("Initialized Hard AI");
    return List.of(
        AiPlayerDebugOption.builder()
            .title("Show Logs")
            .actionListener(action -> showSettingsWindow())
            .mnemonic(KeyCode.X.getInputEventCode())
            .build());
  }

  private static void clearCachedInstances() {
    ProLogUi.setLogDisplay(null);
    if (settingsWindow != null) {
      settingsWindow.dispose();
    }
    settingsWindow = null;
  }

  private static void showSettingsWindow() {
    if (settingsWindow == null) {
      return;
    }
    ProLogger.info("Showing Hard AI settings window");
    settingsWindow.setVisible(true);
  }
}
