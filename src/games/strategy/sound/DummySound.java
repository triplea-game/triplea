package games.strategy.sound;

import java.util.Collection;

import games.strategy.engine.data.PlayerID;


public class DummySound implements ISound {
  @Override
  public void initialize() {}

  @Override
  public void shutDown() {}

  @Override
  public void playSoundForAll(final String clipName, final String subFolder) {}

  @Override
  public void playSoundForAll(final String clipName, final String subFolder, final boolean doNotIncludeHost,
      final boolean doNotIncludeClients, final boolean doNotIncludeObservers) {}

  @Override
  public void playSoundToPlayers(final String clipName, final String subFolder, final Collection<PlayerID> playersToSendTo,
      final Collection<PlayerID> butNotThesePlayers,
      final boolean includeObservers) {}

  @Override
  public void playSoundToPlayer(final String clipName, final String subFolder, final PlayerID playerToSendTo,
      final boolean includeObservers) {}
}
