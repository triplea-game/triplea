package games.strategy.triplea.ui.menubar;

import java.io.File;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.engine.pbem.PBEMMessagePoster;
import games.strategy.triplea.delegate.GameStepPropertiesHelper;
import games.strategy.triplea.ui.MacQuitMenuWrapper;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.history.HistoryLog;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

public class FileMenu {

  private final GameData gameData;
  private final TripleAFrame frame;
  private final IGame game;

  public FileMenu(final TripleAMenuBar menuBar, final TripleAFrame frame) {
    this.frame = frame;
    game = frame.getGame();
    gameData = frame.getGame().getData();

    final Menu fileMenu = new Menu("_File");
    fileMenu.getItems().add(createSaveMenu());

    if (PBEMMessagePoster.GameDataHasPlayByEmailOrForumMessengers(gameData)) {
      fileMenu.getItems().add(addPostPBEM());
    }

    fileMenu.getItems().add(new SeparatorMenuItem());
    addExitMenu(fileMenu);
    menuBar.getMenus().add(fileMenu);
  }

  private MenuItem createSaveMenu() {
    final MenuItem menuFileSave = new MenuItem("_Save");
    menuFileSave.setMnemonicParsing(true);
    menuFileSave.setOnAction(e -> {
      final File f = TripleAMenuBar.getSaveGameLocationDialog(frame);
      if (f != null) {
        game.saveGame(f);
        new Alert(AlertType.INFORMATION, "Game Saved").show();
      }
    });
    menuFileSave.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
    return menuFileSave;
  }

  protected MenuItem addPostPBEM() {
    final MenuItem menuPBEM = new MenuItem("_Post PBEM/PBF Gamesave");
    menuPBEM.setMnemonicParsing(true);
    menuPBEM.setOnAction(e -> {
      if (gameData == null || !PBEMMessagePoster.GameDataHasPlayByEmailOrForumMessengers(gameData)) {
        return;
      }
      final String title = "Manual Gamesave Post";
      try {
        gameData.acquireReadLock();
        final GameStep step = gameData.getSequence().getStep();
        final PlayerID currentPlayer = (step == null ? PlayerID.NULL_PLAYERID
            : (step.getPlayerID() == null ? PlayerID.NULL_PLAYERID : step.getPlayerID()));
        final int round = gameData.getSequence().getRound();
        final HistoryLog historyLog = new HistoryLog();
        historyLog.printFullTurn(gameData, false, GameStepPropertiesHelper.getTurnSummaryPlayers(gameData));
        final PBEMMessagePoster poster = new PBEMMessagePoster(gameData, currentPlayer, round, title);
        PBEMMessagePoster.postTurn(title, historyLog, true, poster, null, frame, null);
      } finally {
        gameData.releaseReadLock();
      }
    });
    menuPBEM.setAccelerator(new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN));
    return menuPBEM;
  }

  protected void addExitMenu(final Menu parentMenu) {
    final boolean isMac = SystemProperties.isMac();
    final MenuItem leaveGameMenuExit = new MenuItem("_Leave Game");
    leaveGameMenuExit.setMnemonicParsing(true);
    leaveGameMenuExit.setOnAction(e -> frame.leaveGame());
    // On Mac OS X, the command-Q is reserved for the Quit action,
    // so set the command-L key combo for the Leave Game action
    // On non-Mac operating systems, set the Ctrl-Q key combo for the Leave Game action
    leaveGameMenuExit
        .setAccelerator(new KeyCodeCombination(isMac ? KeyCode.L : KeyCode.Q, KeyCombination.CONTROL_DOWN));
    parentMenu.getItems().add(leaveGameMenuExit);
    // Mac OS X automatically creates a Quit menu item under the TripleA menu,
    // so all we need to do is register that menu item with triplea's shutdown mechanism
    if (isMac) {
      MacQuitMenuWrapper.registerMacShutdownHandler(frame);
    } else { // On non-Mac operating systems, we need to manually create an Exit menu item
      final MenuItem menuFileExit = new MenuItem("_Exit Program");
      menuFileExit.setMnemonicParsing(true);
      menuFileExit.setOnAction(e -> frame.shutdown());
      parentMenu.getItems().add(menuFileExit);
    }
  }

}
