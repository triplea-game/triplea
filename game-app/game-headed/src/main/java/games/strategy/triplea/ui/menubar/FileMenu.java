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
import lombok.experimental.UtilityClass;
import org.triplea.swing.JMenuBuilder;
import org.triplea.swing.JMenuItemBuilder;
import org.triplea.swing.key.binding.KeyCode;

@UtilityClass
final class FileMenu {

  public static JMenu get(final TripleAFrame frame) {
    final boolean isMac = SystemProperties.isMac();
    // Mac OS X automatically creates a Quit menu item under the TripleA menu,
    // so all we need to do is register that menu item with triplea's shutdown mechanism
    if (isMac) {
      MacOsIntegration.setQuitHandler(frame);
    }

    return new JMenuBuilder("File", TripleAMenuBar.Mnemonic.FILE.getMnemonicCode())
        .addMenuItem(newSaveMenuItem(frame))
        .addMenuItemIf(
            PbemMessagePoster.gameDataHasPlayByEmailOrForumMessengers(frame.getGame().getData()),
            addPostPbem(frame))
        .addSeparator()
        .addMenuItem(
            new JMenuItemBuilder("Leave Game", Mnemonic.LEAVE.getMnemonicCode())
                .accelerator(
                    (isMac
                        ?
                        // On Mac OS X, the command-Q is reserved for the Quit action,
                        // so set the command-W key combo for the Leave Game action
                        KeyCode.W
                        :
                        // On non-Mac operating systems, set the Ctrl-Q key combo for the Leave
                        // Game
                        KeyCode.Q))
                .actionListener(frame::leaveGame)
                .build())
        .addMenuItemIf(
            !isMac,
            // On non-Mac operating systems, we need to manually create an Exit menu item
            new JMenuItemBuilder("Exit Program", Mnemonic.EXIT.getMnemonicCode())
                .actionListener(frame::shutdown)
                .build())
        .build();
  }

  private JMenuItem newSaveMenuItem(final TripleAFrame frame) {
    return new JMenuItemBuilder("Save", Mnemonic.SAVE.getMnemonicCode())
        .accelerator(KeyCode.S)
        .actionListener(
            () -> {
              IGame game = frame.getGame();
              final Optional<Path> f = GameFileSelector.getSaveGameLocation(frame, game.getData());
              if (f.isPresent()) {
                game.saveGame(f.get());
                JOptionPane.showMessageDialog(
                    frame, "Game Saved", "Game Saved", JOptionPane.INFORMATION_MESSAGE);
              }
            })
        .build();
  }

  private JMenuItem addPostPbem(TripleAFrame frame) {
    final GameData gameData = frame.getGame().getData();
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

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @Getter
  public enum Mnemonic {
    SAVE(KeyCode.S),
    POST_PBEM(KeyCode.P),
    LEAVE(KeyCode.L),
    EXIT(KeyCode.X);

    private final KeyCode mnemonicCode;
  }
}
