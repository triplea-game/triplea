package games.strategy.triplea.weakAI;

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
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.net.GUID;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.CanalAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.baseAI.AIUtils;
import games.strategy.triplea.baseAI.AbstractAI;
import games.strategy.triplea.delegate.BattleDelegate;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;
import games.strategy.triplea.delegate.dataObjects.PlaceableUnits;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.IntegerMap;
import games.strategy.util.InverseMatch;
import games.strategy.util.Match;
import games.strategy.util.Tuple;
import games.strategy.util.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/*
 * 
 * A very weak ai, based on some simple rules.<p>
 * 
 * 
 * @author Sean Bridges
 */
public class WeakAI extends AbstractAI implements IGamePlayer, ITripleaPlayer
{
	private final static Logger s_logger = Logger.getLogger(WeakAI.class.getName());
	
	/** Creates new TripleAPlayer */
	public WeakAI(final String name, final String type)
	{
		super(name, type);
	}
	
	@Override
	protected void tech(final ITechDelegate techDelegate, final GameData data, final PlayerID player)
	{
	}
	
	private Route getAmphibRoute(final PlayerID player)
	{
		if (!isAmphibAttack(player))
			return null;
		final GameData data = getPlayerBridge().getGameData();
		final Territory ourCapitol = TerritoryAttachment.getCapital(player, data);
		final Match<Territory> endMatch = new Match<Territory>()
		{
			@Override
			public boolean match(final Territory o)
			{
				final boolean impassable = TerritoryAttachment.get(o) != null && TerritoryAttachment.get(o).getIsImpassible();
				return !impassable && !o.isWater() && Utils.hasLandRouteToEnemyOwnedCapitol(o, player, data);
			}
		};
		final Match<Territory> routeCond = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasNoEnemyUnits(player, data));
		final Route withNoEnemy = Utils.findNearest(ourCapitol, endMatch, routeCond, getPlayerBridge().getGameData());
		if (withNoEnemy != null && withNoEnemy.getLength() > 0)
			return withNoEnemy;
		// this will fail if our capitol is not next to water, c'est la vie.
		final Route route = Utils.findNearest(ourCapitol, endMatch, Matches.TerritoryIsWater, getPlayerBridge().getGameData());
		if (route != null && route.getLength() == 0)
		{
			return null;
		}
		return route;
	}
	
	private boolean isAmphibAttack(final PlayerID player)
	{
		final Territory capitol = TerritoryAttachment.getCapital(player, getPlayerBridge().getGameData());
		// we dont own our own capitol
		if (capitol == null || !capitol.getOwner().equals(player))
			return false;
		// find a land route to an enemy territory from our capitol
		final Route invasionRoute = Utils.findNearest(capitol, Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, getPlayerBridge().getGameData()),
					new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, new InverseMatch<Territory>(Matches.TerritoryIsNeutral)), getPlayerBridge().getGameData());
		return invasionRoute == null;
	}
	
	@Override
	protected void move(final boolean nonCombat, final IMoveDelegate moveDel, final GameData data, final PlayerID player)
	{
		if (nonCombat)
		{
			doNonCombatMove(moveDel, player);
		}
		else
		{
			doCombatMove(moveDel, player);
		}
		pause();
	}
	
	private void doNonCombatMove(final IMoveDelegate moveDel, final PlayerID player)
	{
		final GameData data = getPlayerBridge().getGameData();
		final List<Collection<Unit>> moveUnits = new ArrayList<Collection<Unit>>();
		final List<Route> moveRoutes = new ArrayList<Route>();
		final List<Collection<Unit>> transportsToLoad = new ArrayList<Collection<Unit>>();
		// load the transports first
		// they may be able to move farther
		populateTransportLoad(false, data, moveUnits, moveRoutes, transportsToLoad, player);
		doMove(moveUnits, moveRoutes, transportsToLoad, moveDel);
		moveRoutes.clear();
		moveUnits.clear();
		transportsToLoad.clear();
		// do the rest of the moves
		populateNonCombat(data, moveUnits, moveRoutes, player);
		populateNonCombatSea(true, data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		moveUnits.clear();
		moveRoutes.clear();
		transportsToLoad.clear();
		// load the transports again if we can
		// they may be able to move farther
		populateTransportLoad(false, data, moveUnits, moveRoutes, transportsToLoad, player);
		doMove(moveUnits, moveRoutes, transportsToLoad, moveDel);
		moveRoutes.clear();
		moveUnits.clear();
		transportsToLoad.clear();
		// unload the transports that can be unloaded
		populateTransportUnloadNonCom(true, data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
	}
	
	private void doCombatMove(final IMoveDelegate moveDel, final PlayerID player)
	{
		final GameData data = getPlayerBridge().getGameData();
		final List<Collection<Unit>> moveUnits = new ArrayList<Collection<Unit>>();
		final List<Route> moveRoutes = new ArrayList<Route>();
		final List<Collection<Unit>> transportsToLoad = new ArrayList<Collection<Unit>>();
		// load the transports first
		// they may be able to take part in a battle
		populateTransportLoad(true, data, moveUnits, moveRoutes, transportsToLoad, player);
		doMove(moveUnits, moveRoutes, transportsToLoad, moveDel);
		moveRoutes.clear();
		moveUnits.clear();
		// we want to move loaded transports before we try to fight our battles
		populateNonCombatSea(false, data, moveUnits, moveRoutes, player);
		// find second amphib target
		final Route altRoute = getAlternativeAmphibRoute(player);
		if (altRoute != null)
		{
			moveCombatSea(data, moveUnits, moveRoutes, player, altRoute, 1);
		}
		doMove(moveUnits, moveRoutes, null, moveDel);
		moveUnits.clear();
		moveRoutes.clear();
		transportsToLoad.clear();
		// fight
		populateCombatMove(data, moveUnits, moveRoutes, player);
		populateCombatMoveSea(data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
	}
	
	private void populateTransportLoad(final boolean nonCombat, final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes,
				final List<Collection<Unit>> transportsToLoad, final PlayerID player)
	{
		final TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
		if (!isAmphibAttack(player))
			return;
		final Territory capitol = TerritoryAttachment.getCapital(player, data);
		if (capitol == null || !capitol.getOwner().equals(player))
			return;
		List<Unit> unitsToLoad = capitol.getUnits().getMatches(Matches.UnitIsFactoryOrIsInfrastructure.invert());
		unitsToLoad = Match.getMatches(unitsToLoad, Matches.unitIsOwnedBy(getWhoAmI()));
		for (final Territory neighbor : data.getMap().getNeighbors(capitol))
		{
			if (!neighbor.isWater())
				continue;
			final List<Unit> units = new ArrayList<Unit>();
			for (final Unit transport : neighbor.getUnits().getMatches(Matches.unitIsOwnedBy(player)))
			{
				int free = tracker.getAvailableCapacity(transport);
				if (free <= 0)
					continue;
				final Iterator<Unit> iter = unitsToLoad.iterator();
				while (iter.hasNext() && free > 0)
				{
					final Unit current = iter.next();
					final UnitAttachment ua = UnitAttachment.get(current.getType());
					if (ua.getIsAir())
						continue;
					if (ua.getTransportCost() <= free)
					{
						iter.remove();
						free -= ua.getTransportCost();
						units.add(current);
					}
				}
			}
			if (units.size() > 0)
			{
				final Route route = new Route();
				route.setStart(capitol);
				route.add(neighbor);
				moveUnits.add(units);
				moveRoutes.add(route);
				transportsToLoad.add(neighbor.getUnits().getMatches(Matches.UnitIsTransport));
			}
		}
	}
	
	private void populateTransportUnloadNonCom(final boolean nonCombat, final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		final Route amphibRoute = getAmphibRoute(player);
		if (amphibRoute == null)
			return;
		final Territory lastSeaZoneOnAmphib = amphibRoute.getTerritories().get(amphibRoute.getLength() - 1);
		final Territory landOn = amphibRoute.getEnd();
		final CompositeMatch<Unit> landAndOwned = new CompositeMatchAnd<Unit>(Matches.UnitIsLand, Matches.unitIsOwnedBy(player));
		final List<Unit> units = lastSeaZoneOnAmphib.getUnits().getMatches(landAndOwned);
		if (units.size() > 0)
		{
			// just try to make the move, the engine will stop us if it doesnt work
			final Route route = new Route();
			route.setStart(lastSeaZoneOnAmphib);
			route.add(landOn);
			moveUnits.add(units);
			moveRoutes.add(route);
		}
	}
	
	private List<Unit> load2Transports(final boolean reload, final GameData data, final List<Unit> transportsToLoad, final Territory loadFrom, final PlayerID player)
	{
		final TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
		final List<Unit> units = new ArrayList<Unit>();
		for (final Unit transport : transportsToLoad)
		{
			final Collection<Unit> landunits = tracker.transporting(transport);
			for (final Unit u : landunits)
			{
				units.add(u);
			}
		}
		return units;
	}
	
	private void doMove(final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final List<Collection<Unit>> transportsToLoad, final IMoveDelegate moveDel)
	{
		for (int i = 0; i < moveRoutes.size(); i++)
		{
			pause();
			if (moveRoutes.get(i) == null || moveRoutes.get(i).getEnd() == null || moveRoutes.get(i).getStart() == null)
			{
				s_logger.fine("Route not valid" + moveRoutes.get(i) + " units:" + moveUnits.get(i));
				continue;
			}
			String result;
			if (transportsToLoad == null)
				result = moveDel.move(moveUnits.get(i), moveRoutes.get(i));
			else
				result = moveDel.move(moveUnits.get(i), moveRoutes.get(i), transportsToLoad.get(i));
			if (result != null)
			{
				s_logger.fine("could not move " + moveUnits.get(i) + " over " + moveRoutes.get(i) + " because : " + result);
			}
		}
	}
	
	private void moveCombatSea(final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player, final Route amphibRoute, final int maxTrans)
	{
		// TODO workaround - should check if amphibRoute is in moveRoutes
		if (moveRoutes.size() == 2)
		{
			moveRoutes.remove(1);
			moveUnits.remove(1);
		}
		Territory firstSeaZoneOnAmphib = null;
		Territory lastSeaZoneOnAmphib = null;
		if (amphibRoute != null)
		{
			firstSeaZoneOnAmphib = amphibRoute.getTerritories().get(0);
			lastSeaZoneOnAmphib = amphibRoute.getTerritories().get(amphibRoute.getLength() - 1);
		}
		final Match<Unit> ownedAndNotMoved = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.unitHasNotMoved, Transporting);
		final List<Unit> unitsToMove = new ArrayList<Unit>();
		final List<Unit> transports = firstSeaZoneOnAmphib.getUnits().getMatches(ownedAndNotMoved);
		if (transports.size() <= maxTrans)
		{
			unitsToMove.addAll(transports);
		}
		else
		{
			unitsToMove.addAll(transports.subList(0, maxTrans));
		}
		final List<Unit> landUnits = load2Transports(true, data, unitsToMove, firstSeaZoneOnAmphib, player);
		final Route r = getMaxSeaRoute(data, firstSeaZoneOnAmphib, lastSeaZoneOnAmphib, player);
		moveRoutes.add(r);
		unitsToMove.addAll(landUnits);
		moveUnits.add(unitsToMove);
	}
	
	/**
	 * prepares moves for transports
	 * 
	 * @param nonCombat
	 * @param data
	 * @param moveUnits
	 * @param moveRoutes
	 * @param player
	 * @param amphibRoute
	 * @param maxTrans
	 *            -
	 *            if -1 unlimited
	 */
	private void populateNonCombatSea(final boolean nonCombat, final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		final Route amphibRoute = getAmphibRoute(player);
		Territory firstSeaZoneOnAmphib = null;
		Territory lastSeaZoneOnAmphib = null;
		if (amphibRoute != null)
		{
			firstSeaZoneOnAmphib = amphibRoute.getTerritories().get(1);
			lastSeaZoneOnAmphib = amphibRoute.getTerritories().get(amphibRoute.getLength() - 1);
		}
		final Collection<Unit> alreadyMoved = new HashSet<Unit>();
		final Match<Unit> ownedAndNotMoved = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.unitHasNotMoved);
		for (final Territory t : data.getMap())
		{
			// move sea units to the capitol, unless they are loaded transports
			if (t.isWater())
			{
				// land units, move all towards the end point
				if (t.getUnits().someMatch(Matches.UnitIsLand))
				{
					// move along amphi route
					if (lastSeaZoneOnAmphib != null)
					{
						// two move route to end
						final Route r = getMaxSeaRoute(data, t, lastSeaZoneOnAmphib, player);
						if (r != null && r.getLength() > 0)
						{
							moveRoutes.add(r);
							final List<Unit> unitsToMove = t.getUnits().getMatches(Matches.unitIsOwnedBy(player));
							moveUnits.add(unitsToMove);
							alreadyMoved.addAll(unitsToMove);
						}
					}
				}
				if (nonCombat && t.getUnits().someMatch(ownedAndNotMoved))
				{
					// move toward the start of the amphib route
					if (firstSeaZoneOnAmphib != null)
					{
						final Route r = getMaxSeaRoute(data, t, firstSeaZoneOnAmphib, player);
						moveRoutes.add(r);
						moveUnits.add(t.getUnits().getMatches(ownedAndNotMoved));
					}
				}
			}
		}
	}
	
	private Route getMaxSeaRoute(final GameData data, final Territory start, final Territory destination, final PlayerID player)
	{
		Match<Territory> routeCond = null;
		final Set<CanalAttachment> canalAttachments = CanalAttachment.get(destination);
		if (!canalAttachments.isEmpty())
		{
			routeCond = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasEnemyUnits(player, data).invert());
		}
		else
		{
			routeCond = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasEnemyUnits(player, data).invert(), passableChannel(data, player));
		}
		/*
		        new Match<Territory>()
		     {
		         public boolean match(Territory o)
		         {
		             Set<CanalAttachment> canalAttachments = CanalAttachment.get(o);
		             if(canalAttachments.isEmpty())
		                 return true;
		             
		             Iterator<CanalAttachment> iter = canalAttachments.iterator();
		             while(iter.hasNext() )
		             {
		                 CanalAttachment canalAttachment = iter.next();
		                 if(!Match.allMatch( canalAttachment.getLandTerritories(), Matches.isTerritoryAllied(player, data)))
		                     return false;
		             }
		             return true;
		         }
		     }
		);
		*/
		Route r = data.getMap().getRoute(start, destination, routeCond);
		if (r == null)
			return null;
		if (r.getLength() > 2)
		{
			final Route newRoute = new Route();
			newRoute.setStart(start);
			newRoute.add(r.getTerritories().get(1));
			newRoute.add(r.getTerritories().get(2));
			r = newRoute;
		}
		return r;
	}
	
	private void populateCombatMoveSea(final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		final Collection<Unit> unitsAlreadyMoved = new HashSet<Unit>();
		for (final Territory t : data.getMap())
		{
			if (!t.isWater())
				continue;
			if (!t.getUnits().someMatch(Matches.enemyUnit(player, data)))
			{
				continue;
			}
			final Territory enemy = t;
			final float enemyStrength = AIUtils.strength(enemy.getUnits().getUnits(), false, true);
			if (enemyStrength > 0)
			{
				final CompositeMatch<Unit> attackable = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), new Match<Unit>()
				{
					@Override
					public boolean match(final Unit o)
					{
						return !unitsAlreadyMoved.contains(o);
					}
				});
				final Set<Territory> dontMoveFrom = new HashSet<Territory>();
				// find our strength that we can attack with
				int ourStrength = 0;
				final Collection<Territory> attackFrom = data.getMap().getNeighbors(enemy, Matches.TerritoryIsWater);
				for (final Territory owned : attackFrom)
				{
					// dont risk units we are carrying
					if (owned.getUnits().someMatch(Matches.UnitIsLand))
					{
						dontMoveFrom.add(owned);
						continue;
					}
					ourStrength += AIUtils.strength(owned.getUnits().getMatches(attackable), true, true);
				}
				if (ourStrength > 1.32 * enemyStrength)
				{
					s_logger.fine("Attacking : " + enemy + " our strength:" + ourStrength + " enemy strength" + enemyStrength);
					for (final Territory owned : attackFrom)
					{
						if (dontMoveFrom.contains(owned))
							continue;
						final List<Unit> units = owned.getUnits().getMatches(attackable);
						unitsAlreadyMoved.addAll(units);
						moveUnits.add(units);
						moveRoutes.add(data.getMap().getRoute(owned, enemy));
					}
				}
			}
		}
	}
	
	// searches for amphibious attack on empty territory
	private Route getAlternativeAmphibRoute(final PlayerID player)
	{
		if (!isAmphibAttack(player))
			return null;
		final GameData data = getPlayerBridge().getGameData();
		final Match<Territory> routeCondition = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasNoEnemyUnits(player, data));
		// should select all territories with loaded transports
		final Match<Territory> transportOnSea = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasLandUnitsOwnedBy(player));
		Route altRoute = null;
		final int length = Integer.MAX_VALUE;
		for (final Territory t : data.getMap())
		{
			if (!transportOnSea.match(t))
				continue;
			final CompositeMatchAnd<Unit> ownedTransports = new CompositeMatchAnd<Unit>(Matches.UnitCanTransport, Matches.unitIsOwnedBy(player), Matches.unitHasNotMoved);
			final CompositeMatchAnd<Territory> enemyTerritory = new CompositeMatchAnd<Territory>(Matches.isTerritoryEnemy(player, data), Matches.TerritoryIsLand, new InverseMatch<Territory>(
						Matches.TerritoryIsNeutral), Matches.TerritoryIsEmpty);
			final int trans = t.getUnits().countMatches(ownedTransports);
			if (trans > 0)
			{
				final Route newRoute = Utils.findNearest(t, enemyTerritory, routeCondition, data);
				if (newRoute != null && length > newRoute.getLength())
				{
					altRoute = newRoute;
				}
			}
		}
		return altRoute;
	}
	
	private void populateNonCombat(final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		final Collection<Territory> territories = data.getMap().getTerritories();
		movePlanesHomeNonCom(moveUnits, moveRoutes, player);
		// move our units toward the nearest enemy capitol
		for (final Territory t : territories)
		{
			if (t.isWater())
				continue;
			if (TerritoryAttachment.get(t).isCapital())
			{
				// if they are a threat to take our capitol, dont move
				// compare the strength of units we can place
				final float ourStrength = AIUtils.strength(player.getUnits().getUnits(), false, false);
				final float attackerStrength = Utils.getStrengthOfPotentialAttackers(t, data);
				if (attackerStrength > ourStrength)
					continue;
			}
			// these are the units we can move
			final CompositeMatch<Unit> moveOfType = new CompositeMatchAnd<Unit>();
			moveOfType.add(Matches.unitIsOwnedBy(player));
			moveOfType.add(Matches.UnitIsNotAA);
			// we can never move factories
			moveOfType.add(Matches.UnitIsNotStatic(player));
			moveOfType.add(Matches.UnitIsNotFactory);
			moveOfType.add(Matches.UnitIsLand);
			final CompositeMatchAnd<Territory> moveThrough = new CompositeMatchAnd<Territory>(new InverseMatch<Territory>(Matches.TerritoryIsImpassable), new InverseMatch<Territory>(
						Matches.TerritoryIsNeutral), Matches.TerritoryIsLand);
			final List<Unit> units = t.getUnits().getMatches(moveOfType);
			if (units.size() == 0)
				continue;
			int minDistance = Integer.MAX_VALUE;
			Territory to = null;
			// find the nearest enemy owned capital
			for (final PlayerID otherPlayer : data.getPlayerList().getPlayers())
			{
				final Territory capitol = TerritoryAttachment.getCapital(otherPlayer, data);
				if (capitol != null && !data.getRelationshipTracker().isAllied(player, capitol.getOwner()))
				{
					final Route route = data.getMap().getRoute(t, capitol, moveThrough);
					if (route != null)
					{
						final int distance = route.getLength();
						if (distance != 0 && distance < minDistance)
						{
							minDistance = distance;
							to = capitol;
						}
					}
				}
			}
			if (to != null)
			{
				if (units.size() > 0)
				{
					moveUnits.add(units);
					final Route routeToCapitol = data.getMap().getRoute(t, to, moveThrough);
					final Territory firstStep = routeToCapitol.getTerritories().get(1);
					final Route route = new Route();
					route.setStart(t);
					route.add(firstStep);
					moveRoutes.add(route);
				}
			}
			// if we cant move to a capitol, move towards the enemy
			else
			{
				final CompositeMatchAnd<Territory> routeCondition = new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, Matches.TerritoryIsImpassable.invert());
				Route newRoute = Utils.findNearest(t, Matches.territoryHasEnemyLandUnits(player, data), routeCondition, data);
				// move to any enemy territory
				if (newRoute == null)
				{
					newRoute = Utils.findNearest(t, Matches.isTerritoryEnemy(player, data), routeCondition, data);
				}
				if (newRoute != null && newRoute.getLength() != 0)
				{
					moveUnits.add(units);
					final Territory firstStep = newRoute.getTerritories().get(1);
					final Route route = new Route();
					route.setStart(t);
					route.add(firstStep);
					moveRoutes.add(route);
				}
			}
		}
	}
	
	private void movePlanesHomeNonCom(final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		// the preferred way to get the delegate
		final IMoveDelegate delegateRemote = (IMoveDelegate) getPlayerBridge().getRemote();
		// this works because we are on the server
		final BattleDelegate delegate = DelegateFinder.battleDelegate(getPlayerBridge().getGameData());
		final Match<Territory> canLand = new CompositeMatchAnd<Territory>(Matches.isTerritoryAllied(player, getPlayerBridge().getGameData()), new Match<Territory>()
		{
			@Override
			public boolean match(final Territory o)
			{
				return !delegate.getBattleTracker().wasConquered(o);
			}
		});
		final Match<Territory> routeCondition = new CompositeMatchAnd<Territory>(Matches.territoryHasEnemyAAforCombatOnly(player, getPlayerBridge().getGameData()).invert(),
					Matches.TerritoryIsImpassable.invert());
		for (final Territory t : delegateRemote.getTerritoriesWhereAirCantLand())
		{
			final Route noAARoute = Utils.findNearest(t, canLand, routeCondition, getPlayerBridge().getGameData());
			final Route aaRoute = Utils.findNearest(t, canLand, Matches.TerritoryIsImpassable.invert(), getPlayerBridge().getGameData());
			final Collection<Unit> airToLand = t.getUnits().getMatches(new CompositeMatchAnd<Unit>(Matches.UnitIsAir, Matches.unitIsOwnedBy(player)));
			// dont bother to see if all the air units have enough movement points
			// to move without aa guns firing
			// simply move first over no aa, then with aa
			// one (but hopefully not both) will be rejected
			moveUnits.add(airToLand);
			moveRoutes.add(noAARoute);
			moveUnits.add(airToLand);
			moveRoutes.add(aaRoute);
		}
	}
	
	private void populateCombatMove(final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		populateBomberCombat(data, moveUnits, moveRoutes, player);
		final Collection<Unit> unitsAlreadyMoved = new HashSet<Unit>();
		// find the territories we can just walk into
		final CompositeMatchOr<Territory> walkInto = new CompositeMatchOr<Territory>(Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data),
					Matches.isTerritoryFreeNeutral(data));
		final List<Territory> enemyOwned = Match.getMatches(data.getMap().getTerritories(), walkInto);
		Collections.sort(enemyOwned, new Comparator<Territory>()
		{
			private final Map<Territory, Integer> randomInts = new HashMap<Territory, Integer>();
			
			public int compare(final Territory o1, final Territory o2)
			{
				final TerritoryAttachment ta1 = TerritoryAttachment.get(o1);
				final TerritoryAttachment ta2 = TerritoryAttachment.get(o2);
				// take capitols first if we can
				if (ta1 != null && ta2 != null)
				{
					if (ta1.isCapital() && !ta2.isCapital())
						return -1; // 1;
					if (!ta1.isCapital() && ta2.isCapital())
						return 1; // -1;
				}
				final boolean factoryInT1 = o1.getUnits().someMatch(Matches.UnitIsFactory);
				final boolean factoryInT2 = o2.getUnits().someMatch(Matches.UnitIsFactory);
				// next take territories with factories
				if (factoryInT1 && !factoryInT2)
					return -1; // 1;
				if (!factoryInT1 && factoryInT2)
					return 1; // -1;
				// randomness is a better guide than any other metric
				// sort the remaining randomly
				if (!randomInts.containsKey(o1))
					randomInts.put(o1, (int) (Math.random() * 1000));
				if (!randomInts.containsKey(o2))
					randomInts.put(o2, (int) (Math.random() * 1000));
				return randomInts.get(o1) - randomInts.get(o2);
			}
		});
		final List<Territory> isWaterTerr = Utils.onlyWaterTerr(data, enemyOwned);
		enemyOwned.removeAll(isWaterTerr);
		// first find the territories we can just walk into
		for (final Territory enemy : enemyOwned)
		{
			if (AIUtils.strength(enemy.getUnits().getUnits(), false, false) == 0)
			{
				// only take it with 1 unit
				boolean taken = false;
				for (final Territory attackFrom : data.getMap().getNeighbors(enemy, Matches.territoryHasLandUnitsOwnedBy(player)))
				{
					if (taken)
						break;
					// get the cheapest unit to move in
					final List<Unit> unitsSortedByCost = new ArrayList<Unit>(attackFrom.getUnits().getUnits());
					Collections.sort(unitsSortedByCost, AIUtils.getCostComparator());
					for (final Unit unit : unitsSortedByCost)
					{
						final Match<Unit> match = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsLand, Matches.UnitIsNotFactory, Matches.UnitIsNotStatic(player),
									Matches.UnitIsNotAA);
						if (!unitsAlreadyMoved.contains(unit) && match.match(unit))
						{
							moveRoutes.add(data.getMap().getRoute(attackFrom, enemy));
							// if unloading units, unload all of them,
							// otherwise we wont be able to unload them
							// in non com, for land moves we want to move the minimal
							// number of units, to leave units free to move elsewhere
							if (attackFrom.isWater())
							{
								final List<Unit> units = attackFrom.getUnits().getMatches(Matches.unitIsLandAndOwnedBy(player));
								moveUnits.add(Util.difference(units, unitsAlreadyMoved));
								unitsAlreadyMoved.addAll(units);
							}
							else
							{
								moveUnits.add(Collections.singleton(unit));
							}
							unitsAlreadyMoved.add(unit);
							taken = true;
							break;
						}
					}
				}
			}
		}
		// find the territories we can reasonably expect to take
		for (final Territory enemy : enemyOwned)
		{
			final float enemyStrength = AIUtils.strength(enemy.getUnits().getUnits(), false, false);
			if (enemyStrength > 0)
			{
				final CompositeMatch<Unit> attackable = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsStrategicBomber.invert(), new Match<Unit>()
				{
					@Override
					public boolean match(final Unit o)
					{
						return !unitsAlreadyMoved.contains(o);
					}
				});
				attackable.add(Matches.UnitIsNotAA);
				attackable.add(Matches.UnitIsNotStatic(player));
				attackable.add(Matches.UnitIsNotFactory);
				attackable.add(Matches.UnitIsNotSea);
				final Set<Territory> dontMoveFrom = new HashSet<Territory>();
				// find our strength that we can attack with
				int ourStrength = 0;
				final Collection<Territory> attackFrom = data.getMap().getNeighbors(enemy, Matches.territoryHasLandUnitsOwnedBy(player));
				for (final Territory owned : attackFrom)
				{
					if (TerritoryAttachment.get(owned) != null && TerritoryAttachment.get(owned).isCapital()
								&& (Utils.getStrengthOfPotentialAttackers(owned, getPlayerBridge().getGameData()) > AIUtils.strength(owned.getUnits().getUnits(), false, false)))
					{
						dontMoveFrom.add(owned);
						continue;
					}
					ourStrength += AIUtils.strength(owned.getUnits().getMatches(attackable), true, false);
				}
				// prevents 2 infantry from attacking 1 infantry
				if (ourStrength > 1.37 * enemyStrength)
				{
					// this is all we need to take it, dont go overboard, since we may be able to use the units to attack somewhere else
					double remainingStrengthNeeded = (2.5 * enemyStrength) + 4;
					for (final Territory owned : attackFrom)
					{
						if (dontMoveFrom.contains(owned))
							continue;
						List<Unit> units = owned.getUnits().getMatches(attackable);
						// only take the units we need if
						// 1) we are not an amphibious attack
						// 2) we can potentially attack another territory
						if (!owned.isWater() && data.getMap().getNeighbors(owned, Matches.territoryHasEnemyLandUnits(player, data)).size() > 1)
							units = Utils.getUnitsUpToStrength(remainingStrengthNeeded, units, true, false);
						remainingStrengthNeeded -= AIUtils.strength(units, true, false);
						if (units.size() > 0)
						{
							unitsAlreadyMoved.addAll(units);
							moveUnits.add(units);
							moveRoutes.add(data.getMap().getRoute(owned, enemy));
						}
					}
					s_logger.fine("Attacking : " + enemy + " our strength:" + ourStrength + " enemy strength" + enemyStrength + " remaining strength needed " + remainingStrengthNeeded);
				}
			}
		}
	}
	
	private void populateBomberCombat(final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		final Match<Territory> enemyFactory = Matches.territoryHasEnemyFactory(getPlayerBridge().getGameData(), player);
		final Match<Unit> ownBomber = new CompositeMatchAnd<Unit>(Matches.UnitIsStrategicBomber, Matches.unitIsOwnedBy(player));
		for (final Territory t : data.getMap().getTerritories())
		{
			final Collection<Unit> bombers = t.getUnits().getMatches(ownBomber);
			if (bombers.isEmpty())
				continue;
			final Match<Territory> routeCond = new InverseMatch<Territory>(Matches.territoryHasEnemyAAforCombatOnly(player, getPlayerBridge().getGameData()));
			final Route bombRoute = Utils.findNearest(t, enemyFactory, routeCond, getPlayerBridge().getGameData());
			moveUnits.add(bombers);
			moveRoutes.add(bombRoute);
		}
	}
	
	private int countTransports(final GameData data, final PlayerID player)
	{
		final CompositeMatchAnd<Unit> ownedTransport = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport, Matches.unitIsOwnedBy(player));
		int sum = 0;
		for (final Territory t : data.getMap())
		{
			sum += t.getUnits().countMatches(ownedTransport);
		}
		return sum;
	}
	
	private int countLandUnits(final GameData data, final PlayerID player)
	{
		final CompositeMatchAnd<Unit> ownedLandUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsLand, Matches.unitIsOwnedBy(player));
		int sum = 0;
		for (final Territory t : data.getMap())
		{
			sum += t.getUnits().countMatches(ownedLandUnit);
		}
		return sum;
	}
	
	@Override
	protected void purchase(final boolean purchaseForBid, final int PUsToSpend, final IPurchaseDelegate purchaseDelegate, final GameData data, final PlayerID player)
	{
		if (!this.canWePurchaseOrRepair(purchaseForBid))
			return;
		if (purchaseForBid)
		{
			// bid will only buy land units, due to weak ai placement for bid not being able to handle sea units
			final Resource PUs = data.getResourceList().getResource(Constants.PUS);
			int leftToSpend = PUsToSpend;
			final List<ProductionRule> rules = player.getProductionFrontier().getRules();
			final IntegerMap<ProductionRule> purchase = new IntegerMap<ProductionRule>();
			int minCost = Integer.MAX_VALUE;
			int i = 0;
			while ((minCost == Integer.MAX_VALUE || leftToSpend >= minCost) && i < 100000)
			{
				i++;
				for (final ProductionRule rule : rules)
				{
					final UnitType results = (UnitType) rule.getResults().keySet().iterator().next();
					if (Matches.UnitTypeIsSea.match(results) || Matches.UnitTypeIsAir.match(results) || Matches.UnitTypeIsFactoryOrIsInfrastructure.match(results)
								|| Matches.UnitTypeIsAAforAnything.match(results)
								|| Matches.UnitTypeHasMaxBuildRestrictions.match(results) || Matches.UnitTypeConsumesUnitsOnCreation.match(results) || Matches.unitTypeIsStatic(player).match(results))
					{
						continue;
					}
					final int cost = rule.getCosts().getInt(PUs);
					if (cost < 1)
						continue;
					if (minCost == Integer.MAX_VALUE)
					{
						minCost = cost;
					}
					if (minCost > cost)
					{
						minCost = cost;
					}
					// give a preference to cheap units
					if (Math.random() * cost < 2)
					{
						if (cost <= leftToSpend)
						{
							leftToSpend -= cost;
							purchase.add(rule, 1);
						}
					}
				}
			}
			purchaseDelegate.purchase(purchase);
			return;
		}
		pause();
		final boolean isAmphib = isAmphibAttack(player);
		final Route amphibRoute = getAmphibRoute(player);
		final int transportCount = countTransports(getPlayerBridge().getGameData(), player);
		final int landUnitCount = countLandUnits(getPlayerBridge().getGameData(), player);
		int defUnitsAtAmpibRoute = 0;
		if (isAmphib && amphibRoute != null)
		{
			defUnitsAtAmpibRoute = amphibRoute.getEnd().getUnits().getUnitCount();
		}
		final Resource PUs = data.getResourceList().getResource(Constants.PUS);
		final int totPU = player.getResources().getQuantity(PUs);
		int leftToSpend = totPU;
		final Territory capitol = TerritoryAttachment.getCapital(player, data);
		final List<ProductionRule> rules = player.getProductionFrontier().getRules();
		final IntegerMap<ProductionRule> purchase = new IntegerMap<ProductionRule>();
		List<RepairRule> rrules = Collections.emptyList();
		final CompositeMatch<Unit> ourFactories = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsFactory);
		final List<Territory> rfactories = Utils.findUnitTerr(data, player, ourFactories);
		if (player.getRepairFrontier() != null && games.strategy.triplea.Properties.getSBRAffectsUnitProduction(data)) // figure out if anything needs to be repaired
		{
			rrules = player.getRepairFrontier().getRules();
			final IntegerMap<RepairRule> repairMap = new IntegerMap<RepairRule>();
			final HashMap<Unit, IntegerMap<RepairRule>> repair = new HashMap<Unit, IntegerMap<RepairRule>>();
			final int minimumUnitPrice = 3;
			int diff = 0;
			int totalDamage = 0;
			int capDamage = 0;
			int maxUnits = (totPU - 1) / minimumUnitPrice;
			int currentProduction = 0;
			int maxProduction = 0;
			for (final Territory fixTerr : rfactories)
			{
				if (!Matches.territoryHasOwnedFactory(data, player).match(fixTerr))
					continue;
				final TerritoryAttachment ta = TerritoryAttachment.get(fixTerr);
				maxProduction += ta.getProduction();
				diff = ta.getProduction() - ta.getUnitProduction();
				totalDamage += diff;
				if (fixTerr == capitol)
					capDamage += diff;
				if (ta.getUnitProduction() > 0)
					currentProduction += ta.getUnitProduction();
			}
			rfactories.remove(capitol);
			Collections.shuffle(rfactories); // we should sort this
			// assume minimum unit price is 3, and that we are buying only that... if we over repair, oh well, that is better than under-repairing
			// goal is to be able to produce all our units, and at least half of that production in the capitol
			if (TerritoryAttachment.get(capitol).getUnitProduction() <= maxUnits / 2 || rfactories.isEmpty()) // if capitol is super safe, we don't have to do this. and if capitol is under siege, we should repair enough to place all our units here
			{
				for (final RepairRule rrule : rrules)
				{
					if (!Matches.territoryHasOwnedFactory(data, player).match(capitol))
						continue;
					final TerritoryAttachment ta = TerritoryAttachment.get(capitol);
					diff = ta.getProduction() - ta.getUnitProduction();
					if (!rfactories.isEmpty())
						diff = Math.min(diff, (maxUnits / 2 - ta.getUnitProduction()) + 1);
					else
						diff = Math.min(diff, (maxUnits - ta.getUnitProduction()));
					diff = Math.min(diff, leftToSpend - minimumUnitPrice);
					if (diff > 0)
					{
						if (ta.getUnitProduction() >= 0)
							currentProduction += diff;
						else
							currentProduction += diff + ta.getUnitProduction();
						repairMap.add(rrule, diff);
						repair.put(Match.getMatches(capitol.getUnits().getUnits(), Matches.UnitIsFactoryOrCanBeDamaged).iterator().next(), repairMap);
						leftToSpend -= diff;
						purchaseDelegate.purchaseRepair(repair);
						repair.clear();
						repairMap.clear();
						maxUnits = (leftToSpend - 1) / minimumUnitPrice; // ideally we would adjust this after each single PU spent, then re-evaluate everything.
					}
				}
			}
			int i = 0;
			while (currentProduction < maxUnits && i < 2)
			{
				for (final RepairRule rrule : rrules)
				{
					for (final Territory fixTerr : rfactories)
					{
						if (!Matches.territoryHasOwnedFactory(data, player).match(fixTerr))
							continue;
						// we will repair the first territories in the list as much as we can, until we fulfill the condition, then skip all other territories
						if (currentProduction >= maxUnits)
							continue;
						final TerritoryAttachment ta = TerritoryAttachment.get(fixTerr);
						diff = ta.getProduction() - ta.getUnitProduction();
						if (i == 0)
						{
							if (ta.getUnitProduction() < 0)
								diff = Math.min(diff, (maxUnits - currentProduction) - ta.getUnitProduction());
							else
								diff = Math.min(diff, (maxUnits - currentProduction));
						}
						diff = Math.min(diff, leftToSpend - minimumUnitPrice);
						if (diff > 0)
						{
							if (ta.getUnitProduction() >= 0)
								currentProduction += diff;
							else
								currentProduction += diff + ta.getUnitProduction();
							repairMap.add(rrule, diff);
							repair.put(Match.getMatches(fixTerr.getUnits().getUnits(), Matches.UnitIsFactoryOrCanBeDamaged).iterator().next(), repairMap);
							leftToSpend -= diff;
							purchaseDelegate.purchaseRepair(repair);
							repair.clear();
							repairMap.clear();
							maxUnits = (leftToSpend - 1) / minimumUnitPrice; // ideally we would adjust this after each single PU spent, then re-evaluate everything.
						}
					}
				}
				rfactories.add(capitol);
				i++;
			}
		}
		else if (player.getRepairFrontier() != null && games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data)) // figure out if anything needs to be repaired
		{
			rrules = player.getRepairFrontier().getRules();
			final IntegerMap<RepairRule> repairMap = new IntegerMap<RepairRule>();
			final HashMap<Unit, IntegerMap<RepairRule>> repair = new HashMap<Unit, IntegerMap<RepairRule>>();
			final Collection<Unit> unitsThatCanProduceNeedingRepair = new ArrayList<Unit>();
			final Collection<Unit> unitsThatAreDisabledNeedingRepair = new ArrayList<Unit>();
			final CompositeMatchAnd<Unit> ourDisabled = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsDisabled());
			final int minimumUnitPrice = 3;
			int diff = 0;
			int totalDamage = 0;
			int capDamage = 0;
			int capProduction = 0;
			Unit capUnit = null;
			int maxUnits = (totPU - 1) / minimumUnitPrice;
			int currentProduction = 0;
			int maxProduction = 0;
			Collections.shuffle(rfactories); // we should sort this
			for (final Territory fixTerr : rfactories)
			{
				if (!Matches.territoryHasOwnedFactory(data, player).match(fixTerr))
					continue;
				final Unit possibleFactoryNeedingRepair = TripleAUnit.getBiggestProducer(Match.getMatches(fixTerr.getUnits().getUnits(), ourFactories), fixTerr, player, data, false);
				if (Matches.UnitHasSomeUnitDamage().match(possibleFactoryNeedingRepair))
					unitsThatCanProduceNeedingRepair.add(possibleFactoryNeedingRepair);
				unitsThatAreDisabledNeedingRepair.addAll(Match.getMatches(fixTerr.getUnits().getUnits(), ourDisabled));
				final TripleAUnit taUnit = (TripleAUnit) possibleFactoryNeedingRepair;
				maxProduction += TripleAUnit.getHowMuchCanUnitProduce(possibleFactoryNeedingRepair, fixTerr, player, data, false, true);
				diff = taUnit.getUnitDamage();
				totalDamage += diff;
				if (fixTerr == capitol)
				{
					capDamage += diff;
					capProduction = TripleAUnit.getHowMuchCanUnitProduce(possibleFactoryNeedingRepair, fixTerr, player, data, true, true);
					capUnit = possibleFactoryNeedingRepair;
				}
				currentProduction += TripleAUnit.getHowMuchCanUnitProduce(possibleFactoryNeedingRepair, fixTerr, player, data, true, true);
			}
			rfactories.remove(capitol);
			unitsThatCanProduceNeedingRepair.remove(capUnit);
			// assume minimum unit price is 3, and that we are buying only that... if we over repair, oh well, that is better than under-repairing
			// goal is to be able to produce all our units, and at least half of that production in the capitol
			if ((capProduction <= maxUnits / 2 || rfactories.isEmpty()) && capUnit != null) // if capitol is super safe, we don't have to do this. and if capitol is under siege, we should repair enough to place all our units here
			{
				for (final RepairRule rrule : rrules)
				{
					if (capUnit == null || !capUnit.getUnitType().equals(rrule.getResults().keySet().iterator().next()))
						continue;
					if (!Matches.territoryHasOwnedFactory(data, player).match(capitol))
						continue;
					final TripleAUnit taUnit = (TripleAUnit) capUnit;
					diff = taUnit.getUnitDamage();
					final int unitProductionAllowNegative = TripleAUnit.getHowMuchCanUnitProduce(capUnit, capUnit.getTerritoryUnitIsIn(), player, data, false, true) - diff;
					if (!rfactories.isEmpty())
						diff = Math.min(diff, (maxUnits / 2 - unitProductionAllowNegative) + 1);
					else
						diff = Math.min(diff, (maxUnits - unitProductionAllowNegative));
					diff = Math.min(diff, leftToSpend - minimumUnitPrice);
					if (diff > 0)
					{
						if (unitProductionAllowNegative >= 0)
							currentProduction += diff;
						else
							currentProduction += diff + unitProductionAllowNegative;
						repairMap.add(rrule, diff);
						repair.put(capUnit, repairMap);
						leftToSpend -= diff;
						purchaseDelegate.purchaseRepair(repair);
						repair.clear();
						repairMap.clear();
						maxUnits = (leftToSpend - 1) / minimumUnitPrice; // ideally we would adjust this after each single PU spent, then re-evaluate everything.
					}
				}
			}
			int i = 0;
			while (currentProduction < maxUnits && i < 2)
			{
				for (final RepairRule rrule : rrules)
				{
					for (final Unit fixUnit : unitsThatCanProduceNeedingRepair)
					{
						if (fixUnit == null || !fixUnit.getType().equals(rrule.getResults().keySet().iterator().next()))
							continue;
						if (!Matches.territoryHasOwnedFactory(data, player).match(fixUnit.getTerritoryUnitIsIn()))
							continue;
						// we will repair the first territories in the list as much as we can, until we fulfill the condition, then skip all other territories
						if (currentProduction >= maxUnits)
							continue;
						final TripleAUnit taUnit = (TripleAUnit) fixUnit;
						diff = taUnit.getUnitDamage();
						final int unitProductionAllowNegative = TripleAUnit.getHowMuchCanUnitProduce(fixUnit, fixUnit.getTerritoryUnitIsIn(), player, data, false, true) - diff;
						if (i == 0)
						{
							if (unitProductionAllowNegative < 0)
								diff = Math.min(diff, (maxUnits - currentProduction) - unitProductionAllowNegative);
							else
								diff = Math.min(diff, (maxUnits - currentProduction));
						}
						diff = Math.min(diff, leftToSpend - minimumUnitPrice);
						if (diff > 0)
						{
							if (unitProductionAllowNegative >= 0)
								currentProduction += diff;
							else
								currentProduction += diff + unitProductionAllowNegative;
							repairMap.add(rrule, diff);
							repair.put(fixUnit, repairMap);
							leftToSpend -= diff;
							purchaseDelegate.purchaseRepair(repair);
							repair.clear();
							repairMap.clear();
							maxUnits = (leftToSpend - 1) / minimumUnitPrice; // ideally we would adjust this after each single PU spent, then re-evaluate everything.
						}
					}
				}
				rfactories.add(capitol);
				if (capUnit != null)
					unitsThatCanProduceNeedingRepair.add(capUnit);
				i++;
			}
		}
		int minCost = Integer.MAX_VALUE;
		int i = 0;
		while ((minCost == Integer.MAX_VALUE || leftToSpend >= minCost) && i < 100000)
		{
			i++;
			for (final ProductionRule rule : rules)
			{
				final UnitType results = (UnitType) rule.getResults().keySet().iterator().next();
				if (Matches.UnitTypeIsAir.match(results) || Matches.UnitTypeIsFactoryOrIsInfrastructure.match(results) || Matches.UnitTypeIsAAforAnything.match(results)
							|| Matches.UnitTypeHasMaxBuildRestrictions.match(results) || Matches.UnitTypeConsumesUnitsOnCreation.match(results) || Matches.unitTypeIsStatic(player).match(results))
				{
					continue;
				}
				final int transportCapacity = UnitAttachment.get(results).getTransportCapacity();
				// buy transports if we can be amphibious
				if (Matches.UnitTypeIsSea.match(results))
					if (!isAmphib || transportCapacity <= 0)
					{
						continue;
					}
				final int cost = rule.getCosts().getInt(PUs);
				if (cost < 1)
					continue;
				if (minCost == Integer.MAX_VALUE)
				{
					minCost = cost;
				}
				if (minCost > cost)
				{
					minCost = cost;
				}
				// give a preferene to cheap units, and to transports
				// but dont go overboard with buying transports
				int goodNumberOfTransports = 0;
				final boolean isTransport = transportCapacity > 0;
				if (amphibRoute != null)
				{
					// 25% transports - can be more if frontier is far away
					goodNumberOfTransports = (landUnitCount / 4);
					// boost for transport production
					if (isTransport && defUnitsAtAmpibRoute > goodNumberOfTransports && landUnitCount > defUnitsAtAmpibRoute && defUnitsAtAmpibRoute > transportCount)
					{
						final int transports = (leftToSpend / cost);
						leftToSpend -= cost * transports;
						purchase.add(rule, transports);
						continue;
					}
					// goodNumberOfTransports = ((int) (amphibRoute.getTerritories().size() * 2.6)) + 1;
				}
				final boolean buyBecauseTransport = (Math.random() < 0.7 && transportCount < goodNumberOfTransports) || Math.random() < 0.10;
				final boolean dontBuyBecauseTooManyTransports = transportCount > 2 * goodNumberOfTransports;
				if ((!isTransport && Math.random() * cost < 2) || (isTransport && buyBecauseTransport && !dontBuyBecauseTooManyTransports))
				{
					if (cost <= leftToSpend)
					{
						leftToSpend -= cost;
						purchase.add(rule, 1);
					}
				}
			}
		}
		purchaseDelegate.purchase(purchase);
	}
	
	@Override
	protected void place(final boolean bid, final IAbstractPlaceDelegate placeDelegate, final GameData data, final PlayerID player)
	{
		if (player.getUnits().size() == 0)
			return;
		final Territory capitol = TerritoryAttachment.getCapital(player, data);
		// place in capitol first
		placeAllWeCanOn(data, capitol, placeDelegate, player);
		final List<Territory> randomTerritories = new ArrayList<Territory>(data.getMap().getTerritories());
		Collections.shuffle(randomTerritories);
		for (final Territory t : randomTerritories)
		{
			if (t != capitol && t.getOwner().equals(player) && t.getUnits().someMatch(Matches.UnitIsFactory))
			{
				placeAllWeCanOn(data, t, placeDelegate, player);
			}
		}
	}
	
	private void placeAllWeCanOn(final GameData data, final Territory placeAt, final IAbstractPlaceDelegate placeDelegate, final PlayerID player)
	{
		final PlaceableUnits pu = placeDelegate.getPlaceableUnits(player.getUnits().getUnits(), placeAt);
		if (pu.getErrorMessage() != null)
			return;
		int placementLeft = pu.getMaxUnits();
		if (placementLeft == -1)
			placementLeft = Integer.MAX_VALUE;
		final List<Unit> seaUnits = new ArrayList<Unit>(player.getUnits().getMatches(Matches.UnitIsSea));
		if (seaUnits.size() > 0)
		{
			final Route amphibRoute = getAmphibRoute(player);
			Territory seaPlaceAt = null;
			if (amphibRoute != null)
			{
				seaPlaceAt = amphibRoute.getTerritories().get(1);
			}
			else
			{
				final Set<Territory> seaNeighbors = data.getMap().getNeighbors(placeAt, Matches.TerritoryIsWater);
				if (!seaNeighbors.isEmpty())
					seaPlaceAt = seaNeighbors.iterator().next();
			}
			if (seaPlaceAt != null)
			{
				final int seaPlacement = Math.min(placementLeft, seaUnits.size());
				placementLeft -= seaPlacement;
				final Collection<Unit> toPlace = seaUnits.subList(0, seaPlacement);
				doPlace(seaPlaceAt, toPlace, placeDelegate);
			}
		}
		final List<Unit> landUnits = new ArrayList<Unit>(player.getUnits().getMatches(Matches.UnitIsLand));
		if (!landUnits.isEmpty())
		{
			final int landPlaceCount = Math.min(placementLeft, landUnits.size());
			placementLeft -= landPlaceCount;
			final Collection<Unit> toPlace = landUnits.subList(0, landPlaceCount);
			doPlace(placeAt, toPlace, placeDelegate);
		}
	}
	
	private void doPlace(final Territory where, final Collection<Unit> toPlace, final IAbstractPlaceDelegate del)
	{
		final String message = del.placeUnits(new ArrayList<Unit>(toPlace), where);
		if (message != null)
		{
			s_logger.fine(message);
			s_logger.fine("Attempt was at:" + where + " with:" + toPlace);
		}
		pause();
	}
	
	/*
	 * 
	 * 
	 * @see games.strategy.triplea.player.ITripleaPlayer#selectCasualties(java.lang.String,
	 *      java.util.Collection, java.util.Map, int, java.lang.String,
	 *      games.strategy.triplea.delegate.DiceRoll,
	 *      games.strategy.engine.data.PlayerID, java.util.List)
	 *
	 *      Added new collection autoKilled to handle killing units prior to casualty selection
	 */
	public CasualtyDetails selectCasualties(final Collection<Unit> selectFrom, final Map<Unit, Collection<Unit>> dependents, final int count, final String message, final DiceRoll dice,
				final PlayerID hit, final CasualtyList defaultCasualties, final GUID battleID)
	{
		final List<Unit> rDamaged = new ArrayList<Unit>();
		final List<Unit> rKilled = new ArrayList<Unit>();
		rDamaged.addAll(defaultCasualties.getDamaged());
		rKilled.addAll(defaultCasualties.getKilled());
		/*for(Unit unit : defaultCasualties)
		{
		    boolean twoHit = UnitAttachment.get(unit.getType()).isTwoHit();
		    //if it appears twice it then it both damaged and killed
		    if(unit.getHits() == 0 && twoHit && !rDamaged.contains(unit))
		        rDamaged.add(unit);
		    else
		        rKilled.add(unit);
		}*/
		final CasualtyDetails m2 = new CasualtyDetails(rKilled, rDamaged, false);
		return m2;
	}
	
	/*
	 * @see games.strategy.triplea.player.ITripleaPlayer#shouldBomberBomb(games.strategy.engine.data.Territory)
	 */
	public boolean shouldBomberBomb(final Territory territory)
	{
		return true;
	}
	
	/*
	 * @see games.strategy.triplea.player.ITripleaPlayer#shouldBomberBomb(games.strategy.engine.data.Territory)
	 */
	public Unit whatShouldBomberBomb(final Territory territory, final Collection<Unit> units)
	{
		if (units == null || units.isEmpty())
			return null;
		if (!Match.someMatch(units, Matches.UnitIsFactoryOrCanProduceUnits))
			return units.iterator().next();
		if (Match.someMatch(units, Matches.UnitIsFactory))
			return Match.getMatches(units, Matches.UnitIsFactory).iterator().next();
		else
			return Match.getMatches(units, Matches.UnitCanProduceUnits).iterator().next();
	}
	
	/*
	 * @see games.strategy.triplea.player.ITripleaPlayer#getNumberOfFightersToMoveToNewCarrier(java.util.Collection, games.strategy.engine.data.Territory)
	 */
	public Collection<Unit> getNumberOfFightersToMoveToNewCarrier(final Collection<Unit> fightersThatCanBeMoved, final Territory from)
	{
		final List<Unit> rVal = new ArrayList<Unit>();
		for (final Unit fighter : fightersThatCanBeMoved)
		{
			if (Math.random() < 0.8)
				rVal.add(fighter);
		}
		return rVal;
	}
	
	/*
	 * @see games.strategy.triplea.player.ITripleaPlayer#selectTerritoryForAirToLand(java.util.Collection, java.lang.String)
	 */
	public Territory selectTerritoryForAirToLand(final Collection<Territory> candidates, final Territory currentTerritory, final String unitMessage)
	{
		return candidates.iterator().next();
	}
	
	public boolean confirmMoveInFaceOfAA(final Collection<Territory> aaFiringTerritories)
	{
		return true;
	}
	
	public Territory retreatQuery(final GUID battleID, final boolean submerge, final Collection<Territory> possibleTerritories, final String message)
	{
		return null;
	}
	
	/*public Collection<Unit> scrambleQuery(final GUID battleID, final Collection<Territory> possibleTerritories, final String message, final PlayerID player)
	{
		return null;
	}*/

	public HashMap<Territory, Collection<Unit>> scrambleUnitsQuery(final Territory scrambleTo, final Map<Territory, Tuple<Integer, Collection<Unit>>> possibleScramblers)
	{
		return null;
	}
	
	public Collection<Unit> selectUnitsQuery(final Territory current, final Collection<Unit> possible, final String message)
	{
		return null;
	}
	
	/* (non-Javadoc)
	 * @see games.strategy.triplea.player.ITripleaPlayer#selectFixedDice(int, java.lang.String)
	 */
	public int[] selectFixedDice(final int numRolls, final int hitAt, final boolean hitOnlyIfEquals, final String message, final int diceSides)
	{
		final int[] dice = new int[numRolls];
		for (int i = 0; i < numRolls; i++)
		{
			dice[i] = (int) Math.ceil(Math.random() * diceSides);
		}
		return dice;
	}
	
	public static final Match<Unit> Transporting = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit o)
		{
			return (TripleAUnit.get(o).getTransporting().size() > 0);
		}
	};
	
	public static final Match<Territory> passableChannel(final GameData data, final PlayerID player)
	{
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory o)
			{
				final Set<CanalAttachment> canalAttachments = CanalAttachment.get(o);
				if (canalAttachments.isEmpty())
					return true;
				final Iterator<CanalAttachment> iter = canalAttachments.iterator();
				while (iter.hasNext())
				{
					final CanalAttachment canalAttachment = iter.next();
					if (!Match.allMatch(canalAttachment.getLandTerritories(), Matches.isTerritoryAllied(player, data)))
						return false;
				}
				return true;
			}
		};
	}
}
