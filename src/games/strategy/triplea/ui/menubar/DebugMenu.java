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

  private final TripleAFrame frame;

  public DebugMenu(final JMenuBar menuBar, TripleAFrame frame) {
    this.frame = frame;

    final JMenu debugMenu = new JMenu("Debug");
    menuBar.add(debugMenu);
    debugMenu.addSeparator();
    addChangeProAISettings(debugMenu);
    debugMenu.addSeparator();
    debugMenu.add(new EnablePerformanceLoggingCheckBox());
    debugMenu.setMnemonic(KeyEvent.VK_D);
    addConsoleMenu(debugMenu);
  }

  private void addConsoleMenu(final JMenu parentMenu) {
    parentMenu.add(SwingAction.of("Show Console...", e ->  {
      ErrorConsole.getConsole().setVisible(true);
      ErrorConsole.getConsole().append(DebugUtils.getMemory());
    })).setMnemonic(KeyEvent.VK_C);
  }

  private void addChangeProAISettings(final JMenu parentMenu) {
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

}
