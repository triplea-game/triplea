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

/*
 * MoveValidator.java
 * 
 * Created on November 9, 2001, 4:05 PM
 * 
 * @version $LastChangedDate$
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitCollection;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.CanalAttachment;
import games.strategy.triplea.attatchments.RulesAttachment;
import games.strategy.triplea.attatchments.TechAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.dataObjects.MoveValidationResult;
import games.strategy.triplea.delegate.dataObjects.MustMoveWithDetails;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.ui.MovePanel;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.IntegerMap;
import games.strategy.util.InverseMatch;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author Sean Bridges
 * 
 *         Provides some static methods for validating movement.
 */
public class MoveValidator
{
	
	public static final String TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_IN_A_PREVIOUS_PHASE = "Transport has already unloaded units in a previous phase";
	public static final String TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_TO = "Transport has already unloaded units to ";
	public static final String CANNOT_LOAD_AND_UNLOAD_AN_ALLIED_TRANSPORT_IN_THE_SAME_ROUND = "Cannot load and unload an allied transport in the same round";
	public static final String CANT_MOVE_THROUGH_IMPASSIBLE = "Can't move through impassible territories";
	public static final String CANT_MOVE_THROUGH_RESTRICTED = "Can't move through restricted territories";
	public static final String TOO_POOR_TO_VIOLATE_NEUTRALITY = "Not enough money to pay for violating neutrality";
	public static final String CANNOT_VIOLATE_NEUTRALITY = "Cannot violate neutrality";
	public static final String NOT_ALL_AIR_UNITS_CAN_LAND = "Not all air units can land";
	public static final String TRANSPORT_CANNOT_LOAD_AND_UNLOAD_AFTER_COMBAT = "Transport cannot both load AND unload after being in combat";
	
	/**
	 * Tests the given collection of units to see if they have the movement necessary
	 * to move.
	 * @deprecated use: Match.allMatch(units, Matches.UnitHasEnoughMovementForRoute(route));
	 * @arg alreadyMoved maps Unit -> movement
	 */
	
	@Deprecated
	public static boolean hasEnoughMovement(Collection<Unit> units, Route route)
	{
		
		return Match.allMatch(units, Matches.UnitHasEnoughMovementForRoute(route));
		}
	
	/**
	 * @param route
	 */
	private static int getMechanizedSupportAvail(Route route, Collection<Unit> units, PlayerID player)
	{
		int mechanizedSupportAvailable = 0;
		
		if (isMechanizedInfantry(player))
		{
			CompositeMatch<Unit> transportLand = new CompositeMatchAnd<Unit>(Matches.UnitIsLandTransport, Matches.unitIsOwnedBy(player));
			mechanizedSupportAvailable = Match.countMatches(units, transportLand);
		}
		return mechanizedSupportAvailable;
	}
	
	/**
	 * 
	 * @param targets
	 * @param data
	 * @return
	 */
	public static Map<Unit, Collection<Unit>> getDependents(Collection<Unit> units, GameData data)
	{
		// just worry about transports
		TransportTracker tracker = new TransportTracker();
		
		Map<Unit, Collection<Unit>> dependents = new HashMap<Unit, Collection<Unit>>();
		Iterator<Unit> iter = units.iterator();
		while (iter.hasNext())
		{
			Unit unit = iter.next();
			dependents.put(unit, tracker.transporting(unit));
		}
		return dependents;
	}
	

	/**
     * 
	 * @deprecated use Match.allMatch(units, Matches.UnitHasEnoughMovementForRoute(route));
	 * @param units
	 * @param length
	 * @return
     */
	@Deprecated
	public static boolean hasEnoughMovement(Collection<Unit> units, int length)
	{
		return Match.allMatch(units, Matches.UnitHasEnoughMovement(length));
		}
	
	/**
	 * Tests the given unit to see if it has the movement necessary
	 * to move.
	 * @deprecated use: Matches.UnitHasEnoughMovementForRoute(Route route).match(unit);
	 * @arg alreadyMoved maps Unit -> movement
	 */
	@Deprecated
	public static boolean hasEnoughMovement(Unit unit, Route route)
	{
		return Matches.UnitHasEnoughMovementForRoute(route).match(unit);
		}
		
	/**
	 * @deprecated use: Matches.UnitHasEnoughMovementForRoute(Route route).match(unit);
	*/
	@Deprecated
	public static boolean hasEnoughMovement(Unit unit, int length)
	{
		return Matches.UnitHasEnoughMovement(length).match(unit);
	}
	
	/**
	 * Checks that there are no enemy units on the route except possibly at the end.
	 * Submerged enemy units are not considered as they don't affect
	 * movement.
	 * AA and factory dont count as enemy.
	 */
	public static boolean onlyAlliedUnitsOnPath(Route route, PlayerID player, GameData data)
	{
		CompositeMatch<Unit> alliedOrNonCombat = new CompositeMatchOr<Unit>(Matches.UnitIsAAOrIsFactoryOrIsInfrastructure, Matches.alliedUnit(player, data));
		
		// Submerged units do not interfere with movement
		// only relevant for WW2V2
		alliedOrNonCombat.add(Matches.unitIsSubmerged(data));
		
		for(Territory current:route.getMiddleSteps()) {
			if (!current.getUnits().allMatch(alliedOrNonCombat))
				return false;
		}
		return true;
	}
	
	/**
	 * Checks that there only transports, subs and/or allies on the route except at the end.
	 * AA and factory dont count as enemy.
	 */
	public static boolean onlyIgnoredUnitsOnPath(Route route, PlayerID player, GameData data, boolean ignoreRouteEnd)
	{
		CompositeMatch<Unit> subOnly = new CompositeMatchOr<Unit>(Matches.UnitIsAAOrIsFactoryOrIsInfrastructure, Matches.UnitIsSub, Matches.alliedUnit(player, data));
		CompositeMatch<Unit> transportOnly = new CompositeMatchOr<Unit>(Matches.UnitIsAAOrIsFactoryOrIsInfrastructure, Matches.UnitIsTransportButNotCombatTransport, Matches.UnitIsLand,
					Matches.alliedUnit(player, data));
		CompositeMatch<Unit> transportOrSubOnly = new CompositeMatchOr<Unit>(Matches.UnitIsAAOrIsFactoryOrIsInfrastructure, Matches.UnitIsTransportButNotCombatTransport, Matches.UnitIsLand,
					Matches.UnitIsSub, Matches.alliedUnit(player, data));
		boolean getIgnoreTransportInMovement = isIgnoreTransportInMovement(data);
		boolean getIgnoreSubInMovement = isIgnoreSubInMovement(data);
		boolean validMove = false;
		List<Territory> steps;
		if (ignoreRouteEnd)
		{
			steps = route.getMiddleSteps();
		} else {
			steps = route.getSteps();
		}
		for (Territory current:steps)
		{
			if (current.isWater())
			{
				if (getIgnoreTransportInMovement && getIgnoreSubInMovement && current.getUnits().allMatch(transportOrSubOnly))
				{
					validMove = true;
					continue;
				}
				if (getIgnoreTransportInMovement && !getIgnoreSubInMovement && current.getUnits().allMatch(transportOnly))
				{
					validMove = true;
					continue;
				}
				if (!getIgnoreTransportInMovement && getIgnoreSubInMovement && current.getUnits().allMatch(subOnly))
				{
					validMove = true;
					continue;
				}
				
				return false;
			}
		}
		return validMove;
	}
	
	public static boolean enemyDestroyerOnPath(Route route, PlayerID player, GameData data)
	{
		Match<Unit> enemyDestroyer = new CompositeMatchAnd<Unit>(Matches.UnitIsDestroyer, Matches.enemyUnit(player, data));
		for (Territory current:route.getMiddleSteps()) {
			if (current.getUnits().someMatch(enemyDestroyer))
				return true;
		}
		return false;
	}
	
	private static boolean getEditMode(GameData data)
	{
		return EditDelegate.getEditMode(data);
	}
	
	public static boolean hasConqueredNonBlitzedOnRoute(Route route, GameData data)
	{
		for(Territory current:route.getMiddleSteps()) {
			if (MoveDelegate.getBattleTracker(data).wasConquered(current)
						&& !MoveDelegate.getBattleTracker(data).wasBlitzed(current))
				return true;
		}
		return false;
		
	}
	
	public static boolean isBlitzable(Territory current, GameData data, PlayerID player)
	{
		if (current.isWater())
			return false;
		
		// cant blitz on neutrals
		if (current.getOwner().isNull() && !isNeutralsBlitzable(data))
			return false;
		
		if (MoveDelegate.getBattleTracker(data).wasConquered(current)
					&& !MoveDelegate.getBattleTracker(data).wasBlitzed(current))
			return false;
		
		CompositeMatch<Unit> blitzableUnits = new CompositeMatchOr<Unit>();
		blitzableUnits.add(Matches.alliedUnit(player, data));
		// WW2V2, cant blitz through factories and aa guns
		// WW2V1 you can
		if (!isWW2V2(data) && !IsBlitzThroughFactoriesAndAARestricted(data))
		{
			blitzableUnits.add(Matches.UnitIsAAOrIsFactoryOrIsInfrastructure);
		}
		
		if (!current.getUnits().allMatch(blitzableUnits))
			return false;
		
		return true;
	}
	
	private static boolean isMechanizedInfantry(PlayerID player)
	{
		TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
		if (ta == null)
			return false;
		return ta.hasMechanizedInfantry();
	}
	
	private static boolean isParatroopers(PlayerID player)
	{
		TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
		if (ta == null)
			return false;
		return ta.hasParatroopers();
	}
	
	/**
	 * @deprecated use: route.isUnload();
	 */
	@Deprecated
	public static boolean isUnload(Route route)
	{
		return route.isUnload();
	}
	
	
	/**
	 * @deprecated use: route.isLoad();
	 * @param route
	 * @return
	 */
	@Deprecated
	public static boolean isLoad(Route route)
	{
		return route.isLoad();
	}
	
	// TODO KEV revise these to include paratroop load/unload
	public static boolean isLoad(Collection<Unit> units, Route route, GameData data, PlayerID player)
	{
		Map<Unit, Collection<Unit>> alreadyLoaded = mustMoveWith(units, route.getStart(), data, player);
		
		if (route.hasNoSteps() && alreadyLoaded.isEmpty())
			return false;
		
		// See if we even need to go to the trouble of checking for AirTransported units
		boolean checkForAlreadyTransported = !route.getStart().isWater() && hasWater(route);
		if (checkForAlreadyTransported)
		{
			// TODO Leaving UnitIsTransport for potential use with amphib transports (hovercraft, ducks, etc...)
			List<Unit> transports = Match.getMatches(units, new CompositeMatchOr<Unit>(Matches.UnitIsTransport, Matches.UnitIsAirTransport));
			List<Unit> transportable = Match.getMatches(units, new CompositeMatchOr<Unit>(Matches.UnitCanBeTransported, Matches.UnitIsAirTransportable));
			
			// Check if there are transports in the group to be checked
			if (alreadyLoaded.keySet().containsAll(transports))
			{
				// Check each transportable unit -vs those already loaded.
				for (Unit unit : transportable)
				{
					boolean found = false;
					for (Unit transport : transports)
					{
						if (alreadyLoaded.get(transport) == null || alreadyLoaded.get(transport).contains(unit))
						{
							found = true;
							break;
						}
					}
					if (!found)
						return checkForAlreadyTransported;
				}
			}
			// TODO I think this is right
			else
				return checkForAlreadyTransported;
		}
		
		return false;
	}
	
	
	/**
	 * @deprecated use route.hasNeutralBeforeEnd()
	 * @param route
	 * @return
	 */
	@Deprecated
	public static boolean hasNeutralBeforeEnd(Route route)
	{
		return route.hasNeutralBeforeEnd();
		}
	
	public static int getTransportCost(Collection<Unit> units)
	{
		if (units == null)
			return 0;
		
		int cost = 0;
		Iterator<Unit> iter = units.iterator();
		while (iter.hasNext())
		{
			Unit item = iter.next();
			cost += UnitAttachment.get(item.getType()).getTransportCost();
		}
		return cost;
	}
	
	public static boolean validLoad(Collection<Unit> units, Collection<Unit> transports)
	{
		
		return true;
	}
	
	public static Collection<Unit> getUnitsThatCantGoOnWater(Collection<Unit> units)
	{
		Collection<Unit> retUnits = new ArrayList<Unit>();
		for (Unit unit : units)
		{
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			if (!ua.isSea() && !ua.isAir() && ua.getTransportCost() == -1)
				retUnits.add(unit);
		}
		return retUnits;
	}
	
	public static boolean hasUnitsThatCantGoOnWater(Collection<Unit> units)
	{
		return !getUnitsThatCantGoOnWater(units).isEmpty();
	}
	
	public static int carrierCapacity(Collection<Unit> units, Territory territory)
	{
		int sum = 0;
		Iterator<Unit> iter = units.iterator();
		while (iter.hasNext())
		{
			Unit unit = iter.next();
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			if (ua.getCarrierCapacity() != -1)
			{
				// here we check to see if the unit can no longer carry units
				if (Matches.UnitHasWhenCombatDamagedEffect(UnitAttachment.UNITSMAYNOTLANDONCARRIER).match(unit))
				{
					// and we must check to make sure we let any allied air that are cargo stay here
					if (Matches.UnitHasWhenCombatDamagedEffect(UnitAttachment.UNITSMAYNOTLEAVEALLIEDCARRIER).match(unit))
					{
						int countCargo = 0;
						Collection<Unit> airCargo = territory.getUnits().getMatches(new CompositeMatchAnd<Unit>(Matches.UnitIsAir, Matches.UnitCanLandOnCarrier));
						for (Unit airUnit : airCargo)
						{
							TripleAUnit taUnit = (TripleAUnit) airUnit;
							if (taUnit.getTransportedBy() != null && taUnit.getTransportedBy().equals(unit))
								countCargo++;
						}
						sum += countCargo; // capacity = are cargo only
					}
					else
						continue; // capacity = zero 0
				}
				else
					sum += ua.getCarrierCapacity();
			}
		}
		return sum;
	}
	
	public static int carrierCost(Collection<Unit> units)
	{
		int sum = 0;
		Iterator<Unit> iter = units.iterator();
		while (iter.hasNext())
		{
			Unit unit = iter.next();
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			if (ua.getCarrierCost() != -1)
				sum += ua.getCarrierCost();
		}
		return sum;
	}
	/**
	 * @deprecated use route.hasWater();
	 * @param route
	 * @return
	 */
	@Deprecated
	public static boolean hasWater(Route route)
	{
		return route.hasWater();
	}
	
	/**
	 * @deprecated use route.hasLand();
	 * @param route
	 * @return
	 */
	@Deprecated
	public static boolean hasLand(Route route)
	{
		return route.hasLand();
		}
	
	/**
	 * Returns true if the given air units can land in the
	 * given territory.
	 * Does not take into account whether a battle has been
	 * fought in the territory already.
	 * 
	 * Note units must only be air units
	 */
	public static boolean canLand(Collection<Unit> airUnits, Territory territory, PlayerID player, GameData data)
	{
		if (!Match.allMatch(airUnits, Matches.UnitIsAir))
			throw new IllegalArgumentException("can only test if air will land");
		
		if (!territory.isWater()
					&& MoveDelegate.getBattleTracker(data).wasConquered(territory))
			return false;
		
		if (getScramble_Rules_In_Effect(data))
		{
			if (Match.someMatch(airUnits, Matches.UnitWasScrambled))
				return false;
		}
		
		if (territory.isWater())
		{
			// if they cant all land on carriers
			if (!Match.allMatch(airUnits, Matches.UnitCanLandOnCarrier))
				return false;
			
			// when doing the calculation, make sure to include the units
			// in the territory
			Set<Unit> friendly = new HashSet<Unit>();
			friendly.addAll(getFriendly(territory, player, data));
			friendly.addAll(airUnits);
			
			// make sure we have the carrier capacity
			int capacity = carrierCapacity(friendly, territory);
			int cost = carrierCost(friendly);
			return capacity >= cost;
		}
		else
		{
			return isFriendly(player, territory.getOwner(), data);
		}
	}
	
	public static Collection<Unit> getNonLand(Collection<Unit> units)
	{
		CompositeMatch<Unit> match = new CompositeMatchOr<Unit>();
		match.add(Matches.UnitIsAir);
		match.add(Matches.UnitIsSea);
		return Match.getMatches(units, match);
	}
	
	public static Collection<Unit> getFriendly(Territory territory, PlayerID player, GameData data)
	{
		return territory.getUnits().getMatches(Matches.alliedUnit(player, data));
	}
	
	public static boolean isFriendly(PlayerID p1, PlayerID p2, GameData data)
	{
		if (p1.equals(p2))
			return true;
		else
			return data.getRelationshipTracker().isAllied(p1, p2);
	}
	
	public static boolean ownedByFriendly(Unit unit, PlayerID player, GameData data)
	{
		PlayerID owner = unit.getOwner();
		return (isFriendly(owner, player, data));
	}
	
	public static int getMaxMovement(Collection<Unit> units)
	{
		if (units.size() == 0)
			throw new IllegalArgumentException("no units");
		int max = 0;
		Iterator<Unit> iter = units.iterator();
		while (iter.hasNext())
		{
			Unit unit = iter.next();
			int left = TripleAUnit.get(unit).getMovementLeft();
			max = Math.max(left, max);
		}
		return max;
	}
	
	public static int getLeastMovement(Collection<Unit> units)
	{
		if (units.size() == 0)
			throw new IllegalArgumentException("no units");
		int least = Integer.MAX_VALUE;
		Iterator<Unit> iter = units.iterator();
		while (iter.hasNext())
		{
			Unit unit = iter.next();
			int left = TripleAUnit.get(unit).getMovementLeft();
			least = Math.min(left, least);
		}
		return least;
	}
	
	public static int getTransportCapacityFree(Territory territory, PlayerID id, GameData data, TransportTracker tracker)
	{
		Match<Unit> friendlyTransports = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport,
															Matches.alliedUnit(id, data));
		Collection<Unit> transports = territory.getUnits().getMatches(friendlyTransports);
		int sum = 0;
		Iterator<Unit> iter = transports.iterator();
		while (iter.hasNext())
		{
			Unit transport = iter.next();
			sum += tracker.getAvailableCapacity(transport);
		}
		return sum;
	}
	
	public static boolean hasSomeLand(Collection<Unit> units)
	{
		Match<Unit> notAirOrSea = new CompositeMatchAnd<Unit>(Matches.UnitIsNotAir, Matches.UnitIsNotSea);
		return Match.someMatch(units, notAirOrSea);
	}
	
	private static boolean isWW2V2(GameData data)
	{
		return games.strategy.triplea.Properties.getWW2V2(data);
	}
	
	private static boolean isNeutralsImpassable(GameData data)
	{
		return games.strategy.triplea.Properties.getNeutralsImpassable(data);
	}
	
	private static boolean isNeutralsBlitzable(GameData data)
	{
		return games.strategy.triplea.Properties.getNeutralsBlitzable(data);
	}
	
	private static boolean isWW2V3(GameData data)
	{
		return games.strategy.triplea.Properties.getWW2V3(data);
	}
	
	private static boolean isMultipleAAPerTerritory(GameData data)
	{
		return games.strategy.triplea.Properties.getMultipleAAPerTerritory(data);
	}
	
	/**
	 * @return
	 */
	private static boolean isMovementByTerritoryRestricted(GameData data)
	{
		return games.strategy.triplea.Properties.getMovementByTerritoryRestricted(data);
	}
	
	private static boolean getScramble_Rules_In_Effect(GameData data)
	{
		return games.strategy.triplea.Properties.getScramble_Rules_In_Effect(data);
	}
	
	private static boolean getScrambled_Units_Return_To_Base(GameData data)
	{
		return games.strategy.triplea.Properties.getScrambled_Units_Return_To_Base(data);
	}
	
	private static boolean IsBlitzThroughFactoriesAndAARestricted(GameData data)
	{
		return games.strategy.triplea.Properties.getBlitzThroughFactoriesAndAARestricted(data);
	}
	
	private static boolean IsParatroopersCanMoveDuringNonCombat(GameData data)
	{
		return games.strategy.triplea.Properties.getParatroopersCanMoveDuringNonCombat(data);
	}
	
	private static int getNeutralCharge(GameData data, Route route)
	{
		return getNeutralCharge(data, MoveDelegate.getEmptyNeutral(route).size());
	}
	
	private static int getNeutralCharge(GameData data, int numberOfTerritories)
	{
		return numberOfTerritories * games.strategy.triplea.Properties.getNeutralCharge(data);
	}
	
	public static MoveValidationResult validateMove(Collection<Unit> units,
													Route route,
													PlayerID player,
													Collection<Unit> transportsToLoad,
													boolean isNonCombat,
													final List<UndoableMove> undoableMoves,
													GameData data)
	{
		MoveValidationResult result = new MoveValidationResult();
		
		if(route.hasNoSteps())
			return result;
		
		if (!units.isEmpty()
					&& !Match.allMatch(Match.getMatches(units, Matches.unitIsBeingTransportedByOrIsDependentOfSomeUnitInThisList(units, route, player, data).invert()), Matches.unitIsOwnedBy(player))
					&& !getEditMode(data))
		{
			result.setError("Player, " + player.getName() + ", is not owner of all the units: " + MyFormatter.unitsToTextNoOwner(units));
			return result;
		}
		
		// this should never happen
		if (new HashSet<Unit>(units).size() != units.size())
		{
			result.setError("Not all units unique, units:" + units + " unique:" + new HashSet<Unit>(units));
			return result;
		}
		
		if (!data.getMap().isValidRoute(route))
		{
			result.setError("Invalid route:" + route);
			return result;
		}
		
		if (validateMovementRestrictedByTerritory(data, units, route, player, result).getError() != null)
		{
			return result;
		}
		
		// can not enter territories owned by a player to which we are neutral towards
		Collection<Territory> landOnRoute = route.getMatches(Matches.TerritoryIsLand);
		if (!landOnRoute.isEmpty())
		{
			for (Territory t : landOnRoute)
			{
				if (Match.someMatch(units, Matches.UnitIsLand))
				{
					if (!data.getRelationshipTracker().canMoveLandUnitsOverOwnedLand(player, t.getOwner()))
					{
						result.setError(player.getName() + " may not move land units over land owned by " + t.getOwner().getName());
						return result;
					}
				}
				if (Match.someMatch(units, Matches.UnitIsAir))
				{
					if (!data.getRelationshipTracker().canMoveAirUnitsOverOwnedLand(player, t.getOwner()))
					{
						result.setError(player.getName() + " may not move air units over land owned by " + t.getOwner().getName());
						return result;
					}
				}
			}
		}
		
		if (isNonCombat)
		{
			if (validateNonCombat(data, units, route, player, result).getError() != null)
				return result;
			
			if (validateReturnScrambledUnits(data, units, route, player, result).getError() != null)
				return result;
		}
		else
		{
			if (validateCombat(data, units, route, player, result).getError() != null)
				return result;
			
		}
		
		if (validateNonEnemyUnitsOnPath(data, units, route, player, result).getError() != null)
			return result;
		
		if (validateBasic(isNonCombat, data, units, route, player, transportsToLoad, result).getError() != null)
			return result;
		
		if (validateAirCanLand(data, units, route, player, result).getError() != null)
			return result;
		
		if (validateTransport(data, undoableMoves, units, route, player, transportsToLoad, result).getError() != null)
			return result;
		
		if (validateParatroops(isNonCombat, data, undoableMoves, units, route, player, result).getError() != null)
			return result;
		
		if (validateCanal(data, units, route, player, result).getError() != null)
			return result;
		
		// dont let the user move out of a battle zone
		// the exception is air units and unloading units into a battle zone
		if (MoveDelegate.getBattleTracker(data).hasPendingBattle(route.getStart(), false)
					&& Match.someMatch(units, Matches.UnitIsNotAir))
		{
			// if the units did not move into the territory, then they can move out
			// this will happen if there is a submerged sub in the area, and
			// a different unit moved into the sea zone setting up a battle
			// but the original unit can still remain
			boolean unitsStartedInTerritory = true;
			for (Unit unit : units)
			{
				if (MoveDelegate.getRouteUsedToMoveInto(undoableMoves, unit, route.getEnd()) != null)
				{
					unitsStartedInTerritory = false;
					break;
				}
			}
			
			if (!unitsStartedInTerritory)
			{
				
				boolean unload = MoveValidator.isUnload(route);
				PlayerID endOwner = route.getEnd().getOwner();
				boolean attack = !data.getRelationshipTracker().isAllied(endOwner, player)
								|| MoveDelegate.getBattleTracker(data).wasConquered(route.getEnd());
				// unless they are unloading into another battle
				if (!(unload && attack))
					return result.setErrorReturnResult("Cannot move units out of battle zone");
			}
		}
		
		return result;
	}
	
	private static MoveValidationResult validateCanal(GameData data, Collection<Unit> units, Route route, PlayerID player, MoveValidationResult result)
	{
		if (getEditMode(data))
			return result;
		
		// if no sea units then we can move
		if (Match.noneMatch(units, Matches.UnitIsSea))
			return result;
		
		// TODO: merge validateCanal here and provide granular unit warnings
		return result.setErrorReturnResult(validateCanal(route, player, data));
	}
	
	private static MoveValidationResult validateCombat(GameData data, Collection<Unit> units, Route route, PlayerID player, MoveValidationResult result)
	{
		if (getEditMode(data))
			return result;
		
		if (!route.getStart().isWater() && Matches.isAtWar(route.getStart().getOwner()).match(player) && Matches.isAtWar(route.getEnd().getOwner()).match(player))
		{
			if (!MoveValidator.isBlitzable(route.getStart(), data, player) && !Match.allMatch(units, Matches.UnitIsAir))
				return result.setErrorReturnResult("Can not blitz out of a battle into enemy territory");
			for (Unit u : Match.getMatches(units, new CompositeMatchAnd<Unit>(Matches.UnitCanBlitz.invert(), Matches.UnitIsNotAir)))
			{
				result.addDisallowedUnit("Not all units can blitz out of empty enemy territory", u);
			}
		}
		
		// Don't allow aa guns to move in combat unless they are in a transport
		if (Match.someMatch(units, Matches.UnitIsAAorIsAAmovement) && (!route.getStart().isWater() || !route.getEnd().isWater()))
		{
			for (Unit unit : Match.getMatches(units, Matches.UnitIsAAorIsAAmovement))
				result.addDisallowedUnit("Cannot move AA guns in combat movement phase", unit);
			
			return result;
		}
		
		// if there is a neutral in the middle must stop unless all are air or getNeutralsBlitzable
		if (MoveValidator.hasNeutralBeforeEnd(route))
		{
			if (!Match.allMatch(units, Matches.UnitIsAir) && !isNeutralsBlitzable(data))
				return result.setErrorReturnResult("Must stop land units when passing through neutral territories");
		}
		
		if (Match.someMatch(units, Matches.UnitIsLand) && route.hasSteps())
		{
			// check all the territories but the end, if there are enemy territories, make sure they are blitzable
			// if they are not blitzable, or we aren't all blitz units fail
			int enemyCount = 0;
			boolean allEnemyBlitzable = true;
			
			for(Territory current:route.getMiddleSteps()) {
				
				if (current.isWater())
					continue;
				
				if (!data.getRelationshipTracker().isAllied(current.getOwner(), player)
							|| MoveDelegate.getBattleTracker(data).wasConquered(current))
				{
					enemyCount++;
					allEnemyBlitzable &= MoveValidator.isBlitzable(current, data, player);
				}
			}
			
			if (enemyCount > 0 && !allEnemyBlitzable)
			{
				if (nonParatroopersPresent(player, units))
					return result.setErrorReturnResult("Cannot blitz on that route");
				
			}
			else if (enemyCount >= 0 && allEnemyBlitzable && !(route.getStart().isWater() || route.getEnd().isWater()))
			{
				Match<Unit> blitzingUnit = new CompositeMatchOr<Unit>(Matches.UnitCanBlitz, Matches.UnitIsAir);
				Match<Unit> nonBlitzing = new InverseMatch<Unit>(blitzingUnit);
				Collection<Unit> nonBlitzingUnits = Match.getMatches(units, nonBlitzing);
				
				// remove any units that gain blitz due to certain abilities
				nonBlitzingUnits.removeAll(UnitAttachment.getUnitsWhichReceivesAbilityWhenWith(units, "canBlitz", data));
				
				Match<Territory> territoryIsNotEnd = new InverseMatch<Territory>(Matches.territoryIs(route.getEnd()));
				Match<Territory> nonFriendlyTerritories = new InverseMatch<Territory>(Matches.isTerritoryFriendly(player, data));
				Match<Territory> notEndOrFriendlyTerrs = new CompositeMatchAnd<Territory>(nonFriendlyTerritories, territoryIsNotEnd);
				
				Match<Territory> foughtOver = Matches.territoryWasFoughOver(MoveDelegate.getBattleTracker(data));
				Match<Territory> notEndWasFought = new CompositeMatchAnd<Territory>(territoryIsNotEnd, foughtOver);
				
				Boolean wasStartFoughtOver = MoveDelegate.getBattleTracker(data).wasConquered(route.getStart())
							|| MoveDelegate.getBattleTracker(data).wasBlitzed(route.getStart());
				
				for (Unit unit : nonBlitzingUnits)
				{
					if (Matches.UnitIsAirTransportable.match(unit))
						continue;
					
					if (Matches.UnitIsInfantry.match(unit))
						continue;
					
					TripleAUnit tAUnit = (TripleAUnit) unit;
					if (wasStartFoughtOver || tAUnit.getWasInCombat() || route.someMatch(notEndOrFriendlyTerrs) || route.someMatch(notEndWasFought))
						result.addDisallowedUnit("Not all units can blitz", unit);
				}
			}
		}
		else
		{ // check aircraft
			if (Match.someMatch(units, Matches.UnitIsAir) && route.hasSteps() && (!games.strategy.triplea.Properties.getNeutralFlyoverAllowed(data) || isNeutralsImpassable(data)))
			{
				if (Match.someMatch(route.getMiddleSteps(), Matches.TerritoryIsNeutral))
					return result.setErrorReturnResult("Air units cannot fly over neutral territories");
			}
		}
		
		// make sure no conquered territories on route
		if (MoveValidator.hasConqueredNonBlitzedOnRoute(route, data))
		{
			// unless we are all air or we are in non combat OR the route is water (was a bug in convoy zone movement)
			if (!Match.allMatch(units, Matches.UnitIsAir) && !(route.allMatch(Matches.TerritoryIsWater)))
				return result.setErrorReturnResult("Cannot move through newly captured territories");
		}
		
		// See if they've already been in combat
		if (Match.someMatch(units, Matches.UnitWasInCombat) && Match.someMatch(units, Matches.UnitWasUnloadedThisTurn))
		{
			Collection<Territory> end = Collections.singleton(route.getEnd());
			
			if (Match.allMatch(end, Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data)) && !route.getEnd().getUnits().isEmpty())
				return result.setErrorReturnResult("Units cannot participate in multiple battles");
		}
		
		// See if we are doing invasions in combat phase, with units or transports that can't do invasion.
		if (MoveValidator.isUnload(route) && Matches.isTerritoryEnemy(player, data).match(route.getEnd()))
		{
			for (Unit unit : Match.getMatches(units, Matches.UnitCanInvade.invert()))
			{
				result.addDisallowedUnit(unit.getUnitType().getName() + " can't invade from " + TripleAUnit.get(unit).getTransportedBy().getUnitType().getName(), unit);
			}
		}
		
		return result;
	}
	
	
	private static MoveValidationResult validateNonCombat(GameData data, Collection<Unit> units, Route route, PlayerID player, MoveValidationResult result)
	{
		if (getEditMode(data))
			return result;
		
		if (route.someMatch(Matches.TerritoryIsImpassable))
			return result.setErrorReturnResult(CANT_MOVE_THROUGH_IMPASSIBLE);
		
		if (!route.someMatch(Matches.TerritoryIsPassableAndNotRestricted(player)))
			return result.setErrorReturnResult(CANT_MOVE_THROUGH_RESTRICTED);
		
		CompositeMatch<Territory> battle = new CompositeMatchOr<Territory>();
		battle.add(Matches.TerritoryIsNeutral);
		battle.add(Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data));
		CompositeMatch<Unit> transportsCanNotControl = new CompositeMatchAnd<Unit>();
		transportsCanNotControl.add(Matches.UnitIsTransportAndNotDestroyer);
		transportsCanNotControl.add(Matches.UnitIsTransportButNotCombatTransport);
		
		boolean navalMayNotNonComIntoControlled = isWW2V2(data) || games.strategy.triplea.Properties.getNavalUnitsMayNotNonCombatMoveIntoControlledSeaZones(data);
		
		// TODO need to account for subs AND transports that are ignored, not just OR
		if (battle.match(route.getEnd()))
		{
			// If subs and transports can't control sea zones, it's OK to move there
			if (!navalMayNotNonComIntoControlled && isSubControlSeaZoneRestricted(data) && Match.allMatch(units, Matches.UnitIsSub))
				return result;
			else if (!navalMayNotNonComIntoControlled && !isTransportControlSeaZone(data) && Match.allMatch(units, transportsCanNotControl))
				return result;
			else if (!navalMayNotNonComIntoControlled && route.allMatch(Matches.TerritoryIsWater) && MoveValidator.onlyAlliedUnitsOnPath(route, player, data)
						&& !Matches.territoryHasEnemyUnits(player, data).match(route.getEnd()))
				return result; // fixes a bug in convoy zone movement
			else
				return result.setErrorReturnResult("Cannot advance units to battle in non combat");
		}
		
		// Subs can't travel under DDs
		if (isSubmersibleSubsAllowed(data) && Match.allMatch(units, Matches.UnitIsSub))
		{
			// this is ok unless there are destroyer on the path
			if (MoveValidator.enemyDestroyerOnPath(route, player, data))
				return result.setErrorReturnResult("Cannot move submarines under destroyers");
		}
		
		if (route.getEnd().getUnits().someMatch(Matches.enemyUnit(player, data)))
		{
			if (onlyIgnoredUnitsOnPath(route, player, data, false))
				return result;
			
			CompositeMatch<Unit> friendlyOrSubmerged = new CompositeMatchOr<Unit>();
			friendlyOrSubmerged.add(Matches.alliedUnit(player, data));
			friendlyOrSubmerged.add(Matches.unitIsSubmerged(data));
			if (!route.getEnd().getUnits().allMatch(friendlyOrSubmerged))
				return result.setErrorReturnResult("Cannot advance to battle in non combat");
		}
		
		if (Match.allMatch(units, Matches.UnitIsAir))
		{
			if (route.someMatch(Matches.TerritoryIsNeutral) && (!games.strategy.triplea.Properties.getNeutralFlyoverAllowed(data) || isNeutralsImpassable(data)))
				return result.setErrorReturnResult("Air units cannot fly over neutral territories in non combat");
		}
		else
		{
			CompositeMatch<Territory> neutralOrEnemy = new CompositeMatchOr<Territory>(Matches.TerritoryIsNeutral, Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data));
			if (route.someMatch(neutralOrEnemy))
			{
				if (!(!navalMayNotNonComIntoControlled && route.allMatch(Matches.TerritoryIsWater) && MoveValidator.onlyAlliedUnitsOnPath(route, player, data) && !Matches.territoryHasEnemyUnits(
							player, data).match(route.getEnd())))
				{
					if (!route.allMatch(new CompositeMatchOr<Territory>(Matches.TerritoryIsWater, new CompositeMatchAnd<Territory>(Matches.TerritoryIsPassableAndNotRestricted(player), Matches
								.isTerritoryAllied(player, data), Matches.TerritoryIsLand)))
								|| nonParatroopersPresent(player, units) || !allLandUnitsAreBeingParatroopered(units, route, player))
						return result.setErrorReturnResult("Cannot move units to neutral or enemy territories in non combat");
				}
			}
		}
		return result;
	}
	
	private static MoveValidationResult validateReturnScrambledUnits(GameData data, Collection<Unit> units, Route route, PlayerID player, MoveValidationResult result)
	{
		if (!getScramble_Rules_In_Effect(data))
			return result;
		
		if (getScrambled_Units_Return_To_Base(data))
		{
			Collection<Territory> end = Collections.singleton(route.getEnd());
			for (Unit unit : units)
			{
				if (player == data.getSequence().getStep().getPlayerID())
					continue;
				
				TripleAUnit tAUnit = (TripleAUnit) unit;
				if (!tAUnit.getWasScrambled())
				{
					result.addDisallowedUnit("Only scrambled units may be moved", unit);
					continue;
				}
				// if end doesn't equal beginning and territory is still in friendly hands
				if (route.getEnd() != tAUnit.getOriginatedFrom() && !Match.allMatch(end, Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data)))
					result.addDisallowedUnit("Scrambled units must return to base", unit);
			}
		}
		
		return result;
	}
	
	// Added to handle restriction of movement to listed territories
	private static MoveValidationResult validateMovementRestrictedByTerritory(GameData data, Collection<Unit> units, Route route, PlayerID player, MoveValidationResult result)
	{
		if (getEditMode(data))
			return result;
		
		if (!isMovementByTerritoryRestricted(data))
			return result;
		
		RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
		if (ra == null || ra.getMovementRestrictionTerritories() == null)
			return result;
		
		String movementRestrictionType = ra.getMovementRestrictionType();
		Collection<Territory> listedTerritories = ra.getListedTerritories(ra.getMovementRestrictionTerritories());
		if (movementRestrictionType.equals("allowed"))
		{
			for(Territory current:route.getAllTerritories()) {
				if (!listedTerritories.contains(current))
					return result.setErrorReturnResult("Cannot move outside restricted territories");
			}
		}
		else if (movementRestrictionType.equals("disallowed"))
		{
			for(Territory current:route.getAllTerritories()) {
				if (listedTerritories.contains(current))
					return result.setErrorReturnResult("Cannot move to restricted territories");
			}
		}
		
		return result;
	}
	
	private static MoveValidationResult validateNonEnemyUnitsOnPath(GameData data, Collection<Unit> units, Route route, PlayerID player, MoveValidationResult result)
	{
		if (getEditMode(data))
			return result;
		
		// check to see no enemy units on path
		if (MoveValidator.onlyAlliedUnitsOnPath(route, player, data))
			return result;
		
		// if we are all air, then its ok
		if (Match.allMatch(units, Matches.UnitIsAir))
			return result;
		
		if (isSubmersibleSubsAllowed(data) && Match.allMatch(units, Matches.UnitIsSub))
		{
			// this is ok unless there are destroyer on the path
			if (MoveValidator.enemyDestroyerOnPath(route, player, data))
				return result.setErrorReturnResult("Cannot move submarines under destroyers");
			else
				return result;
		}
		
		if (onlyIgnoredUnitsOnPath(route, player, data, true))
			return result;
		
		// omit paratroops
		if (nonParatroopersPresent(player, units))
			return result.setErrorReturnResult("Enemy units on path");
		
		return result;
	}
	
	private static MoveValidationResult validateBasic(boolean isNonCombat, GameData data, Collection<Unit> units, Route route, PlayerID player, Collection<Unit> transportsToLoad,
				MoveValidationResult result)
	{
		boolean isEditMode = getEditMode(data);
		
		if (units.size() == 0)
			return result.setErrorReturnResult("No units");
		
		for (Unit unit : units)
		{
			if (TripleAUnit.get(unit).getSubmerged())
				result.addDisallowedUnit("Cannot move submerged units", unit);
		}
		
		// make sure all units are actually in the start territory
		if (!route.getStart().getUnits().containsAll(units))
			return result.setErrorReturnResult("Not enough units in starting territory");
		
		// make sure transports in the destination
		if (route.getEnd() != null && !route.getEnd().getUnits().containsAll(transportsToLoad) && !units.containsAll(transportsToLoad))
			return result.setErrorReturnResult("Transports not found in route end");
		
		if (!isEditMode)
		{
			// make sure all units are at least friendly
			for (Unit unit : Match.getMatches(units, Matches.enemyUnit(player, data)))
				result.addDisallowedUnit("Can only move friendly units", unit);
			
			// check we have enough movement
			// exclude transported units
			Collection<Unit> moveTest;
			if (route.getStart().isWater())
			{
				moveTest = MoveValidator.getNonLand(units);
			}
			else
			{
				moveTest = units;
			}
			
			for (Unit unit : Match.getMatches(moveTest, Matches.unitIsOwnedBy(player).invert()))
			{
				// allow allied fighters to move with carriers
				if (!(UnitAttachment.get(unit.getType()).getCarrierCost() > 0 && data.getRelationshipTracker().isAllied(player, unit.getOwner())))
				{
					result.addDisallowedUnit("Can only move own troops", unit);
				}
			}
			
			// Initialize available Mechanized Inf support
			int mechanizedSupportAvailable = getMechanizedSupportAvail(route, units, player);
			
			Map<Unit, Collection<Unit>> dependencies = getDependents(Match.getMatches(units, Matches.UnitCanTransport), data);
			// add those just added
			Map<Unit, Collection<Unit>> justLoaded = MovePanel.getDependents();
			if (!justLoaded.isEmpty())
			{
				for (Unit transport : dependencies.keySet())
				{
					if (dependencies.get(transport).isEmpty())
						dependencies.put(transport, justLoaded.get(transport));
				}
			}
			// check units individually
			for (Unit unit : moveTest)
			{
				if (!Matches.UnitHasEnoughMovementForRoute(route).match(unit))
				{
					boolean unitOK = false;
					if ((Matches.UnitIsAirTransportable.match(unit) && Matches.unitHasNotMoved.match(unit))
								&& (mechanizedSupportAvailable > 0 && Matches.unitHasNotMoved.match(unit) && Matches.UnitIsInfantry.match(unit)))
					{
						// we have paratroopers and mechanized infantry, so we must check for both
						// simple: if it movement group contains an air-transport, then assume we are doing paratroopers. else, assume we are doing mechanized
						if (Match.someMatch(units, Matches.UnitIsAirTransport))
						{
							for (Unit airTransport : dependencies.keySet())
							{
								if (dependencies.get(airTransport) == null || dependencies.get(airTransport).contains(unit))
								{
									unitOK = true;
									break;
								}
							}
							if (!unitOK)
								result.addDisallowedUnit("Not all units have enough movement", unit);
						}
						else
							mechanizedSupportAvailable--;
					}
					else if (Matches.UnitIsAirTransportable.match(unit) && Matches.unitHasNotMoved.match(unit))
					{
						for (Unit airTransport : dependencies.keySet())
						{
							if (dependencies.get(airTransport) == null || dependencies.get(airTransport).contains(unit))
							{
								unitOK = true;
								break;
							}
						}
						if (!unitOK)
							result.addDisallowedUnit("Not all units have enough movement", unit);
					}
					else if (mechanizedSupportAvailable > 0 && Matches.unitHasNotMoved.match(unit) && Matches.UnitIsInfantry.match(unit))
					{
						mechanizedSupportAvailable--;
					}
					else if (Matches.UnitTypeCanLandOnCarrier.match(unit.getType()) && isAlliedAirDependents(data) && Match.someMatch(moveTest, Matches.UnitIsAlliedCarrier(unit.getOwner(), data)))
					{
						continue;
					}
					else
					{
						result.addDisallowedUnit("Not all units have enough movement", unit);
					}
				}
			}
			
			// if there is a neutral in the middle must stop unless all are air or getNeutralsBlitzable
			if (route.hasNeutralBeforeEnd())
			{
				if (!Match.allMatch(units, Matches.UnitIsAir) && !isNeutralsBlitzable(data))
					return result.setErrorReturnResult("Must stop land units when passing through neutral territories");
			}
			
		} // !isEditMode
		
		// make sure that no non sea non transportable no carriable units
		// end at sea
		if (route.getEnd() != null && route.getEnd().isWater())
		{
			for (Unit unit : MoveValidator.getUnitsThatCantGoOnWater(units))
				result.addDisallowedUnit("Not all units can end at water", unit);
		}
		
		// if we are water make sure no land
		if (Match.someMatch(units, Matches.UnitIsSea))
		{
			if (route.hasLand())
				for (Unit unit : Match.getMatches(units, Matches.UnitIsSea))
					result.addDisallowedUnit("Sea units cannot go on land", unit);
		}
		
		// make sure that we dont send aa guns to attack
		if (Match.someMatch(units, Matches.UnitIsAAorIsAAmovement))
		{
			// TODO dont move if some were conquered
			
			for(Territory current:route.getSteps()) {
				if (!(current.isWater() || current.getOwner().equals(player) || data.getRelationshipTracker().isAllied(player, current.getOwner())))
				{
					for (Unit unit : Match.getMatches(units, Matches.UnitIsAAorIsAAmovement))
						result.addDisallowedUnit("AA units cannot advance to battle", unit);
					
					break;
				}
			}
		}
		
		// only allow one aa into a land territory unless WW2V2 or WW2V3 or isMultipleAAPerTerritory.
		// if ((!isWW2V3(data) && !isWW2V2(data)) && Match.someMatch(units, Matches.UnitIsAAorIsAAmovement) && route.getEnd() != null && route.getEnd().getUnits().someMatch(Matches.UnitIsAAorIsAAmovement)
		if ((!isMultipleAAPerTerritory(data) && !isWW2V3(data) && !isWW2V2(data)) && Match.someMatch(units, Matches.UnitIsAAorIsAAmovement) && route.getEnd() != null
					&& route.getEnd().getUnits().someMatch(Matches.UnitIsAAorIsAAmovement)
					&& !route.getEnd().isWater())
		{
			for (Unit unit : Match.getMatches(units, Matches.UnitIsAAorIsAAmovement))
				result.addDisallowedUnit("Only one AA gun allowed in a territory", unit);
		}
		
		// only allow 1 aa to unload unless WW2V2 or WW2V3.
		if (route.getStart().isWater() && !route.getEnd().isWater() && Match.countMatches(units, Matches.UnitIsAAorIsAAmovement) > 1 && (!isWW2V3(data) && !isWW2V2(data)))
		{
			Collection<Unit> aaGuns = Match.getMatches(units, Matches.UnitIsAAorIsAAmovement);
			Iterator<Unit> aaIter = aaGuns.iterator();
			aaIter.next(); // skip first unit
			for (; aaIter.hasNext();)
				result.addUnresolvedUnit("Only one AA gun can unload in a territory", aaIter.next());
		}
		
		// don't allow move through impassible territories
		if (!isEditMode && route.someMatch(Matches.TerritoryIsImpassable))
			return result.setErrorReturnResult(CANT_MOVE_THROUGH_IMPASSIBLE);
		
		if (canCrossNeutralTerritory(data, route, player, result).getError() != null)
			return result;
		
		if (isNeutralsImpassable(data) && !isNeutralsBlitzable(data) && !route.getMatches(Matches.TerritoryIsNeutral).isEmpty())
			return result.setErrorReturnResult(CANNOT_VIOLATE_NEUTRALITY);
		
		return result;
	}
	
	private static MoveValidationResult validateAirCanLand(final GameData data, Collection<Unit> units, Route route, PlayerID player, MoveValidationResult result)
	{
		// First check if we even need to check
		if (	getEditMode(data) || 														// Edit Mode, no need to check
				!Match.someMatch(units, Matches.UnitIsAir) 	|| 								// No Airunits, nothing to check
				route.hasNoSteps()	||														// if there are no steps, we didn't move, so it is always OK! 
				Matches.alliedNonConqueredNonPendingTerritory(data, player).match(route.getEnd())   // we can land at the end, nothing left to check
			)
			return result;
		
		
		//Find which aircraft cannot find friendly land to land on
		Collection<Unit> airThatMustLandOnCarriers = getAirThatMustLandOnCarriers(data, getAirUnitsToValidate(units, route, player) , route, result, player);
		
		if (airThatMustLandOnCarriers.isEmpty())
			return result;		// we are done, everything can find a place to land
		
		/*
		 * Here's where we see if we have carriers available to land.
		 */
		IntegerMap<Territory> usedCarrierSpace = new IntegerMap<Territory>(); // this map of territories tracks how much carrierspace in each territory we already used up.
		Set<Unit> movedCarriers = new HashSet<Unit>(); // this set of units tracks which carriers are already marked as moved to catch fighters in the air.
		for(Unit unit:airThatMustLandOnCarriers) {
			if(!findCarrierToLand(data,player,unit,route,usedCarrierSpace, movedCarriers) && !isKamikazeAircraft(data) && !Matches.UnitIsKamikaze.match(unit)) {
				result.addDisallowedUnit(NOT_ALL_AIR_UNITS_CAN_LAND, unit);
			}
		}
			return result;
	}
		
	/**
	 * @param units the units flying this route
	 * @param route the route flown
	 * @param player the player owning the units
	 * @return the combination of units that fly here and the existing owned units
	 */
	private static Collection<Unit> getAirUnitsToValidate(Collection<Unit> units, Route route, PlayerID player) {
		Match<Unit> ownedAirMatch = new CompositeMatchAnd<Unit>(Matches.UnitIsAir, Matches.unitOwnedBy(player));
		ArrayList<Unit> ownedAir = new ArrayList<Unit>();
		ownedAir.addAll(Match.getMatches(units, ownedAirMatch));
		ownedAir.addAll(Match.getMatches(route.getEnd().getUnits().getUnits(), ownedAirMatch));
		//sort the list by shortest range first so those birds will get first pick of landingspots
		Collections.sort(ownedAir, UnitComparator.getIncreasingMovementComparator());
		return ownedAir;
	}
		
		
private static boolean findCarrierToLand(GameData data,PlayerID player, Unit unit, Route route, IntegerMap<Territory> usedCarrierSpace, Set<Unit> movedCarriers) {
		final Territory currentSpot = route.getEnd();
		if(unit.getTerritoryUnitIsIn().equals(currentSpot))
			route = new Route(currentSpot);
		final int movementLeft = route.getMovementLeft(unit);
		final UnitAttachment ua = UnitAttachment.get(unit.getType());
		
		
		if(movementLeft<0)
			return false;
		
		boolean landAirOnNewCarriers = AirThatCantLandUtil.isLHTRCarrierProduction(data) || AirThatCantLandUtil.isLandExistingFightersOnNewCarriers(data);
		List<Unit> carriersInProductionQueue = player.getUnits().getMatches(Matches.UnitIsCarrier);
		
		// Find Water territories I can reach
		List<Territory> landingSpotsToCheck = Match.getMatches((data.getMap().getNeighbors(currentSpot,movementLeft+1)),Matches.TerritoryIsWater);
		if(currentSpot.isWater())
			landingSpotsToCheck.add(currentSpot);
		
		// check if I can find a legal route to these spots
		for(Territory landingSpot:landingSpotsToCheck) { 
			
			if(!canAirReachThisSpot(data, player, unit, currentSpot, movementLeft,landingSpot)) {
				continue;
				}
				
			// can reach this spot, is there space?
			int capacity = MoveValidator.carrierCapacity(getFriendly(landingSpot, player, data), landingSpot);
			capacity -= usedCarrierSpace.getInt(landingSpot); // remove already claimed space...
			if(capacity>=ua.getCarrierCost()) {
				int newUsedCapacity = usedCarrierSpace.getInt(landingSpot) + ua.getCarrierCost();
				usedCarrierSpace.put(landingSpot, newUsedCapacity); // claim carrier space
				return true;
				}
				
			// take into account landingspots on carriers in the building queue
			if(landAirOnNewCarriers && !carriersInProductionQueue.isEmpty()) {
				if(placeOnBuiltCarrier(unit, landingSpot, usedCarrierSpace,carriersInProductionQueue,data, player)) {
					return true; //TODO if you move 2 fighter groups to 2 seaspots near a factory it will allow you to do it even though there is only room for 1 fightergroup
				}
			}
		}
			
		/*
		 * After all spots are checked and we can't find a good spot, we will check them again 
		 * but look further to see if we can find a friendly carrier that can reach us
		 */
		for(Territory landingSpot:landingSpotsToCheck) {
			if(!canAirReachThisSpot(data, player, unit, currentSpot, movementLeft,landingSpot)) {
				continue;
		}
			Set<Territory> territoriesToCheckForCarriersThatCanMove = data.getMap().getNeighbors(currentSpot,3,Matches.TerritoryIsWater);
			for(Territory carrierSpot:territoriesToCheckForCarriersThatCanMove) {
				int capacity = MoveValidator.carrierCapacity(getFriendly(carrierSpot, player, data), carrierSpot);
				capacity -= usedCarrierSpace.getInt(carrierSpot); // remove already claimed space...
				if(capacity>=ua.getCarrierCost()) {				
					Collection<Unit> carriers = Match.getMatches(carrierSpot.getUnits().getUnits(), Matches.carrierOwnedBy(player));
					Route carrierRoute = data.getMap().getRoute(carrierSpot,landingSpot);
					carriers = Match.getMatches(carriers,Matches.UnitHasEnoughMovementForRoute(carrierRoute));
					carriers.remove(movedCarriers);
					for(Unit carrierCandidate:carriers) {
						UnitAttachment cua = UnitAttachment.get(carrierCandidate.getType());
						if(cua.getCarrierCapacity() >= ua.getCarrierCost()) {
							int newUsedCapacityInCarrierSpot = usedCarrierSpace.getInt(carrierSpot) + cua.getCarrierCapacity();
							usedCarrierSpace.put(carrierSpot, newUsedCapacityInCarrierSpot);
							int newUsedCapacityInLandingSpot = usedCarrierSpace.getInt(landingSpot) + ua.getCarrierCost() - cua.getCarrierCapacity();
							usedCarrierSpace.put(landingSpot, newUsedCapacityInLandingSpot);
							movedCarriers.add(carrierCandidate);
							return true;
						}
					}
				}
			}
		}
		return false;
	}
		
private static boolean canAirReachThisSpot(GameData data, PlayerID player,Unit unit, final Territory currentSpot, final int movementLeft,Territory landingSpot) {
	//TODO EW: existing bug: Need to check for politics allowing Politics.Neutral Fly-Over? this code is still working on the assumption of alliances. not isNeutral state.
	if(areNeutralsPassable(data)) {
		Route neutralViolatingRoute = data.getMap().getRoute(currentSpot, landingSpot, Matches.TerritoryIsPassableAndNotRestricted(player));
		return (neutralViolatingRoute != null && neutralViolatingRoute.getMovementCost(unit)<=movementLeft && getNeutralCharge(data, neutralViolatingRoute) <= player.getResources().getQuantity(Constants.PUS));
	} else {
		Route noNeutralRoute = data.getMap().getRoute(currentSpot, landingSpot, Matches.territoryIsNotNeutralAndNotImpassibleOrRestricted(player));
		return (noNeutralRoute != null && noNeutralRoute.getMovementCost(unit) <= movementLeft);
	}
}
	
private static boolean placeOnBuiltCarrier(Unit airUnit, Territory landingSpot,IntegerMap<Territory> usedCarrierSpace,List<Unit> carriersInQueue, GameData data, PlayerID player) {	
	if(!Matches.territoryHasOwnedFactoryNeighbor(data, player).match(landingSpot))
		return false;
	//TODO EW: existing bug -- can this factory actually produce carriers? ie: shipyards vs. factories
		
	for(Unit carrierCandidate:carriersInQueue) {
		UnitAttachment aua = UnitAttachment.get(airUnit.getType());
		UnitAttachment cua = UnitAttachment.get(carrierCandidate.getType());
		if(cua.getCarrierCapacity() >= aua.getCarrierCost()) {
			//TODO EW: is this the wisest carrier-choice? improve by picking the smartest carrier
		
			/*
			 * add leftover capacity to the usedCarrierSpace because the carrier will virtually be placed in this spot!
			 * if I don't do this and remove the carrier from the queue the carrier could be built in multiple spots.
			 */
			int newUsedCapacity = usedCarrierSpace.getInt(landingSpot) + aua.getCarrierCost() - cua.getCarrierCapacity();
			usedCarrierSpace.put(landingSpot, newUsedCapacity); 
		
			//remove the Carrier from the Queue so it can't be placed somewhere else
			carriersInQueue.remove(carrierCandidate);
		
			return true;
			}
			}
			
	return false;
			
			}
			
private static boolean areNeutralsPassable(GameData data) {
				
	return (games.strategy.triplea.Properties.getNeutralFlyoverAllowed(data) && !isNeutralsImpassable(data));
			}
		
	private static Collection<Unit> getAirThatMustLandOnCarriers(final GameData data, final Collection<Unit> ownedAir, Route route, MoveValidationResult result, final PlayerID player)
	{
		Collection<Unit> airThatMustLandOnCarriers = new ArrayList<Unit>();
		Match<Unit> cantLandMatch = new InverseMatch<Unit>(Matches.UnitCanLandOnCarrier);
		Iterator<Unit> ownedAirIter = ownedAir.iterator();
		while (ownedAirIter.hasNext())
		{
			Unit unit = ownedAirIter.next();
			
			if (!canFindLand(data,unit,route))
			{
				airThatMustLandOnCarriers.add(unit);
				// not everything can land on a carrier (i.e. bombers)
				if (Match.allMatch(Collections.singleton(unit), cantLandMatch))
				{
					if (!isKamikazeAircraft(data) && !Matches.UnitIsKamikaze.match(unit))
						result.addDisallowedUnit(NOT_ALL_AIR_UNITS_CAN_LAND, unit);
				}
			}
		}
		return airThatMustLandOnCarriers;
	}
	
	/**
	 * Can this airunit reach safe land at this point in the route?
	 * @param data
	 * @param unit the airunit in question
	 * @param route the current spot from which he needs to reach safe land.
	 * @return whether the air-unit can find a stretch of friendly land to land on given her current spot and the remaining range.
	 */
	private static boolean canFindLand(GameData data, Unit unit,Route route) {
		final Territory routeEnd = route.getEnd();
		if(unit.getTerritoryUnitIsIn().equals(routeEnd))
			route = new Route(routeEnd);
		
		final int movementLeft = route.getMovementLeft(unit);
		if(movementLeft <= 0)
			return false;
		
		PlayerID player = unit.getOwner();
		List<Territory> possibleSpots =  Match.getMatches(data.getMap().getNeighbors(routeEnd, movementLeft),Matches.alliedNonConqueredNonPendingTerritory(data, player));
		for(Territory landingSpot:possibleSpots) { //TODO EW: Assuming movement cost of 1, this could get VERY slow when the movementcost is very high and airunits have a lot of movementcapacity.
			if(canAirReachThisSpot(data, player, unit, routeEnd, movementLeft,landingSpot))
				return true;
				}
		return false;
			}
					
	
	// Determines whether we can pay the neutral territory charge for a
	// given route for air units. We can't cross neutral territories
	// in WW2V2.
	private static MoveValidationResult canCrossNeutralTerritory(GameData data, Route route, PlayerID player, MoveValidationResult result)
	{
		// neutrals we will overfly in the first place
		Collection<Territory> neutrals = MoveDelegate.getEmptyNeutral(route);
		int PUs = player.getResources().getQuantity(Constants.PUS);
		
		if (PUs < getNeutralCharge(data, neutrals.size()))
			return result.setErrorReturnResult(TOO_POOR_TO_VIOLATE_NEUTRALITY);
		
		return result;
	}
	
	private static Territory getTerritoryTransportHasUnloadedTo(final List<UndoableMove> undoableMoves, Unit transport)
	{
		
		for (UndoableMove undoableMove : undoableMoves)
		{
			if (undoableMove.wasTransportUnloaded(transport))
			{
				return undoableMove.getRoute().getEnd();
			}
		}
		return null;
	}
	
	private static MoveValidationResult validateTransport(GameData data,
															final List<UndoableMove> undoableMoves,
															Collection<Unit> units,
															Route route,
															PlayerID player,
															Collection<Unit> transportsToLoad,
															MoveValidationResult result)
	{
		boolean isEditMode = getEditMode(data);
		
		if (Match.allMatch(units, Matches.UnitIsAir))
			return result;
		
		if (!route.hasWater())
			return result;
		
		// If there are non-sea transports return
		Boolean seaOrNoTransportsPresent = transportsToLoad.isEmpty() || Match.someMatch(transportsToLoad, new CompositeMatchAnd<Unit>(Matches.UnitIsSea, Matches.UnitCanTransport));
		if (!seaOrNoTransportsPresent)
			return result;
		/*if(!MoveValidator.isLoad(units, route, data, player) && !MoveValidator.isUnload(route))
			return result;*/

		TransportTracker transportTracker = new TransportTracker();
		
		// if unloading make sure length of route is only 1
		if (!isEditMode && MoveValidator.isUnload(route))
		{
			if (route.hasMoreThenOneStep())
				return result.setErrorReturnResult("Unloading units must stop where they are unloaded");
			
			for (Unit unit : transportTracker.getUnitsLoadedOnAlliedTransportsThisTurn(units))
				result.addDisallowedUnit(CANNOT_LOAD_AND_UNLOAD_AN_ALLIED_TRANSPORT_IN_THE_SAME_ROUND, unit);
			
			Collection<Unit> transports = MoveDelegate.mapTransports(route, units, null).values();
			for (Unit transport : transports)
			{
				// TODO This is very sensitive to the order of the transport collection. The users may
				// need to modify the order in which they perform their actions.
				
				// check whether transport has already unloaded
				if (transportTracker.hasTransportUnloadedInPreviousPhase(transport))
				{
					for (Unit unit : transportTracker.transporting(transport))
						result.addDisallowedUnit(TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_IN_A_PREVIOUS_PHASE, unit);
				}
				// check whether transport is restricted to another territory
				else if (transportTracker.isTransportUnloadRestrictedToAnotherTerritory(transport, route.getEnd()))
				{
					Territory alreadyUnloadedTo = getTerritoryTransportHasUnloadedTo(undoableMoves, transport);
					for (Unit unit : transportTracker.transporting(transport))
						result.addDisallowedUnit(TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_TO + alreadyUnloadedTo.getName(), unit);
				}
				// Check if the transport has already loaded after being in combat
				else if (transportTracker.isTransportUnloadRestrictedInNonCombat(transport))
				{
					for (Unit unit : transportTracker.transporting(transport))
						result.addDisallowedUnit(TRANSPORT_CANNOT_LOAD_AND_UNLOAD_AFTER_COMBAT, unit);
				}
			}
		}
		
		// if we are land make sure no water in route except for transport
		// situations
		Collection<Unit> land = Match.getMatches(units, Matches.UnitIsLand);
		
		// make sure we can be transported
		Match<Unit> cantBeTransported = new InverseMatch<Unit>(Matches.UnitCanBeTransported);
		for (Unit unit : Match.getMatches(land, cantBeTransported))
			result.addDisallowedUnit("Not all units can be transported", unit);
		
		// make sure that the only the first or last territory is land
		// dont want situation where they go sea land sea
		if (!isEditMode && route.hasLand() && !(route.getStart().isWater() || route.getEnd().isWater()))
		{
			if (nonParatroopersPresent(player, land) || !allLandUnitsAreBeingParatroopered(units, route, player))
			{
				return result.setErrorReturnResult("Invalid move, only start or end can be land when route has water.");
			}
		}
		
		// simply because I dont want to handle it yet
		// checks are done at the start and end, dont want to worry about just
		// using a transport as a bridge yet
		// TODO handle this
		if (!isEditMode && !route.getEnd().isWater() && !route.getStart().isWater() && nonParatroopersPresent(player, land))
			return result.setErrorReturnResult("Must stop units at a transport on route");
		
		if (route.getEnd().isWater() && route.getStart().isWater())
		{
			// make sure units and transports stick together
			Iterator<Unit> iter = units.iterator();
			while (iter.hasNext())
			{
				Unit unit = iter.next();
				UnitAttachment ua = UnitAttachment.get(unit.getType());
				// make sure transports dont leave their units behind
				if (ua.getTransportCapacity() != -1)
				{
					Collection<Unit> holding = transportTracker.transporting(unit);
					if (holding != null && !units.containsAll(holding))
						result.addDisallowedUnit("Transports cannot leave their units", unit);
				}
				// make sure units dont leave their transports behind
				if (ua.getTransportCost() != -1)
				{
					Unit transport = transportTracker.transportedBy(unit);
					if (transport != null && !units.contains(transport))
						result.addDisallowedUnit("Unit must stay with its transport while moving", unit);
				}
			}
		} // end if end is water
		
		if (route.isLoad())
		{
			
			if (!isEditMode && !route.hasExactlyOneStep() && nonParatroopersPresent(player, land))
				return result.setErrorReturnResult("Units cannot move before loading onto transports");
			
			CompositeMatch<Unit> enemyNonSubmerged = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), new InverseMatch<Unit>(Matches
						.unitIsSubmerged(data)));
			if (route.getEnd().getUnits().someMatch(enemyNonSubmerged) && nonParatroopersPresent(player, land))
				if (!onlyIgnoredUnitsOnPath(route, player, data, false))
					return result.setErrorReturnResult("Cannot load when enemy sea units are present");
			
			Map<Unit, Unit> unitsToTransports = MoveDelegate.mapTransports(route, land, transportsToLoad);
			
			Iterator<Unit> iter = land.iterator();
			// CompositeMatch<Unit> landUnitsAtSea = new CompositeMatchOr<Unit>(Matches.unitIsLandAndOwnedBy(player), Matches.UnitCanBeTransported);
			
			while (!isEditMode && iter.hasNext())
			{
				TripleAUnit unit = (TripleAUnit) iter.next();
				if (Matches.unitHasMoved.match(unit))
					result.addDisallowedUnit("Units cannot move before loading onto transports", unit);
				Unit transport = unitsToTransports.get(unit);
				if (transport == null)
					continue;
				
				if (transportTracker.hasTransportUnloadedInPreviousPhase(transport))
				{
					result.addDisallowedUnit(TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_IN_A_PREVIOUS_PHASE, unit);
				}
				else if (transportTracker.isTransportUnloadRestrictedToAnotherTerritory(transport, route.getEnd()))
				{
					Territory alreadyUnloadedTo = getTerritoryTransportHasUnloadedTo(undoableMoves, transport);
					Iterator<Unit> trnIter = transportsToLoad.iterator();
					while (trnIter.hasNext())
					{
						TripleAUnit trn = (TripleAUnit) trnIter.next();
						if (!transportTracker.isTransportUnloadRestrictedToAnotherTerritory(trn, route.getEnd()))
						{
							UnitAttachment ua = UnitAttachment.get(unit.getType());
							// UnitAttachment trna = UnitAttachment.get(trn.getType());
							if (transportTracker.getAvailableCapacity(trn) >= ua.getTransportCost())
							{
								alreadyUnloadedTo = null;
								break;
							}
						}
					}
					if (alreadyUnloadedTo != null)
						result.addDisallowedUnit(TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_TO + alreadyUnloadedTo.getName(), unit);
				}
			}
			
			if (!unitsToTransports.keySet().containsAll(land))
			{
				// some units didn't get mapped to a transport
				Collection<UnitCategory> unitsToLoadCategories = UnitSeperator.categorize(land);
				
				if (unitsToTransports.size() == 0 || unitsToLoadCategories.size() == 1)
				{
					// set all unmapped units as disallowed if there are no transports
					// or only one unit category
					for (Unit unit : land)
					{
						if (unitsToTransports.containsKey(unit))
							continue;
						
						UnitAttachment ua = UnitAttachment.get(unit.getType());
						if (ua.getTransportCost() != -1)
						{
							result.addDisallowedUnit("Not enough transports", unit);
							// System.out.println("adding disallowed unit (Not enough transports): "+unit);
						}
					}
				}
				else
				{
					// set all units as unresolved if there is at least one transport
					// and mixed unit categories
					for (Unit unit : land)
					{
						UnitAttachment ua = UnitAttachment.get(unit.getType());
						if (ua.getTransportCost() != -1)
						{
							result.addUnresolvedUnit("Not enough transports", unit);
							// System.out.println("adding unresolved unit (Not enough transports): "+unit);
						}
					}
				}
			}
			
		}
		
		return result;
	}
	
	public static boolean allLandUnitsAreBeingParatroopered(Collection<Unit> units, Route route, PlayerID player)
	{
		// some units that can't be paratrooped
		if (!Match.allMatch(units, new CompositeMatchOr<Unit>(Matches.UnitIsAirTransportable, Matches.UnitIsAirTransport)))
		{
			return false;
		}
		
		List<Unit> paratroopsRequiringTransport = getParatroopsRequiringTransport(units, route);
		if (paratroopsRequiringTransport.isEmpty())
		{
			return false;
		}
		
		List<Unit> airTransports = Match.getMatches(units, Matches.UnitIsAirTransport);
		List<Unit> allParatroops = MoveDelegate.mapAirTransportPossibilities(route, paratroopsRequiringTransport, airTransports, false, player);
		return allParatroops.containsAll(paratroopsRequiringTransport);
	}
	
	// checks if there are non-paratroopers present that cause move validations to fail
	private static boolean nonParatroopersPresent(PlayerID player, Collection<Unit> units)
	{
		if (!isParatroopers(player))
		{
			return true;
		}
		
		if (!Match.allMatch(units, new CompositeMatchOr<Unit>(Matches.UnitIsAir, Matches.UnitIsLand)))
		{
			return true;
		}
		
		for (Unit unit : Match.getMatches(units, Matches.UnitIsNotAirTransportable))
		{
			if (Matches.UnitIsLand.match(unit))
				return true;
		}
		return false;
		
	}
	
	private static List<Unit> getParatroopsRequiringTransport(
				Collection<Unit> units, final Route route
				)
	{
		
		return Match.getMatches(units,
					new CompositeMatchAnd<Unit>(
								Matches.UnitIsAirTransportable,
								new Match<Unit>()
								{
									
									@Override
									public boolean match(Unit u)
									{
										return TripleAUnit.get(u).getMovementLeft() < route.getMovementCost(u) || route.crossesWater() || route.getEnd().isWater();
									}
									
								}
					));
	}
	
	private static MoveValidationResult validateParatroops(boolean nonCombat,
															GameData data,
															final List<UndoableMove> undoableMoves,
															Collection<Unit> units,
															Route route,
															PlayerID player,
															MoveValidationResult result)
	{
		if (!isParatroopers(player))
			return result;
		
		if (Match.noneMatch(units, Matches.UnitIsAirTransportable) || Match.noneMatch(units, Matches.UnitIsAirTransport))
			return result;
		
		if (nonCombat && !IsParatroopersCanMoveDuringNonCombat(data))
			return result.setErrorReturnResult("Paratroops may not move during NonCombat");
		
		if (!getEditMode(data))
		{
			// if we can move without using paratroop tech, do so
			// this allows moving a bomber/infantry from one friendly
			// territory to another
			List<Unit> paratroopsRequiringTransport = getParatroopsRequiringTransport(units, route);
			if (paratroopsRequiringTransport.isEmpty())
			{
				return result;
			}
			
			List<Unit> airTransports = Match.getMatches(units, Matches.UnitIsAirTransport);
			// TODO kev change below to mapAirTransports (or modify mapTransports to handle air cargo)
			// Map<Unit, Unit> airTransportsAndParatroops = MoveDelegate.mapTransports(route, paratroopsRequiringTransport, airTransports);
			Map<Unit, Unit> airTransportsAndParatroops = MoveDelegate.mapAirTransports(route, paratroopsRequiringTransport, airTransports, true, player);
			
			for (Unit paratroop : airTransportsAndParatroops.keySet())
			{
				if (Matches.unitHasMoved.match(paratroop))
				{
					result.addDisallowedUnit("Cannot paratroop units that have already moved", paratroop);
				}
				
				Unit transport = airTransportsAndParatroops.get(paratroop);
				if (Matches.unitHasMoved.match(transport))
				{
					result.addDisallowedUnit("Cannot move then transport paratroops", transport);
				}
			}
			
			Territory routeEnd = route.getEnd();
			for (Unit paratroop : paratroopsRequiringTransport)
			{
				if (Matches.unitHasMoved.match(paratroop))
				{
					result.addDisallowedUnit("Cannot paratroop units that have already moved", paratroop);
				}
				
				if (Matches.isTerritoryFriendly(player, data).match(routeEnd) && !IsParatroopersCanMoveDuringNonCombat(data))
				{
					result.addDisallowedUnit("Paratroops must advance to battle", paratroop);
				}
				
				if (!nonCombat && Matches.isTerritoryFriendly(player, data).match(routeEnd) && IsParatroopersCanMoveDuringNonCombat(data))
				{
					result.addDisallowedUnit("Paratroops may only airlift during Non-Combat Movement Phase", paratroop);
				}
			}
			
			for(Territory current:route.getMiddleSteps()) {
				if (!Matches.isTerritoryFriendly(player, data).match(current))
					return result.setErrorReturnResult("Must stop paratroops in first enemy territory");
			}
		}
		
		return result;
	}
	
	public static String validateCanal(Route route, PlayerID player, GameData data)
	{
		for (Territory routeTerritory : route.getAllTerritories())
		{
			Set<CanalAttachment> canalAttachments = CanalAttachment.get(routeTerritory);
			if (canalAttachments.isEmpty())
				continue;
			
			Iterator<CanalAttachment> iter = canalAttachments.iterator();
			while (iter.hasNext())
			{
				CanalAttachment attachment = iter.next();
				if (attachment == null)
					continue;
				if (!route.getAllTerritories().containsAll(CanalAttachment.getAllCanalSeaZones(attachment.getCanalName(), data)))
				{
					continue;
				}
				
				for (Territory borderTerritory : attachment.getLandTerritories())
				{
					if (!data.getRelationshipTracker().isAllied(player, borderTerritory.getOwner()))
					{
						return "Must own " + borderTerritory.getName() + " to go through " + attachment.getCanalName();
					}
					if (MoveDelegate.getBattleTracker(data).wasConquered(borderTerritory))
					{
						return "Cannot move through " + attachment.getCanalName() + " without owning " + borderTerritory.getName() + " for an entire turn";
					}
				}
			}
		}
		return null;
	}
	
	public static MustMoveWithDetails getMustMoveWith(Territory start, Collection<Unit> units, GameData data, PlayerID player)
	{
		return new MustMoveWithDetails(mustMoveWith(units, start, data, player));
	}
	
	private static Map<Unit, Collection<Unit>> mustMoveWith(Collection<Unit> units, Territory start, GameData data, PlayerID player)
	{
		
		List<Unit> sortedUnits = new ArrayList<Unit>(units);
		
		Collections.sort(sortedUnits, UnitComparator.getIncreasingMovementComparator());
		
		Map<Unit, Collection<Unit>> mapping = new HashMap<Unit, Collection<Unit>>();
		mapping.putAll(transportsMustMoveWith(sortedUnits));
		
		// Check if there are combined transports (carriers that are transports) and load them.
		if (mapping.isEmpty())
		{
			mapping.putAll(carrierMustMoveWith(sortedUnits, start, data, player));
		}
		else
		{
			Map<Unit, Collection<Unit>> newMapping = new HashMap<Unit, Collection<Unit>>();
			newMapping.putAll(carrierMustMoveWith(sortedUnits, start, data, player));
			if (!newMapping.isEmpty())
				addToMapping(mapping, newMapping);
		}
		
		if (mapping.isEmpty())
		{
			mapping.putAll(airTransportsMustMoveWith(sortedUnits));
		}
		else
		{
			Map<Unit, Collection<Unit>> newMapping = new HashMap<Unit, Collection<Unit>>();
			newMapping.putAll(airTransportsMustMoveWith(sortedUnits));
			if (!newMapping.isEmpty())
				addToMapping(mapping, newMapping);
		}
		return mapping;
	}
	
	private static void addToMapping(Map<Unit, Collection<Unit>> mapping,
				Map<Unit, Collection<Unit>> newMapping)
	{
		for (Unit key : newMapping.keySet())
		{
			if (mapping.containsKey(key))
			{
				Collection<Unit> heldUnits = mapping.get(key);
				heldUnits.addAll(newMapping.get(key));
				mapping.put(key, heldUnits);
			}
			else
			{
				mapping.put(key, newMapping.get(key));
			}
		}
	}
	
	private static Map<Unit, Collection<Unit>> transportsMustMoveWith(Collection<Unit> units)
	{
		TransportTracker transportTracker = new TransportTracker();
		Map<Unit, Collection<Unit>> mustMoveWith = new HashMap<Unit, Collection<Unit>>();
		// map transports
		Collection<Unit> transports = Match.getMatches(units, Matches.UnitIsTransport);
		Iterator<Unit> iter = transports.iterator();
		while (iter.hasNext())
		{
			Unit transport = iter.next();
			Collection<Unit> transporting = transportTracker.transporting(transport);
			mustMoveWith.put(transport, transporting);
		}
		return mustMoveWith;
	}
	
	private static Map<Unit, Collection<Unit>> airTransportsMustMoveWith(Collection<Unit> units)
	{
		TransportTracker transportTracker = new TransportTracker();
		Map<Unit, Collection<Unit>> mustMoveWith = new HashMap<Unit, Collection<Unit>>();
		
		Collection<Unit> airTransports = Match.getMatches(units, Matches.UnitIsAirTransport);
		Map<Unit, Collection<Unit>> selectedDependents = new HashMap<Unit, Collection<Unit>>();
		// first, check if there are any that haven't been updated yet
		for (Unit airTransport : airTransports)
		{
			if (selectedDependents.containsKey(airTransport))
			{
				Collection<Unit> transporting = selectedDependents.get(airTransport);
				mustMoveWith.put(airTransport, transporting);
			}
		}
		
		// Then check those that have already had their transportedBy set
		for (Unit airTransport : airTransports)
		{
			if (!mustMoveWith.containsKey(airTransport))
			{
				Collection<Unit> transporting = transportTracker.transporting(airTransport);
				if (transporting == null || transporting.isEmpty())
				{
					selectedDependents = MovePanel.getDependents();
					if (!selectedDependents.isEmpty())
						transporting = selectedDependents.get(airTransport);
				}
				
				mustMoveWith.put(airTransport, transporting);
			}
		}
		return mustMoveWith;
	}
	
	private static Map<Unit, Collection<Unit>> carrierMustMoveWith(Collection<Unit> units, Territory start, GameData data, PlayerID player)
	{
		return carrierMustMoveWith(units, start.getUnits().getUnits(), data, player);
	}
	
	public static Map<Unit, Collection<Unit>> carrierMustMoveWith(Collection<Unit> units, Collection<Unit> startUnits, GameData data, PlayerID player)
	{
		// we want to get all air units that are owned by our allies
		// but not us that can land on a carrier
		CompositeMatch<Unit> friendlyNotOwnedAir = new CompositeMatchAnd<Unit>();
		friendlyNotOwnedAir.add(Matches.alliedUnit(player, data));
		friendlyNotOwnedAir.addInverse(Matches.unitIsOwnedBy(player));
		friendlyNotOwnedAir.add(Matches.UnitCanLandOnCarrier);
		
		Collection<Unit> alliedAir = Match.getMatches(startUnits, friendlyNotOwnedAir);
		
		if (alliedAir.isEmpty())
			return Collections.emptyMap();
		
		// remove air that can be carried by allied
		CompositeMatch<Unit> friendlyNotOwnedCarrier = new CompositeMatchAnd<Unit>();
		friendlyNotOwnedCarrier.add(Matches.UnitIsCarrier);
		friendlyNotOwnedCarrier.add(Matches.alliedUnit(player, data));
		friendlyNotOwnedCarrier.addInverse(Matches.unitIsOwnedBy(player));
		
		Collection<Unit> alliedCarrier = Match.getMatches(startUnits, friendlyNotOwnedCarrier);
		
		Iterator<Unit> alliedCarrierIter = alliedCarrier.iterator();
		while (alliedCarrierIter.hasNext())
		{
			Unit carrier = alliedCarrierIter.next();
			Collection<Unit> carrying = getCanCarry(carrier, alliedAir);
			alliedAir.removeAll(carrying);
		}
		
		if (alliedAir.isEmpty())
			return Collections.emptyMap();
		
		Map<Unit, Collection<Unit>> mapping = new HashMap<Unit, Collection<Unit>>();
		// get air that must be carried by our carriers
		Collection<Unit> ownedCarrier = Match.getMatches(units, new CompositeMatchAnd<Unit>(Matches.UnitIsCarrier, Matches.unitIsOwnedBy(player)));
		
		Iterator<Unit> ownedCarrierIter = ownedCarrier.iterator();
		while (ownedCarrierIter.hasNext())
		{
			Unit carrier = ownedCarrierIter.next();
			Collection<Unit> carrying = getCanCarry(carrier, alliedAir);
			alliedAir.removeAll(carrying);
			
			mapping.put(carrier, carrying);
		}
		
		return mapping;
	}
	
	private static Collection<Unit> getCanCarry(Unit carrier, Collection<Unit> selectFrom)
	{
		UnitAttachment ua = UnitAttachment.get(carrier.getUnitType());
		Collection<Unit> canCarry = new ArrayList<Unit>();
		
		int available = ua.getCarrierCapacity();
		Iterator<Unit> iter = selectFrom.iterator();
		TripleAUnit tACarrier = (TripleAUnit) carrier;
		while (iter.hasNext())
		{
			Unit plane = iter.next();
			TripleAUnit tAPlane = (TripleAUnit) plane;
			UnitAttachment planeAttatchment = UnitAttachment.get(plane.getUnitType());
			int cost = planeAttatchment.getCarrierCost();
			if (available >= cost)
			{
				// this is to test if they started in the same sea zone or not, however a unit could have their alreadyMoved modified by naval or air bases, so until we unify all the different movement/carrying/transporting methods into a single framework, we will just hack this
				if (tACarrier.getAlreadyMoved() == tAPlane.getAlreadyMoved() || (Matches.unitHasNotMoved.match(plane) && Matches.unitHasNotMoved.match(carrier)))
				{
					available -= cost;
					canCarry.add(plane);
				}
			}
			if (available == 0)
				break;
		}
		return canCarry;
	}
	
	/**
	 * Get the route ignoring forced territories
	 */
	@SuppressWarnings("unchecked")
	public static Route getBestRoute(Territory start, Territory end, GameData data, PlayerID player, Collection<Unit> units)
	{
		// ignore the end territory in our tests. it must be in the route, so it shouldn't affect the route choice
		Match<Territory> territoryIsEnd = Matches.territoryIs(end);
		
		// No neutral countries on route predicate
		Match<Territory> noNeutral = new CompositeMatchOr<Territory>(territoryIsEnd, Matches.TerritoryIsNeutral.invert());
		
		// No aa guns on route predicate
		Match<Territory> noAA = new CompositeMatchOr<Territory>(territoryIsEnd, Matches.territoryHasEnemyAA(player, data).invert());
		
		// no enemy units on the route predicate
		Match<Territory> noEnemy = new CompositeMatchOr<Territory>(territoryIsEnd, Matches.territoryHasEnemyUnits(player, data).invert());
		
		Route defaultRoute;
		if (isWW2V2(data) || isNeutralsImpassable(data))
			defaultRoute = data.getMap().getRoute(start, end, noNeutral);
		
		else
			defaultRoute = data.getMap().getRoute(start, end);
		
		if (defaultRoute == null)
			defaultRoute = data.getMap().getRoute(start, end);
		
		if (defaultRoute == null)
			return null;
		
		// we don't want to look at the dependents
		Collection<Unit> unitsWhichAreNotBeingTransportedOrDependent = new ArrayList<Unit>(Match.getMatches(units,
					Matches.unitIsBeingTransportedByOrIsDependentOfSomeUnitInThisList(units, defaultRoute, player, data).invert()));
		boolean mustGoLand = false;
		boolean mustGoSea = false;
		
		// If start and end are land, try a land route.
		// don't force a land route, since planes may be moving
		if (!start.isWater() && !end.isWater())
		{
			Route landRoute;
			if (isNeutralsImpassable(data))
				landRoute = data.getMap().getRoute(start, end, new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, noNeutral));
			else
				landRoute = data.getMap().getRoute(start, end, Matches.TerritoryIsLand);
			
			if (landRoute != null && (Match.someMatch(unitsWhichAreNotBeingTransportedOrDependent, Matches.UnitIsLand)))
			{
				defaultRoute = landRoute;
				mustGoLand = true;
			}
		}
		
		// if the start and end are in water, try and get a water route
		// dont force a water route, since planes may be moving
		if (start.isWater() && end.isWater())
		{
			Route waterRoute = data.getMap().getRoute(start, end, Matches.TerritoryIsWater);
			if (waterRoute != null && (Match.someMatch(unitsWhichAreNotBeingTransportedOrDependent, Matches.UnitIsSea)))
			{
				defaultRoute = waterRoute;
				mustGoSea = true;
			}
		}
		
		// these are the conditions we would like the route to satisfy, starting
		// with the most important
		List<Match<Territory>> tests;
		if (isNeutralsImpassable(data))
		{
			tests = new ArrayList<Match<Territory>>(Arrays.asList(
						// best if no enemy and no neutral
						new CompositeMatchAnd<Territory>(noEnemy, noNeutral),
						// we will be satisfied if no aa and no neutral
						new CompositeMatchAnd<Territory>(noAA, noNeutral)));
		}
		else
		{
			tests = new ArrayList<Match<Territory>>(Arrays.asList(
						// best if no enemy and no neutral
						new CompositeMatchAnd<Territory>(noEnemy, noNeutral),
						// we will be satisfied if no aa and no neutral
						new CompositeMatchAnd<Territory>(noAA, noNeutral),
						// single matches
						noEnemy, noAA, noNeutral));
		}
		
		for (Match<Territory> t : tests)
		{
			Match<Territory> testMatch = null;
			if (mustGoLand)
				testMatch = new CompositeMatchAnd<Territory>(t, Matches.TerritoryIsLand);
			else if (mustGoSea)
				testMatch = new CompositeMatchAnd<Territory>(t, Matches.TerritoryIsWater);
			else
				testMatch = t;
			
			Route testRoute = data.getMap().getRoute(start, end, new CompositeMatchOr<Territory>(testMatch, territoryIsEnd));
			if (testRoute != null && testRoute.getLargestMovementCost(unitsWhichAreNotBeingTransportedOrDependent) == defaultRoute.getLargestMovementCost(unitsWhichAreNotBeingTransportedOrDependent))
				return testRoute;
		}
		
		return defaultRoute;
	}
	
	/**
	 * @return
	 */
	private static boolean isSubmersibleSubsAllowed(GameData data)
	{
		return games.strategy.triplea.Properties.getSubmersible_Subs(data);
	}
	
	/**
	 * @return
	 */
	private static boolean isKamikazeAircraft(GameData data)
	{
		return games.strategy.triplea.Properties.getKamikaze_Airplanes(data);
	}
	
	/**
	 * @return
	 */
	private static boolean isAlliedAirDependents(GameData data)
	{
		return games.strategy.triplea.Properties.getAlliedAirDependents(data);
	}
	
	/**
	 * @return
	 */
	private static boolean isIgnoreTransportInMovement(GameData data)
	{
		return games.strategy.triplea.Properties.getIgnoreTransportInMovement(data);
	}
	
	/**
	 * @return
	 */
	private static boolean isIgnoreSubInMovement(GameData data)
	{
		return games.strategy.triplea.Properties.getIgnoreSubInMovement(data);
	}
	
	/**
	 * @return
	 */
	private static boolean isSubControlSeaZoneRestricted(GameData data)
	{
		return games.strategy.triplea.Properties.getSubControlSeaZoneRestricted(data);
	}
	
	/**
	 * @return
	 */
	private static boolean isTransportControlSeaZone(GameData data)
	{
		return games.strategy.triplea.Properties.getTransportControlSeaZone(data);
	}
	
	/** Creates new MoveValidator */
	private MoveValidator()
	{
	}
}
