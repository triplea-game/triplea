package org.triplea.sound;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.message.IChannelSubscriber;
import java.util.Collection;

/**
 * A sound channel allowing sounds normally played on the server (for example: in a delegate, such
 * as a the move delegate) to also be played on clients.
 */
public interface ISound extends IChannelSubscriber {

  /**
   * You will want to call this from things that the server only runs (like delegates), and not call
   * this from user interface elements (because all users have these).
   *
   * @param clipName The name of the sound clip to play, found in SoundPath.java
   * @param gamePlayer The player who's sound we want to play (ie: russians infantry might make
   *     different sounds from german infantry, etc). Can be null.
   */
  void playSoundForAll(String clipName, GamePlayer gamePlayer);

  /**
   * You will want to call this from things that the server only runs (like delegates), and not call
   * this from user interface elements (because all users have these).
   *
   * @param clipName The name of the sound clip to play, found in SoundPath.java
   * @param playersToSendTo The machines controlling these PlayerId's who we want to hear this
   *     sound.
   * @param butNotThesePlayers The machines controlling these PlayerId's who we do not want to hear
   *     this sound. If the machine controls players in both playersToSendTo and butNotThesePlayers,
   *     they will not hear a sound. (Can be null.)
   * @param includeObservers Whether to include non-playing machines
   */
  void playSoundToPlayers(
      String clipName,
      Collection<GamePlayer> playersToSendTo,
      Collection<GamePlayer> butNotThesePlayers,
      boolean includeObservers);
}
