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
 * PlayerStartStepMessage.java
 *
 * Created on January 1, 2002, 7:01 PM
 */

package games.strategy.engine.framework;

import games.strategy.engine.data.PlayerID;

import java.io.Serializable;

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