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
package games.strategy.grid.ui;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.formatter.MyFormatter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Represents a play in a game of a Grid Game.
 * 
 * A play has a start Territory and an end territory,
 * which correspond to the piece to be moved, and the desination for the move.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate: 2011-11-22 18:21:37 +0800 (Tue, 22 Nov 2011) $
 */
public class GridPlayData implements IGridPlayData
{
	private static final long serialVersionUID = -1450796130971955757L;
	private final Territory m_start;
	private final List<Territory> m_middleSteps;
	private final Territory m_end;
	private final PlayerID m_player;
	private final boolean m_pass;
	
	/**
	 * Construct a new play, with the given start location and end location.
	 * 
	 * @param start
	 *            <code>Territory</code> where the play should start
	 * @param end
	 *            <code>Territory</code> where the play should end
	 */
	public GridPlayData(final Territory start, final Territory end, final PlayerID player)
	{
		this(start, new ArrayList<Territory>(), end, player);
	}
	
	public GridPlayData(final Territory start, final List<Territory> middleSteps, final Territory end, final PlayerID player)
	{
		m_start = start;
		m_end = end;
		m_middleSteps = (middleSteps == null ? new ArrayList<Territory>() : middleSteps);
		m_player = player;
		m_pass = false;
	}
	
	public GridPlayData(final Territory start, final PlayerID player)
	{
		this(start, new ArrayList<Territory>(), null, player);
	}
	
	public GridPlayData(final boolean pass, final PlayerID player)
	{
		m_start = null;
		m_end = null;
		m_middleSteps = new ArrayList<Territory>();
		m_player = player;
		m_pass = pass;
	}
	
	public boolean isPass()
	{
		return m_pass;
	}
	
	/**
	 * Returns the start location for this play.
	 * 
	 * @return <code>Territory</code> where this play starts.
	 */
	public Territory getStart()
	{
		return m_start;
	}
	
	/**
	 * Returns the end location for this play.
	 * 
	 * @return <code>Territory</code> where this play ends.
	 */
	public Territory getEnd()
	{
		return m_end;
	}
	
	/**
	 * Returns the player making this move.
	 */
	public PlayerID getPlayerID()
	{
		return m_player;
	}
	
	public List<Territory> getMiddleSteps()
	{
		return m_middleSteps;
	}
	
	/**
	 * Will not return any null territories.
	 */
	public List<Territory> getAllSteps()
	{
		final List<Territory> all = new ArrayList<Territory>();
		if (m_start != null)
			all.add(m_start);
		all.addAll(m_middleSteps);
		if (m_end != null)
			all.add(m_end);
		return all;
	}
	
	/**
	 * Will not return any null territories.
	 */
	public List<Territory> getAllStepsExceptStart()
	{
		final List<Territory> all = new ArrayList<Territory>();
		all.addAll(m_middleSteps);
		if (m_end != null)
			all.add(m_end);
		return all;
	}
	
	/**
	 * Returns true if the other play in the argument is smaller than this play, and has all the same steps in the same order.
	 */
	public boolean isBiggerThanAndContains(final IGridPlayData otherPlay)
	{
		final List<Territory> otherSteps = otherPlay.getAllSteps();
		final List<Territory> mySteps = this.getAllSteps();
		if (otherSteps.size() >= mySteps.size())
			return false;
		for (int i = 0; i < otherSteps.size(); i++)
		{
			if (!otherSteps.get(i).equals(mySteps.get(i)))
				return false;
		}
		return true;
	}
	
	public static Comparator<IGridPlayData> LargestToSmallestPlays = new Comparator<IGridPlayData>()
	{
		public int compare(final IGridPlayData p1, final IGridPlayData p2)
		{
			if ((p1 == null && p2 == null) || p1 == p2)
				return 0;
			if (p1 == null && p2 != null)
				return 1;
			if (p1 != null && p2 == null)
				return -1;
			if (p1.equals(p2))
				return 0;
			final int size1 = p1.getAllSteps().size();
			final int size2 = p2.getAllSteps().size();
			if (size1 == size2)
				return 0;
			if (size1 > size2)
				return -1;
			return 1;
		}
	};
	
	public static Comparator<IGridPlayData> SmallestToLargestPlays = new Comparator<IGridPlayData>()
	{
		public int compare(final IGridPlayData p1, final IGridPlayData p2)
		{
			if ((p1 == null && p2 == null) || p1 == p2)
				return 0;
			if (p1 == null && p2 != null)
				return 1;
			if (p1 != null && p2 == null)
				return -1;
			if (p1.equals(p2))
				return 0;
			final int size1 = p1.getAllSteps().size();
			final int size2 = p2.getAllSteps().size();
			if (size1 == size2)
				return 0;
			if (size1 > size2)
				return 1;
			return -1;
		}
	};
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((m_end == null) ? 0 : m_end.hashCode());
		result = prime * result + ((m_middleSteps == null) ? 0 : m_middleSteps.hashCode());
		result = prime * result + ((m_player == null) ? 0 : m_player.hashCode());
		result = prime * result + ((m_start == null) ? 0 : m_start.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(final Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof GridPlayData))
			return false;
		final GridPlayData other = (GridPlayData) obj;
		if (m_end == null)
		{
			if (other.m_end != null)
				return false;
		}
		else if (!m_end.equals(other.m_end))
			return false;
		if (m_middleSteps == null)
		{
			if (other.m_middleSteps != null)
				return false;
		}
		else if (!m_middleSteps.equals(other.m_middleSteps))
			return false;
		if (m_player == null)
		{
			if (other.m_player != null)
				return false;
		}
		else if (!m_player.equals(other.m_player))
			return false;
		if (m_start == null)
		{
			if (other.m_start != null)
				return false;
		}
		else if (!m_start.equals(other.m_start))
			return false;
		return true;
	}
	
	@Override
	public String toString()
	{
		if (m_pass)
			return "Pass of turn" + (m_player == null ? "" : " by " + m_player.getName());
		return (m_player == null ? "" : m_player.getName() + " moving ")
					+ (m_start == null ? "" : (m_start.getUnits().getUnitCount() > 0 ? MyFormatter.unitsToTextNoOwner(m_start.getUnits().getUnits()) + " " : ""))
					+ (m_end == null ? "to " : "from ") + (m_start == null ? "null" : m_start.getName())
					+ (m_end == null ? "" : " to " + m_end.getName()) + (m_middleSteps == null || m_middleSteps.isEmpty() ? "" : " by way of: " + MyFormatter.defaultNamedToTextList(m_middleSteps));
	}
	
}
