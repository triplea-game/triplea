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
import games.strategy.triplea.Dynamix_AI.DUtils;
import games.strategy.triplea.Dynamix_AI.Others.StrategyType;

import java.util.HashMap;

/**
 * 
 * @author Stephen
 */
public class StrategyCenter
{
	private static HashMap<PlayerID, StrategyCenter> s_SCInstances = new HashMap<PlayerID, StrategyCenter>();
	
	public static StrategyCenter get(GameData data, PlayerID player)
	{
		if (!s_SCInstances.containsKey(player))
			s_SCInstances.put(player, create(data, player));
		return s_SCInstances.get(player);
	}
	
	private static StrategyCenter create(GameData data, PlayerID player)
	{
		return new StrategyCenter(data, player);
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
	
	public StrategyCenter(GameData data, PlayerID player)
	{
		m_data = data;
		m_player = player;
	}
	
	private HashMap<PlayerID, StrategyType> CalculatedStrategyAssignments = new HashMap<PlayerID, StrategyType>();
	
	public HashMap<PlayerID, StrategyType> GetCalculatedStrategyAssignments()
	{
		if (CalculatedStrategyAssignments == null || CalculatedStrategyAssignments.isEmpty())
			CalculatedStrategyAssignments = DUtils.CalculateStrategyAssignments(m_data, m_player);
		
		return CalculatedStrategyAssignments;
	}
}
