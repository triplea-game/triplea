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
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Dynamix_AI.DUtils;
import games.strategy.triplea.Dynamix_AI.Group.UnitGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;

/**
 * 
 * @author Stephen
 */
public class TacticalCenter
{
	private static HashMap<PlayerID, TacticalCenter> s_TCInstances = new HashMap<PlayerID, TacticalCenter>();
	
	public static TacticalCenter get(GameData data, PlayerID player)
	{
		if (!s_TCInstances.containsKey(player))
			s_TCInstances.put(player, create(data, player));
		return s_TCInstances.get(player);
	}
	
	private static TacticalCenter create(GameData data, PlayerID player)
	{
		return new TacticalCenter(data, player);
	}
	
	public static void ClearStaticInstances()
	{
		s_TCInstances.clear();
	}
	
	public static void NotifyStartOfRound()
	{
		s_TCInstances.clear();
	}
	
	private GameData m_data = null;
	@SuppressWarnings("unused")
	private PlayerID m_player = null;
	
	public TacticalCenter(GameData data, PlayerID player)
	{
		m_data = data;
		m_player = player;
	}
	
	public List<UnitGroup> AllDelegateUnitGroups = new ArrayList<UnitGroup>();
	
	private HashSet<Unit> UnitsToFreezeSoon = new HashSet<Unit>();
	
	public void PerformBufferedFreezes()
	{
		if (UnitsToFreezeSoon.isEmpty())
			return;
		FrozenUnits.addAll(UnitsToFreezeSoon);
		DUtils.Log(Level.FINER, "          Freezing buffered units for the rest of this phase. Units: {0} New Total Size: {1}", DUtils.UnitList_ToString(UnitsToFreezeSoon), FrozenUnits.size());
		UnitsToFreezeSoon.clear();
	}
	
	private HashSet<Unit> FrozenUnits = new HashSet<Unit>();
	
	public void FreezeUnits(List<Unit> units)
	{
		if (UnitGroup.IsBufferringMoves())
			UnitsToFreezeSoon.addAll(units);
		else
		{
			FrozenUnits.addAll(units);
			DUtils.Log(Level.FINER, "          Freezing units for the rest of this phase. Units: {0} New Total Size: {1}", DUtils.UnitList_ToString(units), FrozenUnits.size());
		}
	}
	
	public HashSet<Unit> GetFrozenUnits()
	{
		return FrozenUnits;
	}
	
	public void ClearFrozenUnits()
	{
		DUtils.Log(Level.FINER, "          Clearing frozen units. Frozen Units: {0}", DUtils.UnitList_ToString(FrozenUnits));
		FrozenUnits.clear();
	}
	
	public HashMap<Territory, Float> BattleRetreatChanceAssignments = new HashMap<Territory, Float>();
	
	private HashMap<Unit, Territory> UnitLocationsAtStartOfTurn = new HashMap<Unit, Territory>();
	
	public void SetUnitStartLocation_IfNotAlreadySet(Unit unit, Territory startTer)
	{
		if (UnitLocationsAtStartOfTurn.containsKey(unit))
			return;
		
		UnitLocationsAtStartOfTurn.put(unit, startTer);
	}
	
	public Territory GetUnitLocationAtStartOfTurn(Unit unit)
	{
		if (!UnitLocationsAtStartOfTurn.containsKey(unit))
			UnitLocationsAtStartOfTurn.put(unit, DUtils.GetUnitLocation(m_data, unit)); // If it's not set, we must not have moved it yet
			
		return UnitLocationsAtStartOfTurn.get(unit);
	}
	
	public void ClearStartOfTurnUnitLocations()
	{
		UnitLocationsAtStartOfTurn.clear();
	}
}
