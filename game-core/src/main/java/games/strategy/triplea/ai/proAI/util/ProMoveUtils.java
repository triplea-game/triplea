package games.strategy.triplea.ai.proAI.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.ai.proAI.ProData;
import games.strategy.triplea.ai.proAI.data.ProTerritory;
import games.strategy.triplea.ai.proAI.logging.ProLogger;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.remote.IMoveDelegate;

/**
 * Pro AI move utilities.
 */
public class ProMoveUtils {

  public static void calculateMoveRoutes(final PlayerID player, final List<Collection<Unit>> moveUnits,
      final List<Route> moveRoutes, final Map<Territory, ProTerritory> attackMap, final boolean isCombatMove) {

    final GameData data = ProData.getData();

    // Find all amphib units
    final Set<Unit> amphibUnits = attackMap.values().stream()
        .map(ProTerritory::getAmphibAttackMap)
        .map(Map::entrySet)
        .flatMap(Collection::stream)
        .flatMap(e -> Stream.concat(Stream.of(e.getKey()), e.getValue().stream()))
        .collect(Collectors.toSet());

    // Loop through all territories to attack
    for (final Territory t : attackMap.keySet()) {

      // Loop through each unit that is attacking the current territory
      for (final Unit u : attackMap.get(t).getUnits()) {

        // Skip amphib units
        if (amphibUnits.contains(u)) {
          continue;
        }

        // Skip if unit is already in move to territory
        final Territory startTerritory = ProData.unitTerritoryMap.get(u);
        if ((startTerritory == null) || startTerritory.equals(t)) {
          continue;
        }

        // Add unit to move list
        final List<Unit> unitList = new ArrayList<>();
        unitList.add(u);
        moveUnits.add(unitList);

        // If carrier has dependent allied fighters then move them too
        if (Matches.unitIsCarrier().test(u)) {
          final Map<Unit, Collection<Unit>> carrierMustMoveWith =
              MoveValidator.carrierMustMoveWith(startTerritory.getUnits().getUnits(), startTerritory, data, player);
          if (carrierMustMoveWith.containsKey(u)) {
            unitList.addAll(carrierMustMoveWith.get(u));
          }
        }

        // Determine route and add to move list
        Route route = null;
        if (unitList.stream().anyMatch(Matches.unitIsSea())) {

          // Sea unit (including carriers with planes)
          route = data.getMap().getRoute_IgnoreEnd(startTerritory, t,
              ProMatches.territoryCanMoveSeaUnitsThrough(player, data, isCombatMove));
        } else if (!unitList.isEmpty() && unitList.stream().allMatch(Matches.unitIsLand())) {

          // Land unit
          route = data.getMap().getRoute_IgnoreEnd(startTerritory, t, ProMatches
              .territoryCanMoveLandUnitsThrough(player, data, u, startTerritory, isCombatMove, new ArrayList<>()));
        } else if (!unitList.isEmpty() && unitList.stream().allMatch(Matches.unitIsAir())) {

          // Air unit
          route = data.getMap().getRoute_IgnoreEnd(startTerritory, t,
              ProMatches.territoryCanMoveAirUnitsAndNoAa(player, data, isCombatMove));
        }
        if (route == null) {
          ProLogger.warn(data.getSequence().getRound() + "-" + data.getSequence().getStep().getName()
              + ": route is null " + startTerritory + " to " + t + ", units=" + unitList);
        }
        moveRoutes.add(route);
      }
    }
  }

  public static void calculateAmphibRoutes(final PlayerID player, final List<Collection<Unit>> moveUnits,
      final List<Route> moveRoutes, final List<Collection<Unit>> transportsToLoad,
      final Map<Territory, ProTerritory> attackMap, final boolean isCombatMove) {

    final GameData data = ProData.getData();

    // Loop through all territories to attack
    for (final Territory t : attackMap.keySet()) {

      // Loop through each amphib attack map
      final Map<Unit, List<Unit>> amphibAttackMap = attackMap.get(t).getAmphibAttackMap();
      for (final Unit transport : amphibAttackMap.keySet()) {
        int movesLeft = TripleAUnit.get(transport).getMovementLeft();
        Territory transportTerritory = ProData.unitTerritoryMap.get(transport);

        // Check if units are already loaded or not
        final List<Unit> loadedUnits = new ArrayList<>();
        final List<Unit> remainingUnitsToLoad = new ArrayList<>();
        if (TransportTracker.isTransporting(transport)) {
          loadedUnits.addAll(amphibAttackMap.get(transport));
        } else {
          remainingUnitsToLoad.addAll(amphibAttackMap.get(transport));
        }

        // Load units and move transport
        while (movesLeft >= 0) {

          // Load adjacent units if no enemies present in transport territory
          if (Matches.territoryHasEnemyUnits(player, data).negate().test(transportTerritory)) {
            final List<Unit> unitsToRemove = new ArrayList<>();
            for (final Unit amphibUnit : remainingUnitsToLoad) {
              if (data.getMap().getDistance(transportTerritory, ProData.unitTerritoryMap.get(amphibUnit)) == 1) {
                moveUnits.add(Collections.singletonList(amphibUnit));
                transportsToLoad.add(Collections.singletonList(transport));
                final Route route = new Route(ProData.unitTerritoryMap.get(amphibUnit), transportTerritory);
                moveRoutes.add(route);
                unitsToRemove.add(amphibUnit);
                loadedUnits.add(amphibUnit);
              }
            }
            for (final Unit u : unitsToRemove) {
              remainingUnitsToLoad.remove(u);
            }
          }

          // Move transport if I'm not already at the end or out of moves
          final Territory unloadTerritory = attackMap.get(t).getTransportTerritoryMap().get(transport);
          int distanceFromEnd = data.getMap().getDistance(transportTerritory, t);
          if (t.isWater()) {
            distanceFromEnd++;
          }
          if ((movesLeft > 0) && ((distanceFromEnd > 1) || !remainingUnitsToLoad.isEmpty()
              || ((unloadTerritory != null) && !unloadTerritory.equals(transportTerritory)))) {
            final Set<Territory> neighbors = data.getMap().getNeighbors(transportTerritory,
                ProMatches.territoryCanMoveSeaUnitsThrough(player, data, isCombatMove));
            Territory territoryToMoveTo = null;
            int minUnitDistance = Integer.MAX_VALUE;
            int maxDistanceFromEnd = Integer.MIN_VALUE; // Used to move to farthest away loading territory first
            for (final Territory neighbor : neighbors) {
              if (MoveValidator.validateCanal(new Route(transportTerritory, neighbor),
                  Collections.singletonList(transport), player, data) != null) {
                continue;
              }
              int distanceFromUnloadTerritory = 0;
              if (unloadTerritory != null) {
                distanceFromUnloadTerritory = data.getMap().getDistance_IgnoreEndForCondition(neighbor, unloadTerritory,
                    ProMatches.territoryCanMoveSeaUnitsThrough(player, data, isCombatMove));
              }
              int neighborDistanceFromEnd = data.getMap().getDistance_IgnoreEndForCondition(neighbor, t,
                  ProMatches.territoryCanMoveSeaUnitsThrough(player, data, isCombatMove));
              if (t.isWater()) {
                neighborDistanceFromEnd++;
              }
              int maxUnitDistance = 0;
              for (final Unit u : remainingUnitsToLoad) {
                final int distance = data.getMap().getDistance(neighbor, ProData.unitTerritoryMap.get(u));
                if (distance > maxUnitDistance) {
                  maxUnitDistance = distance;
                }
              }
              if ((neighborDistanceFromEnd <= movesLeft) && (maxUnitDistance <= minUnitDistance)
                  && (distanceFromUnloadTerritory < movesLeft)
                  && ((maxUnitDistance < minUnitDistance)
                  || ((maxUnitDistance > 1) && (neighborDistanceFromEnd > maxDistanceFromEnd))
                  || ((maxUnitDistance <= 1) && (neighborDistanceFromEnd < maxDistanceFromEnd)))) {
                territoryToMoveTo = neighbor;
                minUnitDistance = maxUnitDistance;
                if (neighborDistanceFromEnd > maxDistanceFromEnd) {
                  maxDistanceFromEnd = neighborDistanceFromEnd;
                }
              }
            }
            if (territoryToMoveTo != null) {
              final List<Unit> unitsToMove = new ArrayList<>();
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
        if (!remainingUnitsToLoad.isEmpty()) {
          ProLogger.warn(data.getSequence().getRound() + "-" + data.getSequence().getStep().getName() + ": " + t
              + ", remainingUnitsToLoad=" + remainingUnitsToLoad);
        }

        // Set territory transport is moving to
        attackMap.get(t).getTransportTerritoryMap().put(transport, transportTerritory);

        // Unload transport
        if (!loadedUnits.isEmpty() && !t.isWater()) {
          moveUnits.add(loadedUnits);
          transportsToLoad.add(null);
          final Route route = new Route(transportTerritory, t);
          moveRoutes.add(route);
        }
      }
    }
  }

  public static void calculateBombardMoveRoutes(final PlayerID player, final List<Collection<Unit>> moveUnits,
      final List<Route> moveRoutes, final Map<Territory, ProTerritory> attackMap) {

    final GameData data = ProData.getData();

    // Loop through all territories to attack
    for (final ProTerritory t : attackMap.values()) {

      // Loop through each unit that is attacking the current territory
      for (final Unit u : t.getBombardTerritoryMap().keySet()) {
        final Territory bombardFromTerritory = t.getBombardTerritoryMap().get(u);

        // Skip if unit is already in move to territory
        final Territory startTerritory = ProData.unitTerritoryMap.get(u);
        if (startTerritory.equals(bombardFromTerritory)) {
          continue;
        }

        // Add unit to move list
        final List<Unit> unitList = new ArrayList<>();
        unitList.add(u);
        moveUnits.add(unitList);

        // Determine route and add to move list
        Route route = null;
        if (!unitList.isEmpty() && unitList.stream().allMatch(ProMatches.unitCanBeMovedAndIsOwnedSea(player, true))) {

          // Naval unit
          route = data.getMap().getRoute_IgnoreEnd(startTerritory, bombardFromTerritory,
              ProMatches.territoryCanMoveSeaUnitsThrough(player, data, true));
        }
        moveRoutes.add(route);
      }
    }
  }

  public static void calculateBombingRoutes(final PlayerID player, final List<Collection<Unit>> moveUnits,
      final List<Route> moveRoutes, final Map<Territory, ProTerritory> attackMap) {

    final GameData data = ProData.getData();

    // Loop through all territories to attack
    for (final Territory t : attackMap.keySet()) {

      // Loop through each unit that is attacking the current territory
      for (final Unit u : attackMap.get(t).getBombers()) {

        // Skip if unit is already in move to territory
        final Territory startTerritory = ProData.unitTerritoryMap.get(u);
        if ((startTerritory == null) || startTerritory.equals(t)) {
          continue;
        }

        // Add unit to move list
        final List<Unit> unitList = new ArrayList<>();
        unitList.add(u);
        moveUnits.add(unitList);

        // Determine route and add to move list
        Route route = null;
        if (!unitList.isEmpty() && unitList.stream().allMatch(Matches.unitIsAir())) {
          route = data.getMap().getRoute_IgnoreEnd(startTerritory, t,
              ProMatches.territoryCanMoveAirUnitsAndNoAa(player, data, true));
        }
        moveRoutes.add(route);
      }
    }
  }

  public static void doMove(final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes,
      final IMoveDelegate moveDel) {
    doMove(moveUnits, moveRoutes, null, moveDel);
  }

  public static void doMove(final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes,
      final List<Collection<Unit>> transportsToLoad, final IMoveDelegate moveDel) {

    final GameData data = ProData.getData();

    // Group non-amphib units of the same type moving on the same route
    if (transportsToLoad == null) {
      for (int i = 0; i < moveRoutes.size(); i++) {
        final Route r = moveRoutes.get(i);
        for (int j = i + 1; j < moveRoutes.size(); j++) {
          final Route r2 = moveRoutes.get(j);
          if (r.equals(r2)) {
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
    for (int i = 0; i < moveRoutes.size(); i++) {
      if (!ProData.isSimulation) {
        ProUtils.pause();
      }
      if ((moveRoutes.get(i) == null)
          || (moveRoutes.get(i).getEnd() == null)
          || (moveRoutes.get(i).getStart() == null)) {
        ProLogger.warn(data.getSequence().getRound() + "-" + data.getSequence().getStep().getName()
            + ": route not valid " + moveRoutes.get(i) + " units: " + moveUnits.get(i));
        continue;
      }
      final String result;
      if ((transportsToLoad == null) || (transportsToLoad.get(i) == null)) {
        result = moveDel.move(moveUnits.get(i), moveRoutes.get(i));
      } else {
        result = moveDel.move(moveUnits.get(i), moveRoutes.get(i), transportsToLoad.get(i));
      }
      if (result != null) {
        ProLogger.warn(data.getSequence().getRound() + "-" + data.getSequence().getStep().getName()
            + ": could not move " + moveUnits.get(i) + " over " + moveRoutes.get(i) + " because: " + result);
      }
    }
  }
}
