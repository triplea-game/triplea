/*
 * PlayerStepEndedMessage.java
 *
 * Created on January 1, 2002, 7:06 PM
 */

package games.strategy.engine.framework;

import java.io.*;

/**
 *
 * @author  Sean Bridges
 */
class PlayerStepEndedMessage implements Serializable
{
	private String m_stepName;

	/** Creates a new instance of PlayerStepEndedMessage */
    PlayerStepEndedMessage(String stepName) 
	{
		m_stepName = stepName;
    }

	public String getStepName()
	{
		return m_stepName;
	}
}
