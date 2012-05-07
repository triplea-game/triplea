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
package games.strategy.triplea.delegate.dataObjects;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.util.IntegerMap;

/**
 * Used to describe a tech roll.
 * advance may be null if the game does not support rolling for
 * specific techs
 */
public class TechRoll
{
	private final TechnologyFrontier m_tech;
	private final int m_rolls;
	private int m_newTokens;
	private final IntegerMap<PlayerID> m_whoPaysHowMuch;
	
	public TechRoll(final TechnologyFrontier advance, final int rolls)
	{
		this(advance, rolls, 0);
	}
	
	public TechRoll(final TechnologyFrontier advance, final int rolls, final int newTokens)
	{
		this(advance, rolls, newTokens, null);
	}
	
	public TechRoll(final TechnologyFrontier advance, final int rolls, final int newTokens, final IntegerMap<PlayerID> whoPaysHowMuch)
	{
		m_rolls = rolls;
		m_tech = advance;
		m_newTokens = newTokens;
		m_whoPaysHowMuch = whoPaysHowMuch;
	}
	
	public int getRolls()
	{
		return m_rolls;
	}
	
	public TechnologyFrontier getTech()
	{
		return m_tech;
	}
	
	public int getNewTokens()
	{
		return m_newTokens;
	}
	
	public void setNewTokens(final int tokens)
	{
		this.m_newTokens = tokens;
	}
	
	public IntegerMap<PlayerID> getWhoPaysHowMuch()
	{
		return m_whoPaysHowMuch;
	}
}
