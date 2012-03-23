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
package games.strategy.triplea.ai.Dynamix_AI.CommandCenter;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ai.Dynamix_AI.Group.PurchaseGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 
 * @author Stephen
 */
public class FactoryCenter
{
	private static HashMap<PlayerID, FactoryCenter> s_FCInstances = new HashMap<PlayerID, FactoryCenter>();
	
	public static FactoryCenter get(final GameData data, final PlayerID player)
	{
		if (!s_FCInstances.containsKey(player))
			s_FCInstances.put(player, create(data, player));
		return s_FCInstances.get(player);
	}
	
	private static FactoryCenter create(final GameData data, final PlayerID player)
	{
		return new FactoryCenter(data, player);
	}
	
	public static void ClearStaticInstances()
	{
		s_FCInstances.clear();
	}
	
	public static void NotifyStartOfRound()
	{
		s_FCInstances.clear();
	}
	
	@SuppressWarnings("unused")
	private GameData m_data = null;
	@SuppressWarnings("unused")
	private PlayerID m_player = null;
	
	public FactoryCenter(final GameData data, final PlayerID player)
	{
		m_data = data;
		m_player = player;
	}
	
	public List<Territory> ChosenFactoryTerritories = new ArrayList<Territory>();
	public List<Territory> ChosenAAPlaceTerritories = new ArrayList<Territory>();
	public HashMap<Territory, PurchaseGroup> TurnTerritoryPurchaseGroups = new HashMap<Territory, PurchaseGroup>();
	public List<PurchaseGroup> FactoryPurchaseGroups = new ArrayList<PurchaseGroup>();
}
