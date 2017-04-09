package games.strategy.engine.delegate;

import java.util.Properties;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.gamePlayer.IRemotePlayer;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.sound.ISound;

/**
 * A class that communicates with the Delegate. DelegateBridge co-ordinates
 * comunication between the Delegate and both the players and the game data.
 * The reason for communicating through a DelegateBridge is to achieve network
 * transparancy.
 * The delegateBridge allows the Delegate to talk to the player in a safe
 * manner.
 */
public interface IDelegateBridge {
  /**
   * equivalent to getRemotePlayer(getPlayerID())
   *
   * @return remote for the current player.
   */
  IRemotePlayer getRemotePlayer();

  /**
   * Get a remote reference to the given player.
   */
  IRemotePlayer getRemotePlayer(PlayerID id);

  PlayerID getPlayerID();

  /**
   * Returns the current step name.
   */
  String getStepName();

  /**
   * Add a change to game data. Use this rather than changing gameData
   * directly since this method allows us to send the changes to other
   * machines.
   *
   * @param aChange
   */
  void addChange(Change aChange);

  /**
   * equivalent to getRandom(max,1,annotation)[0].
   */
  int getRandom(final int max, final PlayerID player, final DiceType diceType, final String annotation);

  /**
   * Return a random value to be used by the delegate.
   * <p>
   * Delegates should not use random data that comes from any other source.
   * <p>
   *
   * @param annotation
   *        -
   *        a string used to describe the random event.
   *        <p>
   */
  int[] getRandom(final int max, final int count, final PlayerID player, final DiceType diceType,
      final String annotation);

  /**
   * return the delegate history writer for this game.
   * <p>
   * The delegate history writer allows writing to the game history.
   * <p>
   */
  IDelegateHistoryWriter getHistoryWriter();

  /**
   * Return an object that implements the IDisplay interface for the game.
   * <p>
   * Methods called on this returned object will be invoked on all displays in the game, including those on remote
   * machines
   * <p>
   */
  IDisplay getDisplayChannelBroadcaster();

  /**
   * Return an object that implements the ISound interface for the game.
   * <p>
   * Methods called on this returned object will be invoked on all sound channels in the game, including those on remote
   * machines
   * <p>
   */
  ISound getSoundChannelBroadcaster();

  /**
   * @return the propertie for this step.
   *         <p>
   */
  Properties getStepProperties();

  /**
   * After this step finishes executing, the next delegate will not be called.
   * <p>
   * This methd allows the delegate to signal that the game is over, but does not force the ui or the display to
   * shutdown.
   * <p>
   */
  void stopGameSequence();

  void leaveDelegateExecution();

  void enterDelegateExecution();

  GameData getData();
}
