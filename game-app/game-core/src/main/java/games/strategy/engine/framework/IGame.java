package games.strategy.engine.framework;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerManager;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.message.RemoteName;
import games.strategy.engine.random.IRandomSource;
import games.strategy.engine.vault.Vault;
import games.strategy.net.Messengers;
import games.strategy.triplea.ResourceLoader;
import java.nio.file.Path;
import javax.annotation.Nullable;
import org.triplea.sound.ISound;

/**
 * Represents a running game.
 *
 * <p>Allows access to the games communication interfaces, and to listen to the current game step.
 */
public interface IGame {
  RemoteName GAME_MODIFICATION_CHANNEL =
      new RemoteName(
          IGame.class.getName() + ".GAME_MODIFICATION_CHANNEL", IGameModifiedChannel.class);

  GameData getData();

  Messengers getMessengers();

  Vault getVault();

  /** Should not be called outside of engine code. */
  void addChange(Change change);

  @Nullable
  IRandomSource getRandomSource();

  /** Set a display that will receive broadcasts from the IDelegateBridge.getDisplayBroadcaster. */
  void setDisplay(@Nullable IDisplay display);

  void setSoundChannel(@Nullable ISound display);

  /**
   * Is the game over. Game over does not relate to the state of the game (eg check-mate in chess)
   * but to the game being shut down and all players have left.
   */
  boolean isGameOver();

  /** Returns a listing of who is playing who. */
  PlayerManager getPlayerManager();

  /** Save the game to the given directory. The file should exist and be writeable. */
  void saveGame(Path f);

  /** Returns the {@link ResourceLoader} for the current game instance. */
  ResourceLoader getResourceLoader();

  /** Sets the {@link ResourceLoader} for the current game instance. */
  void setResourceLoader(ResourceLoader resourceLoader);
}
