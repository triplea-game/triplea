package games.strategy.triplea.ui.menubar;

import java.util.Set;

import games.strategy.debug.DebugUtils;
import games.strategy.debug.ErrorConsole;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.performance.EnablePerformanceLoggingCheckBox;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.ui.TripleAFrame;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;

public class DebugMenu {

  public DebugMenu(final MenuBar menuBar, final TripleAFrame frame) {
    final Menu debugMenu = new Menu("_Debug");
    menuBar.getMenus().add(debugMenu);

    final Set<IGamePlayer> players = frame.getLocalPlayers().getLocalPlayers();
    final boolean areThereProAIs = players.stream().filter(player -> player instanceof ProAI).findFirst().isPresent();
    if (areThereProAIs) {
      ProAI.initialize(frame);
      MenuItem showAILogs = new MenuItem("Show Hard AI _Logs");
      showAILogs.setMnemonicParsing(false);
      showAILogs.setOnAction(e -> ProAI.showSettingsWindow());
      debugMenu.getItems().add(showAILogs);
    }

    debugMenu.getItems().add(new EnablePerformanceLoggingCheckBox());
    MenuItem showConsole = new MenuItem("Show _Console");
    showConsole.setOnAction(e -> {
      ErrorConsole.getConsole().setVisible(true);
      ErrorConsole.getConsole().append(DebugUtils.getMemory());
    });
    showConsole.setMnemonicParsing(true);
    debugMenu.getItems().add(showConsole);
  }
}
