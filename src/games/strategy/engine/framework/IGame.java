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
  public static final RemoteName GAME_MODIFICATION_CHANNEL =
      new RemoteName("games.strategy.engine.framework.IGame.GAME_MODIFICATION_CHANNEL", IGameModifiedChannel.class);

  public GameData getData();

  public void addGameStepListener(GameStepListener listener);

  public void removeGameStepListener(GameStepListener listener);

  public IMessenger getMessenger();

  public IChannelMessenger getChannelMessenger();

  public IRemoteMessenger getRemoteMessenger();

  public Vault getVault();

  /**
   * Should not be called outside of engine code.
   */
  public void addChange(Change aChange);

  public boolean canSave();

  public IRandomSource getRandomSource();

  /**
   * add a display that will recieve broadcasts from the IDelegateBridge.getDisplayBroadvaster
   */
  public void addDisplay(IDisplay display);

  /**
   * remove a display
   */
  public void removeDisplay(IDisplay display);

  public void addSoundChannel(ISound display);

  public void removeSoundChannel(ISound display);

  /**
   * Is the game over. Game over does not relate to the state of the game (eg check-mate in chess)
   * but to the game being shut down and all players have left.
   * <p>
   */
  public boolean isGameOver();

  /**
   * @return a listing of who is playing who.
   */
  public PlayerManager getPlayerManager();

  /**
   * Save the game to the given directory.
   * The file should exist and be writeable.
   */
  public void saveGame(File f);
}
