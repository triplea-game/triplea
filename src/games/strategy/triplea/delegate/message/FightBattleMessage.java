/*
 * FightBattleMessage.java
 *
 * Created on November 19, 2001, 3:27 PM
 */

package games.strategy.triplea.delegate.message;

import java.util.*;

import games.strategy.util.*;
import games.strategy.engine.message.*;
import games.strategy.engine.data.*;

/**
 *  
 * Sent by the game player to the battle delegate to indicate that the battle in 
 * given territory should be fought. <p>
 *
 * Specifies wether the battle is a bombing raid or not since a bombing raid and a 
 * normal battle can occur at the same time in the same territory.
 * 
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class FightBattleMessage implements Message
{
	private Territory m_territory;
	private boolean m_bomb;

	/** Creates new FightBattleMessage */
    public FightBattleMessage(Territory battleSite, boolean bomb) 
	{
		m_bomb = bomb;
		m_territory = battleSite;
    }

	public Territory getTerritory()
	{
		return m_territory;
	}
	
	public boolean getStrategicBombingRaid()
	{
		return m_bomb;
	}
}