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
import games.strategy.ui.SwingComponents;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;

public class DebugMenu {

  public DebugMenu(final MenuBar menuBar, final TripleAFrame frame) {
    final Menu debugMenu = new Menu("_Debug");
    menuBar.getMenus().add(debugMenu);

    final Set<IGamePlayer> players = frame.getLocalPlayers().getLocalPlayers();
    final boolean areThereProAIs = players.stream().filter(player -> player instanceof ProAI).findFirst().isPresent();
    if (areThereProAIs) {
      ProAI.initialize(frame);
      debugMenu.getItems().add(SwingAction.of("Show Hard AI Logs", e -> ProAI.showSettingsWindow())).setMnemonic(KeyEvent.VK_X);
    }

    debugMenu.getItems().add(new EnablePerformanceLoggingCheckBox());
    debugMenu.getItems().add(SwingAction.of("Show Console", e -> {
      ErrorConsole.getConsole().setVisible(true);
      ErrorConsole.getConsole().append(DebugUtils.getMemory());
    })).setMnemonic(KeyEvent.VK_C);
  }
}
