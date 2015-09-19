package games.strategy.sound;

import java.util.Collection;

import games.strategy.engine.data.PlayerID;

/**
 * Just a dummy sound channel that doesn't do squat.
 */
public class DummySoundChannel implements ISound {
  @Override
  public void playSoundForAll(final String clipName, final PlayerID playerID) {}

  @Override
  public void playSoundToPlayers(final String clipName,
      final Collection<PlayerID> playersToSendTo, final Collection<PlayerID> butNotThesePlayers,
      final boolean includeObservers) {}

}
