/*
 * TestDelegateBridge.java
 *
 * Created on November 10, 2001, 7:39 PM
 */

package games.strategy.engine.data;


import java.util.*;

import games.strategy.util.*;
import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;
import games.strategy.engine.message.*;
import games.strategy.net.DummyMessenger;
import games.strategy.engine.transcript.Transcript;




/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 *  Not for actual use, suitable for testing.
 *  Never returns messages, but can get random and implements changes 
 *  immediately.
 */
public class TestDelegateBridge implements DelegateBridge
{
	GameData m_data;
	PlayerID m_id;
	String m_stepName = "no name specified";
	
	Random m_rand = new Random(System.currentTimeMillis());
	
	/** Creates new TestDelegateBridge */
    public TestDelegateBridge(GameData data, PlayerID id) 
	{
		m_data = data;
		m_id = id;
    }
	
	/**
	 * Delegates should not use random data that comes from any other source.
	 */
	public int getRandom(int max) {
		return m_rand.nextInt(max);
	}	
	
	/**
	 * Changing the player has the effect of commiting the current transaction.
	 * Player is initialized to the player specified in the xml data.
	 */
	public void setPlayerID(PlayerID aPlayer) 
	{
		m_id = aPlayer;
		
	}	

	public boolean inTransaction() {
		return false;
	}
	
	public PlayerID getPlayerID() { 
		return m_id;
	}
	
	public int[] getRandom(int max, int count) {
		int[] r = new int[count];
		for(int i = 0; i < count; i++)
		{
			r[i] = getRandom(max);
		}
		return r;
	}
	
	public void addChange(Change aChange) {
		aChange.perform(m_data);
	}
	
	public void commit() {
	}
	
	public void startTransaction() {
	}
	
	/**
	 * Messages are sent to the current player
	 */
	public Message sendMessage(Message message) {
		return null;
	}
	
	public void rollback() {
	}
	
	/**
	 * Sends a message to the given player.
	 */
	public Message sendMessage(Message message, PlayerID player) 
	{
		return null;
	}

	public void setStepName(String name)
	{
		m_stepName = name;
	}
	
	/**
	 * Returns the current step name
	 */
	public String getStepName() 
	{
		return m_stepName;
	}
		
	/**
	 * Get the games transcript.
	 */
	public Transcript getTranscript()
	{
		return new Transcript(new DummyMessenger());
	}
	
}
