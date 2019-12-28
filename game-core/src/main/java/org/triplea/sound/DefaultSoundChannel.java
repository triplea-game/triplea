package org.triplea.sound;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.framework.LocalPlayers;
import java.util.Collection;

/**
 * A sound channel allowing sounds normally played on the server (for example: in a delegate, such
 * as a the move delegate) to also be played on clients.
 */
public class DefaultSoundChannel implements ISound {
  private final LocalPlayers localPlayers;

  public DefaultSoundChannel(final LocalPlayers localPlayers) {
    this.localPlayers = localPlayers;
  }

  @Override
  public void playSoundForAll(final String clipName, final GamePlayer gamePlayer) {
    ClipPlayer.play(clipName, gamePlayer);
  }

  @Override
  public void playSoundToPlayers(
      final String clipName,
      final Collection<GamePlayer> playersToSendTo,
      final Collection<GamePlayer> butNotThesePlayers,
      final boolean includeObservers) {
    if (playersToSendTo == null || playersToSendTo.isEmpty()) {
      return;
    }
    if (butNotThesePlayers != null) {
      for (final GamePlayer p : butNotThesePlayers) {
        if (localPlayers.playing(p)) {
          return;
        }
      }
    }
    final boolean isPlaying = playersToSendTo.stream().anyMatch(localPlayers::playing);
    final boolean includingObserversLocalNotEmpty = localPlayers.getLocalPlayers().isEmpty();

    if (isPlaying || includingObserversLocalNotEmpty) {
      ClipPlayer.play(clipName);
    }
  }
}
