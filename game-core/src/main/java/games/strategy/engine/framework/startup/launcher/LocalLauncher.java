package games.strategy.engine.framework.startup.launcher;

import java.awt.Component;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.triplea.java.Interruptibles;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.player.IGamePlayer;
import games.strategy.engine.random.IRandomSource;
import games.strategy.net.LocalNoOpMessenger;
import games.strategy.net.Messengers;
import lombok.extern.java.Log;

/**
 * Implementation of {@link ILauncher} for a headed local or network client game.
 */
@Log
public class LocalLauncher extends AbstractLauncher<ServerGame> {
  private final GameData gameData;
  private final GameSelectorModel gameSelectorModel;
  private final IRandomSource randomSource;
  private final PlayerListing playerListing;
  private final Component parent;

  public LocalLauncher(final GameSelectorModel gameSelectorModel, final IRandomSource randomSource,
      final PlayerListing playerListing, final Component parent) {
    this.randomSource = randomSource;
    this.playerListing = playerListing;
    this.gameSelectorModel = gameSelectorModel;
    this.gameData = gameSelectorModel.getGameData();
    this.parent = parent;
  }

  @Override
  protected void launchInternal(@Nullable final ServerGame game) {
    try {
      if (game != null) {
        game.startGame();
      }
    } finally {
      // todo(kg), this does not occur on the swing thread, and this notifies setupPanel observers
      // having an oddball issue with the zip stream being closed while parsing to load default game. might be caused
      // by closing of stream while unloading map resources.
      Interruptibles.sleep(100);
      gameSelectorModel.loadDefaultGameNewThread();
      SwingUtilities.invokeLater(() -> JOptionPane.getFrameForComponent(parent).setVisible(true));
    }
  }

  @Override
  Optional<ServerGame> loadGame() {
    try {
      gameData.doPreGameStartDataModifications(playerListing);
      final Messengers messengers = new Messengers(new LocalNoOpMessenger());
      final Set<IGamePlayer> gamePlayers =
          gameData.getGameLoader().newPlayers(playerListing.getLocalPlayerTypeMap());
      final ServerGame game = new ServerGame(gameData, gamePlayers, new HashMap<>(), messengers, false);
      game.setRandomSource(randomSource);
      gameData.getGameLoader().startGame(game, gamePlayers, false, null);
      return Optional.of(game);
    } catch (final Exception ex) {
      log.log(Level.SEVERE, "Failed to start game", ex);
      return Optional.empty();
    }
  }
}
