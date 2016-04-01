package games.strategy.sound;

import java.util.Collection;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.framework.LocalPlayers;

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


  @Override
  public void playSoundForAll(final String clipName, final PlayerID playerID) {
    ClipPlayer.play(clipName, playerID);
  }

  @Override
  public void playSoundToPlayers(final String clipName,
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
      ClipPlayer.play(clipName);
    }
  }

}
