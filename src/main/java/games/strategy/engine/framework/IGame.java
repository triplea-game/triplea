package games.strategy.engine.framework;

import java.io.File;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerManager;
import games.strategy.engine.data.events.GameStepListener;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.RemoteName;
import games.strategy.engine.random.IRandomSource;
import games.strategy.engine.vault.Vault;
import games.strategy.net.IMessenger;
import games.strategy.sound.ISound;

/**
 * Represents a running game.
 * <p>
 * Allows access to the games communication interfaces, and to listen to the current game step.
 */
public interface IGame {
  RemoteName GAME_MODIFICATION_CHANNEL =
      new RemoteName(IGame.class.getName() + ".GAME_MODIFICATION_CHANNEL", IGameModifiedChannel.class);

  GameData getData();

  void addGameStepListener(GameStepListener listener);

  void removeGameStepListener(GameStepListener listener);

  IMessenger getMessenger();

  IChannelMessenger getChannelMessenger();

  IRemoteMessenger getRemoteMessenger();

  Vault getVault();

  /**
   * Should not be called outside of engine code.
   */
  void addChange(Change aChange);

  boolean canSave();

  IRandomSource getRandomSource();

  /**
   * add a display that will recieve broadcasts from the IDelegateBridge.getDisplayBroadvaster
   */
  void addDisplay(IDisplay display);

  /**
   * remove a display.
   */
  void removeDisplay(IDisplay display);

  void addSoundChannel(ISound display);

  void removeSoundChannel(ISound display);

  /**
   * Is the game over. Game over does not relate to the state of the game (eg check-mate in chess)
   * but to the game being shut down and all players have left.
   * <p>
   */
  boolean isGameOver();

  /**
   * @return a listing of who is playing who.
   */
  PlayerManager getPlayerManager();

  /**
   * Save the game to the given directory.
   * The file should exist and be writeable.
   */
  void saveGame(File f);
}
