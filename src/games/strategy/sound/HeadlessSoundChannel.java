package games.strategy.sound;

import java.util.Collection;

import games.strategy.engine.data.PlayerID;

public class HeadlessSoundChannel implements ISound {

  @Override
  public void playSoundForAll(String clipName, PlayerID playerID) {}

  @Override
  public void playSoundToPlayers(String clipName, Collection<PlayerID> playersToSendTo,
      Collection<PlayerID> butNotThesePlayers, boolean includeObservers) {}

}
