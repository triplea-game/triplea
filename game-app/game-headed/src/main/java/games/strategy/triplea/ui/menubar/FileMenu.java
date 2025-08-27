package games.strategy.triplea.ui.menubar;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameFileSelector;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.engine.posted.game.pbem.PbemMessagePoster;
import games.strategy.triplea.delegate.GameStepPropertiesHelper;
import games.strategy.triplea.ui.MacOsIntegration;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.history.HistoryLog;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Optional;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import org.triplea.swing.JMenuItemBuilder;
import org.triplea.swing.SwingAction;
import org.triplea.swing.key.binding.KeyCode;

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

    add(newSaveMenuItem());
    if (PbemMessagePoster.gameDataHasPlayByEmailOrForumMessengers(gameData)) {
      add(addPostPbem());
    }
    addSeparator();
    addExitMenu();
  }

  private JMenuItem newSaveMenuItem() {
    return new JMenuItemBuilder("Save", KeyCode.S)
        .accelerator(KeyCode.S)
        .actionListener(
            () -> {
              final Optional<Path> f = GameFileSelector.getSaveGameLocation(frame, gameData);
              if (f.isPresent()) {
                game.saveGame(f.get());
                JOptionPane.showMessageDialog(
                    frame, "Game Saved", "Game Saved", JOptionPane.INFORMATION_MESSAGE);
              }
            })
        .build();
  }

  private JMenuItem addPostPbem() {
    return new JMenuItemBuilder("Post PBEM/PBF Gamesave", KeyCode.P)
        .accelerator(KeyCode.M)
        .actionListener(
            () -> {
              if (!PbemMessagePoster.gameDataHasPlayByEmailOrForumMessengers(gameData)) {
                return;
              }
              final String title = "Manual Gamesave Post";
              try (GameData.Unlocker ignored = gameData.acquireReadLock()) {
                final GameStep step = gameData.getSequence().getStep();
                final GamePlayer currentPlayer =
                    (step == null
                        ? gameData.getPlayerList().getNullPlayer()
                        : (step.getPlayerId() == null
                            ? gameData.getPlayerList().getNullPlayer()
                            : step.getPlayerId()));
                final int round = gameData.getSequence().getRound();
                final HistoryLog historyLog = new HistoryLog(frame);
                historyLog.printFullTurn(
                    gameData, true, GameStepPropertiesHelper.getTurnSummaryPlayers(gameData));
                final PbemMessagePoster poster =
                    new PbemMessagePoster(gameData, currentPlayer, round, title);
                poster.postTurn(title, historyLog, true, null, frame, frame.getGame(), null);
              }
            })
        .build();
  }

  private void addExitMenu() {
    final boolean isMac = SystemProperties.isMac();
    final JMenuItem leaveGameMenuExit =
        new JMenuItem(SwingAction.of("Leave Game", e -> frame.leaveGame()));
    leaveGameMenuExit.setMnemonic(KeyEvent.VK_L);
    if (isMac) { // On Mac OS X, the command-Q is reserved for the Quit action,
      // so set the command-W key combo for the Leave Game action
      leaveGameMenuExit.setAccelerator(
          KeyStroke.getKeyStroke(
              KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
    } else { // On non-Mac operating systems, set the Ctrl-Q key combo for the Leave Game action
      leaveGameMenuExit.setAccelerator(
          KeyStroke.getKeyStroke(
              KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
    }
    add(leaveGameMenuExit);

    // Mac OS X automatically creates a Quit menu item under the TripleA menu,
    // so all we need to do is register that menu item with triplea's shutdown mechanism
    if (isMac) {
      MacOsIntegration.setQuitHandler(frame);
    } else { // On non-Mac operating systems, we need to manually create an Exit menu item
      final JMenuItem menuFileExit =
          new JMenuItem(SwingAction.of("Exit Program", e -> frame.shutdown()));
      menuFileExit.setMnemonic(KeyEvent.VK_E);
      add(menuFileExit);
    }
  }
}
