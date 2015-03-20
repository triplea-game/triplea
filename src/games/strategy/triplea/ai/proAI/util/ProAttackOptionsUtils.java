package games.strategy.triplea.ai.proAI.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.ai.proAI.ProAmphibData;
import games.strategy.triplea.ai.proAI.ProAttackTerritoryData;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.UnitBattleComparator;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

/**
 * Pro AI attack options utilities.
 * 
 * <ol>
 * <li>Add support for considering carrier landing when calculating air routes</li>
 * </ol>
 * 
 * @author Ron Murhammer
 * @since 2014
 */
public class ProAttackOptionsUtils
{
	private final ProAI ai;
	private final ProUtils utils;
	private final ProBattleUtils battleUtils;
	private final ProTransportUtils transportUtils;
	private final ProPurchaseUtils purchaseUtils;
	
	public ProAttackOptionsUtils(final ProAI ai, final ProUtils utils, final ProBattleUtils battleUtils, final ProTransportUtils transportUtils, final ProPurchaseUtils purchaseUtils)
	{
		this.ai = ai;
		this.utils = utils;
		this.battleUtils = battleUtils;
		this.transportUtils = transportUtils;
		this.purchaseUtils = purchaseUtils;
	}
	
	public Map<Unit, Set<Territory>> sortUnitMoveOptions(final PlayerID player, final Map<Unit, Set<Territory>> unitAttackOptions)
	{
		final GameData data = ai.getGameData();
		final IntegerMap<UnitType> playerCostMap = BattleCalculator.getCostsForTUV(player, data);
		final List<Map.Entry<Unit, Set<Territory>>> list = new LinkedList<Map.Entry<Unit, Set<Territory>>>(unitAttackOptions.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<Unit, Set<Territory>>>()
		{
			public int compare(final Map.Entry<Unit, Set<Territory>> o1, final Map.Entry<Unit, Set<Territory>> o2)
			{
				// Sort by number of move options then cost of unit then unit type
				if (o1.getValue().size() != o2.getValue().size())
					return (o1.getValue().size() - o2.getValue().size());
				else if (playerCostMap.getInt(o1.getKey().getType()) != playerCostMap.getInt(o2.getKey().getType()))
					return (playerCostMap.getInt(o1.getKey().getType()) - playerCostMap.getInt(o2.getKey().getType()));
				return o1.getKey().getType().getName().compareTo(o2.getKey().getType().getName());
			}
		});
		final Map<Unit, Set<Territory>> sortedUnitAttackOptions = new LinkedHashMap<Unit, Set<Territory>>();
		for (final Map.Entry<Unit, Set<Territory>> entry : list)
			sortedUnitAttackOptions.put(entry.getKey(), entry.getValue());
		
		return sortedUnitAttackOptions;
	}
	
	public Map<Unit, Set<Territory>> sortUnitNeededOptions(final PlayerID player, final Map<Unit, Set<Territory>> unitAttackOptions, final Map<Territory, ProAttackTerritoryData> attackMap)
	{
		final GameData data = ai.getGameData();
		final IntegerMap<UnitType> playerCostMap = BattleCalculator.getCostsForTUV(player, data);
		final List<Map.Entry<Unit, Set<Territory>>> list = new LinkedList<Map.Entry<Unit, Set<Territory>>>(unitAttackOptions.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<Unit, Set<Territory>>>()
		{
			public int compare(final Map.Entry<Unit, Set<Territory>> o1, final Map.Entry<Unit, Set<Territory>> o2)
			{
				// Find number of territories that still need units
				int numOptions1 = 0;
				for (final Territory t : o1.getValue())
				{
					final ProAttackTerritoryData patd = attackMap.get(t);
					
					if (patd.getBattleResult() == null)
						patd.setBattleResult(battleUtils.estimateAttackBattleResults(player, t, patd.getUnits(), patd.getMaxEnemyDefenders(player, data), patd.getBombardTerritoryMap().keySet()));
					if (!patd.isCurrentlyWins())
						numOptions1++;
				}
				int numOptions2 = 0;
				for (final Territory t : o2.getValue())
				{
					final ProAttackTerritoryData patd = attackMap.get(t);
					
					if (patd.getBattleResult() == null)
						patd.setBattleResult(battleUtils.estimateAttackBattleResults(player, t, patd.getUnits(), patd.getMaxEnemyDefenders(player, data), patd.getBombardTerritoryMap().keySet()));
					if (!patd.isCurrentlyWins())
						numOptions2++;
				}
				
				// Sort by number of move options then cost of unit then unit type
				if (numOptions1 != numOptions2)
					return (numOptions1 - numOptions2);
				if (playerCostMap.getInt(o1.getKey().getType()) != playerCostMap.getInt(o2.getKey().getType()))
					return (playerCostMap.getInt(o1.getKey().getType()) - playerCostMap.getInt(o2.getKey().getType()));
				return o1.getKey().getType().getName().compareTo(o2.getKey().getType().getName());
			}
		});
		final Map<Unit, Set<Territory>> sortedUnitAttackOptions = new LinkedHashMap<Unit, Set<Territory>>();
		for (final Map.Entry<Unit, Set<Territory>> entry : list)
			sortedUnitAttackOptions.put(entry.getKey(), entry.getValue());
		
		return sortedUnitAttackOptions;
	}
	
	public Map<Unit, Set<Territory>> sortUnitNeededOptionsThenAttack(final PlayerID player, final Map<Unit, Set<Territory>> unitAttackOptions, final Map<Territory, ProAttackTerritoryData> attackMap,
				final Map<Unit, Territory> unitTerritoryMap)
	{
		final GameData data = ai.getGameData();
		final IntegerMap<UnitType> playerCostMap = BattleCalculator.getCostsForTUV(player, data);
		final List<Map.Entry<Unit, Set<Territory>>> list = new LinkedList<Map.Entry<Unit, Set<Territory>>>(unitAttackOptions.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<Unit, Set<Territory>>>()
		{
			public int compare(final Map.Entry<Unit, Set<Territory>> o1, final Map.Entry<Unit, Set<Territory>> o2)
			{
				// Sort by number of territories that still need units
				int numOptions1 = 0;
				for (final Territory t : o1.getValue())
				{
					final ProAttackTerritoryData patd = attackMap.get(t);
					
					if (patd.getBattleResult() == null)
						patd.setBattleResult(battleUtils.estimateAttackBattleResults(player, t, patd.getUnits(), patd.getMaxEnemyDefenders(player, data), patd.getBombardTerritoryMap().keySet()));
					if (!patd.isCurrentlyWins())
						numOptions1++;
				}
				int numOptions2 = 0;
				for (final Territory t : o2.getValue())
				{
					final ProAttackTerritoryData patd = attackMap.get(t);
					
					if (patd.getBattleResult() == null)
						patd.setBattleResult(battleUtils.estimateAttackBattleResults(player, t, patd.getUnits(), patd.getMaxEnemyDefenders(player, data), patd.getBombardTerritoryMap().keySet()));
					if (!patd.isCurrentlyWins())
						numOptions2++;
				}
				if (numOptions1 != numOptions2)
					return (numOptions1 - numOptions2);
				if (numOptions1 == 0)
					return 0;
				
				// Sort by attack efficiency
				int minPower1 = Integer.MAX_VALUE;
				for (final Territory t : o1.getValue())
				{
					if (!attackMap.get(t).isCurrentlyWins())
					{
						final List<Unit> defendingUnits = t.getUnits().getMatches(Matches.enemyUnit(player, data));
						final List<Unit> sortedUnitsList = new ArrayList<Unit>(attackMap.get(t).getUnits());
						Collections.sort(sortedUnitsList, new UnitBattleComparator(false, playerCostMap, TerritoryEffectHelper.getEffects(t), data, false, false));
						Collections.reverse(sortedUnitsList);
						final int powerWithout = DiceRoll.getTotalPowerAndRolls(DiceRoll.getUnitPowerAndRollsForNormalBattles(sortedUnitsList, sortedUnitsList, defendingUnits, false, false,
									player, data, t, TerritoryEffectHelper.getEffects(t), false, null), data).getFirst();
						
						sortedUnitsList.add(o1.getKey());
						Collections.sort(sortedUnitsList, new UnitBattleComparator(false, playerCostMap, TerritoryEffectHelper.getEffects(t), data, false, false));
						Collections.reverse(sortedUnitsList);
						final int powerWith = DiceRoll.getTotalPowerAndRolls(DiceRoll.getUnitPowerAndRollsForNormalBattles(sortedUnitsList, sortedUnitsList, defendingUnits, false, false,
									player, data, t, TerritoryEffectHelper.getEffects(t), false, null), data).getFirst();
						
						final int power = powerWith - powerWithout;
						if (power < minPower1)
							minPower1 = power;
					}
				}
				final UnitAttachment ua1 = UnitAttachment.get(o1.getKey().getType());
				if (ua1.getIsAir())
					minPower1 *= 10;
				final double attackEfficiency1 = (double) minPower1 / playerCostMap.getInt(o1.getKey().getType());
				
				int minPower2 = Integer.MAX_VALUE;
				for (final Territory t : o2.getValue())
				{
					if (!attackMap.get(t).isCurrentlyWins())
					{
						final List<Unit> defendingUnits = t.getUnits().getMatches(Matches.enemyUnit(player, data));
						final List<Unit> sortedUnitsList = new ArrayList<Unit>(attackMap.get(t).getUnits());
						Collections.sort(sortedUnitsList, new UnitBattleComparator(false, playerCostMap, TerritoryEffectHelper.getEffects(t), data, false, false));
						Collections.reverse(sortedUnitsList);
						final int powerWithout = DiceRoll.getTotalPowerAndRolls(DiceRoll.getUnitPowerAndRollsForNormalBattles(sortedUnitsList, sortedUnitsList, defendingUnits, false, false,
									player, data, t, TerritoryEffectHelper.getEffects(t), false, null), data).getFirst();
						
						sortedUnitsList.add(o2.getKey());
						Collections.sort(sortedUnitsList, new UnitBattleComparator(false, playerCostMap, TerritoryEffectHelper.getEffects(t), data, false, false));
						Collections.reverse(sortedUnitsList);
						final int powerWith = DiceRoll.getTotalPowerAndRolls(DiceRoll.getUnitPowerAndRollsForNormalBattles(sortedUnitsList, sortedUnitsList, defendingUnits, false, false,
									player, data, t, TerritoryEffectHelper.getEffects(t), false, null), data).getFirst();
						
						final int power = powerWith - powerWithout;
						if (power < minPower2)
							minPower2 = power;
					}
				}
				final UnitAttachment ua2 = UnitAttachment.get(o2.getKey().getType());
				if (ua2.getIsAir())
					minPower2 *= 10;
				final double attackEfficiency2 = (double) minPower2 / playerCostMap.getInt(o2.getKey().getType());
				if (attackEfficiency1 != attackEfficiency2)
				{
					if (attackEfficiency1 < attackEfficiency2)
						return 1;
					else
						return -1;
				}
				
				// Check if unit types are equal and is air then sort by average distance
				if (o1.getKey().getType().equals(o2.getKey().getType()))
				{
					final boolean isAirUnit = UnitAttachment.get(o1.getKey().getType()).getIsAir();
					if (isAirUnit)
					{
						int distance1 = 0;
						for (final Territory t : o1.getValue())
						{
							if (!attackMap.get(t).isCurrentlyWins())
								distance1 += data.getMap().getDistance_IgnoreEndForCondition(unitTerritoryMap.get(o1.getKey()), t, ProMatches.territoryCanMoveAirUnitsAndNoAA(player, data, true));
						}
						int distance2 = 0;
						for (final Territory t : o2.getValue())
						{
							if (!attackMap.get(t).isCurrentlyWins())
								distance2 += data.getMap().getDistance_IgnoreEndForCondition(unitTerritoryMap.get(o2.getKey()), t, ProMatches.territoryCanMoveAirUnitsAndNoAA(player, data, true));
						}
						if (distance1 != distance2)
							return distance1 - distance2;
					}
				}
				return o1.getKey().getType().getName().compareTo(o2.getKey().getType().getName());
			}
		});
		final Map<Unit, Set<Territory>> sortedUnitAttackOptions = new LinkedHashMap<Unit, Set<Territory>>();
		for (final Map.Entry<Unit, Set<Territory>> entry : list)
			sortedUnitAttackOptions.put(entry.getKey(), entry.getValue());
		
		return sortedUnitAttackOptions;
	}
	
	public void findScrambleOptions(final PlayerID player, final Map<Territory, ProAttackTerritoryData> moveMap)
	{
		final GameData data = ai.getGameData();
		
		if (!Properties.getScramble_Rules_In_Effect(data))
			return;
		
		// Find scramble properties
		final boolean fromIslandOnly = Properties.getScramble_From_Island_Only(data);
		final boolean toSeaOnly = Properties.getScramble_To_Sea_Only(data);
		int maxScrambleDistance = 0;
		final Iterator<UnitType> utIter = data.getUnitTypeList().iterator();
		while (utIter.hasNext())
		{
			final UnitAttachment ua = UnitAttachment.get(utIter.next());
			if (ua.getCanScramble() && maxScrambleDistance < ua.getMaxScrambleDistance())
				maxScrambleDistance = ua.getMaxScrambleDistance();
		}
		final Match<Unit> airbasesCanScramble = new CompositeMatchAnd<Unit>(Matches.unitIsEnemyOf(data, player), Matches.UnitIsAirBase, Matches.UnitIsNotDisabled,
					Matches.unitIsBeingTransported().invert());
		final CompositeMatchAnd<Territory> canScramble = new CompositeMatchAnd<Territory>(new CompositeMatchOr<Territory>(Matches.TerritoryIsWater, Matches.isTerritoryEnemy(player, data)),
					Matches.territoryHasUnitsThatMatch(new CompositeMatchAnd<Unit>(Matches.UnitCanScramble, Matches.unitIsEnemyOf(data, player), Matches.UnitIsNotDisabled)),
					Matches.territoryHasUnitsThatMatch(airbasesCanScramble));
		if (fromIslandOnly)
			canScramble.add(Matches.TerritoryIsIsland);
		
		// Find potential territories to scramble from
		final HashMap<Territory, HashSet<Territory>> scrambleTerrs = new HashMap<Territory, HashSet<Territory>>();
		for (final Territory t : moveMap.keySet())
		{
			if (t.isWater() || !toSeaOnly)
			{
				final HashSet<Territory> canScrambleFrom = new HashSet<Territory>(Match.getMatches(data.getMap().getNeighbors(t, maxScrambleDistance), canScramble));
				if (!canScrambleFrom.isEmpty())
					scrambleTerrs.put(t, canScrambleFrom);
			}
		}
		if (scrambleTerrs.isEmpty())
			return;
		
		// Find potential max units that can be scrambled to each territory
		for (final Territory to : scrambleTerrs.keySet())
		{
			for (final Territory from : scrambleTerrs.get(to))
			{
				// Find potential scramble units from territory
				final Collection<Unit> airbases = from.getUnits().getMatches(airbasesCanScramble);
				final int maxCanScramble = getMaxScrambleCount(airbases);
				final Route toBattleRoute = data.getMap().getRoute_IgnoreEnd(from, to, Matches.TerritoryIsNotImpassable);
				List<Unit> canScrambleAir = from.getUnits().getMatches(new CompositeMatchAnd<Unit>(Matches.unitIsEnemyOf(data, player), Matches.UnitCanScramble,
							Matches.UnitIsNotDisabled, Matches.UnitWasScrambled.invert(), Matches.unitCanScrambleOnRouteDistance(toBattleRoute)));
				
				// Add max scramble units
				if (maxCanScramble > 0 && !canScrambleAir.isEmpty())
				{
					if (maxCanScramble < canScrambleAir.size())
					{
						Collections.sort(canScrambleAir, new Comparator<Unit>()
						{
							public int compare(final Unit o1, final Unit o2)
							{
								final double strength1 = battleUtils.estimateStrength(player, to, Collections.singletonList(o1), new ArrayList<Unit>(), false);
								final double strength2 = battleUtils.estimateStrength(player, to, Collections.singletonList(o2), new ArrayList<Unit>(), false);
								return Double.compare(strength2, strength1);
							}
						});
						canScrambleAir = canScrambleAir.subList(0, maxCanScramble);
					}
					moveMap.get(to).getMaxScrambleUnits().addAll(canScrambleAir);
				}
			}
		}
	}
	
	private static int getMaxScrambleCount(final Collection<Unit> airbases)
	{
		if (!Match.allMatch(airbases, new CompositeMatchAnd<Unit>(Matches.UnitIsAirBase, Matches.UnitIsNotDisabled)))
			throw new IllegalStateException("All units must be viable airbases");
		// find how many is the max this territory can scramble
		int maxScrambled = 0;
		for (final Unit base : airbases)
		{
			final UnitAttachment ua = UnitAttachment.get(base.getType());
			final int baseMax = ua.getMaxScrambleCount();
			if (baseMax == -1)
				return Integer.MAX_VALUE;
			maxScrambled += baseMax;
		}
		return maxScrambled;
	}
	
	public void findAttackOptions(final PlayerID player, final List<Territory> myUnitTerritories, final Map<Territory, ProAttackTerritoryData> moveMap,
				final Map<Unit, Set<Territory>> unitMoveMap, final Map<Unit, Set<Territory>> transportMoveMap, final Map<Unit, Set<Territory>> bombardMap,
				final Map<Territory, Set<Territory>> landRoutesMap, final List<ProAmphibData> transportMapList, final List<Territory> enemyTerritories, final List<Territory> territoriesToCheck,
				final boolean isCheckingEnemyAttacks)
	{
		final GameData data = ai.getGameData();
		final List<Territory> territoriesThatCantBeHeld = new ArrayList<Territory>(enemyTerritories);
		territoriesThatCantBeHeld.addAll(territoriesToCheck);
		findNavalMoveOptions(player, myUnitTerritories, moveMap, unitMoveMap, transportMoveMap, ProMatches.territoryIsEnemyOrHasEnemyUnitsOrCantBeHeld(player, data, territoriesThatCantBeHeld),
					enemyTerritories, true, isCheckingEnemyAttacks);
		findLandMoveOptions(player, myUnitTerritories, moveMap, unitMoveMap, landRoutesMap, ProMatches.territoryIsEnemyOrCantBeHeld(player, data, territoriesThatCantBeHeld),
					enemyTerritories, true);
		findAirMoveOptions(player, myUnitTerritories, moveMap, unitMoveMap, ProMatches.territoryHasEnemyUnitsOrCantBeHeld(player, data, territoriesThatCantBeHeld),
					enemyTerritories, true, isCheckingEnemyAttacks);
		findAmphibMoveOptions(player, myUnitTerritories, moveMap, transportMapList, landRoutesMap, ProMatches.territoryIsEnemyOrCantBeHeld(player, data, territoriesThatCantBeHeld),
					enemyTerritories, true, isCheckingEnemyAttacks);
		findBombardOptions(player, myUnitTerritories, moveMap, bombardMap, transportMapList, isCheckingEnemyAttacks);
	}
	
	public void findDefendOptions(final PlayerID player, final List<Territory> myUnitTerritories, final Map<Territory, ProAttackTerritoryData> moveMap,
				final Map<Unit, Set<Territory>> unitMoveMap, final Map<Unit, Set<Territory>> transportMoveMap, final Map<Territory, Set<Territory>> landRoutesMap,
				final List<ProAmphibData> transportMapList, final List<Territory> clearedTerritories)
	{
		final GameData data = ai.getGameData();
		findNavalMoveOptions(player, myUnitTerritories, moveMap, unitMoveMap, transportMoveMap, ProMatches.territoryHasNoEnemyUnitsOrCleared(player, data, clearedTerritories),
					clearedTerritories, false, false);
		findLandMoveOptions(player, myUnitTerritories, moveMap, unitMoveMap, landRoutesMap, Matches.isTerritoryAllied(player, data), new ArrayList<Territory>(), false);
		findAirMoveOptions(player, myUnitTerritories, moveMap, unitMoveMap, ProMatches.territoryIsNotConqueredAlliedLand(player, data), new ArrayList<Territory>(), false, false);
		findAmphibMoveOptions(player, myUnitTerritories, moveMap, transportMapList, landRoutesMap, Matches.isTerritoryAllied(player, data), new ArrayList<Territory>(), false,
					false);
	}
	
	public void findMaxEnemyAttackUnits(final PlayerID player, final List<Territory> myConqueredTerritories, final List<Territory> territoriesToCheck,
				final Map<Territory, ProAttackTerritoryData> enemyAttackMap)
	{
		final GameData data = ai.getGameData();
		
		// Loop through each enemy to determine the maximum number of enemy units that can attack each territory
		final List<Map<Territory, ProAttackTerritoryData>> enemyAttackMaps = new ArrayList<Map<Territory, ProAttackTerritoryData>>();
		final List<List<ProAmphibData>> enemyTransportMapLists = new ArrayList<List<ProAmphibData>>();
		final List<PlayerID> enemyPlayers = utils.getEnemyPlayers(player);
		final List<Territory> allTerritories = data.getMap().getTerritories();
		for (final PlayerID enemyPlayer : enemyPlayers)
		{
			final List<Territory> enemyUnitTerritories = Match.getMatches(allTerritories, Matches.territoryHasUnitsOwnedBy(enemyPlayer));
			enemyUnitTerritories.removeAll(myConqueredTerritories);
			final Map<Territory, ProAttackTerritoryData> attackMap2 = new HashMap<Territory, ProAttackTerritoryData>();
			final Map<Unit, Set<Territory>> unitAttackMap2 = new HashMap<Unit, Set<Territory>>();
			final Map<Unit, Set<Territory>> transportAttackMap2 = new HashMap<Unit, Set<Territory>>();
			final Map<Unit, Set<Territory>> bombardMap2 = new HashMap<Unit, Set<Territory>>();
			final List<ProAmphibData> transportMapList2 = new ArrayList<ProAmphibData>();
			final Map<Territory, Set<Territory>> landRoutesMap2 = new HashMap<Territory, Set<Territory>>();
			enemyAttackMaps.add(attackMap2);
			enemyTransportMapLists.add(transportMapList2);
			findAttackOptions(enemyPlayer, enemyUnitTerritories, attackMap2, unitAttackMap2, transportAttackMap2, bombardMap2, landRoutesMap2, transportMapList2,
						myConqueredTerritories, territoriesToCheck, true);
		}
		
		// Consolidate enemy player attack maps into one attack map with max units a single enemy can attack with
		for (final Map<Territory, ProAttackTerritoryData> attackMap2 : enemyAttackMaps)
		{
			for (final Territory t : attackMap2.keySet())
			{
				if (!enemyAttackMap.containsKey(t))
				{
					enemyAttackMap.put(t, attackMap2.get(t));
				}
				else
				{
					final Set<Unit> maxUnits = new HashSet<Unit>(enemyAttackMap.get(t).getMaxUnits());
					maxUnits.addAll(enemyAttackMap.get(t).getMaxAmphibUnits());
					double maxStrength = 0;
					if (!maxUnits.isEmpty())
						maxStrength = battleUtils.estimateStrength(maxUnits.iterator().next().getOwner(), t, new ArrayList<Unit>(maxUnits), new ArrayList<Unit>(), true);
					final Set<Unit> currentUnits = new HashSet<Unit>(attackMap2.get(t).getMaxUnits());
					currentUnits.addAll(attackMap2.get(t).getMaxAmphibUnits());
					double currentStrength = 0;
					if (!currentUnits.isEmpty())
						currentStrength = battleUtils.estimateStrength(currentUnits.iterator().next().getOwner(), t, new ArrayList<Unit>(currentUnits), new ArrayList<Unit>(), true);
					final boolean currentHasLandUnits = Match.someMatch(currentUnits, Matches.UnitIsLand);
					final boolean maxHasLandUnits = Match.someMatch(maxUnits, Matches.UnitIsLand);
					
					if ((currentHasLandUnits && ((!maxHasLandUnits && !t.isWater()) || currentStrength > maxStrength))
								|| ((!maxHasLandUnits || t.isWater()) && currentStrength > maxStrength))
					{
						enemyAttackMap.put(t, attackMap2.get(t));
					}
				}
			}
		}
	}
	
	private void findNavalMoveOptions(final PlayerID player, final List<Territory> myUnitTerritories, final Map<Territory, ProAttackTerritoryData> moveMap,
				final Map<Unit, Set<Territory>> unitMoveMap, final Map<Unit, Set<Territory>> transportMoveMap, final Match<Territory> moveToTerritoryMatch,
				final List<Territory> enemyTerritories, final boolean isCombatMove, final boolean isCheckingEnemyAttacks)
	{
		final GameData data = ai.getGameData();
		
		for (final Territory myUnitTerritory : myUnitTerritories)
		{
			// Find my naval units that have movement left
			final List<Unit> mySeaUnits = myUnitTerritory.getUnits().getMatches(ProMatches.unitCanBeMovedAndIsOwnedSea(player, isCombatMove));
			
			// Check each sea unit individually since they can have different ranges
			for (final Unit mySeaUnit : mySeaUnits)
			{
				// Find list of potential territories to move to
				final int range = TripleAUnit.get(mySeaUnit).getMovementLeft();
				final Set<Territory> possibleMoveTerritories = data.getMap().getNeighbors(myUnitTerritory, range, ProMatches.territoryCanMoveSeaUnits(player, data, isCombatMove));
				possibleMoveTerritories.add(myUnitTerritory);
				final Set<Territory> potentialTerritories = new HashSet<Territory>(Match.getMatches(possibleMoveTerritories, moveToTerritoryMatch));
				if (!isCombatMove)
					potentialTerritories.add(myUnitTerritory);
				
				for (final Territory potentialTerritory : potentialTerritories)
				{
					// Find route over water
					boolean hasNoRoute = true;
					final List<Territory> eliminatedTerritories = new ArrayList<Territory>();
					while (true) // Need a loop to consider different route combinations to avoid canals
					{
						Route myRoute = data.getMap().getRoute_IgnoreEnd(myUnitTerritory, potentialTerritory,
									ProMatches.territoryCanMoveSeaUnitsThroughOrClearedAndNotInList(player, data, isCombatMove, enemyTerritories, eliminatedTerritories));
						if (isCheckingEnemyAttacks)
						{
							myRoute = data.getMap().getRoute_IgnoreEnd(myUnitTerritory, potentialTerritory,
										ProMatches.territoryCanMoveSeaUnitsAndNotInList(player, data, isCombatMove, eliminatedTerritories));
						}
						if (myRoute == null)
							break;
						if (MoveValidator.validateCanal(myRoute, Collections.singletonList(mySeaUnit), player, data) != null)
						{
							if (!myRoute.getMiddleSteps().isEmpty())
							{
								eliminatedTerritories.addAll(myRoute.getMiddleSteps()); // Add failed canal territories to list
								continue;
							}
							else
							{
								break;
							}
						}
						final int myRouteLength = myRoute.numberOfSteps();
						if (myRouteLength > range)
							break;
						hasNoRoute = false;
						break;
					}
					if (hasNoRoute)
						continue;
					
					// Populate territories with sea unit
					if (moveMap.containsKey(potentialTerritory))
					{
						moveMap.get(potentialTerritory).addMaxUnit(mySeaUnit);
					}
					else
					{
						final ProAttackTerritoryData moveTerritoryData = new ProAttackTerritoryData(potentialTerritory);
						moveTerritoryData.addMaxUnit(mySeaUnit);
						moveMap.put(potentialTerritory, moveTerritoryData);
					}
					
					// Populate appropriate unit move options map
					if (Matches.UnitIsTransport.match(mySeaUnit))
					{
						if (transportMoveMap.containsKey(mySeaUnit))
						{
							transportMoveMap.get(mySeaUnit).add(potentialTerritory);
						}
						else
						{
							final Set<Territory> unitMoveTerritories = new HashSet<Territory>();
							unitMoveTerritories.add(potentialTerritory);
							transportMoveMap.put(mySeaUnit, unitMoveTerritories);
						}
					}
					else
					{
						if (unitMoveMap.containsKey(mySeaUnit))
						{
							unitMoveMap.get(mySeaUnit).add(potentialTerritory);
						}
						else
						{
							final Set<Territory> unitMoveTerritories = new HashSet<Territory>();
							unitMoveTerritories.add(potentialTerritory);
							unitMoveMap.put(mySeaUnit, unitMoveTerritories);
						}
					}
				}
			}
		}
	}
	
	private void findLandMoveOptions(final PlayerID player, final List<Territory> myUnitTerritories, final Map<Territory, ProAttackTerritoryData> moveMap, final Map<Unit, Set<Territory>> unitMoveMap,
				final Map<Territory, Set<Territory>> landRoutesMap, final Match<Territory> moveToTerritoryMatch, final List<Territory> enemyTerritories,
				final boolean isCombatMove)
	{
		final GameData data = ai.getGameData();
		final Map<Unit, Territory> unitTerritoryMap = utils.createUnitTerritoryMap(player);
		
		for (final Territory myUnitTerritory : myUnitTerritories)
		{
			// Find my land units that have movement left
			final List<Unit> myLandUnits = myUnitTerritory.getUnits().getMatches(ProMatches.unitCanBeMovedAndIsOwnedLand(player, isCombatMove));
			
			// Check each land unit individually since they can have different ranges
			for (final Unit myLandUnit : myLandUnits)
			{
				final Territory startTerritory = unitTerritoryMap.get(myLandUnit);
				final int range = TripleAUnit.get(myLandUnit).getMovementLeft();
				final Set<Territory> possibleMoveTerritories = data.getMap().getNeighbors(myUnitTerritory, range, ProMatches.territoryCanMoveSpecificLandUnit(player, data, isCombatMove, myLandUnit));
				possibleMoveTerritories.add(myUnitTerritory);
				final Set<Territory> potentialTerritories = new HashSet<Territory>(Match.getMatches(possibleMoveTerritories, moveToTerritoryMatch));
				if (!isCombatMove)
					potentialTerritories.add(myUnitTerritory);
				for (final Territory potentialTerritory : potentialTerritories)
				{
					// Find route over land checking whether unit can blitz
					final Route myRoute = data.getMap().getRoute_IgnoreEnd(myUnitTerritory, potentialTerritory,
								ProMatches.territoryCanMoveLandUnitsThrough(player, data, myLandUnit, startTerritory, isCombatMove, enemyTerritories));
					if (myRoute == null)
						continue;
					if (myRoute.hasMoreThenOneStep() && Match.someMatch(myRoute.getMiddleSteps(), Matches.isTerritoryEnemy(player, data))
								&& Matches.unitIsOfTypes(TerritoryEffectHelper.getUnitTypesThatLostBlitz(myRoute.getAllTerritories())).match(myLandUnit))
						continue; // If blitzing then make sure none of the territories cause blitz ability to be lost
					final int myRouteLength = myRoute.numberOfSteps();
					if (myRouteLength > range)
						continue;
					
					// Add to route map
					if (landRoutesMap.containsKey(potentialTerritory))
					{
						landRoutesMap.get(potentialTerritory).add(myUnitTerritory);
					}
					else
					{
						final Set<Territory> territories = new HashSet<Territory>();
						territories.add(myUnitTerritory);
						landRoutesMap.put(potentialTerritory, territories);
					}
					
					// Populate territories with land units
					if (moveMap.containsKey(potentialTerritory))
					{
						moveMap.get(potentialTerritory).addMaxUnit(myLandUnit);
					}
					else
					{
						final ProAttackTerritoryData moveTerritoryData = new ProAttackTerritoryData(potentialTerritory);
						moveTerritoryData.addMaxUnit(myLandUnit);
						moveMap.put(potentialTerritory, moveTerritoryData);
					}
					
					// Populate unit move options map
					if (unitMoveMap.containsKey(myLandUnit))
					{
						unitMoveMap.get(myLandUnit).add(potentialTerritory);
					}
					else
					{
						final Set<Territory> unitMoveTerritories = new HashSet<Territory>();
						unitMoveTerritories.add(potentialTerritory);
						unitMoveMap.put(myLandUnit, unitMoveTerritories);
					}
				}
			}
		}
	}
	
	private void findAirMoveOptions(final PlayerID player, final List<Territory> myUnitTerritories, final Map<Territory, ProAttackTerritoryData> moveMap,
				final Map<Unit, Set<Territory>> unitMoveMap, final Match<Territory> moveToTerritoryMatch, final List<Territory> enemyTerritories, final boolean isCombatMove,
				final boolean isCheckingEnemyAttacks)
	{
		final GameData data = ai.getGameData();
		
		// TODO: add carriers to landing possibilities for non-enemy
		final Set<Territory> possibleCarrierTerritories = new HashSet<Territory>();
		if (isCheckingEnemyAttacks)
		{
			final Map<Unit, Set<Territory>> unitMoveMap2 = new HashMap<Unit, Set<Territory>>();
			findNavalMoveOptions(player, myUnitTerritories, new HashMap<Territory, ProAttackTerritoryData>(), unitMoveMap2, new HashMap<Unit, Set<Territory>>(), Matches.TerritoryIsWater,
						enemyTerritories, false, true);
			for (final Unit u : unitMoveMap2.keySet())
			{
				if (Matches.UnitIsCarrier.match(u))
					possibleCarrierTerritories.addAll(unitMoveMap2.get(u));
			}
		}
		
		for (final Territory myUnitTerritory : myUnitTerritories)
		{
			// Find my air units that have movement left
			final List<Unit> myAirUnits = myUnitTerritory.getUnits().getMatches(ProMatches.unitCanBeMovedAndIsOwnedAir(player, isCombatMove));
			
			// Check each air unit individually since they can have different ranges
			for (final Unit myAirUnit : myAirUnits)
			{
				final int range = TripleAUnit.get(myAirUnit).getMovementLeft();
				final Set<Territory> possibleMoveTerritories = data.getMap().getNeighbors(myUnitTerritory, range, ProMatches.territoryCanMoveAirUnits(player, data, isCombatMove));
				possibleMoveTerritories.add(myUnitTerritory);
				final Set<Territory> potentialTerritories = new HashSet<Territory>(Match.getMatches(possibleMoveTerritories, moveToTerritoryMatch));
				for (final Territory potentialTerritory : potentialTerritories)
				{
					// Find route ignoring impassable and territories with AA
					Match<Territory> canFlyOverMatch = ProMatches.territoryCanMoveAirUnitsAndNoAA(player, data, isCombatMove);
					if (isCheckingEnemyAttacks)
						canFlyOverMatch = ProMatches.territoryCanMoveAirUnits(player, data, isCombatMove);
					final Route myRoute = data.getMap().getRoute_IgnoreEnd(myUnitTerritory, potentialTerritory, canFlyOverMatch);
					if (myRoute == null)
						continue;
					final int myRouteLength = myRoute.numberOfSteps();
					final int remainingMoves = range - myRouteLength;
					if (remainingMoves < 0)
						continue;
					
					// If combat move and my remaining movement is less than the distance I already moved then need to check if I can land
					if (isCombatMove && remainingMoves < myRouteLength)
					{
						final Set<Territory> possibleLandingTerritories = data.getMap().getNeighbors(potentialTerritory, remainingMoves,
									canFlyOverMatch);
						final List<Territory> landingTerritories = Match.getMatches(possibleLandingTerritories, ProMatches.territoryCanLandAirUnits(player, data, isCombatMove, enemyTerritories));
						List<Territory> carrierTerritories = new ArrayList<Territory>();
						if (Matches.UnitCanLandOnCarrier.match(myAirUnit))
							carrierTerritories = Match.getMatches(possibleLandingTerritories, Matches.territoryIsInList(possibleCarrierTerritories));
						if (landingTerritories.isEmpty() && carrierTerritories.isEmpty())
							continue;
					}
					
					// Populate enemy territories with air unit
					if (moveMap.containsKey(potentialTerritory))
					{
						moveMap.get(potentialTerritory).addMaxUnit(myAirUnit);
					}
					else
					{
						final ProAttackTerritoryData moveTerritoryData = new ProAttackTerritoryData(potentialTerritory);
						moveTerritoryData.addMaxUnit(myAirUnit);
						moveMap.put(potentialTerritory, moveTerritoryData);
					}
					
					// Populate unit attack options map
					if (unitMoveMap.containsKey(myAirUnit))
					{
						unitMoveMap.get(myAirUnit).add(potentialTerritory);
					}
					else
					{
						final Set<Territory> unitMoveTerritories = new HashSet<Territory>();
						unitMoveTerritories.add(potentialTerritory);
						unitMoveMap.put(myAirUnit, unitMoveTerritories);
					}
				}
			}
		}
	}
	
	private void findAmphibMoveOptions(final PlayerID player, final List<Territory> myUnitTerritories, final Map<Territory, ProAttackTerritoryData> moveMap,
				final List<ProAmphibData> transportMapList, final Map<Territory, Set<Territory>> landRoutesMap, final Match<Territory> moveAmphibToTerritoryMatch,
				final List<Territory> enemyTerritories, final boolean isCombatMove, final boolean isCheckingEnemyAttacks)
	{
		final GameData data = ai.getGameData();
		
		for (final Territory myUnitTerritory : myUnitTerritories)
		{
			// Find my transports and amphibious units that have movement left
			final List<Unit> myTransportUnits = myUnitTerritory.getUnits().getMatches(ProMatches.unitCanBeMovedAndIsOwnedTransport(player, isCombatMove));
			final Match<Territory> unloadAmphibTerritoryMatch = new CompositeMatchAnd<Territory>(ProMatches.territoryCanMoveLandUnits(player, data, isCombatMove), moveAmphibToTerritoryMatch);
			
			// Check each transport unit individually since they can have different ranges
			for (final Unit myTransportUnit : myTransportUnits)
			{
				int movesLeft = TripleAUnit.get(myTransportUnit).getMovementLeft();
				final ProAmphibData proTransportData = new ProAmphibData(myTransportUnit);
				transportMapList.add(proTransportData);
				final Set<Territory> currentTerritories = new HashSet<Territory>();
				currentTerritories.add(myUnitTerritory);
				
				// Find units to load and territories to unload
				while (movesLeft >= 0)
				{
					final Set<Territory> nextTerritories = new HashSet<Territory>();
					for (final Territory currentTerritory : currentTerritories)
					{
						// Find neighbors I can move to
						final Set<Territory> possibleNeighborTerritories = data.getMap().getNeighbors(currentTerritory, ProMatches.territoryCanMoveSeaUnitsThrough(player, data, isCombatMove));
						for (final Territory possibleNeighborTerritory : possibleNeighborTerritories)
						{
							if (MoveValidator.validateCanal(new Route(currentTerritory, possibleNeighborTerritory), Collections.singletonList(myTransportUnit), player, data) == null)
								nextTerritories.add(possibleNeighborTerritory);
						}
						
						// Get loaded units or get units that can be loaded into current territory if no enemies present
						final List<Unit> units = new ArrayList<Unit>();
						Set<Territory> myUnitsToLoadTerritories = new HashSet<Territory>();
						if (TransportTracker.isTransporting(myTransportUnit))
						{
							units.addAll(TransportTracker.transporting(myTransportUnit));
						}
						else if (Matches.territoryHasEnemySeaUnits(player, data).invert().match(currentTerritory))
						{
							myUnitsToLoadTerritories = data.getMap().getNeighbors(currentTerritory);
							for (final Territory myUnitsToLoadTerritory : myUnitsToLoadTerritories)
							{
								final List<Unit> possibleUnits = myUnitsToLoadTerritory.getUnits().getMatches(ProMatches.unitIsOwnedTransportableUnitAndCanBeLoaded(player, isCombatMove));
								for (final Unit possibleUnit : possibleUnits)
								{
									if (UnitAttachment.get(possibleUnit.getType()).getTransportCost() <= UnitAttachment.get(myTransportUnit.getType()).getTransportCapacity())
										units.add(possibleUnit);
								}
							}
						}
						
						// If there are any units to be transported
						if (!units.isEmpty())
						{
							// Find all water territories I can move to
							final Set<Territory> seaMoveTerritories = new HashSet<Territory>();
							seaMoveTerritories.add(currentTerritory);
							if (movesLeft > 0)
							{
								Set<Territory> neighborTerritories = data.getMap().getNeighbors(currentTerritory, movesLeft, ProMatches.territoryCanMoveSeaUnitsThrough(player, data, isCombatMove));
								if (isCheckingEnemyAttacks)
									neighborTerritories = data.getMap().getNeighbors(currentTerritory, movesLeft, ProMatches.territoryCanMoveSeaUnits(player, data, isCombatMove));
								for (final Territory neighborTerritory : neighborTerritories)
								{
									final Route myRoute = data.getMap().getRoute_IgnoreEnd(currentTerritory, neighborTerritory, ProMatches.territoryCanMoveSeaUnitsThrough(player, data, isCombatMove));
									if (myRoute == null)
										continue;
									if (MoveValidator.validateCanal(myRoute, Collections.singletonList(myTransportUnit), player, data) != null)
										continue;
									seaMoveTerritories.add(neighborTerritory);
								}
							}
							
							// Find possible unload territories
							final Set<Territory> amphibTerritories = new HashSet<Territory>();
							for (final Territory seaMoveTerritory : seaMoveTerritories)
								amphibTerritories.addAll(data.getMap().getNeighbors(seaMoveTerritory, unloadAmphibTerritoryMatch));
							
							// Add to transport map
							proTransportData.addTerritories(amphibTerritories, myUnitsToLoadTerritories);
							proTransportData.addSeaTerritories(seaMoveTerritories, myUnitsToLoadTerritories, data);
						}
					}
					currentTerritories.clear();
					currentTerritories.addAll(nextTerritories);
					movesLeft--;
				}
			}
		}
		
		// Remove any territories from transport map that I can move to on land and transports with no amphib options
		for (final Iterator<ProAmphibData> it = transportMapList.iterator(); it.hasNext();)
		{
			final ProAmphibData proTransportData = it.next();
			final Map<Territory, Set<Territory>> transportMap = proTransportData.getTransportMap();
			final List<Territory> transportTerritoriesToRemove = new ArrayList<Territory>();
			for (final Territory t : transportMap.keySet())
			{
				final Set<Territory> transportMoveTerritories = transportMap.get(t);
				final Set<Territory> landMoveTerritories = landRoutesMap.get(t);
				if (landMoveTerritories != null)
				{
					transportMoveTerritories.removeAll(landMoveTerritories);
					if (transportMoveTerritories.isEmpty())
						transportTerritoriesToRemove.add(t);
				}
			}
			for (final Territory t : transportTerritoriesToRemove)
				transportMap.remove(t);
		}
		
		// Add transport units to attack map
		for (final ProAmphibData proTransportData : transportMapList)
		{
			final Map<Territory, Set<Territory>> transportMap = proTransportData.getTransportMap();
			final Unit transport = proTransportData.getTransport();
			for (final Territory moveTerritory : transportMap.keySet())
			{
				// Get units to transport
				final Set<Territory> territoriesCanLoadFrom = transportMap.get(moveTerritory);
				List<Unit> alreadyAddedToMaxAmphibUnits = new ArrayList<Unit>();
				if (moveMap.containsKey(moveTerritory))
					alreadyAddedToMaxAmphibUnits = moveMap.get(moveTerritory).getMaxAmphibUnits();
				final List<Unit> amphibUnits = transportUtils.getUnitsToTransportFromTerritories(player, transport, territoriesCanLoadFrom, alreadyAddedToMaxAmphibUnits);
				
				// Add amphib units to attack map
				if (moveMap.containsKey(moveTerritory))
				{
					moveMap.get(moveTerritory).addMaxAmphibUnits(amphibUnits);
				}
				else
				{
					final ProAttackTerritoryData moveTerritoryData = new ProAttackTerritoryData(moveTerritory);
					moveTerritoryData.addMaxAmphibUnits(amphibUnits);
					moveMap.put(moveTerritory, moveTerritoryData);
				}
			}
		}
	}
	
	private void findBombardOptions(final PlayerID player, final List<Territory> myUnitTerritories, final Map<Territory, ProAttackTerritoryData> moveMap,
				final Map<Unit, Set<Territory>> bombardMap, final List<ProAmphibData> transportMapList, final boolean isCheckingEnemyAttacks)
	{
		final GameData data = ai.getGameData();
		
		// Find all transport unload from and to territories
		final Set<Territory> unloadFromTerritories = new HashSet<Territory>();
		final Set<Territory> unloadToTerritories = new HashSet<Territory>();
		for (final ProAmphibData amphibData : transportMapList)
		{
			unloadFromTerritories.addAll(amphibData.getSeaTransportMap().keySet());
			unloadToTerritories.addAll(amphibData.getTransportMap().keySet());
		}
		
		// Loop through territories with my units
		for (final Territory myUnitTerritory : myUnitTerritories)
		{
			// Find my bombard units that have movement left
			final List<Unit> mySeaUnits = myUnitTerritory.getUnits().getMatches(ProMatches.unitCanBeMovedAndIsOwnedBombard(player));
			
			// Check each sea unit individually since they can have different ranges
			for (final Unit mySeaUnit : mySeaUnits)
			{
				// Find list of potential territories to move to
				final int range = TripleAUnit.get(mySeaUnit).getMovementLeft();
				final Set<Territory> potentialTerritories = data.getMap().getNeighbors(myUnitTerritory, range, ProMatches.territoryCanMoveSeaUnits(player, data, true));
				potentialTerritories.add(myUnitTerritory);
				potentialTerritories.retainAll(unloadFromTerritories);
				
				for (final Territory bombardFromTerritory : potentialTerritories)
				{
					// Find route over water with no enemy units blocking
					Route myRoute = data.getMap().getRoute(myUnitTerritory, bombardFromTerritory, ProMatches.territoryCanMoveSeaUnitsThrough(player, data, true));
					if (isCheckingEnemyAttacks)
						myRoute = data.getMap().getRoute(myUnitTerritory, bombardFromTerritory, ProMatches.territoryCanMoveSeaUnits(player, data, true));
					if (myRoute == null)
						continue;
					if (MoveValidator.validateCanal(myRoute, Collections.singletonList(mySeaUnit), player, data) != null)
						continue;
					final int myRouteLength = myRoute.numberOfSteps();
					if (myRouteLength > range)
						continue;
					
					// Find potential unload to territories
					final Set<Territory> bombardToTerritories = new HashSet<Territory>(data.getMap().getNeighbors(bombardFromTerritory));
					bombardToTerritories.retainAll(unloadToTerritories);
					
					// Populate attack territories with bombard unit
					for (final Territory bombardToTerritory : bombardToTerritories)
					{
						if (moveMap.containsKey(bombardToTerritory)) // Should always contain it
						{
							moveMap.get(bombardToTerritory).addMaxBombardUnit(mySeaUnit);
							moveMap.get(bombardToTerritory).addBombardOptionsMap(mySeaUnit, bombardFromTerritory);
						}
					}
					
					// Populate bombard options map
					if (bombardMap.containsKey(mySeaUnit))
						bombardMap.get(mySeaUnit).addAll(bombardToTerritories);
					else
						bombardMap.put(mySeaUnit, bombardToTerritories);
				}
			}
		}
	}
	
}
