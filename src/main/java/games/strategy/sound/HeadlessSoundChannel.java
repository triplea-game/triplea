package games.strategy.sound;

import java.util.Collection;

import games.strategy.engine.data.PlayerID;

public class HeadlessSoundChannel implements ISound {

  @Override
  public void playSoundForAll(final String clipName, final PlayerID playerID) {}

  @Override
  public void playSoundToPlayers(final String clipName, final Collection<PlayerID> playersToSendTo,
      final Collection<PlayerID> butNotThesePlayers, final boolean includeObservers) {}

}
