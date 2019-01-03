package games.strategy.engine.framework.startup.launcher;

import java.awt.Component;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.player.IGamePlayer;
import games.strategy.engine.random.IRandomSource;
import games.strategy.net.HeadlessServerMessenger;
import games.strategy.net.Messengers;
import games.strategy.util.Interruptibles;
import lombok.extern.java.Log;

/** Implementation of {@link ILauncher} for a headed local or network client game. */
@Log
public class LocalLauncher extends AbstractLauncher {
  private final IRandomSource randomSource;
  private final PlayerListing playerListing;

  public LocalLauncher(
      final GameSelectorModel gameSelectorModel,
      final IRandomSource randomSource,
      final PlayerListing playerListing) {
    super(gameSelectorModel);
    this.randomSource = randomSource;
    this.playerListing = playerListing;
  }

  @Override
  protected void launchInNewThread(final Component parent) {
    final Optional<ServerGame> game = loadGame();
    try {
      game.ifPresent(ServerGame::startGame);
    } finally {
      // todo(kg), this does not occur on the swing thread, and this notifies setupPanel observers
      // having an oddball issue with the zip stream being closed while parsing to load default
      // game. might be caused
      // by closing of stream while unloading map resources.
      Interruptibles.sleep(100);
      gameSelectorModel.loadDefaultGameNewThread();
      SwingUtilities.invokeLater(() -> JOptionPane.getFrameForComponent(parent).setVisible(true));
    }
  }

  private Optional<ServerGame> loadGame() {
    try {
      gameData.doPreGameStartDataModifications(playerListing);
      final Messengers messengers = new Messengers(new HeadlessServerMessenger());
      final Set<IGamePlayer> gamePlayers =
          gameData.getGameLoader().newPlayers(playerListing.getLocalPlayerTypeMap());
      final ServerGame game =
          new ServerGame(gameData, gamePlayers, new HashMap<>(), messengers, headless);
      game.setRandomSource(randomSource);
      gameData.getGameLoader().startGame(game, gamePlayers, headless, null);
      return Optional.of(game);
    } catch (final Exception ex) {
      log.log(Level.SEVERE, "Failed to start game", ex);
      return Optional.empty();
    } finally {
      gameLoadingWindow.doneWait();
    }
  }
}
