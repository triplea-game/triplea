package org.triplea.sound;

import games.strategy.engine.data.GamePlayer;
import java.util.Collection;

/** Implementation of {@link ISound} that does nothing (i.e. no sounds will be played). */
public class HeadlessSoundChannel implements ISound {

  @Override
  public void playSoundForAll(final String clipName, final GamePlayer gamePlayer) {}

  @Override
  public void playSoundToPlayers(
      final String clipName,
      final Collection<GamePlayer> playersToSendTo,
      final Collection<GamePlayer> butNotThesePlayers,
      final boolean includeObservers) {}
}
