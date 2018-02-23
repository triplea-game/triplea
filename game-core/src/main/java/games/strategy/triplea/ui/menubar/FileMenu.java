package games.strategy.triplea.ui.menubar;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

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
import games.strategy.ui.SwingAction;

final class FileMenu extends JMenu {
  private static final long serialVersionUID = -3855695429784752428L;

  private final GameData gameData;
  private final TripleAFrame frame;
  private final IGame game;

  FileMenu(final TripleAFrame frame) {
    super("File");

    this.frame = frame;
    game = frame.getGame();
    gameData = frame.getGame().getData();

    setMnemonic(KeyEvent.VK_F);

    add(createSaveMenu());
    if (PBEMMessagePoster.gameDataHasPlayByEmailOrForumMessengers(gameData)) {
      add(addPostPbem());
    }
    addSeparator();
    addExitMenu();
  }

  private JMenuItem createSaveMenu() {
    final JMenuItem menuFileSave = new JMenuItem(SwingAction.of("Save", e -> {
      final File f = TripleAMenuBar.getSaveGameLocation(frame);
      if (f != null) {
        game.saveGame(f);
        JOptionPane.showMessageDialog(frame, "Game Saved", "Game Saved", JOptionPane.INFORMATION_MESSAGE);
      }
    }));
    menuFileSave.setMnemonic(KeyEvent.VK_S);
    menuFileSave.setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    return menuFileSave;
  }

  private JMenuItem addPostPbem() {
    final JMenuItem menuPbem = new JMenuItem(SwingAction.of("Post PBEM/PBF Gamesave", e -> {
      if ((gameData == null) || !PBEMMessagePoster.gameDataHasPlayByEmailOrForumMessengers(gameData)) {
        return;
      }
      final String title = "Manual Gamesave Post";
      try {
        gameData.acquireReadLock();
        final GameStep step = gameData.getSequence().getStep();
        final PlayerID currentPlayer = ((step == null) ? PlayerID.NULL_PLAYERID
            : ((step.getPlayerId() == null) ? PlayerID.NULL_PLAYERID : step.getPlayerId()));
        final int round = gameData.getSequence().getRound();
        final HistoryLog historyLog = new HistoryLog();
        historyLog.printFullTurn(gameData, true, GameStepPropertiesHelper.getTurnSummaryPlayers(gameData));
        final PBEMMessagePoster poster = new PBEMMessagePoster(gameData, currentPlayer, round, title);
        PBEMMessagePoster.postTurn(title, historyLog, true, poster, null, frame, null);
      } finally {
        gameData.releaseReadLock();
      }
    }));
    menuPbem.setMnemonic(KeyEvent.VK_P);
    menuPbem.setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    return menuPbem;
  }

  private void addExitMenu() {
    final boolean isMac = SystemProperties.isMac();
    final JMenuItem leaveGameMenuExit = new JMenuItem(SwingAction.of("Leave Game", e -> frame.leaveGame()));
    leaveGameMenuExit.setMnemonic(KeyEvent.VK_L);
    if (isMac) { // On Mac OS X, the command-Q is reserved for the Quit action,
      // so set the command-L key combo for the Leave Game action
      leaveGameMenuExit.setAccelerator(
          KeyStroke.getKeyStroke(KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    } else { // On non-Mac operating systems, set the Ctrl-Q key combo for the Leave Game action
      leaveGameMenuExit.setAccelerator(
          KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    }
    add(leaveGameMenuExit);
    // Mac OS X automatically creates a Quit menu item under the TripleA menu,
    // so all we need to do is register that menu item with triplea's shutdown mechanism
    if (isMac) {
      MacQuitMenuWrapper.registerMacShutdownHandler(frame);
    } else { // On non-Mac operating systems, we need to manually create an Exit menu item
      final JMenuItem menuFileExit = new JMenuItem(SwingAction.of("Exit Program", e -> frame.shutdown()));
      menuFileExit.setMnemonic(KeyEvent.VK_E);
      add(menuFileExit);
    }
  }
}
