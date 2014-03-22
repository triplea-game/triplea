/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
/*
 * GameStep.java
 * 
 * Created on October 14, 2001, 7:28 AM
 */
package games.strategy.engine.data;

import games.strategy.engine.delegate.IDelegate;

import java.util.Properties;

/**
 * A single step in a game.
 * <p>
 * 
 * Typically turn based strategy games are composed of a set of distinct phases (in chess this would be two, white move, black move).
 * 
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class GameStep extends GameDataComponent
{
	private static final long serialVersionUID = -7944468945162840931L;
	private final String m_name;
	private final String m_displayName;
	private final PlayerID m_player;
	private final String m_delegate;
	private int m_hashCode = -1;
	private int m_runCount = 0;
	private int m_maxRunCount = -1;
	private final Properties m_properties;
	
	public static final String PROPERTY_skipPosting = "skipPosting";
	public static final String PROPERTY_turnSummaryPlayers = "turnSummaryPlayers";
	public static final String PROPERTY_airborneMove = "airborneMove";
	public static final String PROPERTY_combatMove = "combatMove";
	public static final String PROPERTY_nonCombatMove = "nonCombatMove";
	public static final String PROPERTY_fireRockets = "fireRockets";
	public static final String PROPERTY_repairUnits = "repairUnits";
	public static final String PROPERTY_giveBonusMovement = "giveBonusMovement";
	public static final String PROPERTY_removeAirThatCanNotLand = "removeAirThatCanNotLand";
	public static final String PROPERTY_resetUnitStateAtStart = "resetUnitStateAtStart";
	public static final String PROPERTY_resetUnitStateAtEnd = "resetUnitStateAtEnd";
	public static final String PROPERTY_bid = "bid";
	public static final String PROPERTY_combinedTurns = "combinedTurns";
	public static final String PROPERTY_repairPlayers = "repairPlayers";
	
	/**
	 * Creates new GameStep
	 * 
	 * @param name
	 *            name of the game step
	 * @param displayName
	 *            name that gets displayed
	 * @param player
	 *            player who executes the game step
	 * @param delegate
	 *            delegate for the game step
	 * @param data
	 *            game data
	 * @param stepProperties
	 *            properties of the game step
	 */
	public GameStep(final String name, final String displayName, final PlayerID player, final IDelegate delegate, final GameData data, final Properties stepProperties)
	{
		super(data);
		m_name = name;
		m_displayName = displayName;
		m_player = player;
		m_delegate = delegate.getName();
		m_properties = stepProperties;
	}
	
	public String getName()
	{
		return m_name;
	}
	
	public PlayerID getPlayerID()
	{
		return m_player;
	}
	
	public IDelegate getDelegate()
	{
		return getData().getDelegateList().getDelegate(m_delegate);
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if (o == null || !(o instanceof GameStep))
			return false;
		final GameStep other = (GameStep) o;
		return other.m_name.equals(this.m_name) && other.m_delegate.equals(this.m_delegate) && other.m_player.equals(this.m_player);
	}
	
	public boolean hasReachedMaxRunCount()
	{
		if (m_maxRunCount == -1)
			return false;
		return m_maxRunCount <= m_runCount;
	}
	
	public int getRunCount()
	{
		return m_runCount;
	}
	
	public void incrementRunCount()
	{
		m_runCount++;
	}
	
	public void setMaxRunCount(final int count)
	{
		m_maxRunCount = count;
	}
	
	public int getMaxRunCount()
	{
		return m_maxRunCount;
	}
	
	@Override
	public int hashCode()
	{
		if (m_hashCode == -1)
		{
			final String s = m_name + m_delegate + m_player;
			m_hashCode = s.hashCode();
		}
		return m_hashCode;
	}
	
	public String getDisplayName()
	{
		if (m_displayName == null)
			return getDelegate().getDisplayName();
		return m_displayName;
	}
	
	/**
	 * Allowed Properties so far:<br>
	 * EndTurn delegates -> skipPosting = true/false<br>
	 * EndTurn delegates -> turnSummaryPlayers = colon separated list of players for this turn summary<br>
	 * Move delegates -> airborneMove = true/false<br>
	 * Move delegates -> combatMove = true/false<br>
	 * Move delegates -> nonCombatMove = true/false<br>
	 * Move delegates -> fireRocketsAfter = true/false<br>
	 * Move & EndTurn delegates -> repairUnits = true/false<br>
	 * Move delegates -> giveBonusMovement = true/false<br>
	 * Move & Place delegates -> removeAirThatCanNotLand = true/false<br>
	 * Move delegates -> resetUnitStateAtStart = true/false<br>
	 * Move delegates -> resetUnitStateAtEnd = true/false<br>
	 * Purchase & Place delegates -> bid = true/false<br>
	 * Purchase delegates -> repairPlayers = colon separated list of players which you can repair for<br>
	 * Move delegates -> combinedTurns = colon separated list of players which have intermeshed phases<br>
	 */
	public Properties getProperties()
	{
		return m_properties;
	}
	
	@Override
	public String toString()
	{
		return "GameStep:" + m_name + " delegate:" + m_delegate + " player:" + m_player + " displayName:" + m_displayName;
	}
}
