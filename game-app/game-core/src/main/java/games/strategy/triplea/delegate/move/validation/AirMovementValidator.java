package games.strategy.triplea.delegate.move.validation;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.AbstractMoveDelegate;
import games.strategy.triplea.delegate.EditDelegate;
import games.strategy.triplea.delegate.GameStepPropertiesHelper;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveDelegate;
import games.strategy.triplea.delegate.data.MoveValidationResult;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

/** Provides static methods for evaluating movement of air units. */
public final class AirMovementValidator {
  public static final String NOT_ALL_AIR_UNITS_CAN_LAND = "Not all air units can land";

  private AirMovementValidator() {}

  // TODO: this class does a pretty good job already, but could be improved by having the carriers
  // that are potentially moved also look for any owned air units that are in sea zones without
  // carriers. these would be air units that have already been moved this turn, and therefore would
  // need pickup.
  static MoveValidationResult validateAirCanLand(
      final Collection<Unit> units,
      final Route route,
      final GamePlayer player,
      final MoveValidationResult result) {
    final GameData data = player.getData();
    // First check if we even need to check
    if (getEditMode(data) // Edit Mode, no need to check
        || units.stream().noneMatch(Matches.unitIsAir()) // No air units, nothing to check
        || route.hasNoSteps() // if there are no steps, we didn't move, so it is always OK!
        // we can land at the end, nothing left to check
        || Matches.airCanLandOnThisAlliedNonConqueredLandTerritory(player).test(route.getEnd())
        // if kamikaze - we do not do any validation at all, cus they can all die and we don't care
        || Properties.getKamikazeAirplanes(data.getProperties())) {
      return result;
    }
    // Find which aircraft cannot find friendly land to land on
    final Collection<Unit> ownedAirThatMustLandOnCarriers =
        getAirThatMustLandOnCarriers(
            data, getAirUnitsToValidate(units, route, player), route, result);
    if (ownedAirThatMustLandOnCarriers.isEmpty()) {
      // we are done, everything can find a place to land
      return result;
    }
    final Territory routeEnd = route.getEnd();
    final Territory routeStart = route.getStart();
    // we cannot forget to account for allied air at our location already
    final Predicate<Unit> airAlliedNotOwned =
        Matches.unitIsOwnedBy(player)
            .negate()
            .and(Matches.isUnitAllied(player))
            .and(Matches.unitIsAir())
            .and(Matches.unitCanLandOnCarrier());
    final Set<Unit> airThatMustLandOnCarriersHash = new HashSet<>();
    airThatMustLandOnCarriersHash.addAll(
        CollectionUtils.getMatches(routeEnd.getUnits(), airAlliedNotOwned));
    airThatMustLandOnCarriersHash.addAll(CollectionUtils.getMatches(units, airAlliedNotOwned));
    // now we must see if we also need to account for units (allied cargo) that are moving with our
    // carriers, if we have
    // selected any carriers
    final Collection<Unit> movingCarriersAtStartLocationBeingMoved =
        CollectionUtils.getMatches(units, Matches.unitIsCarrier());
    if (!movingCarriersAtStartLocationBeingMoved.isEmpty()) {
      final Map<Unit, Collection<Unit>> carrierToAlliedCargo =
          MoveValidator.carrierMustMoveWith(units, routeStart, player);
      for (final Collection<Unit> alliedAirOnCarrier : carrierToAlliedCargo.values()) {
        airThatMustLandOnCarriersHash.addAll(alliedAirOnCarrier);
      }
    }
    // now we can add our owned air. we add our owned air last because it can be moved, while allied
    // air cannot be. we
    // want the lowest movement to be validated first.
    airThatMustLandOnCarriersHash.addAll(ownedAirThatMustLandOnCarriers);
    final List<Unit> airThatMustLandOnCarriers = new ArrayList<>(airThatMustLandOnCarriersHash);
    // sort the list by shortest range first so those birds will get first pick of landing spots
    airThatMustLandOnCarriers.sort(
        getLowestToHighestMovementComparatorIncludingUnitsNotYetMoved(route));
    // now we should see if the carriers we are moving with, plus the carriers already there, can
    // handle all our air
    // units (we check ending territories first, separately, because it is special [it includes
    // units in our selection])
    final Collection<Unit> carriersAtEnd =
        CollectionUtils.getMatches(getFriendly(routeEnd, player, data), Matches.unitIsCarrier());
    carriersAtEnd.addAll(movingCarriersAtStartLocationBeingMoved);
    // to keep track of all carriers, and their fighters, that have moved, so that we do not move
    // them again.
    final Map<Unit, Collection<Unit>> movedCarriersAndTheirFighters = new HashMap<>();
    for (final Unit carrier : carriersAtEnd) {
      movedCarriersAndTheirFighters.put(carrier, new ArrayList<>());
    }
    final Collection<Unit> airNotToConsiderBecauseWeAreValidatingThem =
        new ArrayList<>(airThatMustLandOnCarriers);
    airThatMustLandOnCarriers.removeAll(
        whatAirCanLandOnTheseCarriers(carriersAtEnd, airThatMustLandOnCarriers, routeEnd));
    if (airThatMustLandOnCarriers.isEmpty()) {
      return result;
    }
    // we still have air left, so begin calling carriers to come here to pick up the air

    // figure out what is the max distance of our remaining air units
    // TODO: consider each air unit separately with movement costs
    final int maxMovementLeftForTheseAirUnitsBeingValidated =
        maxMovementLeftForTheseAirUnitsBeingValidated(airThatMustLandOnCarriers, route, player)
            .intValue();
    // figure out what is the max distance of our remaining carrier units
    // TODO: consider each carrier unit separately with movement costs
    final int maxMovementLeftForAllOwnedCarriers =
        maxMovementLeftForAllOwnedCarriers(player, data).intValue();
    final List<Territory> landingSpots = new ArrayList<>(Set.of(routeEnd));
    landingSpots.addAll(
        data.getMap()
            .getNeighbors(
                routeEnd,
                maxMovementLeftForTheseAirUnitsBeingValidated,
                // where can we fly to?
                Matches.airCanFlyOver(player, areNeutralsPassableByAir(data))));
    // we only want to consider
    landingSpots.removeAll(
        CollectionUtils.getMatches(landingSpots, Matches.seaCanMoveOver(player).negate()));
    // places we can move carriers to
    landingSpots.sort(getLowestToHighestDistance(routeEnd, Matches.seaCanMoveOver(player)));
    final Collection<Territory> potentialCarrierOrigins = new LinkedHashSet<>(landingSpots);
    potentialCarrierOrigins.addAll(
        data.getMap()
            .getNeighbors(
                new HashSet<>(landingSpots),
                maxMovementLeftForAllOwnedCarriers,
                Matches.seaCanMoveOver(player)));
    potentialCarrierOrigins.remove(routeEnd);
    potentialCarrierOrigins.removeAll(
        CollectionUtils.getMatches(
            potentialCarrierOrigins, Matches.territoryHasOwnedCarrier(player).negate()));
    // now see if we can move carriers there to pick up
    validateAirCaughtByMovingCarriersAndOwnedAndAlliedAir(
        result,
        landingSpots,
        potentialCarrierOrigins,
        movedCarriersAndTheirFighters,
        airThatMustLandOnCarriers,
        airNotToConsiderBecauseWeAreValidatingThem,
        player,
        route,
        data);
    return result;
  }

  private static Map<Unit, BigDecimal> getMovementLeftForValidatingAir(
      final Collection<Unit> airBeingValidated, final GamePlayer player, final Route route) {
    final LinkedHashMap<Unit, BigDecimal> map = new LinkedHashMap<>();
    for (final Unit unit : airBeingValidated) {
      // unit must be in either start or end.
      final BigDecimal movementLeft;
      if (Matches.unitIsOwnedBy(player).test(unit)) {
        movementLeft = getMovementLeftForAirUnitNotMovedYet(unit, route);
      } else {
        movementLeft = BigDecimal.ZERO;
      }
      map.put(unit, movementLeft);
    }
    return map;
  }

  private static BigDecimal getMovementLeftForAirUnitNotMovedYet(
      final Unit airBeingValidated, final Route route) {
    return route.getEnd().getUnits().contains(airBeingValidated)
        // they are not being moved, they are already at the end
        ? airBeingValidated.getMovementLeft()
        // they are being moved (they are still at the start location)
        : airBeingValidated //
            .getMovementLeft()
            .subtract(route.getMovementCost(airBeingValidated));
  }

  private static IntegerMap<Territory> populateStaticAlliedAndBuildingCarrierCapacity(
      final List<Territory> landingSpots,
      final Map<Unit, Collection<Unit>> movedCarriersAndTheirFighters,
      final GamePlayer player,
      final GameData data) {
    final IntegerMap<Territory> startingSpace = new IntegerMap<>();
    final Predicate<Unit> carrierAlliedNotOwned =
        Matches.unitIsOwnedBy(player)
            .negate()
            .and(Matches.isUnitAllied(player))
            .and(Matches.unitIsCarrier());
    final boolean landAirOnNewCarriers =
        Properties.getLhtrCarrierProductionRules(data.getProperties())
            || Properties.getLandExistingFightersOnNewCarriers(data.getProperties());
    final List<Unit> carriersInProductionQueue =
        GameStepPropertiesHelper.getCombinedTurns(data, player).stream()
            .map(GamePlayer::getUnitCollection)
            .map(units -> units.getMatches(Matches.unitIsCarrier()))
            .flatMap(List::stream)
            .collect(Collectors.toList());
    for (final Territory t : landingSpots) {
      if (landAirOnNewCarriers
          && !carriersInProductionQueue.isEmpty()
          && Matches.territoryIsWater().test(t)
          && Matches.territoryHasOwnedAtBeginningOfTurnIsFactoryOrCanProduceUnitsNeighbor(player)
              .test(t)) {
        // TODO: Here we are assuming that this factory can produce all of the carriers. Actually
        // it might not be able to produce any carriers (because of complex requires units coding)
        // or because of unit damage or maximum production.
        // TODO: Here we are also assuming that the first territory we find that has an adjacent
        // factory is the closest one in terms of unit movement. We have sorted the list of
        // territories so this IS the closest in terms of steps, but each unit may have specific
        // movement allowances for different terrain.
        final int producedCarrierCapacity = carrierCapacity(carriersInProductionQueue, t);
        startingSpace.add(t, producedCarrierCapacity);
        carriersInProductionQueue.clear();
      }
      final Collection<Unit> alliedCarriers = t.getMatches(carrierAlliedNotOwned);
      alliedCarriers.removeAll(movedCarriersAndTheirFighters.keySet());
      final int alliedCarrierCapacity = carrierCapacity(alliedCarriers, t);
      startingSpace.add(t, alliedCarrierCapacity);
    }
    return startingSpace;
  }

  private static void validateAirCaughtByMovingCarriersAndOwnedAndAlliedAir(
      final MoveValidationResult result,
      final List<Territory> landingSpots,
      final Collection<Territory> potentialCarrierOrigins,
      final Map<Unit, Collection<Unit>> movedCarriersAndTheirFighters,
      final Collection<Unit> airThatMustLandOnCarriers,
      final Collection<Unit> airNotToConsider,
      final GamePlayer player,
      final Route route,
      final GameData data) {
    final Predicate<Unit> ownedCarrierMatch =
        Matches.unitIsOwnedBy(player).and(Matches.unitIsCarrier());
    final Predicate<Unit> ownedAirMatch =
        Matches.unitIsOwnedBy(player).and(Matches.unitIsAir()).and(Matches.unitCanLandOnCarrier());
    final Predicate<Unit> alliedNotOwnedAirMatch =
        Matches.unitIsOwnedBy(player)
            .negate()
            .and(Matches.isUnitAllied(player))
            .and(Matches.unitIsAir())
            .and(Matches.unitCanLandOnCarrier());
    final Predicate<Unit> alliedNotOwnedCarrierMatch =
        Matches.unitIsOwnedBy(player)
            .negate()
            .and(Matches.isUnitAllied(player))
            .and(Matches.unitIsCarrier());
    final Territory routeEnd = route.getEnd();
    final boolean areNeutralsPassableByAir = areNeutralsPassableByAir(data);
    final IntegerMap<Territory> landingSpotsWithCarrierCapacity =
        // fill our landing spot capacity with capacity from allied carriers and potential building
        // of new carriers
        populateStaticAlliedAndBuildingCarrierCapacity(
            landingSpots, movedCarriersAndTheirFighters, player, data);
    final Map<Unit, BigDecimal> movementLeftForAirToValidate =
        // calculate movement left only once
        getMovementLeftForValidatingAir(airThatMustLandOnCarriers, player, route);
    for (final Territory landingSpot : landingSpots) {
      // since we are here, no point looking at this place twice
      potentialCarrierOrigins.remove(landingSpot);
      final List<Unit> airCanReach = new ArrayList<>();
      for (final Unit air : airThatMustLandOnCarriers) {
        if (canAirReachThisSpot(
            air,
            data,
            player,
            routeEnd,
            movementLeftForAirToValidate.get(air),
            landingSpot,
            areNeutralsPassableByAir)) {
          // get all air that can reach this spot
          airCanReach.add(air);
        }
      }
      if (airCanReach.isEmpty()) {
        continue;
      }
      final Collection<Unit> unitsInLandingSpot = new ArrayList<>(landingSpot.getUnits());
      unitsInLandingSpot.removeAll(movedCarriersAndTheirFighters.keySet());
      // make sure to remove any units we have already moved, or units that are excluded
      unitsInLandingSpot.removeAll(airNotToConsider);
      // because they are in our mouse selection
      for (final Collection<Unit> fighters : movedCarriersAndTheirFighters.values()) {
        // also remove any fighters that are being moved with carriers that we have already moved
        unitsInLandingSpot.removeAll(fighters);
      }
      final Collection<Unit> ownedCarriersInLandingSpot =
          CollectionUtils.getMatches(unitsInLandingSpot, ownedCarrierMatch);
      // get air we own here, but exclude any air that can fly to allied land
      final Collection<Unit> airInLandingSpot =
          CollectionUtils.getMatches(
              CollectionUtils.getMatches(unitsInLandingSpot, ownedAirMatch),
              unitCanFindLand(data, landingSpot).negate());
      // add allied air (it can't fly away)
      airInLandingSpot.addAll(
          CollectionUtils.getMatches(unitsInLandingSpot, alliedNotOwnedAirMatch));
      // make sure we don't count this again
      // airNotToConsider.addAll(airInLandingSpot);
      // get the current capacity
      int landingSpotCapacity = landingSpotsWithCarrierCapacity.getInt(landingSpot);
      // add capacity of owned carriers
      landingSpotCapacity += carrierCapacity(ownedCarriersInLandingSpot, landingSpot);
      // minus capacity of air in the territory
      landingSpotCapacity -= carrierCost(airInLandingSpot);
      final Iterator<Unit> airIter = airCanReach.iterator();
      while (airIter.hasNext()) {
        final Unit air = airIter.next();
        final int carrierCost = carrierCost(air);
        if (landingSpotCapacity >= carrierCost) {
          landingSpotCapacity -= carrierCost;
          // we can land this one here, yay
          airThatMustLandOnCarriers.remove(air);
          airIter.remove();
        }
      }
      if (airThatMustLandOnCarriers.isEmpty()) {
        // all can land here, so return
        return;
      }
      // final int lowestCarrierCost = getLowestCarrierCost(airCanReach);
      // now bring carriers here...
      final Iterator<Territory> iter = potentialCarrierOrigins.iterator();
      while (iter.hasNext()) {
        final Territory carrierSpot = iter.next();
        final Collection<Unit> unitsInCarrierSpot = new ArrayList<>(carrierSpot.getUnits());
        // remove carriers we have already moved
        unitsInCarrierSpot.removeAll(movedCarriersAndTheirFighters.keySet());
        // remove units we do not want to consider because they are in our mouse selection
        unitsInCarrierSpot.removeAll(airNotToConsider);
        for (final Collection<Unit> ftrs : movedCarriersAndTheirFighters.values()) {
          // remove the fighters that are moving with the carriers we have already moved
          unitsInCarrierSpot.removeAll(ftrs);
        }
        final Collection<Unit> ownedCarriersInCarrierSpot =
            CollectionUtils.getMatches(unitsInCarrierSpot, ownedCarrierMatch);
        if (ownedCarriersInCarrierSpot.isEmpty()) {
          iter.remove();
          continue;
        }
        final Collection<Unit> ownedAirInCarrierSpot =
            CollectionUtils.getMatches(
                // exclude any owned air that can fly to land
                CollectionUtils.getMatches(unitsInCarrierSpot, ownedAirMatch),
                unitCanFindLand(data, carrierSpot).negate());
        final Collection<Unit> alliedNotOwnedAirInCarrierSpot =
            CollectionUtils.getMatches(unitsInCarrierSpot, alliedNotOwnedAirMatch);
        final Map<Unit, Collection<Unit>> mustMoveWithMap =
            // this only returns the allied cargo
            MoveValidator.carrierMustMoveWith(ownedCarriersInCarrierSpot, carrierSpot, player);
        // planes that MUST travel with the carrier
        // get the current capacity for the carrier spot
        int carrierSpotCapacity = landingSpotsWithCarrierCapacity.getInt(carrierSpot);
        // we don't have it because this spot is not in the landing zone area.
        if (!landingSpotsWithCarrierCapacity.containsKey(carrierSpot)) {
          // we still have a capacity for allied carriers, but only to carry other allied or local
          // owned units, not to
          // carry our selected units.
          carrierSpotCapacity =
              carrierCapacity(carrierSpot.getMatches(alliedNotOwnedCarrierMatch), carrierSpot);
          landingSpotsWithCarrierCapacity.put(carrierSpot, carrierSpotCapacity);
        }
        // we have allied air here, so we need to account for them before moving any carriers
        if (!alliedNotOwnedAirInCarrierSpot.isEmpty() || !mustMoveWithMap.isEmpty()) {
          // mustMoveWithMap is only filled if we have so many allied air that our owned carriers
          // are carrying some of
          // them
          if (mustMoveWithMap.isEmpty()) {
            // allied carriers can carry enough
            carrierSpotCapacity -= carrierCost(alliedNotOwnedAirInCarrierSpot);
            // we do not want to consider these units again
            airNotToConsider.addAll(alliedNotOwnedAirInCarrierSpot);
            if (carrierSpotCapacity > 0) {
              // we can hold some of the owned air here too
              final Iterator<Unit> ownedIter = ownedAirInCarrierSpot.iterator();
              while (ownedIter.hasNext()) {
                final Unit air = ownedIter.next();
                final int carrierCost = carrierCost(air);
                if (carrierSpotCapacity >= carrierCost) {
                  carrierSpotCapacity -= carrierCost;
                  // we do not want to consider this one again
                  airNotToConsider.add(air);
                  ownedIter.remove();
                }
              }
            }
            // put correct value for future reference now that we have considered the allied air
            landingSpotsWithCarrierCapacity.put(carrierSpot, carrierSpotCapacity);
          } else {
            // carrierMustMoveWith does not account for any allied cargo already moved out.
            for (final Collection<Unit> airMovingWith : mustMoveWithMap.values()) {
              for (final Collection<Unit> ftrs : movedCarriersAndTheirFighters.values()) {
                // remove the fighters that are moving with the carriers we have already moved
                airMovingWith.removeAll(ftrs);
              }
            }
            for (final Collection<Unit> airMovingWith : mustMoveWithMap.values()) {
              // we will consider these as part of their moving carrier
              alliedNotOwnedAirInCarrierSpot.removeAll(airMovingWith);
            }
            carrierSpotCapacity -= carrierCost(alliedNotOwnedAirInCarrierSpot);
            // we do not want to consider these units again
            airNotToConsider.addAll(alliedNotOwnedAirInCarrierSpot);
            // put correct value for future reference now that we have considered the allied air
            landingSpotsWithCarrierCapacity.put(carrierSpot, carrierSpotCapacity);
          }
        }
        final Optional<Route> optionalToLandingSpot =
            data.getMap()
                .getRouteForUnits(
                    carrierSpot,
                    landingSpot,
                    Matches.seaCanMoveOver(player),
                    ownedCarriersInCarrierSpot,
                    player);
        if (optionalToLandingSpot.isEmpty()) {
          continue;
        }
        final List<Unit> carriersThatCanReach =
            CollectionUtils.getMatches(
                ownedCarriersInCarrierSpot,
                Matches.unitHasEnoughMovementForRoute(optionalToLandingSpot.get()));
        if (carriersThatCanReach.isEmpty()) {
          // none can reach
          continue;
        }
        final List<Unit> carriersThatCantReach = new ArrayList<>(ownedCarriersInCarrierSpot);
        // we want to see if the air units can be put on the carriers that cannot make it
        // first, before taking up room on the carriers that can make it
        carriersThatCantReach.removeAll(carriersThatCanReach);
        final List<Unit> allCarriers = new ArrayList<>(carriersThatCantReach);
        // so we remove them from the list then re-add them so that they will be at the end of the
        // list
        allCarriers.addAll(carriersThatCanReach);
        // now we want to make a map of the carriers to the units they must carry with them (both
        // allied and owned)
        final Map<Unit, Collection<Unit>> carriersToMove = new HashMap<>();
        final List<Unit> carrierFull = new ArrayList<>();
        for (final Unit carrier : allCarriers) {
          final Collection<Unit> airMovingWith = new ArrayList<>();
          // first add allied cargo
          final Collection<Unit> alliedMovingWith = mustMoveWithMap.get(carrier);
          if (alliedMovingWith != null) {
            airMovingWith.addAll(alliedMovingWith);
          }
          // now test if our carrier has any room for owned fighters
          int carrierCapacity = carrierCapacity(carrier, carrierSpot);
          carrierCapacity -= carrierCost(airMovingWith);
          final Iterator<Unit> ownedIter = ownedAirInCarrierSpot.iterator();
          while (ownedIter.hasNext()) {
            final Unit air = ownedIter.next();
            final int carrierCost = carrierCost(air);
            if (carrierCapacity >= carrierCost) {
              carrierCapacity -= carrierCost;
              airMovingWith.add(air);
              ownedIter.remove();
            }
          }
          carriersToMove.put(carrier, airMovingWith);
          if (carrierCapacity <= 0) {
            carrierFull.add(carrier);
          }
        }
        // if all carriers full, remove this carrier spot from consideration
        if (carrierFull.containsAll(allCarriers)) {
          iter.remove();
          continue;
        }
        if (carrierFull.containsAll(carriersThatCantReach)) {
          iter.remove();
        }
        // ok, now lets move them.
        for (final Unit carrier : carriersThatCanReach) {
          movedCarriersAndTheirFighters.put(carrier, carriersToMove.get(carrier));
          landingSpotCapacity += carrierCapacity(carrier, carrierSpot);
          landingSpotCapacity -= carrierCost(carriersToMove.get(carrier));
        }
        // optional for debugging
        final Iterator<Unit> reachIter = airCanReach.iterator();
        while (reachIter.hasNext()) {
          final Unit air = reachIter.next();
          final int carrierCost = carrierCost(air);
          if (landingSpotCapacity >= carrierCost) {
            landingSpotCapacity -= carrierCost;
            // we can land this one here, yay
            airThatMustLandOnCarriers.remove(air);
            reachIter.remove();
          }
        }
        if (airThatMustLandOnCarriers.isEmpty()) {
          // all can land here, so return
          return;
        }
      }
    }
    // anyone left over cannot land
    for (final Unit air : airThatMustLandOnCarriers) {
      result.addDisallowedUnit(NOT_ALL_AIR_UNITS_CAN_LAND, air);
    }
  }

  private static Comparator<Territory> getLowestToHighestDistance(
      final Territory territoryWeMeasureDistanceFrom, final Predicate<Territory> condition) {
    return (t1, t2) -> {
      if (t1.equals(t2)) {
        return 0;
      }
      final GameMap map = t1.getData().getMap();
      final int distance1 = map.getDistance(territoryWeMeasureDistanceFrom, t1, condition);
      final int distance2 = map.getDistance(territoryWeMeasureDistanceFrom, t2, condition);
      if (distance1 == distance2) {
        return 0;
      }
      if (distance1 < 0) {
        return 1;
      }
      if (distance2 < 0) {
        return -1;
      }
      if (distance1 < distance2) {
        return -1;
      }
      return 1;
    };
  }

  private static BigDecimal maxMovementLeftForAllOwnedCarriers(
      final GamePlayer player, final GameState data) {
    BigDecimal max = BigDecimal.ZERO;
    final Predicate<Unit> ownedCarrier = Matches.unitIsCarrier().and(Matches.unitIsOwnedBy(player));
    for (final Territory t : data.getMap().getTerritories()) {
      for (final Unit carrier : t.getMatches(ownedCarrier)) {
        max = max.max(carrier.getMovementLeft());
      }
    }
    return max;
  }

  private static BigDecimal maxMovementLeftForTheseAirUnitsBeingValidated(
      final Collection<Unit> airUnits, final Route route, final GamePlayer player) {
    BigDecimal max = BigDecimal.ZERO;
    for (final Unit u : airUnits) {
      if (Matches.unitIsOwnedBy(player).test(u)) {
        // unit must be in either start or end.
        final BigDecimal movementLeft = getMovementLeftForAirUnitNotMovedYet(u, route);
        if (movementLeft.compareTo(max) > 0) {
          max = movementLeft;
        }
      }
      // allied units can't move....
    }
    return max;
  }

  /**
   * Does not, and is not supposed to, account for any units already on this carrier (like
   * allied/cargo fighters). Instead this method only adds up the total capacity of each unit, and
   * accounts for damaged carriers with special properties and restrictions. So should pass
   * allied/cargo fighters in and have them first in the list.
   */
  public static Collection<Unit> whatAirCanLandOnTheseCarriers(
      final Collection<Unit> carriers,
      final Collection<Unit> airUnits,
      final Territory territoryUnitsAreIn) {
    final Collection<Unit> airThatCanLandOnThem = new ArrayList<>();
    for (final Unit carrier : carriers) {
      int carrierCapacity = carrierCapacity(carrier, territoryUnitsAreIn);
      for (final Unit air : airUnits) {
        if (airThatCanLandOnThem.contains(air)) {
          continue;
        }
        final int airCost = carrierCost(air);
        if (carrierCapacity >= airCost) {
          carrierCapacity -= airCost;
          airThatCanLandOnThem.add(air);
        }
      }
    }
    return airThatCanLandOnThem;
  }

  /**
   * Returns the combination of units that fly here and the existing owned units.
   *
   * @param units the units flying this route.
   * @param route the route flown
   * @param player the player owning the units
   */
  private static List<Unit> getAirUnitsToValidate(
      final Collection<Unit> units, final Route route, final GamePlayer player) {
    final Predicate<Unit> ownedAirMatch =
        Matches.unitIsAir()
            .and(Matches.unitIsOwnedBy(player))
            .and(Matches.unitIsKamikaze().negate());
    final List<Unit> ownedAir = new ArrayList<>();
    ownedAir.addAll(CollectionUtils.getMatches(route.getEnd().getUnits(), ownedAirMatch));
    ownedAir.addAll(CollectionUtils.getMatches(units, ownedAirMatch));

    // Remove suicide units if combat move and any enemy units at destination
    final Collection<Unit> enemyUnitsAtEnd = route.getEnd().getMatches(Matches.enemyUnit(player));
    if (!enemyUnitsAtEnd.isEmpty() && GameStepPropertiesHelper.isCombatMove(player.getData())) {
      ownedAir.removeIf(Matches.unitIsSuicideOnAttack());
    }

    // Sort the list by shortest range first so those birds will get first pick of landing spots
    ownedAir.sort(getLowestToHighestMovementComparatorIncludingUnitsNotYetMoved(route));
    return ownedAir;
  }

  private static Comparator<Unit> getLowestToHighestMovementComparatorIncludingUnitsNotYetMoved(
      final Route route) {
    return Comparator.comparing(u -> getMovementLeftForAirUnitNotMovedYet(u, route));
  }

  private static boolean canAirReachThisSpot(
      final Unit unit,
      final GameState data,
      final GamePlayer player,
      final Territory currentSpot,
      final BigDecimal movementLeft,
      final Territory landingSpot,
      final boolean areNeutralsPassableByAir) {
    final Optional<Route> optionalRoute =
        data.getMap()
            .getRouteForUnit(
                currentSpot,
                landingSpot,
                Matches.airCanFlyOver(player, areNeutralsPassableByAir),
                unit,
                player);
    return (optionalRoute.isPresent())
        && (optionalRoute.get().getMovementCost(unit).compareTo(movementLeft) <= 0)
        && (!areNeutralsPassableByAir
            || getNeutralCharge(data, optionalRoute.get())
                <= player.getResources().getQuantity(Constants.PUS));
  }

  /**
   * Indicates this air unit reached a safe landing at this point in the route.
   *
   * @param unit the air unit in question
   * @param route the current spot from which he needs to reach safe land.
   * @return whether the air-unit can find a stretch of friendly land to land on given her current
   *     spot and the remaining range.
   */
  private static boolean canFindLand(final GameData data, final Unit unit, final Route route) {
    final Territory routeEnd = route.getEnd();
    // unit must be in either start or end.
    final BigDecimal movementLeft = getMovementLeftForAirUnitNotMovedYet(unit, route);
    return canFindLand(data, unit, routeEnd, movementLeft);
  }

  private static boolean canFindLand(
      final GameData data, final Unit unit, final Territory current) {
    final BigDecimal movementLeft = unit.getMovementLeft();
    return canFindLand(data, unit, current, movementLeft);
  }

  private static boolean canFindLand(
      final GameData data,
      final Unit unit,
      final Territory current,
      final BigDecimal movementLeft) {
    if (movementLeft.compareTo(BigDecimal.ZERO) < 0) {
      return false;
    }
    final boolean areNeutralsPassableByAir = areNeutralsPassableByAir(data);
    final GamePlayer player = unit.getOwner();
    final List<Territory> possibleSpots =
        CollectionUtils.getMatches(
            data.getMap()
                .getNeighborsByMovementCost(
                    current, movementLeft, Matches.airCanFlyOver(player, areNeutralsPassableByAir)),
            Matches.airCanLandOnThisAlliedNonConqueredLandTerritory(player));
    for (final Territory landingSpot : possibleSpots) {
      if (canAirReachThisSpot(
          unit, data, player, current, movementLeft, landingSpot, areNeutralsPassableByAir)) {
        return true;
      }
    }
    return false;
  }

  private static Predicate<Unit> unitCanFindLand(final GameData data, final Territory current) {
    return u -> canFindLand(data, u, current);
  }

  /**
   * Returns true if the given air units can land in the given territory. Does take into account
   * whether a battle has been fought in the territory already. Note units must only be air units
   */
  public static boolean canLand(
      final Collection<Unit> airUnits,
      final Territory territory,
      final GamePlayer player,
      final GameData data) {
    if (airUnits.isEmpty() || !airUnits.stream().allMatch(Matches.unitIsAir())) {
      throw new IllegalArgumentException("can only test if air will land");
    }
    if (!territory.isWater()
        && AbstractMoveDelegate.getBattleTracker(data).wasConquered(territory)) {
      return false;
    }
    if (territory.isWater()) {
      // if they cant all land on carriers
      if (airUnits.isEmpty() || !airUnits.stream().allMatch(Matches.unitCanLandOnCarrier())) {
        return false;
      }
      // when doing the calculation, make sure to include the units in the territory
      final Set<Unit> friendly = new HashSet<>();
      friendly.addAll(getFriendly(territory, player, data));
      friendly.addAll(airUnits);
      // make sure we have the carrier capacity
      final int capacity = carrierCapacity(friendly, territory);
      final int cost = carrierCost(friendly);
      return capacity >= cost;
    }

    return data.getRelationshipTracker().canLandAirUnitsOnOwnedLand(player, territory.getOwner());
  }

  private static Collection<Unit> getAirThatMustLandOnCarriers(
      final GameData data,
      final Collection<Unit> ownedAir,
      final Route route,
      final MoveValidationResult result) {
    final Collection<Unit> airThatMustLandOnCarriers = new ArrayList<>();
    final Predicate<Unit> canLandOnCarriers = Matches.unitCanLandOnCarrier();
    for (final Unit unit : ownedAir) {
      if (!canFindLand(data, unit, route)) {
        if (canLandOnCarriers.test(unit)) {
          airThatMustLandOnCarriers.add(unit);
        } else {
          // not everything can land on a carrier (i.e. bombers)
          result.addDisallowedUnit(NOT_ALL_AIR_UNITS_CAN_LAND, unit);
        }
      }
    }
    return airThatMustLandOnCarriers;
  }

  /**
   * Does not, and is not supposed to, account for any units already on this carrier (like
   * allied/cargo fighters). Instead this method only adds up the total capacity of each unit, and
   * accounts for damaged carriers with special properties and restrictions.
   */
  public static int carrierCapacity(
      final Collection<Unit> units, final Territory territoryUnitsAreCurrentlyIn) {
    int sum = 0;
    for (final Unit unit : units) {
      sum += carrierCapacity(unit, territoryUnitsAreCurrentlyIn);
    }
    return sum;
  }

  /**
   * Does not, and is not supposed to, account for any units already on this carrier (like
   * allied/cargo fighters). Instead this method only adds up the total capacity of each unit, and
   * accounts for damaged carriers with special properties and restrictions.
   */
  public static int carrierCapacity(final Unit unit, final Territory territoryUnitsAreCurrentlyIn) {
    if (!Matches.unitIsCarrier().test(unit)) {
      return 0;
    }
    // here we check to see if the unit can no longer carry units
    if (!Matches.unitHasWhenCombatDamagedEffect(UnitAttachment.UNITS_MAY_NOT_LAND_ON_CARRIER)
        .test(unit)) {
      return unit.getUnitAttachment().getCarrierCapacity();
    }
    // and we must check to make sure we let any allied air that are cargo stay here
    if (!Matches.unitHasWhenCombatDamagedEffect(UnitAttachment.UNITS_MAY_NOT_LEAVE_ALLIED_CARRIER)
        .test(unit)) {
      // capacity = zero 0
      return 0;
    }
    int cargo = 0;
    final Collection<Unit> airCargo =
        territoryUnitsAreCurrentlyIn.getMatches(
            Matches.unitIsAir().and(Matches.unitCanLandOnCarrier()));
    for (final Unit airUnit : airCargo) {
      if (airUnit.getTransportedBy() != null && airUnit.getTransportedBy().equals(unit)) {
        // capacity = are cargo only
        cargo += airUnit.getUnitAttachment().getCarrierCost();
      }
    }
    return cargo;
  }

  public static int carrierCost(final Collection<Unit> units) {
    int sum = 0;
    for (final Unit unit : units) {
      sum += carrierCost(unit);
    }
    return sum;
  }

  public static int carrierCost(final Unit unit) {
    if (Matches.unitCanLandOnCarrier().test(unit)) {
      return unit.getUnitAttachment().getCarrierCost();
    }
    return 0;
  }

  private static boolean getEditMode(final GameState data) {
    return EditDelegate.getEditMode(data.getProperties());
  }

  public static Collection<Unit> getFriendly(
      final Territory territory, final GamePlayer player, final GameState data) {
    return territory.getMatches(Matches.alliedUnit(player));
  }

  private static boolean areNeutralsPassableByAir(final GameState data) {
    return Properties.getNeutralFlyoverAllowed(data.getProperties())
        && !Properties.getNeutralsImpassable(data.getProperties());
  }

  private static int getNeutralCharge(final GameState data, final Route route) {
    return getNeutralCharge(data, MoveDelegate.getEmptyNeutral(route).size());
  }

  private static int getNeutralCharge(final GameState data, final int numberOfTerritories) {
    return numberOfTerritories * Properties.getNeutralCharge(data.getProperties());
  }
}
