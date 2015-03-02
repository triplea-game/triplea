package games.strategy.triplea.delegate;

import games.strategy.common.delegate.BaseEditDelegate;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.dataObjects.MoveValidationResult;
import games.strategy.util.CompositeMatchAnd;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides static methods for evaluating movement of air units.
 * 
 * @author veqryn [Mark Christopher Duncan]
 * 
 */
public class AirMovementValidator
{
	public static final String NOT_ALL_AIR_UNITS_CAN_LAND = "Not all air units can land";
	
	// TODO: this class does a pretty good job already, but could be improved by having the carriers that are potentially moved also look for any owned air units that are in sea zones without carriers. these would be air units that have already been moved this turn, and therefore would need pickup.
	
	public static MoveValidationResult validateAirCanLand(final GameData data, final Collection<Unit> units, final Route route, final PlayerID player, final MoveValidationResult result)
	{
		// First check if we even need to check
		if (getEditMode(data) || // Edit Mode, no need to check
					!Match.someMatch(units, Matches.UnitIsAir) || // No Airunits, nothing to check
					route.hasNoSteps() || // if there are no steps, we didn't move, so it is always OK!
					Matches.airCanLandOnThisAlliedNonConqueredLandTerritory(player, data).match(route.getEnd()) || // we can land at the end, nothing left to check
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
		final HashSet<Unit> airThatMustLandOnCarriersHash = new HashSet<Unit>();
		airThatMustLandOnCarriersHash.addAll(Match.getMatches(routeEnd.getUnits().getUnits(), airAlliedNotOwned));
		airThatMustLandOnCarriersHash.addAll(Match.getMatches(units, airAlliedNotOwned));
		// now we must see if we also need to account for units (allied cargo) that are moving with our carriers, if we have selected any carriers
		final Collection<Unit> movingCarriersAtStartLocationBeingMoved = Match.getMatches(units, Matches.UnitIsCarrier);
		if (!movingCarriersAtStartLocationBeingMoved.isEmpty())
		{
			final Map<Unit, Collection<Unit>> carrierToAlliedCargo = MoveValidator.carrierMustMoveWith(units, routeStart, data, player);
			for (final Collection<Unit> alliedAirOnCarrier : carrierToAlliedCargo.values())
			{
				airThatMustLandOnCarriersHash.addAll(alliedAirOnCarrier);
			}
		}
		// now we can add our owned air. we add our owned air last because it can be moved, while allied air can not be. we want the lowest movement to be validated first.
		airThatMustLandOnCarriersHash.addAll(ownedAirThatMustLandOnCarriers);
		final List<Unit> airThatMustLandOnCarriers = new ArrayList<Unit>(airThatMustLandOnCarriersHash);
		// sort the list by shortest range first so those birds will get first pick of landingspots
		Collections.sort(airThatMustLandOnCarriers, getLowestToHighestMovementComparatorIncludingUnitsNotYetMoved(route));
		
		// now we should see if the carriers we are moving with, plus the carriers already there, can handle all our air units (we check ending territories first, separately, because it is special [it includes units in our selection])
		final Collection<Unit> carriersAtEnd = Match.getMatches(getFriendly(routeEnd, player, data), Matches.UnitIsCarrier);
		carriersAtEnd.addAll(movingCarriersAtStartLocationBeingMoved);
		// to keep track of all carriers, and their fighters, that have moved, so that we do not move them again.
		final Map<Unit, Collection<Unit>> movedCarriersAndTheirFighters = new HashMap<Unit, Collection<Unit>>();
		for (final Unit carrier : carriersAtEnd)
		{
			movedCarriersAndTheirFighters.put(carrier, new ArrayList<Unit>());
		}
		final Collection<Unit> airNotToConsiderBecauseWeAreValidatingThem = new ArrayList<Unit>(airThatMustLandOnCarriers);
		
		airThatMustLandOnCarriers.removeAll(whatAirCanLandOnTheseCarriers(carriersAtEnd, airThatMustLandOnCarriers, routeEnd));
		if (airThatMustLandOnCarriers.isEmpty())
			return result;
		// we still have air left, so begin calling carriers to come here to pick up the air
		final int maxMovementLeftForTheseAirUnitsBeingValidated = maxMovementLeftForTheseAirUnitsBeingValidated(airThatMustLandOnCarriers, route, player); // figure out what is the max distance of our remaining air units
		final int maxMovementLeftForAllOwnedCarriers = maxMovementLeftForAllOwnedCarriers(player, data); // figure out what is the max distance of our remaining carrier units
		final List<Territory> landingSpots = new ArrayList<Territory>(Collections.singleton(routeEnd));
		landingSpots.addAll(data.getMap().getNeighbors(routeEnd, maxMovementLeftForTheseAirUnitsBeingValidated, Matches.airCanFlyOver(player, data, areNeutralsPassableByAir(data)))); // where can we fly to?
		landingSpots.removeAll(Match.getMatches(landingSpots, Matches.seaCanMoveOver(player, data).invert())); // we only want to consider places we can move carriers to
		Collections.sort(landingSpots, getLowestToHighestDistance(routeEnd, Matches.seaCanMoveOver(player, data)));
		final Collection<Territory> potentialCarrierOrigins = new LinkedHashSet<Territory>(landingSpots);
		potentialCarrierOrigins.addAll(data.getMap().getNeighbors(new HashSet<Territory>(landingSpots), maxMovementLeftForAllOwnedCarriers, Matches.seaCanMoveOver(player, data)));
		potentialCarrierOrigins.remove(routeEnd);
		potentialCarrierOrigins.removeAll(Match.getMatches(potentialCarrierOrigins, Matches.TerritoryHasOwnedCarrier(player).invert()));
		// now see if we can move carriers there to pick up
		validateAirCaughtByMovingCarriersAndOwnedAndAlliedAir(result, landingSpots, potentialCarrierOrigins, movedCarriersAndTheirFighters, airThatMustLandOnCarriers,
					airNotToConsiderBecauseWeAreValidatingThem, player, route, data);
		return result;
	}
	
	private static LinkedHashMap<Unit, Integer> getMovementLeftForValidatingAir(final Collection<Unit> airBeingValidated, final PlayerID player, final Route route)
	{
		final LinkedHashMap<Unit, Integer> map = new LinkedHashMap<Unit, Integer>();
		for (final Unit unit : airBeingValidated)
		{
			// unit must be in either start or end.
			int movementLeft;
			if (Matches.unitIsOwnedBy(player).match(unit))
			{
				movementLeft = getMovementLeftForAirUnitNotMovedYet(unit, route);
			}
			else
				movementLeft = 0;
			map.put(unit, movementLeft);
		}
		return map;
	}
	
	private static int getMovementLeftForAirUnitNotMovedYet(final Unit airBeingValidated, final Route route)
	{
		if (route.getEnd().getUnits().getUnits().contains(airBeingValidated))
			return ((TripleAUnit) airBeingValidated).getMovementLeft(); // they are not being moved, they are already at the end
		else
			return route.getMovementLeft(airBeingValidated); // they are being moved (they are still at the start location)
	}
	
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
				if (Matches.TerritoryIsWater.match(t) && Matches.territoryHasOwnedAtBeginningOfTurnIsFactoryOrCanProduceUnitsNeighbor(data, player).match(t))
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
	
	private static void validateAirCaughtByMovingCarriersAndOwnedAndAlliedAir(final MoveValidationResult result, final List<Territory> landingSpots,
				final Collection<Territory> potentialCarrierOrigins, final Map<Unit, Collection<Unit>> movedCarriersAndTheirFighters, final Collection<Unit> airThatMustLandOnCarriers,
				final Collection<Unit> airNotToConsider, final PlayerID player, final Route route, final GameData data)
	{
		final Match<Unit> ownedCarrierMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsCarrier);
		final Match<Unit> ownedAirMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsAir, Matches.UnitCanLandOnCarrier);
		final Match<Unit> alliedNotOwnedAirMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player).invert(), Matches.isUnitAllied(player, data), Matches.UnitIsAir,
					Matches.UnitCanLandOnCarrier);
		final Match<Unit> alliedNotOwnedCarrierMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player).invert(), Matches.isUnitAllied(player, data), Matches.UnitIsCarrier);
		final Territory routeEnd = route.getEnd();
		final boolean areNeutralsPassableByAir = areNeutralsPassableByAir(data);
		final IntegerMap<Territory> landingSpotsWithCarrierCapacity = populateStaticAlliedAndBuildingCarrierCapacity(landingSpots, movedCarriersAndTheirFighters, player, data); // fill our landing spot capacity with capacity from allied carriers and potential building of new carriers
		final LinkedHashMap<Unit, Integer> movementLeftForAirToValidate = getMovementLeftForValidatingAir(airThatMustLandOnCarriers, player, route); // calculate movement left only once
		for (final Territory landingSpot : landingSpots)
		{
			potentialCarrierOrigins.remove(landingSpot); // since we are here, no point looking at this place twice
			final List<Unit> airCanReach = new ArrayList<Unit>();
			for (final Unit air : airThatMustLandOnCarriers)
			{
				if (canAirReachThisSpot(data, player, air, routeEnd, movementLeftForAirToValidate.get(air), landingSpot, areNeutralsPassableByAir))
					airCanReach.add(air); // get all air that can reach this spot
			}
			if (airCanReach.isEmpty())
				continue;
			final Collection<Unit> unitsInLandingSpot = landingSpot.getUnits().getUnits();
			unitsInLandingSpot.removeAll(movedCarriersAndTheirFighters.keySet());
			unitsInLandingSpot.removeAll(airNotToConsider); // make sure to remove any units we have already moved, or units that are excluded because they are in our mouse selection
			for (final Collection<Unit> ftrs : movedCarriersAndTheirFighters.values())
			{
				unitsInLandingSpot.removeAll(ftrs); // also remove any fighters that are being moved with carriers that we have already moved
			}
			final Collection<Unit> ownedCarriersInLandingSpot = Match.getMatches(unitsInLandingSpot, ownedCarrierMatch);
			/*for (final Unit carrier : ownedCarriersInLandingSpot)
			{
				movedCarriersAndTheirFighters.put(carrier, new ArrayList<Unit>()); // since we are now counting their capacity, make sure we do not ever count them again.
			}*/
			final Collection<Unit> airInLandingSpot = Match.getMatches(Match.getMatches(unitsInLandingSpot, ownedAirMatch), UnitCanFindLand(data, landingSpot).invert()); // get air we own here, but exclude any air that can fly to allied land
			airInLandingSpot.addAll(Match.getMatches(unitsInLandingSpot, alliedNotOwnedAirMatch)); // add allied air (it can't fly away)
			// airNotToConsider.addAll(airInLandingSpot); // make sure we don't count this again
			int landingSpotCapacity = landingSpotsWithCarrierCapacity.getInt(landingSpot); // get the current capacity
			landingSpotCapacity += carrierCapacity(ownedCarriersInLandingSpot, landingSpot); // add capacity of owned carriers
			landingSpotCapacity -= carrierCost(airInLandingSpot); // minus capacity of air in the territory
			if (!airCanReach.isEmpty())
			{
				final Iterator<Unit> airIter = airCanReach.iterator();
				while (airIter.hasNext())
				{
					final Unit air = airIter.next();
					final int carrierCost = carrierCost(air);
					if (landingSpotCapacity >= carrierCost)
					{
						landingSpotCapacity -= carrierCost;
						airThatMustLandOnCarriers.remove(air); // we can land this one here, yay
						airIter.remove();
					}
				}
			}
			if (airThatMustLandOnCarriers.isEmpty())
				return; // all can land here, so return
				
			// final int lowestCarrierCost = getLowestCarrierCost(airCanReach);
			// now bring carriers here...
			final Iterator<Territory> iter = potentialCarrierOrigins.iterator();
			while (iter.hasNext())
			{
				final Territory carrierSpot = iter.next();
				final Collection<Unit> unitsInCarrierSpot = carrierSpot.getUnits().getUnits();
				unitsInCarrierSpot.removeAll(movedCarriersAndTheirFighters.keySet()); // remove carriers we have already moved
				unitsInCarrierSpot.removeAll(airNotToConsider); // remove units we do not want to consider because they are in our mouse selection
				for (final Collection<Unit> ftrs : movedCarriersAndTheirFighters.values())
				{
					unitsInCarrierSpot.removeAll(ftrs); // remove the fighters that are moving with the carriers we have already moved
				}
				final Collection<Unit> ownedCarriersInCarrierSpot = Match.getMatches(unitsInCarrierSpot, ownedCarrierMatch);
				if (ownedCarriersInCarrierSpot.isEmpty())
				{
					iter.remove();
					continue;
				}
				final Collection<Unit> ownedAirInCarrierSpot = Match.getMatches(Match.getMatches(unitsInCarrierSpot, ownedAirMatch), UnitCanFindLand(data, carrierSpot).invert()); // exclude any owned air that can fly to land
				final Collection<Unit> alliedNotOwnedAirInCarrierSpot = Match.getMatches(unitsInCarrierSpot, alliedNotOwnedAirMatch);
				final Map<Unit, Collection<Unit>> mustMoveWithMap = MoveValidator.carrierMustMoveWith(ownedCarriersInCarrierSpot, carrierSpot, data, player); // this only returns the allied cargo planes that MUST travel with the carrier
				int carrierSpotCapacity = landingSpotsWithCarrierCapacity.getInt(carrierSpot); // get the current capacity for the carrier spot
				if (!landingSpotsWithCarrierCapacity.containsKey(carrierSpot)) // we don't have it because this spot is not in the landing zone area.
				{
					// we still have a capacity for allied carriers, but only to carry other allied or local owned units, not to carry our selected units.
					carrierSpotCapacity = carrierCapacity(carrierSpot.getUnits().getMatches(alliedNotOwnedCarrierMatch), carrierSpot);
					landingSpotsWithCarrierCapacity.put(carrierSpot, carrierSpotCapacity);
				}
				// we have allied air here, so we need to account for them before moving any carriers
				if (!alliedNotOwnedAirInCarrierSpot.isEmpty() || !mustMoveWithMap.isEmpty())
				{
					// mustMoveWithMap is only filled if we have so many allied air that our owned carriers are carrying some of them
					if (mustMoveWithMap.isEmpty())
					{
						// allied carriers can carry enough
						carrierSpotCapacity -= carrierCost(alliedNotOwnedAirInCarrierSpot);
						airNotToConsider.addAll(alliedNotOwnedAirInCarrierSpot); // we do not want to consider these units again
						if (carrierSpotCapacity > 0)
						{
							// we can hold some of the owned air here too
							final Iterator<Unit> ownedIter = ownedAirInCarrierSpot.iterator();
							while (ownedIter.hasNext())
							{
								final Unit air = ownedIter.next();
								final int carrierCost = carrierCost(air);
								if (carrierSpotCapacity >= carrierCost)
								{
									carrierSpotCapacity -= carrierCost;
									airNotToConsider.add(air); // we do not want to consider this one again
									ownedIter.remove();
								}
							}
						}
						landingSpotsWithCarrierCapacity.put(carrierSpot, carrierSpotCapacity); // put correct value for future reference now that we have considered the allied air
					}
					else
					{
						// carrierMustMoveWith does not account for any allied cargo already moved out.
						for (final Collection<Unit> airMovingWith : mustMoveWithMap.values())
						{
							for (final Collection<Unit> ftrs : movedCarriersAndTheirFighters.values())
							{
								airMovingWith.removeAll(ftrs); // remove the fighters that are moving with the carriers we have already moved
							}
						}
						for (final Collection<Unit> airMovingWith : mustMoveWithMap.values())
						{
							alliedNotOwnedAirInCarrierSpot.removeAll(airMovingWith); // we will consider these as part of their moving carrier
						}
						carrierSpotCapacity -= carrierCost(alliedNotOwnedAirInCarrierSpot);
						airNotToConsider.addAll(alliedNotOwnedAirInCarrierSpot); // we do not want to consider these units again
						landingSpotsWithCarrierCapacity.put(carrierSpot, carrierSpotCapacity); // put correct value for future reference now that we have considered the allied air
					}
				}
				final Route toLandingSpot = data.getMap().getRoute(carrierSpot, landingSpot, Matches.seaCanMoveOver(player, data));
				if (toLandingSpot == null)
					continue;
				final List<Unit> carrierCanReach = Match.getMatches(ownedCarriersInCarrierSpot, Matches.UnitHasEnoughMovementForRoute(toLandingSpot));
				if (carrierCanReach.isEmpty())
					continue; // none can reach
				final List<Unit> carrierNotReach = new ArrayList<Unit>(ownedCarriersInCarrierSpot);
				carrierNotReach.removeAll(carrierCanReach); // we want to see if the air units can be put on the carriers that can not make it first, before taking up room on the carriers that can make it
				final List<Unit> allCarriers = new ArrayList<Unit>(carrierNotReach);
				allCarriers.addAll(carrierCanReach); // so we remove them from the list then re-add them so that they will be at the end of the list
				// now we want to make a map of the carriers to the units they must carry with them (both allied and owned)
				final Map<Unit, Collection<Unit>> carriersToMove = new HashMap<Unit, Collection<Unit>>();
				final List<Unit> carrierFull = new ArrayList<Unit>();
				for (final Unit carrier : allCarriers)
				{
					final Collection<Unit> airMovingWith = new ArrayList<Unit>();
					final Collection<Unit> alliedMovingWith = mustMoveWithMap.get(carrier); // first add allied cargo
					if (alliedMovingWith != null)
						airMovingWith.addAll(alliedMovingWith);
					// now test if our carrier has any room for owned fighters
					int carrierCapacity = carrierCapacity(carrier, carrierSpot);
					carrierCapacity -= carrierCost(airMovingWith);
					final Iterator<Unit> ownedIter = ownedAirInCarrierSpot.iterator();
					while (ownedIter.hasNext())
					{
						final Unit air = ownedIter.next();
						final int carrierCost = carrierCost(air);
						if (carrierCapacity >= carrierCost)
						{
							carrierCapacity -= carrierCost;
							airMovingWith.add(air);
							ownedIter.remove();
						}
					}
					carriersToMove.put(carrier, airMovingWith);
					if (carrierCapacity <= 0)
						carrierFull.add(carrier);
				}
				// if all carriers full, remove this carrier spot from consideration
				if (carrierFull.containsAll(allCarriers))
				{
					iter.remove();
					continue;
				}
				if (carrierFull.containsAll(carrierNotReach))
				{
					iter.remove();
				}
				// ok, now lets move them.
				for (final Unit carrier : carrierCanReach)
				{
					movedCarriersAndTheirFighters.put(carrier, carriersToMove.get(carrier));
					landingSpotCapacity += carrierCapacity(carrier, carrierSpot);
					landingSpotCapacity -= carrierCost(carriersToMove.get(carrier));
				}
				// landingSpotsWithCarrierCapacity.put(landingSpot, landingSpotCapacity); // optional for debugging
				final Iterator<Unit> reachIter = airCanReach.iterator();
				while (reachIter.hasNext())
				{
					final Unit air = reachIter.next();
					final int carrierCost = carrierCost(air);
					if (landingSpotCapacity >= carrierCost)
					{
						landingSpotCapacity -= carrierCost;
						airThatMustLandOnCarriers.remove(air); // we can land this one here, yay
						reachIter.remove();
					}
				}
				if (airThatMustLandOnCarriers.isEmpty())
					return; // all can land here, so return
			}
		}
		// anyone left over can not land
		for (final Unit air : airThatMustLandOnCarriers)
		{
			result.addDisallowedUnit(NOT_ALL_AIR_UNITS_CAN_LAND, air);
		}
	}
	
	/*private static int getLowestCarrierCost(final Collection<Unit> air)
	{
		if (air == null || air.isEmpty())
			return 0;
		int min = Integer.MAX_VALUE;
		for (final Unit u : air)
		{
			min = Math.min(min, carrierCost(u));
		}
		return min;
	}*/
	
	private static Comparator<Territory> getLowestToHighestDistance(final Territory territoryWeMeasureDistanceFrom, final Match<Territory> condition)
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
	
	private static int maxMovementLeftForTheseAirUnitsBeingValidated(final Collection<Unit> airUnits, final Route route, final PlayerID player)
	{
		int max = 0;
		for (final Unit u : airUnits)
		{
			if (Matches.unitIsOwnedBy(player).match(u))
			{
				// unit must be in either start or end.
				final int movementLeft = getMovementLeftForAirUnitNotMovedYet(u, route);
				if (movementLeft > max)
					max = movementLeft;
			}
			// allied units can't move....
		}
		return max;
	}
	
	private static Collection<Unit> whatAirCanLandOnTheseCarriers(final Collection<Unit> carriers, final Collection<Unit> airUnits, final Territory territoryUnitsAreIn)
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
	private static List<Unit> getAirUnitsToValidate(final Collection<Unit> units, final Route route, final PlayerID player)
	{
		final Match<Unit> ownedAirMatch = new CompositeMatchAnd<Unit>(Matches.UnitIsAir, Matches.unitOwnedBy(player), Matches.UnitIsKamikaze.invert());
		final List<Unit> ownedAir = new ArrayList<Unit>();
		ownedAir.addAll(Match.getMatches(route.getEnd().getUnits().getUnits(), ownedAirMatch));
		ownedAir.addAll(Match.getMatches(units, ownedAirMatch));
		// sort the list by shortest range first so those birds will get first pick of landingspots
		Collections.sort(ownedAir, getLowestToHighestMovementComparatorIncludingUnitsNotYetMoved(route));
		return ownedAir;
	}
	
	public static Comparator<Unit> getLowestToHighestMovementComparatorIncludingUnitsNotYetMoved(final Route route)
	{
		return new Comparator<Unit>()
		{
			public int compare(final Unit u1, final Unit u2)
			{
				final int left1 = getMovementLeftForAirUnitNotMovedYet(u1, route);
				final int left2 = getMovementLeftForAirUnitNotMovedYet(u2, route);
				if (left1 == left2)
					return 0;
				if (left1 > left2)
					return 1;
				return -1;
			}
		};
	}
	
	public static boolean canAirReachThisSpot(final GameData data, final PlayerID player, final Unit unit, final Territory currentSpot, final int movementLeft, final Territory landingSpot,
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
		final int movementLeft = getMovementLeftForAirUnitNotMovedYet(unit, route);
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
		final List<Territory> possibleSpots = Match.getMatches(data.getMap().getNeighbors(current, movementLeft), Matches.airCanLandOnThisAlliedNonConqueredLandTerritory(player, data));
		for (final Territory landingSpot : possibleSpots)
		{ // TODO EW: Assuming movement cost of 1, this could get VERY slow when the movementcost is very high and airunits have a lot of movementcapacity.
			if (canAirReachThisSpot(data, player, unit, current, movementLeft, landingSpot, areNeutralsPassableByAir))
				return true;
		}
		return false;
	}
	
	public static Match<Unit> UnitCanFindLand(final GameData data, final Territory current)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit u)
			{
				return canFindLand(data, u, current);
			}
		};
	}
	
	/**
	 * Returns true if the given air units can land in the given territory.
	 * Does take into account whether a battle has been fought in the territory already.
	 * 
	 * Note units must only be air units
	 */
	public static boolean canLand(final Collection<Unit> airUnits, final Territory territory, final PlayerID player, final GameData data)
	{
		if (!Match.allMatch(airUnits, Matches.UnitIsAir))
			throw new IllegalArgumentException("can only test if air will land");
		if (!territory.isWater() && AbstractMoveDelegate.getBattleTracker(data).wasConquered(territory))
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
	
	private static boolean getEditMode(final GameData data)
	{
		return BaseEditDelegate.getEditMode(data);
	}
	
	public static Collection<Unit> getFriendly(final Territory territory, final PlayerID player, final GameData data)
	{
		return territory.getUnits().getMatches(Matches.alliedUnit(player, data));
	}
	
	private static boolean isKamikazeAircraft(final GameData data)
	{
		return games.strategy.triplea.Properties.getKamikaze_Airplanes(data);
	}
	
	private static boolean areNeutralsPassableByAir(final GameData data)
	{
		return (games.strategy.triplea.Properties.getNeutralFlyoverAllowed(data) && !isNeutralsImpassable(data));
	}
	
	private static boolean isNeutralsImpassable(final GameData data)
	{
		return games.strategy.triplea.Properties.getNeutralsImpassable(data);
	}
	
	private static int getNeutralCharge(final GameData data, final Route route)
	{
		return getNeutralCharge(data, MoveDelegate.getEmptyNeutral(route).size());
	}
	
	private static int getNeutralCharge(final GameData data, final int numberOfTerritories)
	{
		return numberOfTerritories * games.strategy.triplea.Properties.getNeutralCharge(data);
	}
	
	/* Original Code from Edwin:
	private static MoveValidationResult validateAirCanLandf(final GameData data, final Collection<Unit> units, final Route route, final PlayerID player, final MoveValidationResult result)
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
		final Collection<Unit> airThatMustLandOnCarriers = getAirThatMustLandOnCarriers(data, getAirUnitsToValidate(units, route, player), route, result, player);
		if (airThatMustLandOnCarriers.isEmpty())
			return result; // we are done, everything can find a place to land
		
		// Here's where we see if we have carriers available to land. (VEQRYN: I read up until this point, and know that all the methods before this do what they should)
		
		final IntegerMap<Territory> usedCarrierSpace = new IntegerMap<Territory>(); // this map of territories tracks how much carrierspace in each territory we already used up.
		final Set<Unit> movedCarriers = new HashSet<Unit>(); // this set of units tracks which carriers are already marked as moved to catch fighters in the air.
		for (final Unit unit : airThatMustLandOnCarriers)
		{
			if (!findCarrierToLand(data, player, unit, route, usedCarrierSpace, movedCarriers))
			{
				result.addDisallowedUnit(NOT_ALL_AIR_UNITS_CAN_LAND, unit);
			}
		}
		return result;
	}
	
	private static boolean placeOnBuiltCarrier(final Unit airUnit, final Territory landingSpot, final IntegerMap<Territory> usedCarrierSpace, final List<Unit> carriersInQueue, final GameData data,
				final PlayerID player)
	{
		if (!Matches.territoryHasOwnedIsFactoryOrCanProduceUnitsNeighbor(data, player).match(landingSpot))
			return false;
		// TO DO EW: existing bug -- can this factory actually produce carriers? ie: shipyards vs. factories
		for (final Unit carrierCandidate : carriersInQueue)
		{
			final UnitAttachment aua = UnitAttachment.get(airUnit.getType());
			final UnitAttachment cua = UnitAttachment.get(carrierCandidate.getType());
			if (cua.getCarrierCapacity() >= aua.getCarrierCost())
			{
				// TO DO EW: is this the wisest carrier-choice? improve by picking the smartest carrier
				// add leftover capacity to the usedCarrierSpace because the carrier will virtually be placed in this spot!
				// if I don't do this and remove the carrier from the queue the carrier could be built in multiple spots.
				
				final int newUsedCapacity = usedCarrierSpace.getInt(landingSpot) + aua.getCarrierCost() - cua.getCarrierCapacity();
				usedCarrierSpace.put(landingSpot, newUsedCapacity);
				// remove the Carrier from the Queue so it can't be placed somewhere else
				carriersInQueue.remove(carrierCandidate);
				return true;
			}
		}
		return false;
	}
	
	private static boolean findCarrierToLand(final GameData data, final PlayerID player, final Unit unit, final Route route, final IntegerMap<Territory> usedCarrierSpace, final Set<Unit> movedCarriers)
	{
		//[Fixed] User specified route replaced with a new route with no territories in it, in 2 places.
		//[Fixed] During findCarrierToLand, if the air unit has no movement left, we return false for it, regardless of whether there are spaces for it at the end of the route.  This should be deleted.  We do not validate for the maximum movement for doing this route, during landing-spot validation.  Those get handled by 'validateBasic'.  Since we have to validate not just the units we have selected, but also the units at the end of the route, this could easily return false for a legal move if we have ever edited units, etc.
		//[Not Solved] We do not appear to check anywhere if our currently selection contains any carriers?
		//[Not Solved] We should check the territory we are moving to first, not last, as it might contain all the carriers we need.
		//[Not Solved] We should sort the landing spots in order of closeness to the end territory.
		//[Not Solved] We should figure out the landing spots before we go into the loop, so that we only have to sort them once.
		//[Not Solved] Possible bug with allowing air to go to neighboring territories too far away to reach.  I do not understand why there is a +1 to the movement left.....
		//[Not Solved] We should sort the carrier queue by order from least to most carrier capacity
		//[Fixed] Validation for possibly building a carrier under the fighters only checked for factories, not for canProduceUnits.
		//[Not Solved] At no point during the building-carrier-under-the-aircraft-check do we check if a carrier can hold more than 1 aircraft.
		//[Not Solved] It would be much smarter to move all the carriers FIRST to positions closer to the end territory, THEN start looking for whether our air can land on them.  (Instead of forcing the closest carriers to stay where they are, then checking even further carriers to see if they can reach)
		//[Not Solved] At no point do we remove the units in our mouse selection from the territory they are in, which could result in double capacity for that territory.
		//[Not Solved] The check for moving carriers only tests within 3 spaces of our end territory.  Instead, we need to test within [maximum carrier movement] + [maximum air movement left] territories (which on ww2v3 would mean 6 spaces, and with long range air would mean 8 spaces, and global 1940 would mean maybe 9 spaces)
		//[Not Solved] We do not check if the carrier we are moving already has fighters on it.  Those fighters could be owned or allied.  Owned ones might be able to find land, while allied ones have to move with the carrier.
		//[Not Solved] We also do not check if the carrier we are moving, or the carriers at our end territory, have allied fighters on them.  This would lower our total capacity too.
		//[Not Solved] Currently we try to move carriers belonging to allies in addition to owned carriers.  Only owned carriers can move, while allied have to stay put.
		//[FIXED] Currently we were trying to find carriers for bombers and other units that could not land. 
		
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
					return true; // TO DO FIX ME if you move 2 fighter groups to 2 seaspots near a factory it will allow you to do it even though there is only room for 1 fightergroup
				}
			}
		}
		
		// After all spots are checked and we can't find a good spot, we will check them again
		// but look further to see if we can find a friendly carrier that can reach us
		
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
	}*/
	
	/* Original Code, from Sean and ComradeKev:
	private static MoveValidationResult validateAirCanLand(final GameData data, final Collection<Unit> units, final Route route, final PlayerID player, final MoveValidationResult result,
				final IntegerMap<Unit> movementLeft)
	{
		if (getEditMode(data))
			return result;
		
		// nothing to check
		if (!Match.someMatch(units, Matches.UnitIsAir))
			return result;
		
		// get Route end
		final Territory routeEnd = route.getEnd();
		
		// we can land at the end, nothing left to check
		final CompositeMatch<Territory> friendlyGround = alliedNonConqueredNonPendingTerritory(data, player);
		if (friendlyGround.match(routeEnd))
			return result;
		
		// Find all the air units we'll need to account for
		final Match<Unit> ownedAirMatch = new CompositeMatchAnd<Unit>(Matches.UnitIsAir, Matches.unitOwnedBy(player));
		final Collection<Unit> ownedAir = new ArrayList<Unit>();
		ownedAir.addAll(Match.getMatches(units, ownedAirMatch));
		ownedAir.addAll(Match.getMatches(routeEnd.getUnits().getUnits(), ownedAirMatch));
		
		// Get the farthest we need to search for places to land (including current route length)
		// Generate the IntegerMap containing each aircraft's remaining movement
		final int maxMovement = getAirMovementLeft(data, units, ownedAir, route, player, movementLeft);
		
		// Get the distances to the nearest allied land and owned factory
		final int nearestFactory = getNearestFactory(data, route, player, maxMovement, friendlyGround);
		final int nearestLand = getNearestLand(data, route, player, maxMovement, friendlyGround);
		
		// find the air units that can't make it to land
		// TO DO interesting quirk- if true, aircraft may move their full movement, then one more on retreat due to method ensureCanMoveOneSpaceChange
		final boolean allowKamikaze = isKamikazeAircraft(data);
		final Collection<Unit> airThatMustLandOnCarriers = getAirThatMustLandOnCarriers(ownedAir, allowKamikaze, result, nearestLand, movementLeft);
		
		// we are done, everything can find a place to land
		if (airThatMustLandOnCarriers.isEmpty())
			return result;
		
		// Here's where we see if we have carriers available to land.
		// TO DO can possibly see if we're within remaining fuel from water and skip carriers if not
		// TO DO should we exclude existing air units from the following? I don't think so.
		// now, find out where we can land on carriers
		final IntegerMap<Integer> carrierCapacity = getInitialCarrierCapacity(data, units, route, player, maxMovement,
									airThatMustLandOnCarriers);
		
		// Check to see if there are carriers to be placed
		Collection<Unit> placeUnits = player.getUnits().getUnits();
		final CompositeMatch<Unit> unitIsSeaOrCanLandOnCarrier = new CompositeMatchOr<Unit>(Matches.UnitIsSea, Matches.UnitCanLandOnCarrier);
		placeUnits = Match.getMatches(placeUnits, unitIsSeaOrCanLandOnCarrier);
		final boolean landAirOnNewCarriers = AirThatCantLandUtil.isLHTRCarrierProduction(data) || AirThatCantLandUtil.isLandExistingFightersOnNewCarriers(data);
		final boolean hasProducedCarriers = player.getUnits().someMatch(Matches.UnitIsCarrier);
		if (landAirOnNewCarriers && hasProducedCarriers)
		{
			if (nearestFactory - 1 <= maxMovement)
			{
				placeUnits = Match.getMatches(placeUnits, Matches.UnitIsCarrier);
				carrierCapacity.put(new Integer(nearestFactory - 1), carrierCapacity.getInt(nearestFactory - 1) + carrierCapacity(placeUnits));
			}
		}
		
		final Collection<Territory> neighbors = data.getMap().getNeighbors(routeEnd, 1);
		final boolean anyNeighborsWater = Match.someMatch(neighbors, Matches.TerritoryIsWater);
		
		for (final Unit unit : Match.getMatches(units, Matches.UnitCanLandOnCarrier))
		{
			// If the aircraft can already land, skip it
			if (!airThatMustLandOnCarriers.contains(unit))
				continue;
			
			final int carrierCost = UnitAttachment.get(unit.getType()).getCarrierCost();
			final int movement = movementLeft.getInt(unit);
			
			for (int i = movement; i >= -1; i--)
			{
				if (i == -1 || (i == 0 && !routeEnd.isWater()) || (i == 1 && !anyNeighborsWater))
				{
					if (!allowKamikaze && !Matches.UnitIsKamikaze.match(unit))
						result.addDisallowedUnit(NOT_ALL_AIR_UNITS_CAN_LAND, unit);
					break;
				}
				
				// Check carriers that are within 'i' zones
				final Integer current = new Integer(i);
				if (carrierCost != -1 && carrierCapacity.getInt(current) >= carrierCost)
				{
					carrierCapacity.put(current, carrierCapacity.getInt(current) - carrierCost);
					break;
				}
				
				// Check carriers that could potentially move to within 'i' zones
				// TO DO need to subtract distance that fighter must move to reach water
				final Integer potentialWithNonComMove = new Integer(i) + 2;
				if (carrierCost != -1 && carrierCapacity.getInt(potentialWithNonComMove) >= carrierCost)
				{
					carrierCapacity.put(potentialWithNonComMove, carrierCapacity.getInt(potentialWithNonComMove) - carrierCost);
					break;
				}
				if (i == 0 && !allowKamikaze && !Matches.UnitIsKamikaze.match(unit))
					result.addDisallowedUnit(NOT_ALL_AIR_UNITS_CAN_LAND, unit);
			}
			
		}
		
		return result;
	}
	
	private static IntegerMap<Integer> getInitialCarrierCapacity(final GameData data, final Collection<Unit> units,
							final Route route, final PlayerID player, final int maxMovement, final Collection<Unit> airThatMustLandOnCarriers)
	{
		final IntegerMap<Integer> carrierCapacity = new IntegerMap<Integer>();
		
		// Add in the potential movement for owned carriers
		final int maxMoveIncludingCarrier = maxMovement + 2;
		final Territory currRouteEndTerr = route.getEnd();
		final Territory currRouteStartTerr = route.getStart();
		final int currRouteLength = route.getLength();
		
		// TO DO kev perhaps this could be moved up to where we're getting the neighbors for the max+1 above.
		final Collection<Territory> candidateTerritories = data.getMap().getNeighbors(currRouteEndTerr, maxMoveIncludingCarrier);
		candidateTerritories.add(currRouteEndTerr);
		
		final Match<Territory> isSea = Matches.TerritoryIsWater;
		final Match<Territory> canMoveThrough = new InverseMatch<Territory>(Matches.TerritoryIsImpassable);
		
		final Iterator<Territory> candidateIter = candidateTerritories.iterator();
		while (candidateIter.hasNext())
		{
			final Territory candidateTerr = candidateIter.next();
			final Route candidateRoute = data.getMap().getRoute(currRouteEndTerr, candidateTerr, canMoveThrough);
			
			if (candidateRoute == null)
				continue;
			final Integer candidateRouteLength = new Integer(candidateRoute.getLength());
			
			// Get the unitCollection of all units in the candidate territory.
			final UnitCollection CandidateTerrUnitColl = candidateTerr.getUnits();
			
			final Route seaRoute = data.getMap().getRoute(currRouteEndTerr, candidateTerr, isSea);
			Integer candidateSeaRouteLength = Integer.MAX_VALUE;
			if (seaRoute != null)
			{
				candidateSeaRouteLength = seaRoute.getLength();
			}
			
			// we don't want to count units that moved with us (all friendly units - moving units)
			final Collection<Unit> initialUnitsAtLocation = CandidateTerrUnitColl.getMatches(Matches.alliedUnit(player, data));
			initialUnitsAtLocation.removeAll(units);
			if (initialUnitsAtLocation.isEmpty())
				continue;
			
			// This is all owned units at the location
			final Collection<Unit> ownedUnitsAtLocation = CandidateTerrUnitColl.getMatches(Matches.unitIsOwnedBy(player));
			int extraCapacity = carrierCapacity(initialUnitsAtLocation) - carrierCost(ownedUnitsAtLocation);
			if (candidateTerr.equals(currRouteStartTerr))
			{
				extraCapacity += carrierCost(units);
			}
			
			// check carrierMustMoveWith, and reserve carrier capacity for allied planes as required
			final Collection<Unit> ownedCarrier = Match.getMatches(CandidateTerrUnitColl.getUnits(),
											new CompositeMatchAnd<Unit>(Matches.UnitIsCarrier, Matches.unitIsOwnedBy(player)));
			final Map<Unit, Collection<Unit>> mustMoveWith = carrierMustMoveWith(ownedCarrier, initialUnitsAtLocation, data, player);
			
			int alliedMustMoveCost = 0;
			for (final Unit unit : mustMoveWith.keySet())
			{
				final Collection<Unit> mustMovePlanes = mustMoveWith.get(unit);
				if (mustMovePlanes == null)
					continue;
				alliedMustMoveCost += carrierCost(mustMovePlanes);
			}
			
			// If the territory is within the maxMovement put the max of the existing capacity or the new capacity
			if ((maxMovement) >= candidateRouteLength)
				// carrierCapacity.put(candidateRouteLength, Math.max(carrierCapacity.getInt(candidateRouteLength), carrierCapacity.getInt(candidateRouteLength)- alliedMustMoveCost));
				carrierCapacity.put(candidateRouteLength, Math.max(carrierCapacity.getInt(candidateRouteLength), carrierCapacity.getInt(candidateRouteLength) + extraCapacity - alliedMustMoveCost));
			else
			{
				// Can move OWNED carriers to get them.
				// TO DO KEV change the -2 to the max movement remaining for carriers in the candidate territory.
				// This will fix finding carriers who have already used their move.
				if ((currRouteLength - maxMovement) >= candidateSeaRouteLength - 2)
				{
					if (ownedCarrier.size() > 0 && carrierCapacity(ownedCarrier) - mustMoveWith.size()
															- carrierCost(airThatMustLandOnCarriers) >= 0 &&
															MoveValidator.hasEnoughMovement(ownedCarrier, route.getLength()
																					- candidateSeaRouteLength))
						carrierCapacity.put(candidateSeaRouteLength, carrierCapacity.getInt(candidateSeaRouteLength) + extraCapacity - alliedMustMoveCost);
					// carrierCapacity.put(candidateSeaRouteLength, carrierCapacity.getInt(candidateSeaRouteLength) - alliedMustMoveCost);
				}
				
			}
		}
		return carrierCapacity;
	}
	
	private static int getNearestLand(final GameData data, final Route route, final PlayerID player, final int maxMovement, final CompositeMatch<Territory> friendlyGround)
	{
		return calculateNearestDistances(data, route, player, maxMovement, friendlyGround)[0];
	}
	
	private static int getNearestFactory(final GameData data, final Route route, final PlayerID player, final int maxMovement, final CompositeMatch<Territory> friendlyGround)
	{
		return calculateNearestDistances(data, route, player, maxMovement, friendlyGround)[1];
	}
	
	private static int[] calculateNearestDistances(final GameData data, final Route route, final PlayerID player, final int maxMovement, final CompositeMatch<Territory> friendlyGround)
	{
		int nearestLand = Integer.MAX_VALUE;
		int nearestFactory = Integer.MAX_VALUE;
		
		final Match<Territory> notNeutral = new InverseMatch<Territory>(Matches.TerritoryIsNeutralButNotWater);
		final Match<Territory> notNeutralAndNotImpassibleOrRestricted = new CompositeMatchAnd<Territory>(Matches.TerritoryIsPassableAndNotRestricted(player, data), notNeutral);
		final Match<Unit> ownedFactory = new CompositeMatchAnd<Unit>(Matches.UnitIsFactory, Matches.unitOwnedBy(player));
		final boolean UseNeutrals = (games.strategy.triplea.Properties.getNeutralFlyoverAllowed(data) && !isNeutralsImpassable(data));
		
		// find the closest land territory where everyone can land
		final Iterator<Territory> iter = data.getMap().getNeighbors(route.getEnd(), maxMovement + 1).iterator();
		
		while (iter.hasNext())
		{
			final Territory territory = iter.next();
			
			// can we land there?
			if (!friendlyGround.match(territory))
				continue;
			
			final boolean hasOwnedFactory = territory.getUnits().someMatch(ownedFactory);
			
			// Get a path WITHOUT using neutrals
			final Route noNeutralRoute = data.getMap().getRoute(route.getEnd(), territory, notNeutralAndNotImpassibleOrRestricted);
			if (noNeutralRoute != null)
			{
				nearestLand = Math.min(nearestLand, noNeutralRoute.getLength());
				
				// Get nearest factory
				if (hasOwnedFactory)
				{
					nearestFactory = Math.min(nearestFactory, noNeutralRoute.getLength());
				}
			}
			// Get a path WITH using neutrals
			if (UseNeutrals)
			{
				final Route neutralViolatingRoute = data.getMap().getRoute(route.getEnd(), territory, Matches.TerritoryIsPassableAndNotRestricted(player, data));
				if ((neutralViolatingRoute != null) && getNeutralCharge(data, neutralViolatingRoute) <= player.getResources().getQuantity(Constants.PUS))
				{
					nearestLand = Math.min(nearestLand, neutralViolatingRoute.getLength());
					
					// Get nearest factory
					if (hasOwnedFactory)
					{
						nearestFactory = Math.min(nearestFactory, neutralViolatingRoute.getLength());
					}
				}
			}
		}
		return new int[] { nearestLand, nearestFactory };
	}
	
	private static int getAirMovementLeft(final GameData data, final Collection<Unit> units, final Collection<Unit> ownedAir, final Route route, final PlayerID player,
				final IntegerMap<Unit> movementLeft)
	{
		// this is the farthest we need to look for places to land
		// Set up everything we'll need
		final Territory startTerr = route.getStart();
		final Territory endTerr = route.getEnd();
		
		final int routeLength = route.getLength();
		int maxMovement = 0;
		boolean startAirBase = false;
		boolean endAirBase = false;
		final TerritoryAttachment taStart = TerritoryAttachment.get(startTerr);
		final TerritoryAttachment taEnd = TerritoryAttachment.get(endTerr);
		
		if (taStart != null && taStart.getAirBase() && data.getRelationshipTracker().isAllied(startTerr.getOwner(), player))
			startAirBase = true;
		
		if (taEnd != null && taEnd.getAirBase() && data.getRelationshipTracker().isAllied(endTerr.getOwner(), player))
			endAirBase = true;
		
		// Go through the list of units and set each unit's movement as well as the overall group maxMovement
		final Iterator<Unit> ownedAirIter = ownedAir.iterator();
		while (ownedAirIter.hasNext())
		{
			final TripleAUnit unit = (TripleAUnit) ownedAirIter.next();
			int movement = unit.getMovementLeft();
			// int left = TripleAUnit.get(ownedAirIter.next()).getMovementLeft();
			if (units.contains(unit))
				movement -= routeLength;
			// If the unit started at an airbase, or is within max range of an airbase, increase the range.
			if (startAirBase)
				movement++;
			
			if (endAirBase)
				movement++;
			
			maxMovement = Math.max(movement, maxMovement);
			
			movementLeft.put(unit, movement);
		}
		
		return maxMovement;
	}*/
}
