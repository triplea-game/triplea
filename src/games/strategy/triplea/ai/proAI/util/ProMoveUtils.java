package games.strategy.triplea.ai.proAI.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.ai.proAI.ProAttackTerritoryData;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

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
 * Pro AI move utilities.
 * 
 * @author Ron Murhammer
 * @since 2014
 */
public class ProMoveUtils
{
	private final ProAI ai;
	private final ProUtils utils;
	
	public ProMoveUtils(final ProAI ai, final ProUtils utils)
	{
		this.ai = ai;
		this.utils = utils;
	}
	
	public void calculateMoveRoutes(final PlayerID player, final boolean areNeutralsPassableByAir, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes,
				final Map<Territory, ProAttackTerritoryData> attackMap, final boolean isCombatMove)
	{
		final GameData data = ai.getGameData();
		final Map<Unit, Territory> unitTerritoryMap = utils.createUnitTerritoryMap(player);
		
		// Find all amphib units
		final Set<Unit> amphibUnits = new HashSet<Unit>();
		for (final Territory t : attackMap.keySet())
		{
			amphibUnits.addAll(attackMap.get(t).getAmphibAttackMap().keySet());
			for (final Unit transport : attackMap.get(t).getAmphibAttackMap().keySet())
				amphibUnits.addAll(attackMap.get(t).getAmphibAttackMap().get(transport));
		}
		
		// Loop through all territories to attack
		for (final Territory t : attackMap.keySet())
		{
			// Loop through each unit that is attacking the current territory
			for (final Unit u : attackMap.get(t).getUnits())
			{
				// Skip amphib units
				if (amphibUnits.contains(u))
					continue;
				
				// Skip if unit is already in move to territory
				final Territory startTerritory = unitTerritoryMap.get(u);
				if (startTerritory.equals(t))
					continue;
				
				// Add unit to move list
				final List<Unit> unitList = new ArrayList<Unit>();
				unitList.add(u);
				moveUnits.add(unitList);
				
				// Determine route and add to move list
				Route route = null;
				if (Match.allMatch(unitList, ProMatches.unitCanBeMovedAndIsOwnedSea(player, isCombatMove)))
				{
					// Naval unit
					route = data.getMap().getRoute_IgnoreEnd(startTerritory, t, ProMatches.territoryCanMoveSeaUnitsThrough(player, data, isCombatMove));
				}
				else if (Match.allMatch(unitList, ProMatches.unitCanBeMovedAndIsOwnedLand(player, isCombatMove))) // && (!Matches.UnitCanBlitz.match(u) || !isCombatMove))
				{
					// Land unit
					route = data.getMap().getRoute_IgnoreEnd(startTerritory, t, ProMatches.territoryCanMoveLandUnitsThrough(player, data, u, startTerritory, isCombatMove, new ArrayList<Territory>()));
				}
				else if (Match.allMatch(unitList, ProMatches.unitCanBeMovedAndIsOwnedAir(player, isCombatMove)))
				{
					// Air unit
					route = data.getMap().getRoute_IgnoreEnd(startTerritory, t, ProMatches.territoryCanMoveAirUnitsAndNoAA(player, data, isCombatMove));
				}
				moveRoutes.add(route);
			}
		}
	}
	
	public void calculateAmphibRoutes(final PlayerID player, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final List<Collection<Unit>> transportsToLoad,
				final Map<Territory, ProAttackTerritoryData> attackMap, final boolean isCombatMove)
	{
		final GameData data = ai.getGameData();
		final Map<Unit, Territory> unitTerritoryMap = utils.createUnitTerritoryMap(player);
		
		// Loop through all territories to attack
		for (final Territory t : attackMap.keySet())
		{
			// Loop through each amphib attack map
			final Map<Unit, List<Unit>> amphibAttackMap = attackMap.get(t).getAmphibAttackMap();
			for (final Unit transport : amphibAttackMap.keySet())
			{
				int movesLeft = TripleAUnit.get(transport).getMovementLeft();
				Territory transportTerritory = unitTerritoryMap.get(transport);
				
				// Check if units are already loaded or not
				final List<Unit> loadedUnits = new ArrayList<Unit>();
				final List<Unit> remainingUnitsToLoad = new ArrayList<Unit>();
				if (TransportTracker.isTransporting(transport))
					loadedUnits.addAll(amphibAttackMap.get(transport));
				else
					remainingUnitsToLoad.addAll(amphibAttackMap.get(transport));
				
				// Load units and move transport
				while (movesLeft >= 0)
				{
					// Load adjacent units if no enemies present in transport territory
					if (Matches.territoryHasEnemyUnits(player, data).invert().match(transportTerritory))
					{
						final List<Unit> unitsToRemove = new ArrayList<Unit>();
						for (final Unit amphibUnit : remainingUnitsToLoad)
						{
							if (data.getMap().getDistance(transportTerritory, unitTerritoryMap.get(amphibUnit)) == 1)
							{
								moveUnits.add(Collections.singletonList(amphibUnit));
								transportsToLoad.add(Collections.singletonList(transport));
								final Route route = new Route(unitTerritoryMap.get(amphibUnit), transportTerritory);
								moveRoutes.add(route);
								unitsToRemove.add(amphibUnit);
								loadedUnits.add(amphibUnit);
							}
						}
						for (final Unit u : unitsToRemove)
							remainingUnitsToLoad.remove(u);
					}
					
					// Move transport if I'm not already at the end or out of moves
					final Territory unloadTerritory = attackMap.get(t).getTransportTerritoryMap().get(transport);
					int distanceFromEnd = data.getMap().getDistance(transportTerritory, t);
					if (t.isWater())
						distanceFromEnd++;
					if (movesLeft > 0 && (distanceFromEnd > 1 || !remainingUnitsToLoad.isEmpty() || (unloadTerritory != null && !unloadTerritory.equals(transportTerritory))))
					{
						final Set<Territory> neighbors = data.getMap().getNeighbors(transportTerritory, ProMatches.territoryCanMoveSeaUnitsThrough(player, data, isCombatMove));
						Territory territoryToMoveTo = null;
						int minUnitDistance = Integer.MAX_VALUE;
						int maxDistanceFromEnd = Integer.MIN_VALUE; // Used to move to farthest away loading territory first
						for (final Territory neighbor : neighbors)
						{
							if (MoveValidator.validateCanal(new Route(transportTerritory, neighbor), Collections.singletonList(transport), player, data) != null)
								continue;
							int distanceFromUnloadTerritory = 0;
							if (unloadTerritory != null)
								distanceFromUnloadTerritory = data.getMap().getDistance_IgnoreEndForCondition(neighbor, unloadTerritory,
											ProMatches.territoryCanMoveSeaUnitsThrough(player, data, isCombatMove));
							int neighborDistanceFromEnd = data.getMap().getDistance_IgnoreEndForCondition(neighbor, t, ProMatches.territoryCanMoveSeaUnitsThrough(player, data, isCombatMove));
							if (t.isWater())
								neighborDistanceFromEnd++;
							int maxUnitDistance = 0;
							for (final Unit u : remainingUnitsToLoad)
							{
								final int distance = data.getMap().getDistance(neighbor, unitTerritoryMap.get(u));
								if (distance > maxUnitDistance)
									maxUnitDistance = distance;
							}
							if (neighborDistanceFromEnd <= movesLeft && maxUnitDistance <= minUnitDistance && distanceFromUnloadTerritory < movesLeft
										&& (maxUnitDistance < minUnitDistance || neighborDistanceFromEnd > maxDistanceFromEnd))
							{
								territoryToMoveTo = neighbor;
								minUnitDistance = maxUnitDistance;
								maxDistanceFromEnd = neighborDistanceFromEnd;
							}
						}
						if (territoryToMoveTo != null)
						{
							final List<Unit> unitsToMove = new ArrayList<Unit>();
							unitsToMove.add(transport);
							unitsToMove.addAll(loadedUnits);
							moveUnits.add(unitsToMove);
							transportsToLoad.add(null);
							final Route route = new Route(transportTerritory, territoryToMoveTo);
							moveRoutes.add(route);
							transportTerritory = territoryToMoveTo;
						}
					}
					movesLeft--;
				}
				
				// Set territory transport is moving to
				attackMap.get(t).getTransportTerritoryMap().put(transport, transportTerritory);
				
				// Unload transport
				if (!loadedUnits.isEmpty() && !t.isWater())
				{
					moveUnits.add(loadedUnits);
					transportsToLoad.add(null);
					final Route route = new Route(transportTerritory, t);
					moveRoutes.add(route);
				}
			}
		}
	}
	
	public void calculateBombardMoveRoutes(final PlayerID player, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes,
				final Map<Territory, ProAttackTerritoryData> attackMap)
	{
		final GameData data = ai.getGameData();
		final Map<Unit, Territory> unitTerritoryMap = utils.createUnitTerritoryMap(player);
		
		// Loop through all territories to attack
		for (final Territory t : attackMap.keySet())
		{
			// Loop through each unit that is attacking the current territory
			for (final Unit u : attackMap.get(t).getBombardTerritoryMap().keySet())
			{
				final Territory bombardFromTerritory = attackMap.get(t).getBombardTerritoryMap().get(u);
				
				// Skip if unit is already in move to territory
				final Territory startTerritory = unitTerritoryMap.get(u);
				if (startTerritory.equals(bombardFromTerritory))
					continue;
				
				// Add unit to move list
				final List<Unit> unitList = new ArrayList<Unit>();
				unitList.add(u);
				moveUnits.add(unitList);
				
				// Determine route and add to move list
				Route route = null;
				if (Match.allMatch(unitList, ProMatches.unitCanBeMovedAndIsOwnedSea(player, true)))
				{
					// Naval unit
					route = data.getMap().getRoute_IgnoreEnd(startTerritory, bombardFromTerritory, ProMatches.territoryCanMoveSeaUnitsThrough(player, data, true));
				}
				moveRoutes.add(route);
			}
		}
	}
	
	public void doMove(final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final List<Collection<Unit>> transportsToLoad, final IMoveDelegate moveDel, final boolean isSimulation)
	{
		final GameData data = ai.getGameData();
		
		// Group non-amphib units of the same type moving on the same route
		if (transportsToLoad == null)
		{
			for (int i = 0; i < moveRoutes.size(); i++)
			{
				final Route r = moveRoutes.get(i);
				// final UnitType ut = moveUnits.get(i).iterator().next().getType();
				for (int j = i + 1; j < moveRoutes.size(); j++)
				{
					final Route r2 = moveRoutes.get(j);
					// final UnitType ut2 = moveUnits.get(j).iterator().next().getType();
					if (r.equals(r2))// && ut.equals(ut2))
					{
						moveUnits.get(j).addAll(moveUnits.get(i));
						moveUnits.remove(i);
						moveRoutes.remove(i);
						i--;
						break;
					}
				}
			}
		}
		
		// Move units
		for (int i = 0; i < moveRoutes.size(); i++)
		{
			if (!isSimulation)
				utils.pause();
			if (moveRoutes.get(i) == null || moveRoutes.get(i).getEnd() == null || moveRoutes.get(i).getStart() == null)
			{
				LogUtils.log(Level.WARNING, data.getSequence().getRound() + "-" + data.getSequence().getStep().getName() + ": route not valid " + moveRoutes.get(i) + " units: " + moveUnits.get(i));
				continue;
			}
			String result;
			if (transportsToLoad == null || transportsToLoad.get(i) == null)
				result = moveDel.move(moveUnits.get(i), moveRoutes.get(i));
			else
				result = moveDel.move(moveUnits.get(i), moveRoutes.get(i), transportsToLoad.get(i));
			if (result != null)
			{
				LogUtils.log(Level.WARNING, data.getSequence().getRound() + "-" + data.getSequence().getStep().getName() + ": could not move " + moveUnits.get(i) + " over " + moveRoutes.get(i)
							+ " because: " + result);
			}
		}
	}
	
}
