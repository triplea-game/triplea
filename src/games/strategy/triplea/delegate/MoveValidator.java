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

import games.strategy.common.delegate.BaseEditDelegate;
import games.strategy.engine.data.GameData;
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
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.InverseMatch;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
	public static final String TRANSPORT_MAY_NOT_UNLOAD_TO_FRIENDLY_TERRITORIES_UNTIL_AFTER_COMBAT_IS_RESOLVED = "Transport may not unload to friendly territories until after combat is resolved";
	public static final String ENEMY_SUBMARINE_PREVENTING_UNESCORTED_AMPHIBIOUS_ASSAULT_LANDING = "Enemy Submarine Preventing Unescorted Amphibious Assault Landing";
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
	
	public static MoveValidationResult validateMove(final Collection<Unit> units, final Route route, final PlayerID player, final Collection<Unit> transportsToLoad,
				final Map<Unit, Collection<Unit>> newDependents, final boolean isNonCombat, final List<UndoableMove> undoableMoves, final GameData data)
	{
		final MoveValidationResult result = new MoveValidationResult();
		if (route.hasNoSteps())
			return result;
		if (validateFirst(data, units, route, player, result).getError() != null)
		{
			return result;
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
		if (validateBasic(isNonCombat, data, units, route, player, transportsToLoad, newDependents, result).getError() != null)
			return result;
		if (AirMovementValidator.validateAirCanLand(data, units, route, player, result).getError() != null)
			return result;
		if (validateTransport(isNonCombat, data, undoableMoves, units, route, player, transportsToLoad, newDependents, result).getError() != null)
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
	
	static MoveValidationResult validateFirst(final GameData data, final Collection<Unit> units, final Route route, final PlayerID player, final MoveValidationResult result)
	{
		if (!units.isEmpty()
					&& !getEditMode(data)
					&& !Match.allMatch(Match.getMatches(units, Matches.unitIsBeingTransportedByOrIsDependentOfSomeUnitInThisList(units, route, player, data, true).invert()),
								Matches.unitIsOwnedBy(player)))
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
		return result;
	}
	
	static MoveValidationResult validateFuel(final GameData data, final Collection<Unit> units, final Route route, final PlayerID player, final MoveValidationResult result)
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
				if (data.getRelationshipTracker().isAtWar(current.getOwner(), player) || MoveDelegate.getBattleTracker(data).wasConquered(current))
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
			else if (allEnemyBlitzable && !(route.getStart().isWater() || route.getEnd().isWater()))
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
					// TODO: we need to actually test if the units is being air transported, or mech-land-transported.
					if (Matches.UnitIsAirTransportable.match(unit))
						continue;
					if (Matches.UnitIsInfantry.match(unit))
						continue;
					final TripleAUnit tAUnit = (TripleAUnit) unit;
					if (wasStartFoughtOver || tAUnit.getWasInCombat() || route.someMatch(notEndOrFriendlyTerrs) || route.someMatch(notEndWasFought))
						result.addDisallowedUnit(NOT_ALL_UNITS_CAN_BLITZ, unit);
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
		final CompositeMatch<Territory> neutralOrEnemy = new CompositeMatchOr<Territory>(Matches.TerritoryIsNeutralButNotWater,
					Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data));
		// final CompositeMatch<Unit> transportsCanNotControl = new CompositeMatchAnd<Unit>(Matches.UnitIsTransportAndNotDestroyer, Matches.UnitIsTransportButNotCombatTransport);
		final boolean navalMayNotNonComIntoControlled = isWW2V2(data) || games.strategy.triplea.Properties.getNavalUnitsMayNotNonCombatMoveIntoControlledSeaZones(data);
		// TODO need to account for subs AND transports that are ignored, not just OR
		final Territory end = route.getEnd();
		if (neutralOrEnemy.match(end))
		{
			// a convoy zone is controlled, so we must make sure we can still move there if there are actual battle there
			if (!end.isWater() || navalMayNotNonComIntoControlled)
				return result.setErrorReturnResult("Cannot advance units to battle in non combat");
		}
		// Subs can't travel under DDs
		if (isSubmersibleSubsAllowed(data) && Match.allMatch(units, Matches.UnitIsSub))
		{
			// this is ok unless there are destroyer on the path
			if (MoveValidator.enemyDestroyerOnPath(route, player, data))
				return result.setErrorReturnResult("Cannot move submarines under destroyers");
		}
		if (end.getUnits().someMatch(Matches.enemyUnit(player, data)))
		{
			if (!onlyIgnoredUnitsOnPath(route, player, data, false))
			{
				final CompositeMatch<Unit> friendlyOrSubmerged = new CompositeMatchOr<Unit>();
				friendlyOrSubmerged.add(Matches.enemyUnit(player, data).invert());
				friendlyOrSubmerged.add(Matches.unitIsSubmerged(data));
				if (!end.getUnits().allMatch(friendlyOrSubmerged) && !(Match.allMatch(units, Matches.UnitIsAir) && end.isWater()))
				{
					if (!Match.allMatch(units, Matches.UnitIsSub) || !games.strategy.triplea.Properties.getSubsCanEndNonCombatMoveWithEnemies(data))
						return result.setErrorReturnResult("Cannot advance to battle in non combat");
				}
			}
		}
		// if there are enemy units on the path blocking us, that is validated elsewhere (validateNonEnemyUnitsOnPath)
		// now check if we can move over neutral or enemies territories in noncombat
		if (Match.allMatch(units, Matches.UnitIsAir) || (Match.noneMatch(units, Matches.UnitIsSea) && !nonParatroopersPresent(player, units, route)))
		{
			// if there are non-paratroopers present, then we can not fly over stuff
			// if there are neutral territories in the middle, we can not fly over (unless allowed to)
			// otherwise we can generally fly over anything in noncombat
			if (route.someMatch(new CompositeMatchAnd<Territory>(Matches.TerritoryIsNeutralButNotWater, Matches.TerritoryIsWater.invert()))
						&& (!games.strategy.triplea.Properties.getNeutralFlyoverAllowed(data) || isNeutralsImpassable(data)))
				return result.setErrorReturnResult("Air units cannot fly over neutral territories in non combat");
		}
		// if sea units, or land units moving over/onto sea (ex: loading onto a transport), then only check if old rules stop us
		else if (Match.someMatch(units, Matches.UnitIsSea) || route.someMatch(Matches.TerritoryIsWater))
		{
			// if there are neutral or owned territories, we can not move through them (only under old rules. under new rules we can move through owned sea zones.)
			if (navalMayNotNonComIntoControlled && route.someMatch(neutralOrEnemy))
				return result.setErrorReturnResult("Cannot move units through neutral or enemy territories in non combat");
		}
		else
		{
			if (route.someMatch(neutralOrEnemy))
				return result.setErrorReturnResult("Cannot move units through neutral or enemy territories in non combat");
		}
		return result;
	}
	
	// Added to handle restriction of movement to listed territories
	static MoveValidationResult validateMovementRestrictedByTerritory(final GameData data, final Collection<Unit> units, final Route route, final PlayerID player,
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
		// subs may possibly carry units...
		if (isSubmersibleSubsAllowed(data) && Match.allMatch(Match.getMatches(units, Matches.unitIsBeingTransported().invert()), Matches.UnitIsSub))
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
				final Collection<Unit> transportsToLoad, final Map<Unit, Collection<Unit>> newDependents, final MoveValidationResult result)
	{
		final boolean isEditMode = getEditMode(data);
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
			// TODO: do not EVER user something from the UI in a validation method. Only the local computer (ie: client) has a copy of this UI data. The server has a different copy!!!!
			// TODO: re-write the entire fucking Paratroopers code. It is garbage! We need a single all encompassing UI and engine for all the different types of transportation that exist.
			if (!newDependents.isEmpty())
			{
				for (final Unit transport : dependencies.keySet())
				{
					if (dependencies.get(transport).isEmpty())
						dependencies.put(transport, newDependents.get(transport));
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
			// a territory effect can disallow unit types in
			if (Match.someMatch(units, Matches.unitIsOfTypes(TerritoryEffectHelper.getUnitTypesForUnitsNotAllowedIntoTerritory(route.getSteps()))))
				return result.setErrorReturnResult("Territory Effects disallow some units into " + (route.numberOfSteps() > 1 ? "these territories" : "this territory"));
			
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
		final CompositeMatch<Unit> alliedOrNonCombat = new CompositeMatchOr<Unit>(Matches.UnitIsInfrastructure, Matches.enemyUnit(player, data).invert(), Matches.unitIsSubmerged(data));
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
		final CompositeMatch<Unit> subOnly = new CompositeMatchOr<Unit>(Matches.UnitIsInfrastructure, Matches.UnitIsSub, Matches.enemyUnit(player, data).invert());
		final CompositeMatch<Unit> transportOnly = new CompositeMatchOr<Unit>(Matches.UnitIsInfrastructure, Matches.UnitIsTransportButNotCombatTransport, Matches.UnitIsLand,
					Matches.enemyUnit(player, data).invert());
		final CompositeMatch<Unit> transportOrSubOnly = new CompositeMatchOr<Unit>(Matches.UnitIsInfrastructure, Matches.UnitIsTransportButNotCombatTransport, Matches.UnitIsLand,
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
		// if there are no steps, then we began in this sea zone, so see if there are ignored units in this sea zone (not sure if we need !ignoreRouteEnd here).
		if (steps.isEmpty() && route.numberOfStepsIncludingStart() == 1 && !ignoreRouteEnd)
			steps.add(route.getStart());
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
		return BaseEditDelegate.getEditMode(data);
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
	public static boolean isLoad(final Collection<Unit> units, final Map<Unit, Collection<Unit>> newDependents, final Route route, final GameData data, final PlayerID player)
	{
		final Map<Unit, Collection<Unit>> alreadyLoaded = mustMoveWith(units, newDependents, route.getStart(), data, player);
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
	
	/*
	public static boolean validLoad(final Collection<Unit> units, final Collection<Unit> transports)
	{
		return true;
	}
	*/

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
	
	private static MoveValidationResult validateTransport(final boolean isNonCombat, final GameData data, final List<UndoableMove> undoableMoves, final Collection<Unit> units, final Route route,
				final PlayerID player, final Collection<Unit> transportsToLoad, final Map<Unit, Collection<Unit>> newDependents, final MoveValidationResult result)
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
		final Territory routeEnd = route.getEnd();
		final Territory routeStart = route.getStart();
		// if unloading make sure length of route is only 1
		if (!isEditMode && MoveValidator.isUnload(route))
		{
			if (route.hasMoreThenOneStep())
				return result.setErrorReturnResult("Unloading units must stop where they are unloaded");
			for (final Unit unit : transportTracker.getUnitsLoadedOnAlliedTransportsThisTurn(units))
				result.addDisallowedUnit(CANNOT_LOAD_AND_UNLOAD_AN_ALLIED_TRANSPORT_IN_THE_SAME_ROUND, unit);
			final Collection<Unit> transports = MoveDelegate.mapTransports(route, units, null).values();
			final boolean isScramblingOrKamikazeAttacksEnabled = games.strategy.triplea.Properties.getScramble_Rules_In_Effect(data)
						|| games.strategy.triplea.Properties.getUseKamikazeSuicideAttacks(data);
			final boolean submarinesPreventUnescortedAmphibAssaults = games.strategy.triplea.Properties.getSubmarinesPreventUnescortedAmphibiousAssaults(data);
			final Match<Unit> enemySubmarineMatch = new CompositeMatchAnd<Unit>(Matches.unitIsEnemyOf(data, player), Matches.UnitIsSub);
			final Match<Unit> ownedSeaNonTransportMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsSea, Matches.UnitIsNotTransportButCouldBeCombatTransport);
			for (final Unit transport : transports)
			{
				if (!isNonCombat && route.numberOfStepsIncludingStart() == 2)
				{
					if (Matches.territoryHasEnemyUnits(player, data).match(routeEnd) || Matches.isTerritoryEnemyAndNotUnownedWater(player, data).match(routeEnd))
					{
						// this is an amphibious assault
						if (submarinesPreventUnescortedAmphibAssaults && !Matches.territoryHasUnitsThatMatch(ownedSeaNonTransportMatch).match(routeStart) &&
									Matches.territoryHasUnitsThatMatch(enemySubmarineMatch).match(routeStart))
						{
							// we must have at least one warship (non-transport) unit, otherwise the enemy sub stops our unloading for amphibious assault
							for (final Unit unit : transportTracker.transporting(transport))
							{
								result.addDisallowedUnit(ENEMY_SUBMARINE_PREVENTING_UNESCORTED_AMPHIBIOUS_ASSAULT_LANDING, unit);
							}
						}
					}
					else if (!MoveDelegate.getBattleTracker(data).wasConquered(routeEnd))
					{
						// this is an unload to a friendly territory
						if (isScramblingOrKamikazeAttacksEnabled || Matches.territoryHasEnemyUnits(player, data).match(routeStart))
						{
							// Unloading a transport from a sea zone with a battle, to a friendly land territory, during combat move phase, is illegal
							// and in addition to being illegal, it is also causing problems if the sea transports get killed (the land units are not dying)
							// TODO: should we use the battle tracker for this instead?
							for (final Unit unit : transportTracker.transporting(transport))
							{
								result.addDisallowedUnit(TRANSPORT_MAY_NOT_UNLOAD_TO_FRIENDLY_TERRITORIES_UNTIL_AFTER_COMBAT_IS_RESOLVED, unit);
							}
						}
					}
				}
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
			{
				if (!onlyIgnoredUnitsOnPath(route, player, data, false))
				{
					if (!MoveDelegate.getBattleTracker(data).didAllThesePlayersJustGoToWarThisTurn(player, route.getEnd().getUnits().getUnits(), data))
						return result.setErrorReturnResult("Cannot load when enemy sea units are present");
				}
			}
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
		if (nonCombat && !isParatroopersCanMoveDuringNonCombat(data))
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
				if (Matches.isTerritoryFriendly(player, data).match(routeEnd) && !isParatroopersCanMoveDuringNonCombat(data))
				{
					result.addDisallowedUnit("Paratroops must advance to battle", paratroop);
				}
				if (!nonCombat && Matches.isTerritoryFriendly(player, data).match(routeEnd) && isParatroopersCanMoveDuringNonCombat(data))
				{
					result.addDisallowedUnit("Paratroops may only airlift during Non-Combat Movement Phase", paratroop);
				}
			}
			if (!games.strategy.triplea.Properties.getParatroopersCanAttackDeepIntoEnemyTerritory(data))
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
	
	public static MustMoveWithDetails getMustMoveWith(final Territory start, final Collection<Unit> units, final Map<Unit, Collection<Unit>> newDependents, final GameData data, final PlayerID player)
	{
		return new MustMoveWithDetails(mustMoveWith(units, newDependents, start, data, player));
	}
	
	private static Map<Unit, Collection<Unit>> mustMoveWith(final Collection<Unit> units, final Map<Unit, Collection<Unit>> newDependents, final Territory start, final GameData data,
				final PlayerID player)
	{
		final List<Unit> sortedUnits = new ArrayList<Unit>(units);
		Collections.sort(sortedUnits, UnitComparator.getHighestToLowestMovementComparator());
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
			mapping.putAll(airTransportsMustMoveWith(sortedUnits, newDependents));
		}
		else
		{
			final Map<Unit, Collection<Unit>> newMapping = new HashMap<Unit, Collection<Unit>>();
			newMapping.putAll(airTransportsMustMoveWith(sortedUnits, newDependents));
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
	
	private static Map<Unit, Collection<Unit>> airTransportsMustMoveWith(final Collection<Unit> units, final Map<Unit, Collection<Unit>> newDependents)
	{
		final TransportTracker transportTracker = new TransportTracker();
		final Map<Unit, Collection<Unit>> mustMoveWith = new HashMap<Unit, Collection<Unit>>();
		final Collection<Unit> airTransports = Match.getMatches(units, Matches.UnitIsAirTransport);
		/*Map<Unit, Collection<Unit>> selectedDependents = new HashMap<Unit, Collection<Unit>>();
		// first, check if there are any that haven't been updated yet
		for (final Unit airTransport : airTransports)
		{
			if (selectedDependents.containsKey(airTransport))
			{
				final Collection<Unit> transporting = selectedDependents.get(airTransport);
				mustMoveWith.put(airTransport, transporting);
			}
		}*/
		// Then check those that have already had their transportedBy set
		for (final Unit airTransport : airTransports)
		{
			if (!mustMoveWith.containsKey(airTransport))
			{
				Collection<Unit> transporting = transportTracker.transporting(airTransport);
				if (transporting == null || transporting.isEmpty())
				{
					if (!newDependents.isEmpty())
						transporting = newDependents.get(airTransport);
				}
				mustMoveWith.put(airTransport, transporting);
			}
		}
		return mustMoveWith;
	}
	
	public static Map<Unit, Collection<Unit>> carrierMustMoveWith(final Collection<Unit> units, final Territory start, final GameData data, final PlayerID player)
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
					Matches.unitIsBeingTransportedByOrIsDependentOfSomeUnitInThisList(units, defaultRoute, player, data, true).invert()));
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
	
	/*private static boolean areNeutralsPassableByAir(final GameData data)
	{
		return (games.strategy.triplea.Properties.getNeutralFlyoverAllowed(data) && !isNeutralsImpassable(data));
	}
	
	private static boolean isWW2V3(final GameData data)
	{
		return games.strategy.triplea.Properties.getWW2V3(data);
	}
	
	private static boolean isMultipleAAPerTerritory(final GameData data)
	{
		return games.strategy.triplea.Properties.getMultipleAAPerTerritory(data);
	}
	
	private static int getNeutralCharge(final GameData data, final Route route)
	{
		return getNeutralCharge(data, MoveDelegate.getEmptyNeutral(route).size());
	}
	
	private static boolean isKamikazeAircraft(final GameData data)
	{
		return games.strategy.triplea.Properties.getKamikaze_Airplanes(data);
	}
	
	private static boolean isSubControlSeaZoneRestricted(final GameData data)
	{
		return games.strategy.triplea.Properties.getSubControlSeaZoneRestricted(data);
	}
	
	private static boolean isTransportControlSeaZone(final GameData data)
	{
		return games.strategy.triplea.Properties.getTransportControlSeaZone(data);
	}*/

	private static boolean isMovementByTerritoryRestricted(final GameData data)
	{
		return games.strategy.triplea.Properties.getMovementByTerritoryRestricted(data);
	}
	
	private static boolean isParatroopersCanMoveDuringNonCombat(final GameData data)
	{
		return games.strategy.triplea.Properties.getParatroopersCanMoveDuringNonCombat(data);
	}
	
	private static int getNeutralCharge(final GameData data, final int numberOfTerritories)
	{
		return numberOfTerritories * games.strategy.triplea.Properties.getNeutralCharge(data);
	}
	
	private static boolean isSubmersibleSubsAllowed(final GameData data)
	{
		return games.strategy.triplea.Properties.getSubmersible_Subs(data);
	}
	
	private static boolean isAlliedAirDependents(final GameData data)
	{
		return games.strategy.triplea.Properties.getAlliedAirDependents(data);
	}
	
	private static boolean isIgnoreTransportInMovement(final GameData data)
	{
		return games.strategy.triplea.Properties.getIgnoreTransportInMovement(data);
	}
	
	private static boolean isIgnoreSubInMovement(final GameData data)
	{
		return games.strategy.triplea.Properties.getIgnoreSubInMovement(data);
	}
	
	/** Creates new MoveValidator */
	private MoveValidator()
	{
	}
}
