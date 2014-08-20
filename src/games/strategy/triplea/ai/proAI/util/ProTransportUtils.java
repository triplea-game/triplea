package games.strategy.triplea.ai.proAI.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.ai.proAI.ProAttackTerritoryData;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
 * Pro AI transport utilities.
 * 
 * @author Ron Murhammer
 * @since 2014
 */
public class ProTransportUtils
{
	private final ProAI ai;
	private final ProUtils utils;
	
	public ProTransportUtils(final ProAI ai, final ProUtils utils)
	{
		this.ai = ai;
		this.utils = utils;
	}
	
	public int findNumUnitsThatCanBeTransported(final PlayerID player, final Territory t)
	{
		final GameData data = ai.getGameData();
		
		final CompositeMatch<Unit> myUnitsToLoadMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanBeTransported, Matches.UnitCanNotMoveDuringCombatMove.invert());
		int numUnitsToLoad = 0;
		final Set<Territory> neighbors = data.getMap().getNeighbors(t, Matches.TerritoryIsLand);
		for (final Territory neighbor : neighbors)
		{
			numUnitsToLoad += Match.getMatches(neighbor.getUnits().getUnits(), myUnitsToLoadMatch).size();
		}
		return numUnitsToLoad;
	}
	
	public List<Unit> getUnitsToTransportFromTerritories(final PlayerID player, final Unit transport, final Set<Territory> territoriesToLoadFrom, final List<Unit> unitsToIgnore)
	{
		final List<Unit> attackers = new ArrayList<Unit>();
		
		// Get units if transport already loaded
		if (TransportTracker.isTransporting(transport))
		{
			attackers.addAll(TransportTracker.transporting(transport));
		}
		else
		{
			// Get all units that can be transported
			final CompositeMatch<Unit> myUnitsToLoadMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanBeTransported, Matches.unitHasNotMoved,
						Matches.UnitCanNotMoveDuringCombatMove.invert(), Matches.unitIsBeingTransported().invert());
			final List<Unit> units = new ArrayList<Unit>();
			for (final Territory loadFrom : territoriesToLoadFrom)
			{
				units.addAll(loadFrom.getUnits().getMatches(myUnitsToLoadMatch));
			}
			units.removeAll(unitsToIgnore);
			
			// Sort units by attack
			Collections.sort(units, new Comparator<Unit>()
			{
				public int compare(final Unit o1, final Unit o2)
				{
					int attack1 = UnitAttachment.get(o1.getType()).getAttack(player);
					if (UnitAttachment.get(o1.getType()).getArtillery())
						attack1++;
					int attack2 = UnitAttachment.get(o2.getType()).getAttack(player);
					if (UnitAttachment.get(o2.getType()).getArtillery())
						attack2++;
					return attack2 - attack1;
				}
			});
			
			// Get best attackers that can be loaded
			final int capacity = UnitAttachment.get(transport.getType()).getTransportCapacity();
			int capacityCount = 0;
			for (final Unit unit : units)
			{
				final int cost = UnitAttachment.get(unit.getType()).getTransportCost();
				if (cost <= (capacity - capacityCount))
				{
					attackers.add(unit);
					capacityCount += cost;
					if (capacityCount >= capacity)
						break;
				}
			}
		}
		
		return attackers;
	}
	
	public Territory findTransportUnloadTerritory(final PlayerID player, final Territory t, final Unit transport, final List<Unit> amphibUnits, final Map<Territory, ProAttackTerritoryData> attackMap,
				final boolean isCombatMove, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final List<Collection<Unit>> transportsToLoad)
	{
		final GameData data = ai.getGameData();
		final Map<Unit, Territory> unitTerritoryMap = utils.createUnitTerritoryMap(player);
		final Match<Territory> canMoveNavalTerritoryMatch = new CompositeMatchAnd<Territory>(Matches.territoryDoesNotCostMoneyToEnter(data),
					Matches.TerritoryIsPassableAndNotRestrictedAndOkByRelationships(player, data, isCombatMove, false, true, false, false));
		
		final Match<Territory> canMoveNavalThroughMatch = new CompositeMatchAnd<Territory>(canMoveNavalTerritoryMatch, Matches.territoryHasNoEnemyUnits(player, data));
		int movesLeft = TripleAUnit.get(transport).getMovementLeft();
		Territory transportTerritory = unitTerritoryMap.get(transport);
		
		// Check if units are already loaded or not
		final List<Unit> loadedUnits = new ArrayList<Unit>();
		final List<Unit> remainingUnitsToLoad = new ArrayList<Unit>();
		if (TransportTracker.isTransporting(transport))
			loadedUnits.addAll(amphibUnits);
		else
			remainingUnitsToLoad.addAll(amphibUnits);
		
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
			int distanceFromEnd = data.getMap().getDistance(transportTerritory, t);
			if (t.isWater())
				distanceFromEnd++;
			if (movesLeft > 0 && (distanceFromEnd > 1 || !remainingUnitsToLoad.isEmpty()))
			{
				final Set<Territory> neighbors = data.getMap().getNeighbors(transportTerritory, canMoveNavalThroughMatch);
				Territory territoryToMoveTo = null;
				int minUnitDistance = Integer.MAX_VALUE;
				for (final Territory neighbor : neighbors)
				{
					if (MoveValidator.validateCanal(new Route(transportTerritory, neighbor), Collections.singletonList(transport), player, data) != null)
						continue;
					int neighborDistanceFromEnd = data.getMap().getDistance_IgnoreEndForCondition(neighbor, t, canMoveNavalThroughMatch);
					if (t.isWater())
						neighborDistanceFromEnd++;
					int maxUnitDistance = 0;
					for (final Unit u : remainingUnitsToLoad)
					{
						final int distance = data.getMap().getDistance(neighbor, unitTerritoryMap.get(u));
						if (distance > maxUnitDistance)
							maxUnitDistance = distance;
					}
					if (neighborDistanceFromEnd <= movesLeft && maxUnitDistance < minUnitDistance)
					{
						territoryToMoveTo = neighbor;
						minUnitDistance = maxUnitDistance;
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
		
		return transportTerritory;
	}
	
}
