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
import java.nio.file.Path;
import java.util.Optional;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.swing.JMenuItemBuilder;
import org.triplea.swing.key.binding.KeyCode;

final class FileMenu extends JMenu {
  private static final long serialVersionUID = -3855695429784752428L;

  private final GameData gameData;
  private final TripleAFrame frame;
  private final IGame game;

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @Getter
  public enum Mnemonic {
    FILE_MENU(KeyCode.F),
    SAVE(KeyCode.S),
    POST_PBEM(KeyCode.P),
    LEAVE(KeyCode.L),
    EXIT(KeyCode.X);

    private final KeyCode mnemonicCode;
  }

  FileMenu(final TripleAFrame frame) {
    super("File");

    this.frame = frame;
    game = frame.getGame();
    gameData = frame.getGame().getData();

    setMnemonic(Mnemonic.FILE_MENU.getMnemonicCode().getInputEventCode());

    add(newSaveMenuItem());
    if (PbemMessagePoster.gameDataHasPlayByEmailOrForumMessengers(gameData)) {
      add(addPostPbem());
    }
    addSeparator();
    addExitMenu();
  }

  private JMenuItem newSaveMenuItem() {
    return new JMenuItemBuilder("Save", Mnemonic.SAVE.getMnemonicCode())
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
    return new JMenuItemBuilder("Post PBEM/PBF Gamesave", Mnemonic.POST_PBEM.getMnemonicCode())
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
    add(
        new JMenuItemBuilder("Leave Game", Mnemonic.LEAVE.getMnemonicCode())
            .accelerator(
                (isMac
                    ?
                    // On Mac OS X, the command-Q is reserved for the Quit action,
                    // so set the command-W key combo for the Leave Game action
                    KeyCode.W
                    :
                    // On non-Mac operating systems, set the Ctrl-Q key combo for the Leave Game
                    KeyCode.Q))
            .actionListener(frame::leaveGame)
            .build());

    // Mac OS X automatically creates a Quit menu item under the TripleA menu,
    // so all we need to do is register that menu item with triplea's shutdown mechanism
    if (isMac) {
      MacOsIntegration.setQuitHandler(frame);
    } else { // On non-Mac operating systems, we need to manually create an Exit menu item
      add(
          new JMenuItemBuilder("Exit Program", Mnemonic.EXIT.getMnemonicCode())
              .actionListener(frame::shutdown)
              .build());
    }
  }
}
