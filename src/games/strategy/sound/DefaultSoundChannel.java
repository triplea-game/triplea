package games.strategy.sound;

import java.util.Collection;
import java.util.Collections;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.framework.LocalPlayers;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.triplea.TripleAPlayer;

/**
 * A sound channel allowing sounds normally played on the server (for example: in a delegate, such as a the move
 * delegate) to also be played
 * on clients.
 */
public class DefaultSoundChannel implements ISound {
  private LocalPlayers m_localPlayers;

  public DefaultSoundChannel(final LocalPlayers localPlayers) {
    m_localPlayers = localPlayers;
  }

  /**
   * Plays a sound clip on this local machine.
   * You will want to call this from UI elements (because all users have these), and not call it from delegates (because
   * only the host has
   * these).
   *
   * @param clipName
   *        the name of the sound clip to play, found in SoundPath.java
   * @param subFolder
   *        the name of the player nation who's sound we want to play (ie: russians infantry might make different sounds
   *        from german
   *        infantry, etc). Can be null.
   */
  public static void playSoundOnLocalMachine(final String clipName, final String subFolder) {
    ClipPlayer.play(clipName, subFolder);
  }


  @Override
  public void playSoundForAll(final String clipName, final String subFolder) {
    playSoundOnLocalMachine(clipName, subFolder);
  }

  @Override
  public void playSoundToPlayers(final String clipName, final String subFolder,
      final Collection<PlayerID> playersToSendTo, final Collection<PlayerID> butNotThesePlayers,
      final boolean includeObservers) {
    if (playersToSendTo == null || playersToSendTo.isEmpty()) {
      return;
    }
    if (butNotThesePlayers != null) {
      for (final PlayerID p : butNotThesePlayers) {
        if (m_localPlayers.playing(p)) {
          return;
        }
      }
    }
    boolean isPlaying = false;
    for (final PlayerID p : playersToSendTo) {
      if (m_localPlayers.playing(p)) {
        isPlaying = true;
        break;
      }
    }
    if (includeObservers && m_localPlayers.getLocalPlayers().isEmpty()) {
      isPlaying = true;
    }
    if (isPlaying) {
      playSoundOnLocalMachine(clipName, subFolder);
    }
  }

}
