package games.strategy.triplea.ai.proAI;

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
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.proAI.util.LogUtils;
import games.strategy.triplea.ai.proAI.util.ProAttackOptionsUtils;
import games.strategy.triplea.ai.proAI.util.ProBattleUtils;
import games.strategy.triplea.ai.proAI.util.ProMoveUtils;
import games.strategy.triplea.ai.proAI.util.ProTransportUtils;
import games.strategy.triplea.ai.proAI.util.ProUtils;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Pro non-combat move AI.
 * 
 * <ol>
 * <li>None yet</li>
 * </ol>
 * 
 * @author Ron Murhammer
 * @since 2014
 */
public class ProNonCombatMoveAI
{
	public final static double WIN_PERCENTAGE = 95.0;
	
	// Utilities
	private final ProUtils utils;
	private final ProBattleUtils battleUtils;
	private final ProTransportUtils transportUtils;
	private final ProAttackOptionsUtils attackOptionsUtils;
	private final ProMoveUtils moveUtils;
	
	// Current map settings
	private boolean areNeutralsPassableByAir;
	
	// Current data
	private GameData data;
	private PlayerID player;
	private Territory myCapital;
	private List<PlayerID> enemyPlayers;
	private List<Territory> allTerritories;
	private Map<Unit, Territory> unitTerritoryMap;
	
	public ProNonCombatMoveAI(final ProUtils utils, final ProBattleUtils battleUtils, final ProTransportUtils transportUtils, final ProAttackOptionsUtils attackOptionsUtils,
				final ProMoveUtils moveUtils)
	{
		this.utils = utils;
		this.battleUtils = battleUtils;
		this.transportUtils = transportUtils;
		this.attackOptionsUtils = attackOptionsUtils;
		this.moveUtils = moveUtils;
	}
	
	public void doNonCombatMove(final IMoveDelegate moveDel, final GameData data, final PlayerID player)
	{
		LogUtils.log(Level.FINE, "Starting non-combat move phase");
		
		// Current data at the start of combat move
		this.data = data;
		this.player = player;
		areNeutralsPassableByAir = (Properties.getNeutralFlyoverAllowed(data) && !Properties.getNeutralsImpassable(data));
		myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		enemyPlayers = utils.getEnemyPlayers(player);
		allTerritories = data.getMap().getTerritories();
		unitTerritoryMap = createUnitTerritoryMap(player);
		
		// Initialize data containers
		final Map<Territory, ProAttackTerritoryData> moveMap = new HashMap<Territory, ProAttackTerritoryData>();
		final Map<Unit, Set<Territory>> unitMoveMap = new HashMap<Unit, Set<Territory>>();
		final Map<Unit, Set<Territory>> transportMoveMap = new HashMap<Unit, Set<Territory>>();
		final List<ProAmphibData> transportMapList = new ArrayList<ProAmphibData>();
		final Map<Territory, Set<Territory>> landRoutesMap = new HashMap<Territory, Set<Territory>>();
		
		// Find the maximum number of units that can move to each allied territory
		final Match<Unit> myUnitsThatCanMoveMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.unitHasMovementLeft);
		final Match<Territory> myUnitTerritoriesMatch = Matches.territoryHasUnitsThatMatch(myUnitsThatCanMoveMatch);
		final List<Territory> myUnitTerritories = Match.getMatches(allTerritories, myUnitTerritoriesMatch);
		attackOptionsUtils.findDefendOptions(player, areNeutralsPassableByAir, myUnitTerritories, moveMap, unitMoveMap, transportMoveMap, landRoutesMap, transportMapList);
		
		// Find number of units in each allied territory that can't move anywhere else
		findUnitsThatCantMove(moveMap, unitMoveMap);
		
		// Prioritize territories to defend
		final List<ProAttackTerritoryData> prioritizedTerritories = prioritizeDefendOptions(player, moveMap);
		
		// Find max enemy attackers
		
		// Log results
		LogUtils.log(Level.FINE, "Logging results");
		final Map<Territory, ProAttackTerritoryData> enemyAttackMap = new HashMap<Territory, ProAttackTerritoryData>();
		logAttackMoves(moveMap, unitMoveMap, transportMapList, prioritizedTerritories, enemyAttackMap);
	}
	
	private void findUnitsThatCantMove(final Map<Territory, ProAttackTerritoryData> moveMap, final Map<Unit, Set<Territory>> unitMoveMap)
	{
		final Match<Unit> unitHasNoMovementMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.unitHasMovementLeft.invert(), Matches.UnitIsNotInfrastructure);
		for (final Territory t : moveMap.keySet())
		{
			final List<Unit> units = t.getUnits().getMatches(unitHasNoMovementMatch);
			moveMap.get(t).setCantMoveUnits(units);
		}
		for (final Iterator<Unit> it = unitMoveMap.keySet().iterator(); it.hasNext();)
		{
			final Unit u = it.next();
			if (unitMoveMap.get(u).size() == 1 && unitMoveMap.get(u).iterator().next().equals(unitTerritoryMap.get(u)))
			{
				final ProAttackTerritoryData patd = moveMap.get(unitTerritoryMap.get(u));
				patd.getMaxUnits().remove(u);
				patd.addCantMoveUnit(u);
				it.remove();
			}
		}
	}
	
	private List<ProAttackTerritoryData> prioritizeDefendOptions(final PlayerID player, final Map<Territory, ProAttackTerritoryData> moveMap)
	{
		LogUtils.log(Level.FINE, "Prioritizing territories that can be defended");
		
		// Calculate value of attacking territory
		for (final Territory t : moveMap.keySet())
		{
			// Determine if it is adjacent to my capital
			int isAdjacentToMyCapital = 0;
			if (!data.getMap().getNeighbors(t, Matches.territoryIs(myCapital)).isEmpty())
				isAdjacentToMyCapital = 1;
			
			// Determine if it has a factory
			int isFactory = 0;
			if (t.getUnits().someMatch(Matches.UnitCanProduceUnits))
				isFactory = 1;
			
			// Determine production value and if it is an enemy capital
			int production = 0;
			int isCapital = 0;
			final TerritoryAttachment ta = TerritoryAttachment.get(t);
			if (ta != null)
			{
				production = ta.getProduction();
				if (ta.isCapital())
					isCapital = 1;
			}
			
			// Calculate attack value for prioritization
			// TODO: Add can't move unit value
			final double territoryValue = (2 * production + 5 * isFactory) * (1 + 4 * isCapital) * (1 + 2 * isAdjacentToMyCapital);
			moveMap.get(t).setAttackValue(territoryValue);
		}
		
		// Sort attack territories by value
		final List<ProAttackTerritoryData> prioritizedTerritories = new ArrayList<ProAttackTerritoryData>(moveMap.values());
		Collections.sort(prioritizedTerritories, new Comparator<ProAttackTerritoryData>()
		{
			public int compare(final ProAttackTerritoryData t1, final ProAttackTerritoryData t2)
			{
				final double value1 = t1.getAttackValue();
				final double value2 = t2.getAttackValue();
				return Double.compare(value2, value1);
			}
		});
		
		// Log prioritized territories
		for (final ProAttackTerritoryData attackTerritoryData : prioritizedTerritories)
		{
			LogUtils.log(Level.FINER, "AttackValue=" + attackTerritoryData.getAttackValue() + ", " + attackTerritoryData.getTerritory().getName());
		}
		
		return prioritizedTerritories;
	}
	
	private void logAttackMoves(final Map<Territory, ProAttackTerritoryData> attackMap, final Map<Unit, Set<Territory>> unitAttackMap, final List<ProAmphibData> transportMapList,
				final List<ProAttackTerritoryData> prioritizedTerritories, final Map<Territory, ProAttackTerritoryData> enemyAttackMap)
	{
		// Print prioritization
		LogUtils.log(Level.FINER, "Prioritized territories:");
		for (final ProAttackTerritoryData attackTerritoryData : prioritizedTerritories)
		{
			LogUtils.log(Level.FINEST, "  " + attackTerritoryData.getTUVSwing() + "  " + attackTerritoryData.getAttackValue() + "  " + attackTerritoryData.getTerritory().getName());
		}
		
		// Print transport map
		LogUtils.log(Level.FINER, "Transport territories:");
		int tcount = 0;
		int count = 0;
		for (final ProAmphibData proTransportData : transportMapList)
		{
			final Map<Territory, Set<Territory>> transportMap = proTransportData.getTransportMap();
			tcount++;
			LogUtils.log(Level.FINEST, "Transport #" + tcount);
			for (final Territory t : transportMap.keySet())
			{
				count++;
				LogUtils.log(Level.FINEST, count + ". Can attack " + t.getName());
				final Set<Territory> territories = transportMap.get(t);
				LogUtils.log(Level.FINEST, "  --- From territories ---");
				for (final Territory fromTerritory : territories)
				{
					LogUtils.log(Level.FINEST, "    " + fromTerritory.getName());
				}
			}
		}
		
		// Print enemy territories with enemy units vs my units
		LogUtils.log(Level.FINER, "Enemy counter attack units:");
		count = 0;
		for (final Territory t : enemyAttackMap.keySet())
		{
			count++;
			LogUtils.log(Level.FINEST, count + ". ---" + t.getName());
			final Set<Unit> combinedUnits = new HashSet<Unit>(attackMap.get(t).getMaxUnits());
			combinedUnits.addAll(attackMap.get(t).getMaxAmphibUnits());
			LogUtils.log(Level.FINEST, "  --- Enemy max units ---");
			final Map<String, Integer> printMap = new HashMap<String, Integer>();
			for (final Unit unit : combinedUnits)
			{
				if (printMap.containsKey(unit.toStringNoOwner()))
				{
					printMap.put(unit.toStringNoOwner(), printMap.get(unit.toStringNoOwner()) + 1);
				}
				else
				{
					printMap.put(unit.toStringNoOwner(), 1);
				}
			}
			for (final String key : printMap.keySet())
			{
				LogUtils.log(Level.FINEST, "    " + printMap.get(key) + " " + key);
			}
		}
		
		// Print enemy territories with enemy units vs my units
		LogUtils.log(Level.FINER, "Territories that can be attacked:");
		count = 0;
		for (final Territory t : attackMap.keySet())
		{
			count++;
			LogUtils.log(Level.FINEST, count + ". ---" + t.getName());
			final Set<Unit> combinedUnits = new HashSet<Unit>(attackMap.get(t).getMaxUnits());
			combinedUnits.addAll(attackMap.get(t).getMaxAmphibUnits());
			LogUtils.log(Level.FINEST, "  --- My max units ---");
			final Map<String, Integer> printMap = new HashMap<String, Integer>();
			for (final Unit unit : combinedUnits)
			{
				if (printMap.containsKey(unit.toStringNoOwner()))
				{
					printMap.put(unit.toStringNoOwner(), printMap.get(unit.toStringNoOwner()) + 1);
				}
				else
				{
					printMap.put(unit.toStringNoOwner(), 1);
				}
			}
			for (final String key : printMap.keySet())
			{
				LogUtils.log(Level.FINEST, "    " + printMap.get(key) + " " + key);
			}
			final List<Unit> units3 = attackMap.get(t).getUnits();
			LogUtils.log(Level.FINEST, "  --- My actual units ---");
			final Map<String, Integer> printMap3 = new HashMap<String, Integer>();
			for (final Unit unit : units3)
			{
				if (printMap3.containsKey(unit.toStringNoOwner()))
				{
					printMap3.put(unit.toStringNoOwner(), printMap3.get(unit.toStringNoOwner()) + 1);
				}
				else
				{
					printMap3.put(unit.toStringNoOwner(), 1);
				}
			}
			for (final String key : printMap3.keySet())
			{
				LogUtils.log(Level.FINEST, "    " + printMap3.get(key) + " " + key);
			}
			LogUtils.log(Level.FINEST, "  --- Enemy units ---");
			final Map<String, Integer> printMap2 = new HashMap<String, Integer>();
			final List<Unit> units2 = t.getUnits().getMatches(Matches.enemyUnit(player, data));
			for (final Unit unit : units2)
			{
				if (printMap2.containsKey(unit.toStringNoOwner()))
				{
					printMap2.put(unit.toStringNoOwner(), printMap2.get(unit.toStringNoOwner()) + 1);
				}
				else
				{
					printMap2.put(unit.toStringNoOwner(), 1);
				}
			}
			for (final String key : printMap2.keySet())
			{
				LogUtils.log(Level.FINEST, "    " + printMap2.get(key) + " " + key);
			}
		}
	}
	
	private Map<Unit, Territory> createUnitTerritoryMap(final PlayerID player)
	{
		final List<Territory> allTerritories = data.getMap().getTerritories();
		final CompositeMatchAnd<Territory> myUnitTerritoriesMatch = new CompositeMatchAnd<Territory>(Matches.territoryHasUnitsOwnedBy(player));
		final List<Territory> myUnitTerritories = Match.getMatches(allTerritories, myUnitTerritoriesMatch);
		final Map<Unit, Territory> unitTerritoryMap = new HashMap<Unit, Territory>();
		for (final Territory t : myUnitTerritories)
		{
			final List<Unit> myUnits = t.getUnits().getMatches(Matches.unitIsOwnedBy(player));
			for (final Unit u : myUnits)
				unitTerritoryMap.put(u, t);
		}
		return unitTerritoryMap;
	}
	
}
