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
 * AllianceTracker.java
 *
 * Created on October 13, 2001, 9:37 AM
 */

package games.strategy.engine.data;

import java.util.*;

/**
 * Tracks alliances between players.
 *
 * An alliance is a named entity, players are added to an alliance.
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class AllianceTracker extends GameDataComponent
{

	//maps PlayerID to Collection of alliances names
	private Map m_alliances = new HashMap();

	/** Creates new Alliance Tracker. */
    public AllianceTracker(GameData data)
	{
		super(data);
	}

	/**
	 *  Creates an alliance beteen the two players.  Note that
	 *  addAlliance(a,b) addAlliance(b,c) still results in
	 *  isAllied(a,c) returning false
	 */
	protected void addToAlliance(PlayerID player, String allianceName)
	{
		if(!m_alliances.containsKey(player))
		{
			Collection alliances = new HashSet();
			alliances.add(allianceName);
			m_alliances.put(player, alliances);
		}
		else
		{
			Collection alliances = (Collection) m_alliances.get(player);
			alliances.add(allianceName);
		}
	}

	/**
	 * Returns wether two players are allied.<br>
	 * isAllied(a,a) returns true.
	 */
	public boolean isAllied(PlayerID p1, PlayerID p2)
	{

		if(p1 == null || p2 == null)
			throw new IllegalArgumentException("Arguments cannot be null p1:" + p1 + " p2:" + p2);

		if(p1.equals(p2))
			return true;
		if(!m_alliances.containsKey(p1))
			return false;
		if(!m_alliances.containsKey(p2))
			return false;

		Collection a1 = (Collection) m_alliances.get(p1);
		Collection a2 = (Collection) m_alliances.get(p2);

		return !games.strategy.util.Util.intersection(a1,a2).isEmpty();
	}

	/**
	 *
	 * @return a set of all the games alliances
	 */
	public Set getAliances()
	{
		Iterator keys = m_alliances.keySet().iterator();
		Set rVal = new HashSet();

		while(keys.hasNext())
		{
			rVal.addAll((Collection) m_alliances.get(keys.next()));
		}
		return rVal;

	}


	/*
	 * @param alliance Alliance name
	 * @return all the players in the given alliance
	 */
	public Set getPlayersInAlliance(String alliance)
	{

		Iterator keys = m_alliances.keySet().iterator();
		Set rVal = new HashSet();

		while(keys.hasNext())
		{
			Object player = keys.next();
			Collection alliances = (Collection) m_alliances.get(player);
			if(alliances.contains(alliance))
				rVal.add(player);
		}
		return rVal;
	}
}
