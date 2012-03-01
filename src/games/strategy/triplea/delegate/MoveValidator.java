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
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.CanalAttachment;
import games.strategy.triplea.attatchments.PlayerAttachment;
import games.strategy.triplea.attatchments.RulesAttachment;
import games.strategy.triplea.attatchments.TechAttachment;
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
import java.util.LinkedHashSet;
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
	public static final String LOST_BLITZ_ABILITY = "Unit lost blitz ability";
	public static final String NOT_ALL_UNITS_CAN_BLITZ = "Not all units can blitz";
	
	public static MoveValidationResult validateMove(final Collection<Unit> units, final Route route, final PlayerID player, final Collection<Unit> transportsToLoad, final boolean isNonCombat,
				final List<UndoableMove> undoableMoves, final GameData data)
	{
		final MoveValidationResult result = new MoveValidationResult();
		if (route.hasNoSteps())
			return result;
		if (!units.isEmpty() && !getEditMode(data)
					&& !Match.allMatch(Match.getMatches(units, Matches.unitIsBeingTransportedByOrIsDependentOfSomeUnitInThisList(units, route, player, data).invert()), Matches.unitIsOwnedBy(player)))
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
		final Collection<Territory> landOnRoute = route.getMatches(Matches.TerritoryIsLand);
		if (!landOnRoute.isEmpty())
		{
			// TODO: if this ever changes, we need to also update getBestRoute(), because getBestRoute is also checking to make sure we avoid land territories owned by nations with these 2 relationship type attachment options
			for (final Territory t : landOnRoute)
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
		if (validateFuel(data, units, route, player, result).getError() != null)
			return result;
		// dont let the user move out of a battle zone
		// the exception is air units and unloading units into a battle zone
		if (MoveDelegate.getBattleTracker(data).hasPendingBattle(route.getStart(), false) && Match.someMatch(units, Matches.UnitIsNotAir))
		{
			// if the units did not move into the territory, then they can move out
			// this will happen if there is a submerged sub in the area, and
			// a different unit moved into the sea zone setting up a battle
			// but the original unit can still remain
			boolean unitsStartedInTerritory = true;
			for (final Unit unit : units)
			{
				if (MoveDelegate.getRouteUsedToMoveInto(undoableMoves, unit, route.getEnd()) != null)
				{
					unitsStartedInTerritory = false;
					break;
				}
			}
			if (!unitsStartedInTerritory)
			{
				final boolean unload = MoveValidator.isUnload(route);
				final PlayerID endOwner = route.getEnd().getOwner();
				final boolean attack = !data.getRelationshipTracker().isAllied(endOwner, player) || MoveDelegate.getBattleTracker(data).wasConquered(route.getEnd());
				// unless they are unloading into another battle
				if (!(unload && attack))
					return result.setErrorReturnResult("Cannot move units out of battle zone");
			}
		}
		return result;
	}
	
	private static MoveValidationResult validateFuel(final GameData data, final Collection<Unit> units, final Route route, final PlayerID player, final MoveValidationResult result)
	{
		if (getEditMode(data))
			return result;
		final ResourceCollection fuelCost = Route.getMovementCharge(units, route);
		
		if (player.getResources().has(fuelCost.getResourcesCopy()))
			return result;
		
		return result.setErrorReturnResult("Not enough resources to perform this move, you need: " + fuelCost + " for this move");
	}
	
	private static MoveValidationResult validateCanal(final GameData data, final Collection<Unit> units, final Route route, final PlayerID player, final MoveValidationResult result)
	{
		if (getEditMode(data))
			return result;
		// TODO: merge validateCanal here and provide granular unit warnings
		return result.setErrorReturnResult(validateCanal(route, units, player, data));
	}
	
	private static MoveValidationResult validateCombat(final GameData data, final Collection<Unit> units, final Route route, final PlayerID player, final MoveValidationResult result)
	{
		if (getEditMode(data))
			return result;
		for (final Territory t : route.getSteps())
		{
			if (!Matches.territoryOwnerRelationshipTypeCanMoveIntoDuringCombatMove(player).match(t))
				return result.setErrorReturnResult("Can not move into territories owned by " + t.getOwner().getName() + " during Combat Movement Phase");
		}
		if (!route.getStart().isWater() && Matches.isAtWar(route.getStart().getOwner(), data).match(player) && Matches.isAtWar(route.getEnd().getOwner(), data).match(player))
		{
			if (!Matches.TerritoryIsBlitzable(player, data).match(route.getStart()) && !Match.allMatch(units, Matches.UnitIsAir))
				return result.setErrorReturnResult("Can not blitz out of a battle into enemy territory");
			for (final Unit u : Match.getMatches(units, new CompositeMatchAnd<Unit>(Matches.UnitCanBlitz.invert(), Matches.UnitIsNotAir)))
			{
				result.addDisallowedUnit("Not all units can blitz out of empty enemy territory", u);
			}
		}
		// Don't allow aa guns (and other disallowed units) to move in combat unless they are in a transport
		if (Match.someMatch(units, Matches.UnitCanNotMoveDuringCombatMove) && (!route.getStart().isWater() || !route.getEnd().isWater()))
		{
			for (final Unit unit : Match.getMatches(units, Matches.UnitCanNotMoveDuringCombatMove))
				result.addDisallowedUnit("Cannot move AA guns in combat movement phase", unit);
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
			for (final Territory current : route.getMiddleSteps())
			{
				if (current.isWater())
					continue;
				if (!data.getRelationshipTracker().isAllied(current.getOwner(), player) || MoveDelegate.getBattleTracker(data).wasConquered(current))
				{
					enemyCount++;
					allEnemyBlitzable &= Matches.TerritoryIsBlitzable(player, data).match(current);
				}
			}
			if (enemyCount > 0 && !allEnemyBlitzable)
			{
				if (nonParatroopersPresent(player, units, route))
					return result.setErrorReturnResult("Cannot blitz on that route");
			}
			else if (enemyCount >= 0 && allEnemyBlitzable && !(route.getStart().isWater() || route.getEnd().isWater()))
			{
				final Match<Unit> blitzingUnit = new CompositeMatchOr<Unit>(Matches.UnitCanBlitz, Matches.UnitIsAir);
				final Match<Unit> nonBlitzing = new InverseMatch<Unit>(blitzingUnit);
				final Collection<Unit> nonBlitzingUnits = Match.getMatches(units, nonBlitzing);
				// remove any units that gain blitz due to certain abilities
				nonBlitzingUnits.removeAll(UnitAttachment.getUnitsWhichReceivesAbilityWhenWith(units, "canBlitz", data));
				final Match<Territory> territoryIsNotEnd = new InverseMatch<Territory>(Matches.territoryIs(route.getEnd()));
				final Match<Territory> nonFriendlyTerritories = new InverseMatch<Territory>(Matches.isTerritoryFriendly(player, data));
				final Match<Territory> notEndOrFriendlyTerrs = new CompositeMatchAnd<Territory>(nonFriendlyTerritories, territoryIsNotEnd);
				final Match<Territory> foughtOver = Matches.territoryWasFoughOver(MoveDelegate.getBattleTracker(data));
				final Match<Territory> notEndWasFought = new CompositeMatchAnd<Territory>(territoryIsNotEnd, foughtOver);
				final Boolean wasStartFoughtOver = MoveDelegate.getBattleTracker(data).wasConquered(route.getStart()) || MoveDelegate.getBattleTracker(data).wasBlitzed(route.getStart());
				nonBlitzingUnits.addAll(Match.getMatches(units,
							Matches.unitIsOfTypes(TerritoryEffectHelper.getUnitTypesThatLostBlitz((wasStartFoughtOver ? route.getAllTerritories() : route.getSteps())))));
				for (final Unit unit : nonBlitzingUnits)
				{
					if (Matches.UnitIsAirTransportable.match(unit))
						continue;
					if (Matches.UnitIsInfantry.match(unit))
						continue;
					final TripleAUnit tAUnit = (TripleAUnit) unit;
					if (wasStartFoughtOver || tAUnit.getWasInCombat() || route.someMatch(notEndOrFriendlyTerrs) || route.someMatch(notEndWasFought))
						result.addDisallowedUnit("Not all units can blitz", unit);
				}
				
			}
		}
		if (Match.someMatch(units, Matches.UnitIsAir))
		{ // check aircraft
			if (route.hasSteps() && (!games.strategy.triplea.Properties.getNeutralFlyoverAllowed(data) || isNeutralsImpassable(data)))
			{
				if (Match.someMatch(route.getMiddleSteps(), Matches.TerritoryIsNeutralButNotWater))
					return result.setErrorReturnResult("Air units cannot fly over neutral territories");
			}
		}
		// make sure no conquered territories on route
		if (MoveValidator.hasConqueredNonBlitzedNonWaterOnRoute(route, data))
		{
			// unless we are all air or we are in non combat OR the route is water (was a bug in convoy zone movement)
			if (!Match.allMatch(units, Matches.UnitIsAir))
			{
				// what if we are paratroopers?
				return result.setErrorReturnResult("Cannot move through newly captured territories");
			}
		}
		// See if they've already been in combat
		if (Match.someMatch(units, Matches.UnitWasInCombat) && Match.someMatch(units, Matches.UnitWasUnloadedThisTurn))
		{
			final Collection<Territory> end = Collections.singleton(route.getEnd());
			if (Match.allMatch(end, Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data)) && !route.getEnd().getUnits().isEmpty())
				return result.setErrorReturnResult("Units cannot participate in multiple battles");
		}
		// See if we are doing invasions in combat phase, with units or transports that can't do invasion.
		if (MoveValidator.isUnload(route) && Matches.isTerritoryEnemy(player, data).match(route.getEnd()))
		{
			for (final Unit unit : Match.getMatches(units, Matches.UnitCanInvade.invert()))
			{
				result.addDisallowedUnit(unit.getUnitType().getName() + " can't invade from " + TripleAUnit.get(unit).getTransportedBy().getUnitType().getName(), unit);
			}
		}
		return result;
	}
	
	private static MoveValidationResult validateNonCombat(final GameData data, final Collection<Unit> units, final Route route, final PlayerID player, final MoveValidationResult result)
	{
		if (getEditMode(data))
			return result;
		if (route.someMatch(Matches.TerritoryIsImpassable))
			return result.setErrorReturnResult(CANT_MOVE_THROUGH_IMPASSIBLE);
		if (!route.someMatch(Matches.TerritoryIsPassableAndNotRestricted(player, data)))
			return result.setErrorReturnResult(CANT_MOVE_THROUGH_RESTRICTED);
		final CompositeMatch<Territory> battle = new CompositeMatchOr<Territory>();
		battle.add(Matches.TerritoryIsNeutralButNotWater);
		battle.add(Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data));
		final CompositeMatch<Unit> transportsCanNotControl = new CompositeMatchAnd<Unit>();
		transportsCanNotControl.add(Matches.UnitIsTransportAndNotDestroyer);
		transportsCanNotControl.add(Matches.UnitIsTransportButNotCombatTransport);
		final boolean navalMayNotNonComIntoControlled = isWW2V2(data) || games.strategy.triplea.Properties.getNavalUnitsMayNotNonCombatMoveIntoControlledSeaZones(data);
		// TODO need to account for subs AND transports that are ignored, not just OR
		if (battle.match(route.getEnd()))
		{
			// If subs and transports can't control sea zones, it's OK to move there
			if (!navalMayNotNonComIntoControlled && isSubControlSeaZoneRestricted(data) && Match.allMatch(units, Matches.UnitIsSub))
				return result;
			else if (!navalMayNotNonComIntoControlled && !isTransportControlSeaZone(data) && Match.allMatch(units, transportsCanNotControl))
				return result;
			else if (!navalMayNotNonComIntoControlled && route.allMatch(Matches.TerritoryIsWater) && MoveValidator.noEnemyUnitsOnPathMiddleSteps(route, player, data)
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
			final CompositeMatch<Unit> friendlyOrSubmerged = new CompositeMatchOr<Unit>();
			friendlyOrSubmerged.add(Matches.alliedUnit(player, data));
			friendlyOrSubmerged.add(Matches.unitIsSubmerged(data));
			if (!route.getEnd().getUnits().allMatch(friendlyOrSubmerged))
				return result.setErrorReturnResult("Cannot advance to battle in non combat");
		}
		if (Match.allMatch(units, Matches.UnitIsAir))
		{
			if (route.someMatch(new CompositeMatchAnd<Territory>(Matches.TerritoryIsNeutralButNotWater, Matches.TerritoryIsWater.invert()))
						&& (!games.strategy.triplea.Properties.getNeutralFlyoverAllowed(data) || isNeutralsImpassable(data)))
				return result.setErrorReturnResult("Air units cannot fly over neutral territories in non combat");
		}
		else
		{
			final CompositeMatch<Territory> neutralOrEnemy = new CompositeMatchOr<Territory>(Matches.TerritoryIsNeutralButNotWater, Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(
						player,
						data));
			if (route.someMatch(neutralOrEnemy))
			{
				if (!(!navalMayNotNonComIntoControlled && route.allMatch(Matches.TerritoryIsWater) && MoveValidator.noEnemyUnitsOnPathMiddleSteps(route, player, data) && !Matches
							.territoryHasEnemyUnits(
										player, data).match(route.getEnd())))
				{
					if (!route.allMatch(new CompositeMatchOr<Territory>(Matches.TerritoryIsWater, new CompositeMatchAnd<Territory>(Matches.TerritoryIsPassableAndNotRestricted(player, data), Matches
								.isTerritoryAllied(player, data), Matches.TerritoryIsLand))) || nonParatroopersPresent(player, units, route))
						return result.setErrorReturnResult("Cannot move units to neutral or enemy territories in non combat");
				}
			}
		}
		return result;
	}
	
	// Added to handle restriction of movement to listed territories
	private static MoveValidationResult validateMovementRestrictedByTerritory(final GameData data, final Collection<Unit> units, final Route route, final PlayerID player,
				final MoveValidationResult result)
	{
		if (getEditMode(data))
			return result;
		if (!isMovementByTerritoryRestricted(data))
			return result;
		final RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
		if (ra == null || ra.getMovementRestrictionTerritories() == null)
			return result;
		final String movementRestrictionType = ra.getMovementRestrictionType();
		final Collection<Territory> listedTerritories = ra.getListedTerritories(ra.getMovementRestrictionTerritories());
		if (movementRestrictionType.equals("allowed"))
		{
			for (final Territory current : route.getAllTerritories())
			{
				if (!listedTerritories.contains(current))
					return result.setErrorReturnResult("Cannot move outside restricted territories");
			}
		}
		else if (movementRestrictionType.equals("disallowed"))
		{
			for (final Territory current : route.getAllTerritories())
			{
				if (listedTerritories.contains(current))
					return result.setErrorReturnResult("Cannot move to restricted territories");
			}
		}
		return result;
	}
	
	private static MoveValidationResult validateNonEnemyUnitsOnPath(final GameData data, final Collection<Unit> units, final Route route, final PlayerID player, final MoveValidationResult result)
	{
		if (getEditMode(data))
			return result;
		// check to see no enemy units on path
		if (MoveValidator.noEnemyUnitsOnPathMiddleSteps(route, player, data))
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
		if (nonParatroopersPresent(player, units, route))
			return result.setErrorReturnResult("Enemy units on path");
		return result;
	}
	
	private static MoveValidationResult validateBasic(final boolean isNonCombat, final GameData data, final Collection<Unit> units, final Route route, final PlayerID player,
				final Collection<Unit> transportsToLoad, final MoveValidationResult result)
	{
		final boolean isEditMode = getEditMode(data);
		if (units.size() == 0)
			return result.setErrorReturnResult("No units");
		for (final Unit unit : units)
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
			for (final Unit unit : Match.getMatches(units, Matches.enemyUnit(player, data)))
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
			for (final Unit unit : Match.getMatches(moveTest, Matches.unitIsOwnedBy(player).invert()))
			{
				// allow allied fighters to move with carriers
				if (!(UnitAttachment.get(unit.getType()).getCarrierCost() > 0 && data.getRelationshipTracker().isAllied(player, unit.getOwner())))
				{
					result.addDisallowedUnit("Can only move own troops", unit);
				}
			}
			// Initialize available Mechanized Inf support
			int mechanizedSupportAvailable = getMechanizedSupportAvail(route, units, player);
			final Map<Unit, Collection<Unit>> dependencies = getDependents(Match.getMatches(units, Matches.UnitCanTransport), data);
			// add those just added
			final Map<Unit, Collection<Unit>> justLoaded = MovePanel.getDependents();
			if (!justLoaded.isEmpty())
			{
				for (final Unit transport : dependencies.keySet())
				{
					if (dependencies.get(transport).isEmpty())
						dependencies.put(transport, justLoaded.get(transport));
				}
			}
			// check units individually
			for (final Unit unit : moveTest)
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
							for (final Unit airTransport : dependencies.keySet())
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
						for (final Unit airTransport : dependencies.keySet())
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
			for (final Unit unit : MoveValidator.getUnitsThatCantGoOnWater(units))
				result.addDisallowedUnit("Not all units can end at water", unit);
		}
		// if we are water make sure no land
		if (Match.someMatch(units, Matches.UnitIsSea))
		{
			if (route.hasLand())
				for (final Unit unit : Match.getMatches(units, Matches.UnitIsSea))
					result.addDisallowedUnit("Sea units cannot go on land", unit);
		}
		// test for stack limits per unit
		if (route.getEnd() != null)
		{
			final Collection<Unit> unitsWithStackingLimits = Match.getMatches(units, new CompositeMatchOr<Unit>(Matches.UnitHasMovementLimit, Matches.UnitHasAttackingLimit));
			for (final Territory t : route.getSteps())
			{
				final Collection<Unit> unitsAllowedSoFar = new ArrayList<Unit>();
				if (Matches.isTerritoryEnemyAndNotUnownedWater(player, data).match(t) || t.getUnits().someMatch(Matches.unitIsEnemyOf(data, player)))
				{
					for (final Unit unit : unitsWithStackingLimits)
					{
						final UnitType ut = unit.getType();
						int maxAllowed = UnitAttachment.getMaximumNumberOfThisUnitTypeToReachStackingLimit("attackingLimit", ut, t, player, data);
						maxAllowed -= Match.countMatches(unitsAllowedSoFar, Matches.unitIsOfType(ut));
						if (maxAllowed > 0)
							unitsAllowedSoFar.add(unit);
						else
							result.addDisallowedUnit("UnitType " + ut.getName() + " has reached stacking limit", unit);
					}
					if (!PlayerAttachment.getCanTheseUnitsMoveWithoutViolatingStackingLimit("attackingLimit", units, t, player, data))
						return result.setErrorReturnResult("Units Can Not Go Over Stacking Limit");
				}
				else
				{
					for (final Unit unit : unitsWithStackingLimits)
					{
						final UnitType ut = unit.getType();
						int maxAllowed = UnitAttachment.getMaximumNumberOfThisUnitTypeToReachStackingLimit("movementLimit", ut, t, player, data);
						maxAllowed -= Match.countMatches(unitsAllowedSoFar, Matches.unitIsOfType(ut));
						if (maxAllowed > 0)
							unitsAllowedSoFar.add(unit);
						else
							result.addDisallowedUnit("UnitType " + ut.getName() + " has reached stacking limit", unit);
					}
					if (!PlayerAttachment.getCanTheseUnitsMoveWithoutViolatingStackingLimit("movementLimit", units, t, player, data))
						return result.setErrorReturnResult("Units Can Not Go Over Stacking Limit");
				}
			}
		}
		// don't allow move through impassible territories
		if (!isEditMode && route.someMatch(Matches.TerritoryIsImpassable))
			return result.setErrorReturnResult(CANT_MOVE_THROUGH_IMPASSIBLE);
		if (canCrossNeutralTerritory(data, route, player, result).getError() != null)
			return result;
		if (isNeutralsImpassable(data) && !isNeutralsBlitzable(data) && !route.getMatches(Matches.TerritoryIsNeutralButNotWater).isEmpty())
			return result.setErrorReturnResult(CANNOT_VIOLATE_NEUTRALITY);
		return result;
	}
	
	private static MoveValidationResult validateAirCanLand(final GameData data, final Collection<Unit> units, final Route route, final PlayerID player, final MoveValidationResult result)
	{
		// First check if we even need to check
		if (getEditMode(data) || // Edit Mode, no need to check
					!Match.someMatch(units, Matches.UnitIsAir) || // No Airunits, nothing to check
					route.hasNoSteps() || // if there are no steps, we didn't move, so it is always OK!
					Matches.airCanLandOnThisAlliedNonConqueredNonPendingLandTerritory(player, data).match(route.getEnd()) || // we can land at the end, nothing left to check
					isKamikazeAircraft(data) // we do not do any validation at all, cus they can all die and we don't care
		)
			return result;
		// Find which aircraft cannot find friendly land to land on
		final Collection<Unit> ownedAirThatMustLandOnCarriers = getAirThatMustLandOnCarriers(data, getAirUnitsToValidate(units, route, player), route, result, player);
		if (ownedAirThatMustLandOnCarriers.isEmpty())
			return result; // we are done, everything can find a place to land
			
		final Territory routeEnd = route.getEnd();
		final Territory routeStart = route.getStart();
		// we can not forget to account for allied air at our location already
		final Match<Unit> airAlliedNotOwned = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player).invert(), Matches.isUnitAllied(player, data), Matches.UnitIsAir, Matches.UnitCanLandOnCarrier);
		final LinkedHashSet<Unit> airThatMustLandOnCarriers = new LinkedHashSet<Unit>();
		airThatMustLandOnCarriers.addAll(Match.getMatches(routeEnd.getUnits().getUnits(), airAlliedNotOwned));
		airThatMustLandOnCarriers.addAll(Match.getMatches(units, airAlliedNotOwned));
		// now we must see if we also need to account for units (allied cargo) that are moving with our carriers, if we have selected any carriers
		final Collection<Unit> movingCarriersAtStartLocationBeingMoved = Match.getMatches(units, Matches.UnitIsCarrier);
		if (!movingCarriersAtStartLocationBeingMoved.isEmpty())
		{
			final Map<Unit, Collection<Unit>> carrierToAlliedCargo = MoveValidator.carrierMustMoveWith(units, routeStart, data, player);
			for (final Collection<Unit> alliedAirOnCarrier : carrierToAlliedCargo.values())
			{
				airThatMustLandOnCarriers.addAll(alliedAirOnCarrier);
			}
		}
		// now we can add our owned air. we add our owned air last because it can be moved, while allied air can not be. we want the lowest movement to be validated first.
		airThatMustLandOnCarriers.addAll(ownedAirThatMustLandOnCarriers);
		
		// now we should see if the carriers we are moving with, plus the carriers already there, can handle all our air units (we check beginning and ending territories first, separately, because they are special [they include or do not include units in our selection])
		final Collection<Unit> carriersAtEnd = Match.getMatches(getFriendly(routeEnd, player, data), Matches.UnitIsCarrier);
		carriersAtEnd.addAll(movingCarriersAtStartLocationBeingMoved);
		// to keep track of all carriers, and their fighters, that have moved, so that we do not move them again.
		final Map<Unit, Collection<Unit>> movedCarriersAndTheirFighters = new HashMap<Unit, Collection<Unit>>();
		for (final Unit carrier : carriersAtEnd)
		{
			movedCarriersAndTheirFighters.put(carrier, new ArrayList<Unit>(airThatMustLandOnCarriers));
		}
		
		airThatMustLandOnCarriers.removeAll(whatAirCanLandOnTheseCarriers(carriersAtEnd, airThatMustLandOnCarriers, routeEnd));
		if (airThatMustLandOnCarriers.isEmpty())
			return result;
		// TODO: don't forget the end of the route may be beside a factory....
		// we still have air left, so begin calling carriers to come here to pick up the air
		final int maxMovementLeftForTheseAirUnitsBeingValidated = maxMovementLeftForTheseAirUnitsBeingValidated(airThatMustLandOnCarriers, route, player); // figure out what is the max distance of our remaining air units
		final int maxMovementLeftForAllOwnedCarriers = maxMovementLeftForAllOwnedCarriers(player, data); // figure out what is the max distance of our remaining carrier units
		final List<Territory> landingSpots = new ArrayList<Territory>(data.getMap().getNeighbors(routeEnd, maxMovementLeftForTheseAirUnitsBeingValidated, Matches.seaCanMoveOver(player, data)));
		landingSpots.add(routeEnd);
		// landingSpots.remove(routeEnd);
		Collections.sort(landingSpots, getLowestToHighestDistance(routeEnd, Matches.seaCanMoveOver(player, data)));
		List<Territory> potentialCarrierOrigins = new ArrayList<Territory>(data.getMap().getNeighbors(routeEnd,
					maxMovementLeftForTheseAirUnitsBeingValidated + maxMovementLeftForAllOwnedCarriers, Matches.seaCanMoveOver(player, data)));
		potentialCarrierOrigins.remove(routeEnd);
		potentialCarrierOrigins = Match.getMatches(potentialCarrierOrigins, Matches.TerritoryHasOwnedCarrier(player));
		/*
		final Map<Unit, Territory> potentialCarriersForMoving = getAllCarriersThatCanMoveToPickUpAir(potentialCarrierOrigins, player);
		for (final Unit u : carriersAtEnd)
		{
			potentialCarriersForMoving.remove(u);
		}
		final Map<Unit, Territory> carriersCanMoveToCurrent = new HashMap<Unit, Territory>();
		for (final Entry<Unit, Territory> entry : potentialCarriersForMoving.entrySet())
		{
			if (Matches.UnitHasEnoughMovementForRoute(data.getMap().getRoute(entry.getValue(), routeEnd, Matches.seaCanMoveOver(player, data))).match(entry.getKey()))
				carriersCanMoveToCurrent.put(entry.getKey(), entry.getValue());
		}
		for (final Unit u : carriersCanMoveToCurrent.keySet())
		{
			potentialCarriersForMoving.remove(u);
		}
		// movedCarriers.addAll(carriersCanMoveToCurrent);
		airThatMustLandOnCarriers.removeAll(whatAirCanLandOnTheseCarriers(carriersCanMoveToCurrent, airThatMustLandOnCarriers, routeEnd));
		if (airThatMustLandOnCarriers.isEmpty())
			return result;
		*/

		/*
		 * Here's where we see if we have carriers available to land.
		 */
		final Set<Unit> movedCarriers = new HashSet<Unit>(); // this set of units tracks which carriers are already marked as moved to catch fighters in the air.
		final IntegerMap<Territory> usedCarrierSpace = new IntegerMap<Territory>(); // this map of territories tracks how much carrierspace in each territory we already used up.
		for (final Unit unit : airThatMustLandOnCarriers)
		{
			if (!findCarrierToLand(data, player, unit, route, usedCarrierSpace, movedCarriers))
			{
				result.addDisallowedUnit(NOT_ALL_AIR_UNITS_CAN_LAND, unit);
			}
		}
		return result;
	}
	
	/*private static Map<Unit, Territory> getAllCarriersThatCanMoveToPickUpAir(final List<Territory> territories, final PlayerID player)
	{
		final Map<Unit, Territory> carriers = new HashMap<Unit, Territory>();
		final Match<Unit> ownedCarrier = new CompositeMatchAnd<Unit>(Matches.UnitIsCarrier, Matches.unitIsOwnedBy(player));
		for (final Territory t : territories)
		{
			for (final Unit u : t.getUnits().getMatches(ownedCarrier))
			{
				carriers.put(u, t);
			}
		}
		return carriers;
	}*/

	private static IntegerMap<Territory> populateStaticAlliedAndBuildingCarrierCapacity(final List<Territory> landingSpots, final Map<Unit, Collection<Unit>> movedCarriersAndTheirFighters,
				final PlayerID player, final GameData data)
	{
		final IntegerMap<Territory> startingSpace = new IntegerMap<Territory>();
		final Match<Unit> carrierAlliedNotOwned = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player).invert(), Matches.isUnitAllied(player, data), Matches.UnitIsCarrier);
		// final Match<Unit> airAlliedNotOwned = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player).invert(), Matches.isUnitAllied(player, data), Matches.UnitIsAir, Matches.UnitCanLandOnCarrier);
		final boolean landAirOnNewCarriers = AirThatCantLandUtil.isLHTRCarrierProduction(data) || AirThatCantLandUtil.isLandExistingFightersOnNewCarriers(data);
		// final boolean areNeutralsPassableByAir = areNeutralsPassableByAir(data);
		final List<Unit> carriersInProductionQueue = player.getUnits().getMatches(Matches.UnitIsCarrier);
		for (final Territory t : landingSpots)
		{
			if (landAirOnNewCarriers && !carriersInProductionQueue.isEmpty())
			{
				if (Matches.territoryHasOwnedIsFactoryOrCanProduceUnitsNeighbor(data, player).match(t))
				{
					// TODO: Here we are assuming that this factory can produce all of the carriers. Actually it might not be able to produce any carriers (because of complex requires units coding) or because of unit damage or maximum production.
					// TODO: Here we are also assuming that the first territory we find that has an adjacent factory is the closest one in terms of unit movement. We have sorted the list of territories so this IS the closest in terms of steps, but each unit may have specific movement allowances for different terrain or some bullshit like that.
					final int producedCarrierCapacity = carrierCapacity(carriersInProductionQueue, t);
					startingSpace.add(t, producedCarrierCapacity);
					carriersInProductionQueue.clear();
				}
			}
			final Collection<Unit> alliedCarriers = t.getUnits().getMatches(carrierAlliedNotOwned);
			alliedCarriers.removeAll(movedCarriersAndTheirFighters.keySet());
			// Collection<Unit> alliedAir = t.getUnits().getMatches(airAlliedNotOwned);
			final int alliedCarrierCapacity = carrierCapacity(alliedCarriers, t);
			startingSpace.add(t, alliedCarrierCapacity);
		}
		return startingSpace;
	}
	
	private static void populateMovingCarriersAndOwnedAndAlliedAir(final IntegerMap<Territory> landingSpotsWithCarrierCapacity, final List<Territory> landingSpots,
				final List<Territory> potentialCarrierOrigins, final Map<Unit, Collection<Unit>> movedCarriersAndTheirFighters, final PlayerID player, final GameData data)
	{
		final Match<Unit> ownedCarrierMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsCarrier);
		final Match<Unit> ownedAirMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsAir, Matches.UnitCanLandOnCarrier);
		final Match<Unit> alliedNotOwnedAirMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player).invert(), Matches.isUnitAllied(player, data), Matches.UnitIsAir,
					Matches.UnitCanLandOnCarrier);
		for (final Territory landingSpot : landingSpots)
		{
			final Iterator<Territory> iter = potentialCarrierOrigins.iterator();
			while (iter.hasNext())
			{
				final Territory carrierSpot = iter.next();
				final Collection<Unit> unitsInCarrierSpot = carrierSpot.getUnits().getUnits();
				unitsInCarrierSpot.removeAll(movedCarriersAndTheirFighters.keySet());
				for (final Collection<Unit> ftrs : movedCarriersAndTheirFighters.values())
				{
					unitsInCarrierSpot.removeAll(ftrs);
				}
				final Collection<Unit> ownedCarriers = Match.getMatches(unitsInCarrierSpot, ownedCarrierMatch);
				final Collection<Unit> ownedAir = Match.getMatches(unitsInCarrierSpot, ownedAirMatch);
				final Collection<Unit> alliedNotOwnedAir = Match.getMatches(unitsInCarrierSpot, alliedNotOwnedAirMatch);
				final Map<Unit, Collection<Unit>> mustMoveWithMap = carrierMustMoveWith(ownedCarriers, carrierSpot, data, player);
			}
		}
	}
	
	public static Comparator<Territory> getLowestToHighestDistance(final Territory territoryWeMeasureDistanceFrom, final Match<Territory> condition)
	{
		return new Comparator<Territory>()
		{
			public int compare(final Territory t1, final Territory t2)
			{
				if (t1.equals(t2))
					return 0;
				final GameMap map = t1.getData().getMap();
				final int distance1 = map.getDistance(territoryWeMeasureDistanceFrom, t1, condition);
				final int distance2 = map.getDistance(territoryWeMeasureDistanceFrom, t2, condition);
				if (distance1 == distance2)
					return 0;
				if (distance1 < 0)
					return 1;
				if (distance2 < 0)
					return -1;
				if (distance1 < distance2)
					return -1;
				return 1;
			}
		};
	}
	
	private static int maxMovementLeftForAllOwnedCarriers(final PlayerID player, final GameData data)
	{
		int max = 0;
		final Match<Unit> ownedCarrier = new CompositeMatchAnd<Unit>(Matches.UnitIsCarrier, Matches.unitIsOwnedBy(player));
		for (final Territory t : data.getMap().getTerritories())
		{
			for (final Unit carrier : t.getUnits().getMatches(ownedCarrier))
			{
				max = Math.max(max, ((TripleAUnit) carrier).getMovementLeft());
			}
		}
		return max;
	}
	
	private static int maxMovementLeftForTheseAirUnitsBeingValidated(final Set<Unit> airUnits, final Route route, final PlayerID player)
	{
		int max = 0;
		for (final Unit u : airUnits)
		{
			if (Matches.unitIsOwnedBy(player).match(u))
			{
				final Territory currentSpot = route.getEnd();
				// unit must be in either start or end.
				int movementLeft;
				if (currentSpot.getUnits().getUnits().contains(u))
					movementLeft = (new Route(currentSpot)).getMovementLeft(u);
				else
					movementLeft = route.getMovementLeft(u);
				if (movementLeft > max)
					max = movementLeft;
			}
			// allied units can't move....
		}
		return max;
	}
	
	private static Collection<Unit> whatAirCanLandOnTheseCarriers(final Collection<Unit> carriers, final Set<Unit> airUnits, final Territory territoryUnitsAreIn)
	{
		final Collection<Unit> airThatCanLandOnThem = new ArrayList<Unit>();
		for (final Unit carrier : carriers)
		{
			int carrierCapacity = carrierCapacity(carrier, territoryUnitsAreIn);
			for (final Unit air : airUnits)
			{
				if (airThatCanLandOnThem.contains(air))
					continue;
				final int airCost = carrierCost(air);
				if (carrierCapacity >= airCost)
				{
					carrierCapacity -= airCost;
					airThatCanLandOnThem.add(air);
				}
			}
		}
		return airThatCanLandOnThem;
	}
	
	/**
	 * @param units
	 *            the units flying this route
	 * @param route
	 *            the route flown
	 * @param player
	 *            the player owning the units
	 * @return the combination of units that fly here and the existing owned units
	 */
	private static Collection<Unit> getAirUnitsToValidate(final Collection<Unit> units, final Route route, final PlayerID player)
	{
		final Match<Unit> ownedAirMatch = new CompositeMatchAnd<Unit>(Matches.UnitIsAir, Matches.unitOwnedBy(player), Matches.UnitIsKamikaze.invert());
		final ArrayList<Unit> ownedAir = new ArrayList<Unit>();
		ownedAir.addAll(Match.getMatches(units, ownedAirMatch));
		ownedAir.addAll(Match.getMatches(route.getEnd().getUnits().getUnits(), ownedAirMatch));
		// sort the list by shortest range first so those birds will get first pick of landingspots
		Collections.sort(ownedAir, UnitComparator.getIncreasingMovementComparator());
		return ownedAir;
	}
	
	private static boolean findCarrierToLand(final GameData data, final PlayerID player, final Unit unit, final Route route, final IntegerMap<Territory> usedCarrierSpace, final Set<Unit> movedCarriers)
	{
		final Territory currentSpot = route.getEnd();
		// unit must be in either start or end.
		int movementLeft;
		if (currentSpot.getUnits().getUnits().contains(unit))
			movementLeft = (new Route(currentSpot)).getMovementLeft(unit);
		else
			movementLeft = route.getMovementLeft(unit);
		if (movementLeft < 0)
			movementLeft = 0;
		final UnitAttachment ua = UnitAttachment.get(unit.getType());
		final boolean landAirOnNewCarriers = AirThatCantLandUtil.isLHTRCarrierProduction(data) || AirThatCantLandUtil.isLandExistingFightersOnNewCarriers(data);
		final boolean areNeutralsPassableByAir = areNeutralsPassableByAir(data);
		final List<Unit> carriersInProductionQueue = player.getUnits().getMatches(Matches.UnitIsCarrier);
		// Find Water territories I can reach
		final List<Territory> landingSpotsToCheck = Match.getMatches((data.getMap().getNeighbors(currentSpot, movementLeft + 1)), Matches.TerritoryIsWater);
		if (currentSpot.isWater())
			landingSpotsToCheck.add(currentSpot);
		
		for (final Territory landingSpot : landingSpotsToCheck)
		{
			// check if I can find a legal route to these spots
			if (!canAirReachThisSpot(data, player, unit, currentSpot, movementLeft, landingSpot, areNeutralsPassableByAir))
			{
				continue;
			}
			// can reach this spot, is there space?
			int capacity = carrierCapacity(getFriendly(landingSpot, player, data), landingSpot);
			capacity -= carrierCost(getFriendly(landingSpot, player, data)); // remove space of planes in the territory
			capacity -= usedCarrierSpace.getInt(landingSpot); // remove already claimed space by planes that wil move to the territory
			capacity -= ua.getCarrierCost();
			if (capacity >= 0)
			{
				final int newUsedCapacity = usedCarrierSpace.getInt(landingSpot) + ua.getCarrierCost();
				usedCarrierSpace.put(landingSpot, newUsedCapacity); // claim carrier space
				return true;
			}
			// take into account landingspots on carriers in the building queue
			if (landAirOnNewCarriers && !carriersInProductionQueue.isEmpty())
			{
				if (placeOnBuiltCarrier(unit, landingSpot, usedCarrierSpace, carriersInProductionQueue, data, player))
				{
					return true; // TODO FIXME if you move 2 fighter groups to 2 seaspots near a factory it will allow you to do it even though there is only room for 1 fightergroup
				}
			}
		}
		/*
		 * After all spots are checked and we can't find a good spot, we will check them again
		 * but look further to see if we can find a friendly carrier that can reach us
		 */
		for (final Territory landingSpot : landingSpotsToCheck)
		{
			if (!canAirReachThisSpot(data, player, unit, currentSpot, movementLeft, landingSpot, areNeutralsPassableByAir))
			{
				continue;
			}
			final Set<Territory> territoriesToCheckForCarriersThatCanMove = data.getMap().getNeighbors(currentSpot, 3, Matches.TerritoryIsWater);
			for (final Territory carrierSpot : territoriesToCheckForCarriersThatCanMove)
			{
				int capacity = carrierCapacity(getFriendly(carrierSpot, player, data), carrierSpot);
				capacity -= carrierCost(getFriendly(landingSpot, player, data)); // remove space of planes in the territory
				capacity -= usedCarrierSpace.getInt(carrierSpot); // remove already claimed space...
				if (capacity >= ua.getCarrierCost()) // there is enough free capacity in that zone, see if we can move a carrier to the landing spot
				{
					Collection<Unit> carriers = Match.getMatches(carrierSpot.getUnits().getUnits(), Matches.carrierOwnedBy(player));
					final Route carrierRoute = data.getMap().getRoute(carrierSpot, landingSpot);
					carriers = Match.getMatches(carriers, Matches.UnitHasEnoughMovementForRoute(carrierRoute));
					carriers.remove(movedCarriers); // remove carriers that have already moved, they can't move again to reach our landingspot
					for (final Unit carrierCandidate : carriers)
					{
						final UnitAttachment cua = UnitAttachment.get(carrierCandidate.getType());
						final int dependantCost = carrierCost(((TripleAUnit) carrierCandidate).getDependents());
						if (cua.getCarrierCapacity() >= ua.getCarrierCost() + dependantCost) // move this carrier
						{
							final int newUsedCapacityInCarrierSpot = usedCarrierSpace.getInt(carrierSpot) + cua.getCarrierCapacity() - dependantCost; // virtually remove the carrier as if it is full for future checks on this territory
							usedCarrierSpace.put(carrierSpot, newUsedCapacityInCarrierSpot);
							final int newUsedCapacityInLandingSpot = usedCarrierSpace.getInt(landingSpot) + ua.getCarrierCost() - cua.getCarrierCapacity() + dependantCost;
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
	
	private static boolean canAirReachThisSpot(final GameData data, final PlayerID player, final Unit unit, final Territory currentSpot, final int movementLeft, final Territory landingSpot,
				final boolean areNeutralsPassableByAir)
	{
		if (areNeutralsPassableByAir)
		{
			final Route neutralViolatingRoute = data.getMap().getRoute(currentSpot, landingSpot, Matches.airCanFlyOver(player, data, areNeutralsPassableByAir));
			return (neutralViolatingRoute != null && neutralViolatingRoute.getMovementCost(unit) <= movementLeft && getNeutralCharge(data, neutralViolatingRoute) <= player.getResources().getQuantity(
						Constants.PUS));
		}
		else
		{
			final Route noNeutralRoute = data.getMap().getRoute(currentSpot, landingSpot, Matches.airCanFlyOver(player, data, areNeutralsPassableByAir));
			return (noNeutralRoute != null && noNeutralRoute.getMovementCost(unit) <= movementLeft);
		}
	}
	
	private static boolean placeOnBuiltCarrier(final Unit airUnit, final Territory landingSpot, final IntegerMap<Territory> usedCarrierSpace, final List<Unit> carriersInQueue, final GameData data,
				final PlayerID player)
	{
		if (!Matches.territoryHasOwnedIsFactoryOrCanProduceUnitsNeighbor(data, player).match(landingSpot))
			return false;
		// TODO EW: existing bug -- can this factory actually produce carriers? ie: shipyards vs. factories
		for (final Unit carrierCandidate : carriersInQueue)
		{
			final UnitAttachment aua = UnitAttachment.get(airUnit.getType());
			final UnitAttachment cua = UnitAttachment.get(carrierCandidate.getType());
			if (cua.getCarrierCapacity() >= aua.getCarrierCost())
			{
				// TODO EW: is this the wisest carrier-choice? improve by picking the smartest carrier
				/*
				 * add leftover capacity to the usedCarrierSpace because the carrier will virtually be placed in this spot!
				 * if I don't do this and remove the carrier from the queue the carrier could be built in multiple spots.
				 */
				final int newUsedCapacity = usedCarrierSpace.getInt(landingSpot) + aua.getCarrierCost() - cua.getCarrierCapacity();
				usedCarrierSpace.put(landingSpot, newUsedCapacity);
				// remove the Carrier from the Queue so it can't be placed somewhere else
				carriersInQueue.remove(carrierCandidate);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Can this airunit reach safe land at this point in the route?
	 * 
	 * @param data
	 * @param unit
	 *            the airunit in question
	 * @param route
	 *            the current spot from which he needs to reach safe land.
	 * @return whether the air-unit can find a stretch of friendly land to land on given her current spot and the remaining range.
	 */
	private static boolean canFindLand(final GameData data, final Unit unit, final Route route)
	{
		final Territory routeEnd = route.getEnd();
		// unit must be in either start or end.
		int movementLeft;
		if (routeEnd.getUnits().getUnits().contains(unit))
			movementLeft = (new Route(routeEnd)).getMovementLeft(unit);
		else
			movementLeft = route.getMovementLeft(unit);
		return canFindLand(data, unit, routeEnd, movementLeft);
	}
	
	private static boolean canFindLand(final GameData data, final Unit unit, final Territory current)
	{
		final int movementLeft = ((TripleAUnit) unit).getMovementLeft();
		return canFindLand(data, unit, current, movementLeft);
	}
	
	private static boolean canFindLand(final GameData data, final Unit unit, final Territory current, final int movementLeft)
	{
		if (movementLeft <= 0)
			return false;
		final boolean areNeutralsPassableByAir = areNeutralsPassableByAir(data);
		final PlayerID player = unit.getOwner();
		final List<Territory> possibleSpots = Match.getMatches(data.getMap().getNeighbors(current, movementLeft), Matches.airCanLandOnThisAlliedNonConqueredNonPendingLandTerritory(player, data));
		for (final Territory landingSpot : possibleSpots)
		{ // TODO EW: Assuming movement cost of 1, this could get VERY slow when the movementcost is very high and airunits have a lot of movementcapacity.
			if (canAirReachThisSpot(data, player, unit, current, movementLeft, landingSpot, areNeutralsPassableByAir))
				return true;
		}
		return false;
	}
	
	/**
	 * Returns true if the given air units can land in the
	 * given territory.
	 * Does not take into account whether a battle has been
	 * fought in the territory already.
	 * 
	 * Note units must only be air units
	 */
	public static boolean canLand(final Collection<Unit> airUnits, final Territory territory, final PlayerID player, final GameData data)
	{
		if (!Match.allMatch(airUnits, Matches.UnitIsAir))
			throw new IllegalArgumentException("can only test if air will land");
		if (!territory.isWater() && MoveDelegate.getBattleTracker(data).wasConquered(territory))
			return false;
		if (territory.isWater())
		{
			// if they cant all land on carriers
			if (!Match.allMatch(airUnits, Matches.UnitCanLandOnCarrier))
				return false;
			// when doing the calculation, make sure to include the units
			// in the territory
			final Set<Unit> friendly = new HashSet<Unit>();
			friendly.addAll(getFriendly(territory, player, data));
			friendly.addAll(airUnits);
			// make sure we have the carrier capacity
			final int capacity = carrierCapacity(friendly, territory);
			final int cost = carrierCost(friendly);
			return capacity >= cost;
		}
		else
		{
			return data.getRelationshipTracker().canLandAirUnitsOnOwnedLand(player, territory.getOwner());
		}
	}
	
	private static Collection<Unit> getAirThatMustLandOnCarriers(final GameData data, final Collection<Unit> ownedAir, final Route route, final MoveValidationResult result, final PlayerID player)
	{
		final Collection<Unit> airThatMustLandOnCarriers = new ArrayList<Unit>();
		final Match<Unit> canLandOnCarriers = Matches.UnitCanLandOnCarrier;
		for (final Unit unit : ownedAir)
		{
			if (!canFindLand(data, unit, route))
			{
				if (canLandOnCarriers.match(unit))
					airThatMustLandOnCarriers.add(unit);
				else
					result.addDisallowedUnit(NOT_ALL_AIR_UNITS_CAN_LAND, unit); // not everything can land on a carrier (i.e. bombers)
			}
		}
		return airThatMustLandOnCarriers;
	}
	
	/**
	 * Does not, and is not supposed to, account for any units already on this carrier (like allied/cargo fighters).
	 * Instead this method only adds up the total capacity of each unit, and accounts for damaged carriers with special properties and restrictions.
	 * 
	 * @param units
	 * @param territory
	 * @return
	 */
	public static int carrierCapacity(final Collection<Unit> units, final Territory territoryUnitsAreCurrentlyIn)
	{
		int sum = 0;
		for (final Unit unit : units)
		{
			sum += carrierCapacity(unit, territoryUnitsAreCurrentlyIn);
		}
		return sum;
	}
	
	/**
	 * Does not, and is not supposed to, account for any units already on this carrier (like allied/cargo fighters).
	 * Instead this method only adds up the total capacity of each unit, and accounts for damaged carriers with special properties and restrictions.
	 * 
	 * @param unit
	 * @param territoryUnitsAreCurrentlyIn
	 * @return
	 */
	public static int carrierCapacity(final Unit unit, final Territory territoryUnitsAreCurrentlyIn)
	{
		if (Matches.UnitIsCarrier.match(unit))
		{
			// here we check to see if the unit can no longer carry units
			if (Matches.UnitHasWhenCombatDamagedEffect(UnitAttachment.UNITSMAYNOTLANDONCARRIER).match(unit))
			{
				// and we must check to make sure we let any allied air that are cargo stay here
				if (Matches.UnitHasWhenCombatDamagedEffect(UnitAttachment.UNITSMAYNOTLEAVEALLIEDCARRIER).match(unit))
				{
					int cargo = 0;
					final Collection<Unit> airCargo = territoryUnitsAreCurrentlyIn.getUnits().getMatches(new CompositeMatchAnd<Unit>(Matches.UnitIsAir, Matches.UnitCanLandOnCarrier));
					for (final Unit airUnit : airCargo)
					{
						final TripleAUnit taUnit = (TripleAUnit) airUnit;
						if (taUnit.getTransportedBy() != null && taUnit.getTransportedBy().equals(unit))
						{
							cargo += UnitAttachment.get(taUnit.getType()).getCarrierCost(); // capacity = are cargo only
						}
					}
					return cargo;
				}
				else
					return 0; // capacity = zero 0
			}
			else
			{
				final UnitAttachment ua = UnitAttachment.get(unit.getType());
				return ua.getCarrierCapacity();
			}
		}
		return 0;
	}
	
	public static int carrierCost(final Collection<Unit> units)
	{
		int sum = 0;
		for (final Unit unit : units)
		{
			sum += carrierCost(unit);
		}
		return sum;
	}
	
	public static int carrierCost(final Unit unit)
	{
		if (Matches.UnitCanLandOnCarrier.match(unit))
		{
			return UnitAttachment.get(unit.getType()).getCarrierCost();
		}
		return 0;
	}
	
	private static boolean areNeutralsPassableByAir(final GameData data)
	{
		return (games.strategy.triplea.Properties.getNeutralFlyoverAllowed(data) && !isNeutralsImpassable(data));
	}
	
	/**
	 * Tests the given collection of units to see if they have the movement necessary
	 * to move.
	 * 
	 * @deprecated use: Match.allMatch(units, Matches.UnitHasEnoughMovementForRoute(route));
	 * @arg alreadyMoved maps Unit -> movement
	 */
	@Deprecated
	public static boolean hasEnoughMovement(final Collection<Unit> units, final Route route)
	{
		return Match.allMatch(units, Matches.UnitHasEnoughMovementForRoute(route));
	}
	
	/**
	 * @param route
	 */
	private static int getMechanizedSupportAvail(final Route route, final Collection<Unit> units, final PlayerID player)
	{
		int mechanizedSupportAvailable = 0;
		if (isMechanizedInfantry(player))
		{
			final CompositeMatch<Unit> transportLand = new CompositeMatchAnd<Unit>(Matches.UnitIsLandTransport, Matches.unitIsOwnedBy(player));
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
	public static Map<Unit, Collection<Unit>> getDependents(final Collection<Unit> units, final GameData data)
	{
		// just worry about transports
		final TransportTracker tracker = new TransportTracker();
		final Map<Unit, Collection<Unit>> dependents = new HashMap<Unit, Collection<Unit>>();
		final Iterator<Unit> iter = units.iterator();
		while (iter.hasNext())
		{
			final Unit unit = iter.next();
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
	public static boolean hasEnoughMovement(final Collection<Unit> units, final int length)
	{
		return Match.allMatch(units, Matches.UnitHasEnoughMovement(length));
	}
	
	/**
	 * Tests the given unit to see if it has the movement necessary
	 * to move.
	 * 
	 * @deprecated use: Matches.UnitHasEnoughMovementForRoute(Route route).match(unit);
	 * @arg alreadyMoved maps Unit -> movement
	 */
	@Deprecated
	public static boolean hasEnoughMovement(final Unit unit, final Route route)
	{
		return Matches.UnitHasEnoughMovementForRoute(route).match(unit);
	}
	
	/**
	 * @deprecated use: Matches.UnitHasEnoughMovementForRoute(Route route).match(unit);
	 */
	@Deprecated
	public static boolean hasEnoughMovement(final Unit unit, final int length)
	{
		return Matches.UnitHasEnoughMovement(length).match(unit);
	}
	
	/**
	 * Checks that there are no enemy units on the route except possibly at the end.
	 * Submerged enemy units are not considered as they don't affect
	 * movement.
	 * AA and factory dont count as enemy.
	 */
	public static boolean noEnemyUnitsOnPathMiddleSteps(final Route route, final PlayerID player, final GameData data)
	{
		final CompositeMatch<Unit> alliedOrNonCombat = new CompositeMatchOr<Unit>(Matches.UnitIsFactoryOrIsInfrastructure, Matches.enemyUnit(player, data).invert(), Matches.unitIsSubmerged(data));
		// Submerged units do not interfere with movement
		for (final Territory current : route.getMiddleSteps())
		{
			if (!current.getUnits().allMatch(alliedOrNonCombat))
				return false;
		}
		return true;
	}
	
	/**
	 * Checks that there only transports, subs and/or allies on the route except at the end.
	 * AA and factory dont count as enemy.
	 */
	public static boolean onlyIgnoredUnitsOnPath(final Route route, final PlayerID player, final GameData data, final boolean ignoreRouteEnd)
	{
		final CompositeMatch<Unit> subOnly = new CompositeMatchOr<Unit>(Matches.UnitIsFactoryOrIsInfrastructure, Matches.UnitIsSub, Matches.enemyUnit(player, data).invert());
		final CompositeMatch<Unit> transportOnly = new CompositeMatchOr<Unit>(Matches.UnitIsFactoryOrIsInfrastructure, Matches.UnitIsTransportButNotCombatTransport, Matches.UnitIsLand,
					Matches.enemyUnit(player, data).invert());
		final CompositeMatch<Unit> transportOrSubOnly = new CompositeMatchOr<Unit>(Matches.UnitIsFactoryOrIsInfrastructure, Matches.UnitIsTransportButNotCombatTransport, Matches.UnitIsLand,
					Matches.UnitIsSub, Matches.enemyUnit(player, data).invert());
		final boolean getIgnoreTransportInMovement = isIgnoreTransportInMovement(data);
		final boolean getIgnoreSubInMovement = isIgnoreSubInMovement(data);
		boolean validMove = false;
		List<Territory> steps;
		if (ignoreRouteEnd)
		{
			steps = route.getMiddleSteps();
		}
		else
		{
			steps = route.getSteps();
		}
		for (final Territory current : steps)
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
	
	public static boolean enemyDestroyerOnPath(final Route route, final PlayerID player, final GameData data)
	{
		final Match<Unit> enemyDestroyer = new CompositeMatchAnd<Unit>(Matches.UnitIsDestroyer, Matches.enemyUnit(player, data));
		for (final Territory current : route.getMiddleSteps())
		{
			if (current.getUnits().someMatch(enemyDestroyer))
				return true;
		}
		return false;
	}
	
	private static boolean getEditMode(final GameData data)
	{
		return EditDelegate.getEditMode(data);
	}
	
	public static boolean hasConqueredNonBlitzedNonWaterOnRoute(final Route route, final GameData data)
	{
		for (final Territory current : route.getMiddleSteps())
		{
			if (!Matches.TerritoryIsWater.match(current) && MoveDelegate.getBattleTracker(data).wasConquered(current) && !MoveDelegate.getBattleTracker(data).wasBlitzed(current))
				return true;
		}
		return false;
	}
	
	private static boolean isMechanizedInfantry(final PlayerID player)
	{
		final TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
		if (ta == null)
			return false;
		return ta.getMechanizedInfantry();
	}
	
	private static boolean isParatroopers(final PlayerID player)
	{
		final TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
		if (ta == null)
			return false;
		return ta.getParatroopers();
	}
	
	/**
	 * @deprecated use: route.isUnload();
	 */
	@Deprecated
	public static boolean isUnload(final Route route)
	{
		return route.isUnload();
	}
	
	/**
	 * @deprecated use: route.isLoad();
	 * @param route
	 * @return
	 */
	@Deprecated
	public static boolean isLoad(final Route route)
	{
		return route.isLoad();
	}
	
	// TODO KEV revise these to include paratroop load/unload
	public static boolean isLoad(final Collection<Unit> units, final Route route, final GameData data, final PlayerID player)
	{
		final Map<Unit, Collection<Unit>> alreadyLoaded = mustMoveWith(units, route.getStart(), data, player);
		if (route.hasNoSteps() && alreadyLoaded.isEmpty())
			return false;
		// See if we even need to go to the trouble of checking for AirTransported units
		final boolean checkForAlreadyTransported = !route.getStart().isWater() && hasWater(route);
		if (checkForAlreadyTransported)
		{
			// TODO Leaving UnitIsTransport for potential use with amphib transports (hovercraft, ducks, etc...)
			final List<Unit> transports = Match.getMatches(units, new CompositeMatchOr<Unit>(Matches.UnitIsTransport, Matches.UnitIsAirTransport));
			final List<Unit> transportable = Match.getMatches(units, new CompositeMatchOr<Unit>(Matches.UnitCanBeTransported, Matches.UnitIsAirTransportable));
			// Check if there are transports in the group to be checked
			if (alreadyLoaded.keySet().containsAll(transports))
			{
				// Check each transportable unit -vs those already loaded.
				for (final Unit unit : transportable)
				{
					boolean found = false;
					for (final Unit transport : transports)
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
	public static boolean hasNeutralBeforeEnd(final Route route)
	{
		return route.hasNeutralBeforeEnd();
	}
	
	public static int getTransportCost(final Collection<Unit> units)
	{
		if (units == null)
			return 0;
		int cost = 0;
		final Iterator<Unit> iter = units.iterator();
		while (iter.hasNext())
		{
			final Unit item = iter.next();
			cost += UnitAttachment.get(item.getType()).getTransportCost();
		}
		return cost;
	}
	
	public static boolean validLoad(final Collection<Unit> units, final Collection<Unit> transports)
	{
		return true;
	}
	
	public static Collection<Unit> getUnitsThatCantGoOnWater(final Collection<Unit> units)
	{
		final Collection<Unit> retUnits = new ArrayList<Unit>();
		for (final Unit unit : units)
		{
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			if (!ua.getIsSea() && !ua.getIsAir() && ua.getTransportCost() == -1)
				retUnits.add(unit);
		}
		return retUnits;
	}
	
	public static boolean hasUnitsThatCantGoOnWater(final Collection<Unit> units)
	{
		return !getUnitsThatCantGoOnWater(units).isEmpty();
	}
	
	/**
	 * @deprecated use route.hasWater();
	 * @param route
	 * @return
	 */
	@Deprecated
	public static boolean hasWater(final Route route)
	{
		return route.hasWater();
	}
	
	/**
	 * @deprecated use route.hasLand();
	 * @param route
	 * @return
	 */
	@Deprecated
	public static boolean hasLand(final Route route)
	{
		return route.hasLand();
	}
	
	public static Collection<Unit> getNonLand(final Collection<Unit> units)
	{
		final CompositeMatch<Unit> match = new CompositeMatchOr<Unit>();
		match.add(Matches.UnitIsAir);
		match.add(Matches.UnitIsSea);
		return Match.getMatches(units, match);
	}
	
	public static Collection<Unit> getFriendly(final Territory territory, final PlayerID player, final GameData data)
	{
		return territory.getUnits().getMatches(Matches.alliedUnit(player, data));
	}
	
	public static int getMaxMovement(final Collection<Unit> units)
	{
		if (units.size() == 0)
			throw new IllegalArgumentException("no units");
		int max = 0;
		final Iterator<Unit> iter = units.iterator();
		while (iter.hasNext())
		{
			final Unit unit = iter.next();
			final int left = TripleAUnit.get(unit).getMovementLeft();
			max = Math.max(left, max);
		}
		return max;
	}
	
	public static int getLeastMovement(final Collection<Unit> units)
	{
		if (units.size() == 0)
			throw new IllegalArgumentException("no units");
		int least = Integer.MAX_VALUE;
		final Iterator<Unit> iter = units.iterator();
		while (iter.hasNext())
		{
			final Unit unit = iter.next();
			final int left = TripleAUnit.get(unit).getMovementLeft();
			least = Math.min(left, least);
		}
		return least;
	}
	
	public static int getTransportCapacityFree(final Territory territory, final PlayerID id, final GameData data, final TransportTracker tracker)
	{
		final Match<Unit> friendlyTransports = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport, Matches.alliedUnit(id, data));
		final Collection<Unit> transports = territory.getUnits().getMatches(friendlyTransports);
		int sum = 0;
		final Iterator<Unit> iter = transports.iterator();
		while (iter.hasNext())
		{
			final Unit transport = iter.next();
			sum += tracker.getAvailableCapacity(transport);
		}
		return sum;
	}
	
	private static boolean isWW2V2(final GameData data)
	{
		return games.strategy.triplea.Properties.getWW2V2(data);
	}
	
	private static boolean isNeutralsImpassable(final GameData data)
	{
		return games.strategy.triplea.Properties.getNeutralsImpassable(data);
	}
	
	private static boolean isNeutralsBlitzable(final GameData data)
	{
		return games.strategy.triplea.Properties.getNeutralsBlitzable(data) && !isNeutralsImpassable(data);
	}
	
	private static boolean isWW2V3(final GameData data)
	{
		return games.strategy.triplea.Properties.getWW2V3(data);
	}
	
	private static boolean isMultipleAAPerTerritory(final GameData data)
	{
		return games.strategy.triplea.Properties.getMultipleAAPerTerritory(data);
	}
	
	/**
	 * @return
	 */
	private static boolean isMovementByTerritoryRestricted(final GameData data)
	{
		return games.strategy.triplea.Properties.getMovementByTerritoryRestricted(data);
	}
	
	private static boolean IsParatroopersCanMoveDuringNonCombat(final GameData data)
	{
		return games.strategy.triplea.Properties.getParatroopersCanMoveDuringNonCombat(data);
	}
	
	private static int getNeutralCharge(final GameData data, final Route route)
	{
		return getNeutralCharge(data, MoveDelegate.getEmptyNeutral(route).size());
	}
	
	private static int getNeutralCharge(final GameData data, final int numberOfTerritories)
	{
		return numberOfTerritories * games.strategy.triplea.Properties.getNeutralCharge(data);
	}
	
	// Determines whether we can pay the neutral territory charge for a
	// given route for air units. We can't cross neutral territories
	// in WW2V2.
	private static MoveValidationResult canCrossNeutralTerritory(final GameData data, final Route route, final PlayerID player, final MoveValidationResult result)
	{
		// neutrals we will overfly in the first place
		final Collection<Territory> neutrals = MoveDelegate.getEmptyNeutral(route);
		final int PUs = player.getResources().getQuantity(Constants.PUS);
		if (PUs < getNeutralCharge(data, neutrals.size()))
			return result.setErrorReturnResult(TOO_POOR_TO_VIOLATE_NEUTRALITY);
		return result;
	}
	
	private static Territory getTerritoryTransportHasUnloadedTo(final List<UndoableMove> undoableMoves, final Unit transport)
	{
		for (final UndoableMove undoableMove : undoableMoves)
		{
			if (undoableMove.wasTransportUnloaded(transport))
			{
				return undoableMove.getRoute().getEnd();
			}
		}
		return null;
	}
	
	private static MoveValidationResult validateTransport(final GameData data, final List<UndoableMove> undoableMoves, final Collection<Unit> units, final Route route, final PlayerID player,
				final Collection<Unit> transportsToLoad, final MoveValidationResult result)
	{
		final boolean isEditMode = getEditMode(data);
		if (Match.allMatch(units, Matches.UnitIsAir))
			return result;
		if (!route.hasWater())
			return result;
		// If there are non-sea transports return
		final Boolean seaOrNoTransportsPresent = transportsToLoad.isEmpty() || Match.someMatch(transportsToLoad, new CompositeMatchAnd<Unit>(Matches.UnitIsSea, Matches.UnitCanTransport));
		if (!seaOrNoTransportsPresent)
			return result;
		/*if(!MoveValidator.isLoad(units, route, data, player) && !MoveValidator.isUnload(route))
			return result;*/
		final TransportTracker transportTracker = new TransportTracker();
		// if unloading make sure length of route is only 1
		if (!isEditMode && MoveValidator.isUnload(route))
		{
			if (route.hasMoreThenOneStep())
				return result.setErrorReturnResult("Unloading units must stop where they are unloaded");
			for (final Unit unit : transportTracker.getUnitsLoadedOnAlliedTransportsThisTurn(units))
				result.addDisallowedUnit(CANNOT_LOAD_AND_UNLOAD_AN_ALLIED_TRANSPORT_IN_THE_SAME_ROUND, unit);
			final Collection<Unit> transports = MoveDelegate.mapTransports(route, units, null).values();
			for (final Unit transport : transports)
			{
				// TODO This is very sensitive to the order of the transport collection. The users may
				// need to modify the order in which they perform their actions.
				// check whether transport has already unloaded
				if (transportTracker.hasTransportUnloadedInPreviousPhase(transport))
				{
					for (final Unit unit : transportTracker.transporting(transport))
						result.addDisallowedUnit(TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_IN_A_PREVIOUS_PHASE, unit);
				}
				// check whether transport is restricted to another territory
				else if (transportTracker.isTransportUnloadRestrictedToAnotherTerritory(transport, route.getEnd()))
				{
					final Territory alreadyUnloadedTo = getTerritoryTransportHasUnloadedTo(undoableMoves, transport);
					for (final Unit unit : transportTracker.transporting(transport))
						result.addDisallowedUnit(TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_TO + alreadyUnloadedTo.getName(), unit);
				}
				// Check if the transport has already loaded after being in combat
				else if (transportTracker.isTransportUnloadRestrictedInNonCombat(transport))
				{
					for (final Unit unit : transportTracker.transporting(transport))
						result.addDisallowedUnit(TRANSPORT_CANNOT_LOAD_AND_UNLOAD_AFTER_COMBAT, unit);
				}
			}
		}
		// if we are land make sure no water in route except for transport
		// situations
		final Collection<Unit> land = Match.getMatches(units, Matches.UnitIsLand);
		final Collection<Unit> landAndAir = Match.getMatches(units, new CompositeMatchOr<Unit>(Matches.UnitIsLand, Matches.UnitIsAir));
		// make sure we can be transported
		final Match<Unit> cantBeTransported = new InverseMatch<Unit>(Matches.UnitCanBeTransported);
		for (final Unit unit : Match.getMatches(land, cantBeTransported))
			result.addDisallowedUnit("Not all units can be transported", unit);
		// make sure that the only the first or last territory is land
		// dont want situation where they go sea land sea
		if (!isEditMode && route.hasLand() && !(route.getStart().isWater() || route.getEnd().isWater()))
		{
			// needs to include all land and air to work, since it makes sure the land units can be carried by the air and that the air has enough capacity
			if (nonParatroopersPresent(player, landAndAir, route))
			{
				return result.setErrorReturnResult("Invalid move, only start or end can be land when route has water.");
			}
		}
		// simply because I dont want to handle it yet
		// checks are done at the start and end, dont want to worry about just
		// using a transport as a bridge yet
		// TODO handle this
		if (!isEditMode && !route.getEnd().isWater() && !route.getStart().isWater() && nonParatroopersPresent(player, landAndAir, route))
			return result.setErrorReturnResult("Must stop units at a transport on route");
		if (route.getEnd().isWater() && route.getStart().isWater())
		{
			// make sure units and transports stick together
			final Iterator<Unit> iter = units.iterator();
			while (iter.hasNext())
			{
				final Unit unit = iter.next();
				final UnitAttachment ua = UnitAttachment.get(unit.getType());
				// make sure transports dont leave their units behind
				if (ua.getTransportCapacity() != -1)
				{
					final Collection<Unit> holding = transportTracker.transporting(unit);
					if (holding != null && !units.containsAll(holding))
						result.addDisallowedUnit("Transports cannot leave their units", unit);
				}
				// make sure units dont leave their transports behind
				if (ua.getTransportCost() != -1)
				{
					final Unit transport = transportTracker.transportedBy(unit);
					if (transport != null && !units.contains(transport))
						result.addDisallowedUnit("Unit must stay with its transport while moving", unit);
				}
			}
		} // end if end is water
		if (route.isLoad())
		{
			if (!isEditMode && !route.hasExactlyOneStep() && nonParatroopersPresent(player, landAndAir, route))
				return result.setErrorReturnResult("Units cannot move before loading onto transports");
			final CompositeMatch<Unit> enemyNonSubmerged = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), new InverseMatch<Unit>(Matches.unitIsSubmerged(data)));
			if (route.getEnd().getUnits().someMatch(enemyNonSubmerged) && nonParatroopersPresent(player, landAndAir, route))
				if (!onlyIgnoredUnitsOnPath(route, player, data, false))
					return result.setErrorReturnResult("Cannot load when enemy sea units are present");
			final Map<Unit, Unit> unitsToTransports = MoveDelegate.mapTransports(route, land, transportsToLoad);
			final Iterator<Unit> iter = land.iterator();
			// CompositeMatch<Unit> landUnitsAtSea = new CompositeMatchOr<Unit>(Matches.unitIsLandAndOwnedBy(player), Matches.UnitCanBeTransported);
			while (!isEditMode && iter.hasNext())
			{
				final TripleAUnit unit = (TripleAUnit) iter.next();
				if (Matches.unitHasMoved.match(unit))
					result.addDisallowedUnit("Units cannot move before loading onto transports", unit);
				final Unit transport = unitsToTransports.get(unit);
				if (transport == null)
					continue;
				if (transportTracker.hasTransportUnloadedInPreviousPhase(transport))
				{
					result.addDisallowedUnit(TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_IN_A_PREVIOUS_PHASE, unit);
				}
				else if (transportTracker.isTransportUnloadRestrictedToAnotherTerritory(transport, route.getEnd()))
				{
					Territory alreadyUnloadedTo = getTerritoryTransportHasUnloadedTo(undoableMoves, transport);
					final Iterator<Unit> trnIter = transportsToLoad.iterator();
					while (trnIter.hasNext())
					{
						final TripleAUnit trn = (TripleAUnit) trnIter.next();
						if (!transportTracker.isTransportUnloadRestrictedToAnotherTerritory(trn, route.getEnd()))
						{
							final UnitAttachment ua = UnitAttachment.get(unit.getType());
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
				final Collection<UnitCategory> unitsToLoadCategories = UnitSeperator.categorize(land);
				if (unitsToTransports.size() == 0 || unitsToLoadCategories.size() == 1)
				{
					// set all unmapped units as disallowed if there are no transports
					// or only one unit category
					for (final Unit unit : land)
					{
						if (unitsToTransports.containsKey(unit))
							continue;
						final UnitAttachment ua = UnitAttachment.get(unit.getType());
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
					for (final Unit unit : land)
					{
						final UnitAttachment ua = UnitAttachment.get(unit.getType());
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
	
	public static boolean allLandUnitsAreBeingParatroopered(final Collection<Unit> units, final Route route, final PlayerID player)
	{
		// some units that can't be paratrooped
		if (!Match.allMatch(units, new CompositeMatchOr<Unit>(Matches.UnitIsAirTransportable, Matches.UnitIsAirTransport, Matches.UnitIsAir)))
		{
			return false;
		}
		// final List<Unit> paratroopsRequiringTransport = getParatroopsRequiringTransport(units, route);
		// due to various problems with units like tanks, we will assume that if we are in this method, then all the land units need transports
		final List<Unit> paratroopsRequiringTransport = Match.getMatches(units, Matches.UnitIsAirTransportable);
		if (paratroopsRequiringTransport.isEmpty())
		{
			return false;
		}
		final List<Unit> airTransports = Match.getMatches(units, Matches.UnitIsAirTransport);
		final List<Unit> allParatroops = MoveDelegate.mapAirTransportPossibilities(route, paratroopsRequiringTransport, airTransports, false, player);
		if (!allParatroops.containsAll(paratroopsRequiringTransport))
			return false;
		final Map<Unit, Unit> transportLoadMap = MoveDelegate.mapAirTransports(route, units, airTransports, true, player);
		if (!transportLoadMap.keySet().containsAll(paratroopsRequiringTransport))
			return false;
		return true;
	}
	
	// checks if there are non-paratroopers present that cause move validations to fail
	private static boolean nonParatroopersPresent(final PlayerID player, final Collection<Unit> units, final Route route)
	{
		if (!isParatroopers(player))
		{
			return true;
		}
		if (!Match.allMatch(units, new CompositeMatchOr<Unit>(Matches.UnitIsAir, Matches.UnitIsLand)))
		{
			return true;
		}
		for (final Unit unit : Match.getMatches(units, Matches.UnitIsNotAirTransportable))
		{
			if (Matches.UnitIsLand.match(unit))
				return true;
		}
		if (!allLandUnitsAreBeingParatroopered(units, route, player))
			return true;
		return false;
	}
	
	private static List<Unit> getParatroopsRequiringTransport(final Collection<Unit> units, final Route route)
	{
		return Match.getMatches(units, new CompositeMatchAnd<Unit>(Matches.UnitIsAirTransportable, new Match<Unit>()
		{
			@Override
			public boolean match(final Unit u)
			{
				return TripleAUnit.get(u).getMovementLeft() < route.getMovementCost(u) || route.crossesWater() || route.getEnd().isWater();
			}
		}));
	}
	
	private static MoveValidationResult validateParatroops(final boolean nonCombat, final GameData data, final List<UndoableMove> undoableMoves, final Collection<Unit> units, final Route route,
				final PlayerID player, final MoveValidationResult result)
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
			final List<Unit> paratroopsRequiringTransport = getParatroopsRequiringTransport(units, route);
			if (paratroopsRequiringTransport.isEmpty())
			{
				return result;
			}
			final List<Unit> airTransports = Match.getMatches(units, Matches.UnitIsAirTransport);
			// TODO kev change below to mapAirTransports (or modify mapTransports to handle air cargo)
			// Map<Unit, Unit> airTransportsAndParatroops = MoveDelegate.mapTransports(route, paratroopsRequiringTransport, airTransports);
			final Map<Unit, Unit> airTransportsAndParatroops = MoveDelegate.mapAirTransports(route, paratroopsRequiringTransport, airTransports, true, player);
			for (final Unit paratroop : airTransportsAndParatroops.keySet())
			{
				if (Matches.unitHasMoved.match(paratroop))
				{
					result.addDisallowedUnit("Cannot paratroop units that have already moved", paratroop);
				}
				final Unit transport = airTransportsAndParatroops.get(paratroop);
				if (Matches.unitHasMoved.match(transport))
				{
					result.addDisallowedUnit("Cannot move then transport paratroops", transport);
				}
			}
			final Territory routeEnd = route.getEnd();
			for (final Unit paratroop : paratroopsRequiringTransport)
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
			if (!games.strategy.triplea.Properties.getParatroopersCanAttackDeepIntoEnemyTerritory(data) || nonCombat)
			{
				for (final Territory current : Match.getMatches(route.getMiddleSteps(), Matches.TerritoryIsLand))
				{
					if (Matches.isTerritoryEnemy(player, data).match(current))
						return result.setErrorReturnResult("Must stop paratroops in first enemy territory");
				}
			}
		}
		return result;
	}
	
	/**
	 * Test a route's canals to see if you can move through it.
	 * 
	 * @param route
	 * @param units
	 *            (Can be null. If null we will assume all units would be stopped by the canal.)
	 * @param player
	 * @param data
	 * @return
	 */
	public static String validateCanal(final Route route, final Collection<Unit> units, final PlayerID player, final GameData data)
	{
		for (final Territory routeTerritory : route.getAllTerritories())
		{
			final String result = validateCanal(routeTerritory, route, units, player, data);
			if (result != null)
				return result;
		}
		return null;
	}
	
	/**
	 * Used for testing a single territory, either as part of a route, or just by itself.
	 * 
	 * @param territory
	 * @param route
	 *            (Can be null. If not null, we will check to see if the route includes both sea zones, and if it doesn't we will not test the canal)
	 * @param units
	 *            (Can be null. If null we will assume all units would be stopped by the canal.)
	 * @param player
	 * @param data
	 * @return
	 */
	public static String validateCanal(final Territory territory, final Route route, final Collection<Unit> units, final PlayerID player, final GameData data)
	{
		final Set<CanalAttachment> canalAttachments = CanalAttachment.get(territory);
		if (canalAttachments.isEmpty())
			return null;
		final Iterator<CanalAttachment> iter = canalAttachments.iterator();
		while (iter.hasNext())
		{
			final CanalAttachment attachment = iter.next();
			if (attachment == null)
				continue;
			if (route != null)
			{
				boolean mustCheck = false;
				Territory last = null;
				final Set<Territory> connectionToCheck = CanalAttachment.getAllCanalSeaZones(attachment.getCanalName(), data);
				for (final Territory current : route.getAllTerritories())
				{
					if (last != null)
					{
						final Collection<Territory> lastTwo = new ArrayList<Territory>();
						lastTwo.add(last);
						lastTwo.add(current);
						mustCheck = lastTwo.containsAll(connectionToCheck);
						if (mustCheck)
							break;
					}
					last = current;
				}
				if (!mustCheck)
					continue;
			}
			if (units != null && Match.allMatch(units, Matches.unitIsOfTypes(attachment.getExcludedUnits(data))))
				continue;
			for (final Territory borderTerritory : attachment.getLandTerritories())
			{
				if (!data.getRelationshipTracker().canMoveThroughCanals(player, borderTerritory.getOwner()))
				{
					return "Must own " + borderTerritory.getName() + " to go through " + attachment.getCanalName();
				}
				if (MoveDelegate.getBattleTracker(data).wasConquered(borderTerritory))
				{
					return "Cannot move through " + attachment.getCanalName() + " without owning " + borderTerritory.getName() + " for an entire turn";
				}
			}
		}
		return null;
	}
	
	public static MustMoveWithDetails getMustMoveWith(final Territory start, final Collection<Unit> units, final GameData data, final PlayerID player)
	{
		return new MustMoveWithDetails(mustMoveWith(units, start, data, player));
	}
	
	private static Map<Unit, Collection<Unit>> mustMoveWith(final Collection<Unit> units, final Territory start, final GameData data, final PlayerID player)
	{
		final List<Unit> sortedUnits = new ArrayList<Unit>(units);
		Collections.sort(sortedUnits, UnitComparator.getIncreasingMovementComparator());
		final Map<Unit, Collection<Unit>> mapping = new HashMap<Unit, Collection<Unit>>();
		mapping.putAll(transportsMustMoveWith(sortedUnits));
		// Check if there are combined transports (carriers that are transports) and load them.
		if (mapping.isEmpty())
		{
			mapping.putAll(carrierMustMoveWith(sortedUnits, start, data, player));
		}
		else
		{
			final Map<Unit, Collection<Unit>> newMapping = new HashMap<Unit, Collection<Unit>>();
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
			final Map<Unit, Collection<Unit>> newMapping = new HashMap<Unit, Collection<Unit>>();
			newMapping.putAll(airTransportsMustMoveWith(sortedUnits));
			if (!newMapping.isEmpty())
				addToMapping(mapping, newMapping);
		}
		return mapping;
	}
	
	private static void addToMapping(final Map<Unit, Collection<Unit>> mapping, final Map<Unit, Collection<Unit>> newMapping)
	{
		for (final Unit key : newMapping.keySet())
		{
			if (mapping.containsKey(key))
			{
				final Collection<Unit> heldUnits = mapping.get(key);
				heldUnits.addAll(newMapping.get(key));
				mapping.put(key, heldUnits);
			}
			else
			{
				mapping.put(key, newMapping.get(key));
			}
		}
	}
	
	private static Map<Unit, Collection<Unit>> transportsMustMoveWith(final Collection<Unit> units)
	{
		final TransportTracker transportTracker = new TransportTracker();
		final Map<Unit, Collection<Unit>> mustMoveWith = new HashMap<Unit, Collection<Unit>>();
		// map transports
		final Collection<Unit> transports = Match.getMatches(units, Matches.UnitIsTransport);
		final Iterator<Unit> iter = transports.iterator();
		while (iter.hasNext())
		{
			final Unit transport = iter.next();
			final Collection<Unit> transporting = transportTracker.transporting(transport);
			mustMoveWith.put(transport, transporting);
		}
		return mustMoveWith;
	}
	
	private static Map<Unit, Collection<Unit>> airTransportsMustMoveWith(final Collection<Unit> units)
	{
		final TransportTracker transportTracker = new TransportTracker();
		final Map<Unit, Collection<Unit>> mustMoveWith = new HashMap<Unit, Collection<Unit>>();
		final Collection<Unit> airTransports = Match.getMatches(units, Matches.UnitIsAirTransport);
		Map<Unit, Collection<Unit>> selectedDependents = new HashMap<Unit, Collection<Unit>>();
		// first, check if there are any that haven't been updated yet
		for (final Unit airTransport : airTransports)
		{
			if (selectedDependents.containsKey(airTransport))
			{
				final Collection<Unit> transporting = selectedDependents.get(airTransport);
				mustMoveWith.put(airTransport, transporting);
			}
		}
		// Then check those that have already had their transportedBy set
		for (final Unit airTransport : airTransports)
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
	
	private static Map<Unit, Collection<Unit>> carrierMustMoveWith(final Collection<Unit> units, final Territory start, final GameData data, final PlayerID player)
	{
		return carrierMustMoveWith(units, start.getUnits().getUnits(), data, player);
	}
	
	public static Map<Unit, Collection<Unit>> carrierMustMoveWith(final Collection<Unit> units, final Collection<Unit> startUnits, final GameData data, final PlayerID player)
	{
		// we want to get all air units that are owned by our allies
		// but not us that can land on a carrier
		final CompositeMatch<Unit> friendlyNotOwnedAir = new CompositeMatchAnd<Unit>();
		friendlyNotOwnedAir.add(Matches.alliedUnit(player, data));
		friendlyNotOwnedAir.addInverse(Matches.unitIsOwnedBy(player));
		friendlyNotOwnedAir.add(Matches.UnitCanLandOnCarrier);
		final Collection<Unit> alliedAir = Match.getMatches(startUnits, friendlyNotOwnedAir);
		if (alliedAir.isEmpty())
			return Collections.emptyMap();
		// remove air that can be carried by allied
		final CompositeMatch<Unit> friendlyNotOwnedCarrier = new CompositeMatchAnd<Unit>();
		friendlyNotOwnedCarrier.add(Matches.UnitIsCarrier);
		friendlyNotOwnedCarrier.add(Matches.alliedUnit(player, data));
		friendlyNotOwnedCarrier.addInverse(Matches.unitIsOwnedBy(player));
		final Collection<Unit> alliedCarrier = Match.getMatches(startUnits, friendlyNotOwnedCarrier);
		final Iterator<Unit> alliedCarrierIter = alliedCarrier.iterator();
		while (alliedCarrierIter.hasNext())
		{
			final Unit carrier = alliedCarrierIter.next();
			final Collection<Unit> carrying = getCanCarry(carrier, alliedAir);
			alliedAir.removeAll(carrying);
		}
		if (alliedAir.isEmpty())
			return Collections.emptyMap();
		final Map<Unit, Collection<Unit>> mapping = new HashMap<Unit, Collection<Unit>>();
		// get air that must be carried by our carriers
		final Collection<Unit> ownedCarrier = Match.getMatches(units, new CompositeMatchAnd<Unit>(Matches.UnitIsCarrier, Matches.unitIsOwnedBy(player)));
		final Iterator<Unit> ownedCarrierIter = ownedCarrier.iterator();
		while (ownedCarrierIter.hasNext())
		{
			final Unit carrier = ownedCarrierIter.next();
			final Collection<Unit> carrying = getCanCarry(carrier, alliedAir);
			alliedAir.removeAll(carrying);
			mapping.put(carrier, carrying);
		}
		return mapping;
	}
	
	private static Collection<Unit> getCanCarry(final Unit carrier, final Collection<Unit> selectFrom)
	{
		final UnitAttachment ua = UnitAttachment.get(carrier.getUnitType());
		final Collection<Unit> canCarry = new ArrayList<Unit>();
		int available = ua.getCarrierCapacity();
		final Iterator<Unit> iter = selectFrom.iterator();
		final TripleAUnit tACarrier = (TripleAUnit) carrier;
		while (iter.hasNext())
		{
			final Unit plane = iter.next();
			final TripleAUnit tAPlane = (TripleAUnit) plane;
			final UnitAttachment planeAttachment = UnitAttachment.get(plane.getUnitType());
			final int cost = planeAttachment.getCarrierCost();
			if (available >= cost)
			{
				// this is to test if they started in the same sea zone or not, and its not a very good way of testing it.
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
	public static Route getBestRoute(final Territory start, final Territory end, final GameData data, final PlayerID player, final Collection<Unit> units)
	{
		final boolean hasLand = Match.someMatch(units, Matches.UnitIsLand);
		final boolean hasAir = Match.someMatch(units, Matches.UnitIsAir);
		// final boolean hasSea = Match.someMatch(units, Matches.UnitIsSea);
		final boolean isNeutralsImpassable = isNeutralsImpassable(data) || (hasAir && !games.strategy.triplea.Properties.getNeutralFlyoverAllowed(data));
		
		// Ignore the end territory in our tests. it must be in the route, so it shouldn't affect the route choice
		// final Match<Territory> territoryIsEnd = Matches.territoryIs(end);
		// No neutral countries on route predicate
		final Match<Territory> noNeutral = Matches.TerritoryIsNeutralButNotWater.invert();
		// No aa guns on route predicate
		final Match<Territory> noAA = Matches.territoryHasEnemyAAforAnything(player, data).invert();
		// no enemy units on the route predicate
		final Match<Territory> noEnemy = Matches.territoryHasEnemyUnits(player, data).invert();
		// no impassible or restricted territories
		final CompositeMatchAnd<Territory> noImpassible = new CompositeMatchAnd<Territory>(Matches.TerritoryIsPassableAndNotRestricted(player, data));
		// if we have air or land, we don't want to move over territories owned by players who's relationships will not let us move into them
		if (hasAir)
			noImpassible.add(Matches.TerritoryAllowsCanMoveAirUnitsOverOwnedLand(player, data));
		if (hasLand)
			noImpassible.add(Matches.TerritoryAllowsCanMoveLandUnitsOverOwnedLand(player, data));
		
		// now find the default route
		Route defaultRoute;
		if (isNeutralsImpassable)
			defaultRoute = data.getMap().getRoute_IgnoreEnd(start, end, new CompositeMatchAnd<Territory>(noNeutral, noImpassible));
		else
			defaultRoute = data.getMap().getRoute_IgnoreEnd(start, end, noImpassible);
		// since all routes require at least noImpassible, then if we can not find a route without impassibles, just return any route
		if (defaultRoute == null)
		{
			// at least try for a route without impassible territories, but allowing restricted territories, since there is a chance politics may change in the future.
			defaultRoute = data.getMap().getRoute_IgnoreEnd(start, end,
						(isNeutralsImpassable ? new CompositeMatchAnd<Territory>(noNeutral, Matches.TerritoryIsImpassable) : Matches.TerritoryIsImpassable));
			// ok, so there really is nothing, so just return any route, without conditions
			if (defaultRoute == null)
				return data.getMap().getRoute(start, end);
			return defaultRoute;
		}
		// we don't want to look at the dependents
		final Collection<Unit> unitsWhichAreNotBeingTransportedOrDependent = new ArrayList<Unit>(Match.getMatches(units,
					Matches.unitIsBeingTransportedByOrIsDependentOfSomeUnitInThisList(units, defaultRoute, player, data).invert()));
		boolean mustGoLand = false;
		boolean mustGoSea = false;
		// If start and end are land, try a land route.
		// don't force a land route, since planes may be moving
		if (!start.isWater() && !end.isWater())
		{
			Route landRoute;
			if (isNeutralsImpassable)
				landRoute = data.getMap().getRoute_IgnoreEnd(start, end, new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, noNeutral, noImpassible));
			else
				landRoute = data.getMap().getRoute_IgnoreEnd(start, end, new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, noImpassible));
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
			final Route waterRoute = data.getMap().getRoute_IgnoreEnd(start, end, new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, noImpassible));
			if (waterRoute != null && (Match.someMatch(unitsWhichAreNotBeingTransportedOrDependent, Matches.UnitIsSea)))
			{
				defaultRoute = waterRoute;
				mustGoSea = true;
			}
		}
		// these are the conditions we would like the route to satisfy, starting
		// with the most important
		List<Match<Territory>> tests;
		if (isNeutralsImpassable)
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
		for (final Match<Territory> t : tests)
		{
			Match<Territory> testMatch = null;
			if (mustGoLand)
				testMatch = new CompositeMatchAnd<Territory>(t, Matches.TerritoryIsLand, noImpassible);
			else if (mustGoSea)
				testMatch = new CompositeMatchAnd<Territory>(t, Matches.TerritoryIsWater, noImpassible);
			else
				testMatch = new CompositeMatchAnd<Territory>(t, noImpassible);
			final Route testRoute = data.getMap().getRoute_IgnoreEnd(start, end, testMatch);
			if (testRoute != null && testRoute.getLargestMovementCost(unitsWhichAreNotBeingTransportedOrDependent) <= defaultRoute.getLargestMovementCost(unitsWhichAreNotBeingTransportedOrDependent))
				return testRoute;
		}
		return defaultRoute;
	}
	
	/**
	 * @return
	 */
	private static boolean isSubmersibleSubsAllowed(final GameData data)
	{
		return games.strategy.triplea.Properties.getSubmersible_Subs(data);
	}
	
	/**
	 * @return
	 */
	private static boolean isKamikazeAircraft(final GameData data)
	{
		return games.strategy.triplea.Properties.getKamikaze_Airplanes(data);
	}
	
	/**
	 * @return
	 */
	private static boolean isAlliedAirDependents(final GameData data)
	{
		return games.strategy.triplea.Properties.getAlliedAirDependents(data);
	}
	
	/**
	 * @return
	 */
	private static boolean isIgnoreTransportInMovement(final GameData data)
	{
		return games.strategy.triplea.Properties.getIgnoreTransportInMovement(data);
	}
	
	/**
	 * @return
	 */
	private static boolean isIgnoreSubInMovement(final GameData data)
	{
		return games.strategy.triplea.Properties.getIgnoreSubInMovement(data);
	}
	
	/**
	 * @return
	 */
	private static boolean isSubControlSeaZoneRestricted(final GameData data)
	{
		return games.strategy.triplea.Properties.getSubControlSeaZoneRestricted(data);
	}
	
	/**
	 * @return
	 */
	private static boolean isTransportControlSeaZone(final GameData data)
	{
		return games.strategy.triplea.Properties.getTransportControlSeaZone(data);
	}
	
	/** Creates new MoveValidator */
	private MoveValidator()
	{
	}
}
