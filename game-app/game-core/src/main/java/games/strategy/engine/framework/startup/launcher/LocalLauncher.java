package games.strategy.engine.framework.startup.launcher;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.startup.launcher.local.PlayerCountrySelection;
import games.strategy.engine.framework.startup.mc.GameSelector;
import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.engine.player.Player;
import games.strategy.engine.random.IRandomSource;
import games.strategy.engine.random.PlainRandomSource;
import games.strategy.net.LocalNoOpMessenger;
import games.strategy.net.Messengers;
import games.strategy.net.websocket.ClientNetworkBridge;
import java.awt.Component;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import org.triplea.debug.error.reporting.StackTraceReportModel;
import org.triplea.java.Interruptibles;
import org.triplea.java.ThreadRunner;

/** Implementation of {@link ILauncher} for a headed local or network client game. */
@Slf4j
public class LocalLauncher implements ILauncher {
  private final GameData gameData;
  private final GameSelector gameSelector;
  private final IRandomSource randomSource;
  private final PlayerListing playerListing;
  private final Component parent;
  private final LaunchAction launchAction;
  private final PlayerTypes playerTypes;

  public LocalLauncher(
      final GameSelector gameSelector,
      final IRandomSource randomSource,
      final PlayerListing playerListing,
      final Component parent,
      final LaunchAction launchAction,
      final PlayerTypes playerTypes) {
    this.randomSource = randomSource;
    this.playerListing = playerListing;
    this.gameSelector = gameSelector;
    this.gameData = gameSelector.getGameData();
    this.parent = parent;
    this.launchAction = launchAction;
    this.playerTypes = playerTypes;
  }

  @Override
  public void launch() {
    StackTraceReportModel.setCurrentMapNameFromGameData(gameData);
    final Optional<ServerGame> result = loadGame();
    ThreadRunner.runInNewThread(() -> launchInternal(result.orElse(null)));
  }

  private Optional<ServerGame> loadGame() {
    try {
      playerListing.doPreGameStartDataModifications(gameData);
      final Messengers messengers = new Messengers(new LocalNoOpMessenger());
      final Set<Player> gamePlayers =
          gameData.getGameLoader().newPlayers(playerListing.getLocalPlayerTypeMap(playerTypes));
      final ServerGame game =
          new ServerGame(
              gameData,
              gamePlayers,
              new HashMap<>(),
              messengers,
              ClientNetworkBridge.NO_OP_SENDER,
              launchAction);
      game.setRandomSource(randomSource);
      gameData.getGameLoader().startGame(game, gamePlayers, launchAction, null);
      return Optional.of(game);
    } catch (final MapNotFoundException e) {
      // The throwing method of MapNotFoundException notifies and prompts user to download the map.
      return Optional.empty();
    } catch (final Exception ex) {
      log.error("Failed to start game", ex);
      return Optional.empty();
    }
  }

  private void launchInternal(@Nullable final ServerGame game) {
    try {
      if (game != null) {
        game.startGame();
      }
    } finally {
      // todo(kg), this does not occur on the swing thread, and this notifies setupPanel observers
      // having an oddball issue with the zip stream being closed while parsing to load default
      // game. might be caused by closing of stream while unloading map resources.
      Interruptibles.sleep(100);
      gameSelector.onGameEnded();
      SwingUtilities.invokeLater(() -> JOptionPane.getFrameForComponent(parent).setVisible(true));
    }
  }

  /** Creates a launcher for a single player local (no network) game. */
  public static LocalLauncher create(
      final GameSelectorModel gameSelectorModel,
      final Collection<? extends PlayerCountrySelection> playerRows,
      final Component parent,
      final LaunchAction launchAction) {

    final Map<String, PlayerTypes.Type> playerTypes =
        playerRows.stream()
            .collect(
                Collectors.toMap(
                    PlayerCountrySelection::getPlayerName, PlayerCountrySelection::getPlayerType));

    final Map<String, Boolean> playersEnabled =
        playerRows.stream()
            .collect(
                Collectors.toMap(
                    PlayerCountrySelection::getPlayerName,
                    PlayerCountrySelection::isPlayerEnabled));

    final PlayerListing playerListing =
        new PlayerListing(
            playersEnabled,
            playerTypes,
            gameSelectorModel.getGameName(),
            gameSelectorModel.getGameRound());
    return new LocalLauncher(
        gameSelectorModel,
        new PlainRandomSource(),
        playerListing,
        parent,
        launchAction,
        new PlayerTypes(launchAction.getPlayerTypes()));
  }
}
