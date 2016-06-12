package games.strategy.triplea.ui.menubar;

import java.awt.event.KeyEvent;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

import games.strategy.debug.DebugUtils;
import games.strategy.debug.ErrorConsole;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.performance.EnablePerformanceLoggingCheckBox;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.ui.SwingAction;

public class DebugMenu {

  public DebugMenu(final JMenuBar menuBar, final TripleAFrame frame) {
    final JMenu debugMenu = new JMenu("Debug");
    menuBar.add(debugMenu);
    addChangeProAISettings(debugMenu, frame);
    debugMenu.add(new EnablePerformanceLoggingCheckBox());
    debugMenu.setMnemonic(KeyEvent.VK_D);
    addConsoleMenu(debugMenu);
  }

  private void addChangeProAISettings(final JMenu parentMenu, final TripleAFrame frame) {
    boolean areThereProAIs = false;
    final Set<IGamePlayer> players = frame.getLocalPlayers().getLocalPlayers();
    for (final IGamePlayer player : players) {
      if (player instanceof ProAI) {
        areThereProAIs = true;
      }
    }
    if (areThereProAIs) {
      ProAI.initialize(frame);
      parentMenu.add(SwingAction.of("Show Hard AI Logs", e -> ProAI.showSettingsWindow())).setMnemonic(KeyEvent.VK_X);
    }
  }

  private void addConsoleMenu(final JMenu parentMenu) {
    parentMenu.add(SwingAction.of("Show Console...", e -> {
      ErrorConsole.getConsole().setVisible(true);
      ErrorConsole.getConsole().append(DebugUtils.getMemory());
    })).setMnemonic(KeyEvent.VK_C);
  }

}
