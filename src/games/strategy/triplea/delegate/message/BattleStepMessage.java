/*
 * BattleStepMessage.java
 *
 * Created on January 16, 2002, 10:28 AM
 */

package games.strategy.triplea.delegate.message;

import java.util.*;

/**
 * Sent by the battle delegate to the game player to indicate what steps
 * are possible in the current battle.  This will be the first message 
 * received in the battle.
 *
 * 
 *
 * @version 1.0
 * @author  Sean Bridges
 */
public class BattleStepMessage extends BattleMessage
{
	//a collection of strings
	private List m_steps;
	private String m_title; //decription of the battle

	/** Creates a new instance of BattleStepMessage */
    public BattleStepMessage(String step, String title, List steps) 
	{
		
		super(step);
		m_steps = Collections.unmodifiableList(steps);
		m_title = title;
    }
	
	/**
	 * @return - a list of steps that this battle will go through.
	 */
	public List getSteps()
	{
		return m_steps;
	}
	
	public String getTitle()
	{
		return m_title;
	}

}
