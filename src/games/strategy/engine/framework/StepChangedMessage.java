/*
 * StepChangedMessage.java
 *
 * Created on January 1, 2002, 12:30 PM
 */

package games.strategy.engine.framework;

import java.io.*;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.GameObjectInputStream;

/**
 *
 * @author  Sean Bridges
 */
class StepChangedMessage implements Serializable
{
	
	private static final long serialVersionUID = 3330970682208872242L;
	
	private String m_stepName;
	private String m_delegateName;
	private PlayerID m_player;
	
	/** Creates a new instance of StepChangedMessage */
    StepChangedMessage(String stepName, String delegateName, PlayerID player) 
	{
		m_delegateName = delegateName;
		m_player = player;
		m_stepName = stepName;
    }
	
	public String getStepName()
	{
		return m_stepName;
	}
	
	public String getDelegateName()
	{
		return m_delegateName;
	}

	public PlayerID getPlayer()
	{
		return m_player;
	}


}
