package games.strategy.sound;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.message.IChannelSubscribor;

import java.util.Collection;

/**
 * A sound channel allowing sounds normally played on the server (for example: in a delegate, such as a the move delegate) to also be played on clients.
 * 
 * @author veqryn (Mark Christopher Duncan)
 * 
 */
public interface ISound extends IChannelSubscribor
{
	/**
	 * Before recieving messages, this method will be called by the game engine.
	 * 
	 * @param bridge
	 */
	public void initialize();
	
	public void shutDown();
	
	/**
	 * You will want to call this from things that the server only runs (like delegates), and not call this from user interface elements (because all users have these).
	 * 
	 * @param clipName
	 *            The name of the sound clip to play, found in SoundPath.java
	 * @param subFolder
	 *            The name of the player nation who's sound we want to play (ie: russians infantry might make different sounds from german infantry, etc). Can be null.
	 */
	public void playSoundForAll(final String clipName, final String subFolder);
	
	/**
	 * You will want to call this from things that the server only runs (like delegates), and not call this from user interface elements (because all users have these).
	 * 
	 * @param clipName
	 *            The name of the sound clip to play, found in SoundPath.java
	 * @param subFolder
	 *            The name of the player nation who's sound we want to play (ie: russians infantry might make different sounds from german infantry, etc). Can be null.
	 * @param doNotIncludeHost
	 * @param doNotIncludeClients
	 * @param doNotIncludeObservers
	 */
	public void playSoundForAll(final String clipName, final String subFolder, final boolean doNotIncludeHost, final boolean doNotIncludeClients, final boolean doNotIncludeObservers);
	
	/**
	 * You will want to call this from things that the server only runs (like delegates), and not call this from user interface elements (because all users have these).
	 * 
	 * @param clipName
	 *            The name of the sound clip to play, found in SoundPath.java
	 * @param subFolder
	 *            The name of the player nation who's sound we want to play (ie: russians infantry might make different sounds from german infantry, etc). (Can be null.)
	 * @param playersToSendTo
	 *            The machines controlling these PlayerID's who we want to hear this sound.
	 * @param butNotThesePlayers
	 *            The machines controlling these PlayerID's who we do not want to hear this sound. If the machine controls players in both playersToSendTo and butNotThesePlayers, they will not hear a sound. (Can be null.)
	 */
	public void playSoundToPlayers(final String clipName, final String subFolder, final Collection<PlayerID> playersToSendTo, final Collection<PlayerID> butNotThesePlayers);
	
	/**
	 * You will want to call this from things that the server only runs (like delegates), and not call this from user interface elements (because all users have these).
	 * 
	 * @param clipName
	 *            The name of the sound clip to play, found in SoundPath.java
	 * @param subFolder
	 *            The name of the player nation who's sound we want to play (ie: russians infantry might make different sounds from german infantry, etc). (Can be null.)
	 * @param playerToSendTo
	 *            The machine which controls this PlayerID who we want to hear this sound
	 */
	public void playSoundToPlayer(final String clipName, final String subFolder, final PlayerID playerToSendTo);
}
