package games.strategy.triplea.ai.proAI.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.ai.proAI.ProAttackTerritoryData;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
	
	public void calculateAttackRoutes(final PlayerID player, final boolean areNeutralsPassableByAir, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes,
				final Map<Territory, ProAttackTerritoryData> attackMap)
	{
		final GameData data = ai.getGameData();
		final Map<Unit, Territory> unitTerritoryMap = createUnitTerritoryMap(player);
		
		final CompositeMatch<Unit> mySeaUnitMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsSea, Matches.UnitCanMove);
		final CompositeMatch<Unit> myLandUnitMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsLand, Matches.UnitIsNotAA, Matches.UnitCanMove,
					Matches.UnitIsNotInfrastructure, Matches.UnitCanNotMoveDuringCombatMove.invert(), Matches.unitIsBeingTransported().invert());
		final CompositeMatch<Unit> myAirUnitMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsAir, Matches.UnitCanMove);
		final CompositeMatch<Territory> canFlyOverMatch = new CompositeMatchAnd<Territory>(Matches.airCanFlyOver(player, data, areNeutralsPassableByAir), Matches.territoryHasEnemyAAforAnything(
					player, data).invert());
		
		// Loop through all territories to attack
		for (final Territory t : attackMap.keySet())
		{
			// Loop through each unit that is attacking the current territory
			for (final Unit u : attackMap.get(t).getUnits())
			{
				// Skip amphib units
				boolean isAmphib = false;
				for (final Unit transport : attackMap.get(t).getAmphibAttackMap().keySet())
				{
					if (attackMap.get(t).getAmphibAttackMap().get(transport).contains(u))
						isAmphib = true;
				}
				if (isAmphib)
					continue;
				
				// Add unit to move list
				final List<Unit> unitList = new ArrayList<Unit>();
				unitList.add(u);
				moveUnits.add(unitList);
				
				// Determine route and add to move list
				Route route = null;
				final Territory startTerritory = unitTerritoryMap.get(u);
				if (Match.allMatch(unitList, mySeaUnitMatch))
				{
					// Naval unit
					final Match<Territory> canMoveSeaThroughMatch = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasEnemyUnits(player, data).invert(), Matches
								.territoryHasNonAllowedCanal(player, unitList, data).invert());
					route = data.getMap().getRoute_IgnoreEnd(startTerritory, t, canMoveSeaThroughMatch);
				}
				else if (Match.allMatch(unitList, myLandUnitMatch) && !Matches.UnitCanBlitz.match(u))
				{
					// Land unit that can't blitz
					route = data.getMap().getRoute_IgnoreEnd(startTerritory, t, Matches.TerritoryAllowsCanMoveLandUnitsOverOwnedLand(player, data));
				}
				else if (Match.allMatch(unitList, myLandUnitMatch) && Matches.UnitCanBlitz.match(u))
				{
					// Land unit that can blitz
					route = data.getMap().getRoute_IgnoreEnd(startTerritory, t, Matches.TerritoryIsBlitzable(player, data));
				}
				else if (Match.allMatch(unitList, myAirUnitMatch))
				{
					// Air unit
					route = data.getMap().getRoute_IgnoreEnd(startTerritory, t, canFlyOverMatch);
				}
				moveRoutes.add(route);
			}
		}
	}
	
	public void calculateAmphibRoutes(final PlayerID player, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final List<Collection<Unit>> transportsToLoad,
				final Map<Territory, ProAttackTerritoryData> attackMap)
	{
		final GameData data = ai.getGameData();
		final Map<Unit, Territory> unitTerritoryMap = createUnitTerritoryMap(player);
		
		// Loop through all territories to attack
		for (final Territory t : attackMap.keySet())
		{
			// Loop through each amphib attack map
			final Map<Unit, List<Unit>> amphibAttackMap = attackMap.get(t).getAmphibAttackMap();
			for (final Unit transport : amphibAttackMap.keySet())
			{
				final Match<Territory> canMoveSeaThroughMatch = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasEnemyUnits(player, data).invert(), Matches
							.territoryHasNonAllowedCanal(player, Collections.singletonList(transport), data).invert());
				int movesLeft = UnitAttachment.get(transport.getType()).getMovement(player);
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
					final int distanceFromEnd = data.getMap().getDistance(transportTerritory, t);
					if (movesLeft > 0 && (distanceFromEnd > 1 || !remainingUnitsToLoad.isEmpty()))
					{
						final Set<Territory> neighbors = data.getMap().getNeighbors(transportTerritory, canMoveSeaThroughMatch);
						Territory territoryToMoveTo = null;
						int minUnitDistance = Integer.MAX_VALUE;
						for (final Territory neighbor : neighbors)
						{
							final int neighborDistanceFromEnd = data.getMap().getDistance_IgnoreEndForCondition(neighbor, t, canMoveSeaThroughMatch);
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
				
				// Unload transport
				if (!loadedUnits.isEmpty())
				{
					moveUnits.add(loadedUnits);
					transportsToLoad.add(null);
					final Route route = new Route(transportTerritory, t);
					moveRoutes.add(route);
				}
			}
		}
	}
	
	public void doMove(final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final List<Collection<Unit>> transportsToLoad, final IMoveDelegate moveDel)
	{
		for (int i = 0; i < moveRoutes.size(); i++)
		{
			utils.pause();
			if (moveRoutes.get(i) == null || moveRoutes.get(i).getEnd() == null || moveRoutes.get(i).getStart() == null)
			{
				LogUtils.log(Level.WARNING, "Route not valid" + moveRoutes.get(i) + " units:" + moveUnits.get(i));
				continue;
			}
			String result;
			if (transportsToLoad == null || transportsToLoad.get(i) == null)
				result = moveDel.move(moveUnits.get(i), moveRoutes.get(i));
			else
				result = moveDel.move(moveUnits.get(i), moveRoutes.get(i), transportsToLoad.get(i));
			if (result != null)
			{
				LogUtils.log(Level.WARNING, "could not move " + moveUnits.get(i) + " over " + moveRoutes.get(i) + " because : " + result + "\n");
			}
		}
	}
	
	private Map<Unit, Territory> createUnitTerritoryMap(final PlayerID player)
	{
		final List<Territory> allTerritories = ai.getGameData().getMap().getTerritories();
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
