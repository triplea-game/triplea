package games.strategy.sound;

import java.util.Collection;

import games.strategy.engine.data.PlayerId;
import games.strategy.engine.framework.LocalPlayers;

/**
 * A sound channel allowing sounds normally played on the server (for example: in a delegate, such as a the move
 * delegate) to also be played
 * on clients.
 */
public class DefaultSoundChannel implements ISound {
  private final LocalPlayers localPlayers;

  public DefaultSoundChannel(final LocalPlayers localPlayers) {
    this.localPlayers = localPlayers;
  }


  @Override
  public void playSoundForAll(final String clipName, final PlayerId playerId) {
    ClipPlayer.play(clipName, playerId);
  }

  @Override
  public void playSoundToPlayers(final String clipName,
      final Collection<PlayerId> playersToSendTo, final Collection<PlayerId> butNotThesePlayers,
      final boolean includeObservers) {
    if (playersToSendTo == null || playersToSendTo.isEmpty()) {
      return;
    }
    if (butNotThesePlayers != null) {
      for (final PlayerId p : butNotThesePlayers) {
        if (localPlayers.playing(p)) {
          return;
        }
      }
    }
    final boolean isPlaying = playersToSendTo.stream()
        .anyMatch(localPlayers::playing);
    final boolean includingObserversLocalNotEmpty = localPlayers.getLocalPlayers().isEmpty();

    if (isPlaying || includingObserversLocalNotEmpty) {
      ClipPlayer.play(clipName);
    }
  }

}
