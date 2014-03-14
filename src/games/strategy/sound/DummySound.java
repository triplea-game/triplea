package games.strategy.sound;

import games.strategy.engine.data.PlayerID;

import java.util.Collection;

/**
 * 
 * @author veqryn
 * 
 */
public class DummySound implements ISound
{
	public void initialize()
	{
	}
	
	public void shutDown()
	{
	}
	
	public void playSoundForAll(final String clipName, final String subFolder)
	{
	}
	
	public void playSoundForAll(final String clipName, final String subFolder, final boolean doNotIncludeHost, final boolean doNotIncludeClients, final boolean doNotIncludeObservers)
	{
	}
	
	public void playSoundToPlayers(final String clipName, final String subFolder, final Collection<PlayerID> playersToSendTo, final Collection<PlayerID> butNotThesePlayers,
				final boolean includeObservers)
	{
	}
	
	public void playSoundToPlayer(final String clipName, final String subFolder, final PlayerID playerToSendTo, final boolean includeObservers)
	{
	}
}
