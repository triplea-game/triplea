/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * BattleStepMessage.java
 *
 * Created on January 16, 2002, 10:28 AM
 */

package games.strategy.triplea.delegate.message;

import java.util.*;
import games.strategy.engine.data.Territory;

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
	private Territory m_territory;

	/** Creates a new instance of BattleStepMessage */
	public BattleStepMessage(String step, String title, List steps, Territory territory) 
	{
		super(step);
		m_steps = Collections.unmodifiableList(steps);
		m_title = title;
		m_territory = territory;
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

	public Territory getTerritory()
	{
		return m_territory;
	}
	
}
