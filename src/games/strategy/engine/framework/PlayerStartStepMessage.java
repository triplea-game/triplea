/*
 * PlayerStartStepMessage.java
 *
 * Created on January 1, 2002, 7:01 PM
 */

package games.strategy.engine.framework;

import java.io.*;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.GameObjectInputStream;

/**
 *
 * @author  Sean Bridges
 */
class PlayerStartStepMessage implements Serializable
{

	private PlayerID m_id;
	private String m_stepName;
	
	/** Creates a new instance of PlayerStartStepMessage */
    PlayerStartStepMessage(String stepName, PlayerID player) 
	{
		m_id = player;
		m_stepName = stepName;
    }

	public String getStepName()
	{
		return m_stepName;
	}
	
	public PlayerID getPlayerID()
	{
		return m_id;
	}
	
	public String toString()
	{
		return "PlayerStartMessage id:" + m_id + " stepName:" + m_stepName;
	}
}