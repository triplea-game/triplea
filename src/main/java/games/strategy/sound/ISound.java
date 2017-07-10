package games.strategy.sound;

import java.util.Collection;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.message.IChannelSubscribor;

/**
 * A sound channel allowing sounds normally played on the server (for example: in a delegate, such as a the move
 * delegate) to also be played
 * on clients.
 */
public interface ISound extends IChannelSubscribor {

  /**
   * You will want to call this from things that the server only runs (like delegates), and not call this from user
   * interface elements
   * (because all users have these).
   *
   * @param clipName
   *        The name of the sound clip to play, found in SoundPath.java
   * @param playerId
   *        The player who's sound we want to play (ie: russians infantry might make different sounds from german
   *        infantry, etc). Can be null.
   */
  void playSoundForAll(final String clipName, final PlayerID playerId);


  /**
   * You will want to call this from things that the server only runs (like delegates), and not call this from user
   * interface elements
   * (because all users have these).
   *
   * @param clipName
   *        The name of the sound clip to play, found in SoundPath.java
   * @param playersToSendTo
   *        The machines controlling these PlayerID's who we want to hear this sound.
   * @param butNotThesePlayers
   *        The machines controlling these PlayerID's who we do not want to hear this sound. If the machine controls
   *        players in both
   *        playersToSendTo and butNotThesePlayers, they will not hear a sound. (Can be null.)
   * @param includeObservers
   *        Whether to include non-playing machines
   */
  void playSoundToPlayers(final String clipName,
      final Collection<PlayerID> playersToSendTo, final Collection<PlayerID> butNotThesePlayers,
      final boolean includeObservers);

}
