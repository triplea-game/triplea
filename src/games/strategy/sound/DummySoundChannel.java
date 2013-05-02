package games.strategy.sound;

import games.strategy.engine.data.PlayerID;

import java.util.Collection;

/**
 * Just a dummy sound channel that doesn't do squat.
 * 
 * @author veqryn
 * 
 */
public class DummySoundChannel implements ISound
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
	
	public void playSoundToPlayers(final String clipName, final String subFolder, final Collection<PlayerID> playersToSendTo, final Collection<PlayerID> butNotThesePlayers)
	{
	}
	
	public void playSoundToPlayer(final String clipName, final String subFolder, final PlayerID playerToSendTo)
	{
	}
}
