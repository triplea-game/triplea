/*
 * DelegateBridge.java
 *
 * Created on October 13, 2001, 4:35 PM
 */

package games.strategy.engine.delegate;

import java.util.*;
import games.strategy.engine.data.*;
import games.strategy.engine.message.Message;
import games.strategy.engine.transcript.Transcript;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * A class that communicates with the Delegate.
 * DelegateBridge co-ordinates comunication between the Delegate and both the players
 * and the game data.
 * 
 * The reason for communicating through a DelegateBridge is to achieve network
 * transparancy.
 * 
 * The delegateBridge allows the Delegate to talk to the player in a safe manner.
 */
public interface DelegateBridge 
{
	/**
	 * Messages are sent to the current player
	 */
	public Message sendMessage(Message message);
	
	/**
	 * Sends a message to the given player.
	 */
	public Message sendMessage(Message message, PlayerID player);
		
	/**
	 * Changing the player has the effect of commiting the current transaction.
	 * Player is initialized to the player specified in the xml data.
	 */
	public void setPlayerID(PlayerID aPlayer);
	public PlayerID getPlayerID();
	
	/**
	 * Returns the current step name
	 */
	public String getStepName();
	
	public void addChange(Change aChange);
	
	/**
	 * Delegates should not use random data that comes from any other source.
	 */
	public int getRandom(int max);
	public int[] getRandom(int max, int count);
	
	/**
	 * Get the games transcript.
	 */
	public Transcript getTranscript();
}