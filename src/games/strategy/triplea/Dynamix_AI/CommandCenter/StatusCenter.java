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

package games.strategy.triplea.Dynamix_AI.CommandCenter;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.Dynamix_AI.Others.TerritoryStatus;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 
 * @author Stephen
 */
public class StatusCenter
{
	private static HashMap<PlayerID, StatusCenter> s_SCInstances = new HashMap<PlayerID, StatusCenter>();
	
	public static StatusCenter get(GameData data, PlayerID player)
	{
		if (!s_SCInstances.containsKey(player))
			s_SCInstances.put(player, create(data, player));
		return s_SCInstances.get(player);
	}
	
	private static StatusCenter create(GameData data, PlayerID player)
	{
		return new StatusCenter(data, player);
	}
	
	public static void ClearStaticInstances()
	{
		s_SCInstances.clear();
	}
	
	public static void NotifyStartOfRound()
	{
		s_SCInstances.clear();
	}
	
	private GameData m_data = null;
	private PlayerID m_player = null;
	
	public StatusCenter(GameData data, PlayerID player)
	{
		m_data = data;
		m_player = player;
	}
	
	private HashMap<String, TerritoryStatus> TerritoryStatuses = new HashMap<String, TerritoryStatus>();
	
	public TerritoryStatus GetStatusOfTerritory(Territory ter)
	{
		return GetStatusOfTerritory(ter.getName());
	}
	
	public TerritoryStatus GetStatusOfTerritory(String terName)
	{
		if (!TerritoryStatuses.containsKey(terName))
			TerritoryStatuses.put(terName, new TerritoryStatus());
		return TerritoryStatuses.get(terName);
	}
	
	public List<Territory> GetTerritoriesThatHaveStatusesMatching(GameData data, Match<TerritoryStatus> match)
	{
		List<Territory> result = new ArrayList<Territory>();
		for (String key : TerritoryStatuses.keySet())
		{
			TerritoryStatus status = TerritoryStatuses.get(key);
			if (status != null && match.match(status))
				result.add(data.getMap().getTerritory(key));
		}
		return result;
	}
}
