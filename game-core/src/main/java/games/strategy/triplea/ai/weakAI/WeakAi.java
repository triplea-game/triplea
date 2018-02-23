package games.strategy.triplea.ai.weakAI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import com.google.common.collect.Streams;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.ai.AbstractAi;
import games.strategy.triplea.ai.AiUtils;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.BattleDelegate;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.dataObjects.PlaceableUnits;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;

/**
 * A very weak ai, based on some simple rules.
 */
public class WeakAi extends AbstractAi {

  public WeakAi(final String name, final String type) {
    super(name, type);
  }

  @Override
  protected void tech(final ITechDelegate techDelegate, final GameData data, final PlayerID player) {}

  private static Route getAmphibRoute(final PlayerID player, final GameData data) {
    if (!isAmphibAttack(player, data)) {
      return null;
    }
    final Territory ourCapitol = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
    final Predicate<Territory> endMatch = o -> {
      final boolean impassable = (TerritoryAttachment.get(o) != null) && TerritoryAttachment.get(o).getIsImpassable();
      return !impassable && !o.isWater() && Utils.hasLandRouteToEnemyOwnedCapitol(o, player, data);
    };
    final Predicate<Territory> routeCond =
        Matches.territoryIsWater().and(Matches.territoryHasNoEnemyUnits(player, data));
    final Route withNoEnemy = Utils.findNearest(ourCapitol, endMatch, routeCond, data);
    if ((withNoEnemy != null) && (withNoEnemy.numberOfSteps() > 0)) {
      return withNoEnemy;
    }
    // this will fail if our capitol is not next to water, c'est la vie.
    final Route route = Utils.findNearest(ourCapitol, endMatch, Matches.territoryIsWater(), data);
    if ((route != null) && (route.numberOfSteps() == 0)) {
      return null;
    }
    return route;
  }

  private static boolean isAmphibAttack(final PlayerID player, final GameData data) {
    final Territory capitol = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
    // we dont own our own capitol
    if ((capitol == null) || !capitol.getOwner().equals(player)) {
      return false;
    }
    // find a land route to an enemy territory from our capitol
    final Route invasionRoute =
        Utils.findNearest(capitol, Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassableOrRestricted(player, data),
            Matches.territoryIsLand().and(Matches.territoryIsNeutralButNotWater().negate()), data);
    return invasionRoute == null;
  }

  @Override
  protected void move(final boolean nonCombat, final IMoveDelegate moveDel, final GameData data,
      final PlayerID player) {
    if (nonCombat) {
      doNonCombatMove(moveDel, player, data);
    } else {
      doCombatMove(moveDel, player, data);
    }
    pause();
  }

  private void doNonCombatMove(final IMoveDelegate moveDel, final PlayerID player, final GameData data) {
    final List<Collection<Unit>> moveUnits = new ArrayList<>();
    final List<Route> moveRoutes = new ArrayList<>();
    final List<Collection<Unit>> transportsToLoad = new ArrayList<>();
    // load the transports first
    // they may be able to move farther
    populateTransportLoad(data, moveUnits, moveRoutes, transportsToLoad, player);
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
    populateTransportLoad(data, moveUnits, moveRoutes, transportsToLoad, player);
    doMove(moveUnits, moveRoutes, transportsToLoad, moveDel);
    moveRoutes.clear();
    moveUnits.clear();
    transportsToLoad.clear();
    // unload the transports that can be unloaded
    populateTransportUnloadNonCom(data, moveUnits, moveRoutes, player);
    doMove(moveUnits, moveRoutes, null, moveDel);
  }

  private void doCombatMove(final IMoveDelegate moveDel, final PlayerID player, final GameData data) {
    final List<Collection<Unit>> moveUnits = new ArrayList<>();
    final List<Route> moveRoutes = new ArrayList<>();
    final List<Collection<Unit>> transportsToLoad = new ArrayList<>();
    // load the transports first
    // they may be able to take part in a battle
    populateTransportLoad(data, moveUnits, moveRoutes, transportsToLoad, player);
    doMove(moveUnits, moveRoutes, transportsToLoad, moveDel);
    moveRoutes.clear();
    moveUnits.clear();
    // we want to move loaded transports before we try to fight our battles
    populateNonCombatSea(false, data, moveUnits, moveRoutes, player);
    // find second amphib target
    final Route altRoute = getAlternativeAmphibRoute(player, data);
    if (altRoute != null) {
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

  private void populateTransportLoad(final GameData data, final List<Collection<Unit>> moveUnits,
      final List<Route> moveRoutes, final List<Collection<Unit>> transportsToLoad, final PlayerID player) {
    if (!isAmphibAttack(player, data)) {
      return;
    }
    final Territory capitol = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
    if ((capitol == null) || !capitol.getOwner().equals(player)) {
      return;
    }
    List<Unit> unitsToLoad = capitol.getUnits().getMatches(Matches.unitIsInfrastructure().negate());
    unitsToLoad = CollectionUtils.getMatches(unitsToLoad, Matches.unitIsOwnedBy(getPlayerId()));
    for (final Territory neighbor : data.getMap().getNeighbors(capitol)) {
      if (!neighbor.isWater()) {
        continue;
      }
      final List<Unit> units = new ArrayList<>();
      for (final Unit transport : neighbor.getUnits().getMatches(Matches.unitIsOwnedBy(player))) {
        int free = TransportTracker.getAvailableCapacity(transport);
        if (free <= 0) {
          continue;
        }
        final Iterator<Unit> iter = unitsToLoad.iterator();
        while (iter.hasNext() && (free > 0)) {
          final Unit current = iter.next();
          final UnitAttachment ua = UnitAttachment.get(current.getType());
          if (ua.getIsAir()) {
            continue;
          }
          if (ua.getTransportCost() <= free) {
            iter.remove();
            free -= ua.getTransportCost();
            units.add(current);
          }
        }
      }
      if (units.size() > 0) {
        final Route route = new Route();
        route.setStart(capitol);
        route.add(neighbor);
        moveUnits.add(units);
        moveRoutes.add(route);
        transportsToLoad.add(neighbor.getUnits().getMatches(Matches.unitIsTransport()));
      }
    }
  }

  private static void populateTransportUnloadNonCom(final GameData data, final List<Collection<Unit>> moveUnits,
      final List<Route> moveRoutes, final PlayerID player) {
    final Route amphibRoute = getAmphibRoute(player, data);
    if (amphibRoute == null) {
      return;
    }
    final Territory lastSeaZoneOnAmphib = amphibRoute.getAllTerritories().get(amphibRoute.numberOfSteps() - 1);
    final Territory landOn = amphibRoute.getEnd();
    final Predicate<Unit> landAndOwned = Matches.unitIsLand().and(Matches.unitIsOwnedBy(player));
    final List<Unit> units = lastSeaZoneOnAmphib.getUnits().getMatches(landAndOwned);
    if (units.size() > 0) {
      // just try to make the move, the engine will stop us if it doesnt work
      final Route route = new Route();
      route.setStart(lastSeaZoneOnAmphib);
      route.add(landOn);
      moveUnits.add(units);
      moveRoutes.add(route);
    }
  }

  private static List<Unit> load2Transports(final List<Unit> transportsToLoad) {
    final List<Unit> units = new ArrayList<>();
    for (final Unit transport : transportsToLoad) {
      final Collection<Unit> landunits = TransportTracker.transporting(transport);
      units.addAll(landunits);
    }
    return units;
  }

  private static void doMove(final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes,
      final List<Collection<Unit>> transportsToLoad, final IMoveDelegate moveDel) {
    for (int i = 0; i < moveRoutes.size(); i++) {
      pause();
      if ((moveRoutes.get(i) == null) || (moveRoutes.get(i).getEnd() == null) || (moveRoutes.get(i).getStart() == null)
          || moveRoutes.get(i).hasNoSteps()) {
        continue;
      }

      if (transportsToLoad == null) {
        moveDel.move(moveUnits.get(i), moveRoutes.get(i));
      } else {
        moveDel.move(moveUnits.get(i), moveRoutes.get(i), transportsToLoad.get(i));
      }
    }
  }

  private static void moveCombatSea(final GameData data, final List<Collection<Unit>> moveUnits,
      final List<Route> moveRoutes, final PlayerID player, final Route amphibRoute, final int maxTrans) {
    // TODO workaround - should check if amphibRoute is in moveRoutes
    if (moveRoutes.size() == 2) {
      moveRoutes.remove(1);
      moveUnits.remove(1);
    }
    if (amphibRoute == null) {
      return;
    }
    final Territory firstSeaZoneOnAmphib = amphibRoute.getAllTerritories().get(0);
    final Territory lastSeaZoneOnAmphib = amphibRoute.getAllTerritories().get(amphibRoute.numberOfSteps() - 1);
    final Predicate<Unit> ownedAndNotMoved = Matches.unitIsOwnedBy(player)
        .and(Matches.unitHasNotMoved())
        .and(Matches.unitIsTransporting());
    final List<Unit> unitsToMove = new ArrayList<>();
    final List<Unit> transports = firstSeaZoneOnAmphib.getUnits().getMatches(ownedAndNotMoved);
    if (transports.size() <= maxTrans) {
      unitsToMove.addAll(transports);
    } else {
      unitsToMove.addAll(transports.subList(0, maxTrans));
    }
    final List<Unit> landUnits = load2Transports(unitsToMove);
    final Route r = getMaxSeaRoute(data, firstSeaZoneOnAmphib, lastSeaZoneOnAmphib, player);
    moveRoutes.add(r);
    unitsToMove.addAll(landUnits);
    moveUnits.add(unitsToMove);
  }

  /**
   * prepares moves for transports.
   */
  private static void populateNonCombatSea(final boolean nonCombat, final GameData data,
      final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player) {
    final Route amphibRoute = getAmphibRoute(player, data);
    Territory firstSeaZoneOnAmphib = null;
    Territory lastSeaZoneOnAmphib = null;
    if (amphibRoute != null) {
      firstSeaZoneOnAmphib = amphibRoute.getAllTerritories().get(1);
      lastSeaZoneOnAmphib = amphibRoute.getAllTerritories().get(amphibRoute.numberOfSteps() - 1);
    }
    final Predicate<Unit> ownedAndNotMoved = Matches.unitIsOwnedBy(player).and(Matches.unitHasNotMoved());
    for (final Territory t : data.getMap()) {
      // move sea units to the capitol, unless they are loaded transports
      if (t.isWater()) {
        // land units, move all towards the end point
        if (t.getUnits().anyMatch(Matches.unitIsLand())) {
          // move along amphi route
          if (lastSeaZoneOnAmphib != null) {
            // two move route to end
            final Route r = getMaxSeaRoute(data, t, lastSeaZoneOnAmphib, player);
            if ((r != null) && (r.numberOfSteps() > 0)) {
              moveRoutes.add(r);
              final List<Unit> unitsToMove = t.getUnits().getMatches(Matches.unitIsOwnedBy(player));
              moveUnits.add(unitsToMove);
            }
          }
        }
        if (nonCombat && t.getUnits().anyMatch(ownedAndNotMoved)) {
          // move toward the start of the amphib route
          if (firstSeaZoneOnAmphib != null) {
            final Route r = getMaxSeaRoute(data, t, firstSeaZoneOnAmphib, player);
            moveRoutes.add(r);
            moveUnits.add(t.getUnits().getMatches(ownedAndNotMoved));
          }
        }
      }
    }
  }

  private static Route getMaxSeaRoute(final GameData data, final Territory start, final Territory destination,
      final PlayerID player) {
    final Predicate<Territory> routeCond = Matches.territoryIsWater()
        .and(Matches.territoryHasEnemyUnits(player, data).negate())
        .and(Matches.territoryHasNonAllowedCanal(player, null, data).negate());
    Route r = data.getMap().getRoute(start, destination, routeCond);
    if (r == null) {
      return null;
    }
    if (r.numberOfSteps() > 2) {
      final Route newRoute = new Route();
      newRoute.setStart(start);
      newRoute.add(r.getAllTerritories().get(1));
      newRoute.add(r.getAllTerritories().get(2));
      r = newRoute;
    }
    return r;
  }

  private static void populateCombatMoveSea(final GameData data, final List<Collection<Unit>> moveUnits,
      final List<Route> moveRoutes, final PlayerID player) {
    final Collection<Unit> unitsAlreadyMoved = new HashSet<>();
    for (final Territory t : data.getMap()) {
      if (!t.isWater()) {
        continue;
      }
      if (!t.getUnits().anyMatch(Matches.enemyUnit(player, data))) {
        continue;
      }
      final Territory enemy = t;
      final float enemyStrength = AiUtils.strength(enemy.getUnits().getUnits(), false, true);
      if (enemyStrength > 0) {
        final Predicate<Unit> attackable = Matches.unitIsOwnedBy(player).and(o -> !unitsAlreadyMoved.contains(o));
        final Set<Territory> dontMoveFrom = new HashSet<>();
        // find our strength that we can attack with
        float ourStrength = 0;
        final Collection<Territory> attackFrom = data.getMap().getNeighbors(enemy, Matches.territoryIsWater());
        for (final Territory owned : attackFrom) {
          // dont risk units we are carrying
          if (owned.getUnits().anyMatch(Matches.unitIsLand())) {
            dontMoveFrom.add(owned);
            continue;
          }
          ourStrength += AiUtils.strength(owned.getUnits().getMatches(attackable), true, true);
        }
        if (ourStrength > (1.32 * enemyStrength)) {
          for (final Territory owned : attackFrom) {
            if (dontMoveFrom.contains(owned)) {
              continue;
            }
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
  private static Route getAlternativeAmphibRoute(final PlayerID player, final GameData data) {
    if (!isAmphibAttack(player, data)) {
      return null;
    }
    final Predicate<Territory> routeCondition =
        Matches.territoryIsWater().and(Matches.territoryHasNoEnemyUnits(player, data));
    // should select all territories with loaded transports
    final Predicate<Territory> transportOnSea =
        Matches.territoryIsWater().and(Matches.territoryHasLandUnitsOwnedBy(player));
    Route altRoute = null;
    final int length = Integer.MAX_VALUE;
    for (final Territory t : data.getMap()) {
      if (!transportOnSea.test(t)) {
        continue;
      }
      final Predicate<Unit> ownedTransports = Matches.unitCanTransport()
          .and(Matches.unitIsOwnedBy(player))
          .and(Matches.unitHasNotMoved());
      final Predicate<Territory> enemyTerritory = Matches.isTerritoryEnemy(player, data)
          .and(Matches.territoryIsLand())
          .and(Matches.territoryIsNeutralButNotWater().negate())
          .and(Matches.territoryIsEmpty());
      final int trans = t.getUnits().countMatches(ownedTransports);
      if (trans > 0) {
        final Route newRoute = Utils.findNearest(t, enemyTerritory, routeCondition, data);
        if ((newRoute != null) && (length > newRoute.numberOfSteps())) {
          altRoute = newRoute;
        }
      }
    }
    return altRoute;
  }

  private void populateNonCombat(final GameData data, final List<Collection<Unit>> moveUnits,
      final List<Route> moveRoutes, final PlayerID player) {
    final Collection<Territory> territories = data.getMap().getTerritories();
    movePlanesHomeNonCom(moveUnits, moveRoutes, player, data);
    // move our units toward the nearest enemy capitol
    for (final Territory t : territories) {
      if (t.isWater()) {
        continue;
      }
      if ((TerritoryAttachment.get(t) != null) && TerritoryAttachment.get(t).isCapital()) {
        // if they are a threat to take our capitol, dont move
        // compare the strength of units we can place
        final float ourStrength = AiUtils.strength(player.getUnits().getUnits(), false, false);
        final float attackerStrength = Utils.getStrengthOfPotentialAttackers(t, data);
        if (attackerStrength > ourStrength) {
          continue;
        }
      }
      // these are the units we can move
      final Predicate<Unit> moveOfType = Matches.unitIsOwnedBy(player)
          .and(Matches.unitIsNotAa())
          // we can never move factories
          .and(Matches.unitCanMove())
          .and(Matches.unitIsNotInfrastructure())
          .and(Matches.unitIsLand());
      final Predicate<Territory> moveThrough = Matches.territoryIsImpassable().negate()
          .and(Matches.territoryIsNeutralButNotWater().negate())
          .and(Matches.territoryIsLand());
      final List<Unit> units = t.getUnits().getMatches(moveOfType);
      if (units.size() == 0) {
        continue;
      }
      int minDistance = Integer.MAX_VALUE;
      Territory to = null;
      // find the nearest enemy owned capital
      for (final PlayerID otherPlayer : data.getPlayerList().getPlayers()) {
        final Territory capitol = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(otherPlayer, data);
        if ((capitol != null) && !data.getRelationshipTracker().isAllied(player, capitol.getOwner())) {
          final Route route = data.getMap().getRoute(t, capitol, moveThrough);
          if (route != null) {
            final int distance = route.numberOfSteps();
            if ((distance != 0) && (distance < minDistance)) {
              minDistance = distance;
              to = capitol;
            }
          }
        }
      }
      if (to != null) {
        if (units.size() > 0) {
          moveUnits.add(units);
          final Route routeToCapitol = data.getMap().getRoute(t, to, moveThrough);
          final Territory firstStep = routeToCapitol.getAllTerritories().get(1);
          final Route route = new Route();
          route.setStart(t);
          route.add(firstStep);
          moveRoutes.add(route);
        }
      } else { // if we cant move to a capitol, move towards the enemy
        final Predicate<Territory> routeCondition = Matches.territoryIsLand()
            .and(Matches.territoryIsImpassable().negate());
        Route newRoute = Utils.findNearest(t, Matches.territoryHasEnemyLandUnits(player, data), routeCondition, data);
        // move to any enemy territory
        if (newRoute == null) {
          newRoute = Utils.findNearest(t, Matches.isTerritoryEnemy(player, data), routeCondition, data);
        }
        if ((newRoute != null) && (newRoute.numberOfSteps() != 0)) {
          moveUnits.add(units);
          final Territory firstStep = newRoute.getAllTerritories().get(1);
          final Route route = new Route();
          route.setStart(t);
          route.add(firstStep);
          moveRoutes.add(route);
        }
      }
    }
  }

  private void movePlanesHomeNonCom(final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes,
      final PlayerID player, final GameData data) {
    // the preferred way to get the delegate
    final IMoveDelegate delegateRemote = (IMoveDelegate) getPlayerBridge().getRemoteDelegate();
    // this works because we are on the server
    final BattleDelegate delegate = DelegateFinder.battleDelegate(data);
    final Predicate<Territory> canLand = Matches.isTerritoryAllied(player, data)
        .and(o -> !delegate.getBattleTracker().wasConquered(o));
    final Predicate<Territory> routeCondition = Matches.territoryHasEnemyAaForCombatOnly(player, data).negate()
        .and(Matches.territoryIsImpassable().negate());
    for (final Territory t : delegateRemote.getTerritoriesWhereAirCantLand()) {
      final Route noAaRoute = Utils.findNearest(t, canLand, routeCondition, data);
      final Route aaRoute = Utils.findNearest(t, canLand, Matches.territoryIsImpassable().negate(), data);
      final Collection<Unit> airToLand =
          t.getUnits().getMatches(Matches.unitIsAir().and(Matches.unitIsOwnedBy(player)));
      // dont bother to see if all the air units have enough movement points
      // to move without aa guns firing
      // simply move first over no aa, then with aa
      // one (but hopefully not both) will be rejected
      moveUnits.add(airToLand);
      moveRoutes.add(noAaRoute);
      moveUnits.add(airToLand);
      moveRoutes.add(aaRoute);
    }
  }

  private static void populateCombatMove(final GameData data, final List<Collection<Unit>> moveUnits,
      final List<Route> moveRoutes, final PlayerID player) {
    populateBomberCombat(data, moveUnits, moveRoutes, player);
    final Collection<Unit> unitsAlreadyMoved = new HashSet<>();
    // find the territories we can just walk into
    final Predicate<Territory> walkInto =
        Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassableOrRestricted(player, data)
            .or(Matches.isTerritoryFreeNeutral(data));
    final List<Territory> enemyOwned = CollectionUtils.getMatches(data.getMap().getTerritories(), walkInto);
    Collections.shuffle(enemyOwned);
    Collections.sort(enemyOwned, (o1, o2) -> {
      // -1 means o1 goes first. 1 means o2 goes first. zero means they are equal.
      if (Objects.equals(o1, o2)) {
        return 0;
      }
      if (o1 == null) {
        return 1;
      }
      if (o2 == null) {
        return -1;
      }
      final TerritoryAttachment ta1 = TerritoryAttachment.get(o1);
      final TerritoryAttachment ta2 = TerritoryAttachment.get(o2);
      if ((ta1 == null) && (ta2 == null)) {
        return 0;
      }
      if (ta1 == null) {
        return 1;
      }
      if (ta2 == null) {
        return -1;
      }
      // take capitols first if we can
      if (ta1.isCapital() && !ta2.isCapital()) {
        return -1;
      }
      if (!ta1.isCapital() && ta2.isCapital()) {
        return 1;
      }
      final boolean factoryInT1 = o1.getUnits().anyMatch(Matches.unitCanProduceUnits());
      final boolean factoryInT2 = o2.getUnits().anyMatch(Matches.unitCanProduceUnits());
      // next take territories which can produce
      if (factoryInT1 && !factoryInT2) {
        return -1;
      }
      if (!factoryInT1 && factoryInT2) {
        return 1;
      }
      final boolean infrastructureInT1 = o1.getUnits().anyMatch(Matches.unitIsInfrastructure());
      final boolean infrastructureInT2 = o2.getUnits().anyMatch(Matches.unitIsInfrastructure());
      // next take territories with infrastructure
      if (infrastructureInT1 && !infrastructureInT2) {
        return -1;
      }
      if (!infrastructureInT1 && infrastructureInT2) {
        return 1;
      }
      // next take territories with largest PU value
      return ta2.getProduction() - ta1.getProduction();
    });
    final List<Territory> isWaterTerr = Utils.onlyWaterTerr(enemyOwned);
    enemyOwned.removeAll(isWaterTerr);
    // first find the territories we can just walk into
    for (final Territory enemy : enemyOwned) {
      if (AiUtils.strength(enemy.getUnits().getUnits(), false, false) == 0) {
        // only take it with 1 unit
        boolean taken = false;
        for (final Territory attackFrom : data.getMap().getNeighbors(enemy,
            Matches.territoryHasLandUnitsOwnedBy(player))) {
          if (taken) {
            break;
          }
          // get the cheapest unit to move in
          final List<Unit> unitsSortedByCost = new ArrayList<>(attackFrom.getUnits().getUnits());
          Collections.sort(unitsSortedByCost, AiUtils.getCostComparator());
          for (final Unit unit : unitsSortedByCost) {
            final Predicate<Unit> match = Matches.unitIsOwnedBy(player)
                .and(Matches.unitIsLand())
                .and(Matches.unitIsNotInfrastructure())
                .and(Matches.unitCanMove())
                .and(Matches.unitIsNotAa())
                .and(Matches.unitCanNotMoveDuringCombatMove().negate());
            if (!unitsAlreadyMoved.contains(unit) && match.test(unit)) {
              moveRoutes.add(data.getMap().getRoute(attackFrom, enemy));
              // if unloading units, unload all of them,
              // otherwise we wont be able to unload them
              // in non com, for land moves we want to move the minimal
              // number of units, to leave units free to move elsewhere
              if (attackFrom.isWater()) {
                final List<Unit> units = attackFrom.getUnits().getMatches(Matches.unitIsLandAndOwnedBy(player));
                moveUnits.add(CollectionUtils.difference(units, unitsAlreadyMoved));
                unitsAlreadyMoved.addAll(units);
              } else {
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
    for (final Territory enemy : enemyOwned) {
      final float enemyStrength = AiUtils.strength(enemy.getUnits().getUnits(), false, false);
      if (enemyStrength > 0) {
        final Predicate<Unit> attackable = Matches.unitIsOwnedBy(player)
            .and(Matches.unitIsStrategicBomber().negate())
            .and(o -> !unitsAlreadyMoved.contains(o))
            .and(Matches.unitIsNotAa())
            .and(Matches.unitCanMove())
            .and(Matches.unitIsNotInfrastructure())
            .and(Matches.unitCanNotMoveDuringCombatMove().negate())
            .and(Matches.unitIsNotSea());
        final Set<Territory> dontMoveFrom = new HashSet<>();
        // find our strength that we can attack with
        float ourStrength = 0;
        final Collection<Territory> attackFrom =
            data.getMap().getNeighbors(enemy, Matches.territoryHasLandUnitsOwnedBy(player));
        for (final Territory owned : attackFrom) {
          if ((TerritoryAttachment.get(owned) != null) && TerritoryAttachment.get(owned).isCapital()
              && (Utils.getStrengthOfPotentialAttackers(owned, data) > AiUtils.strength(owned.getUnits().getUnits(),
              false, false))) {
            dontMoveFrom.add(owned);
            continue;
          }
          ourStrength += AiUtils.strength(owned.getUnits().getMatches(attackable), true, false);
        }
        // prevents 2 infantry from attacking 1 infantry
        if (ourStrength > (1.37 * enemyStrength)) {
          // this is all we need to take it, dont go overboard, since we may be able to use the units to attack
          // somewhere else
          double remainingStrengthNeeded = (2.5 * enemyStrength) + 4;
          for (final Territory owned : attackFrom) {
            if (dontMoveFrom.contains(owned)) {
              continue;
            }
            List<Unit> units = owned.getUnits().getMatches(attackable);
            // only take the units we need if
            // 1) we are not an amphibious attack
            // 2) we can potentially attack another territory
            if (!owned.isWater()
                && (data.getMap().getNeighbors(owned, Matches.territoryHasEnemyLandUnits(player, data)).size() > 1)) {
              units = Utils.getUnitsUpToStrength(remainingStrengthNeeded, units, false);
            }
            remainingStrengthNeeded -= AiUtils.strength(units, true, false);
            if (units.size() > 0) {
              unitsAlreadyMoved.addAll(units);
              moveUnits.add(units);
              moveRoutes.add(data.getMap().getRoute(owned, enemy));
            }
          }
        }
      }
    }
  }

  private static void populateBomberCombat(final GameData data, final List<Collection<Unit>> moveUnits,
      final List<Route> moveRoutes, final PlayerID player) {
    final Predicate<Territory> enemyFactory = Matches.territoryIsEnemyNonNeutralAndHasEnemyUnitMatching(data, player,
        Matches.unitCanProduceUnitsAndCanBeDamaged());
    final Predicate<Unit> ownBomber = Matches.unitIsStrategicBomber().and(Matches.unitIsOwnedBy(player));
    for (final Territory t : data.getMap().getTerritories()) {
      final Collection<Unit> bombers = t.getUnits().getMatches(ownBomber);
      if (bombers.isEmpty()) {
        continue;
      }
      final Predicate<Territory> routeCond = Matches.territoryHasEnemyAaForCombatOnly(player, data).negate();
      final Route bombRoute = Utils.findNearest(t, enemyFactory, routeCond, data);
      moveUnits.add(bombers);
      moveRoutes.add(bombRoute);
    }
  }

  private static int countTransports(final GameData data, final PlayerID player) {
    final Predicate<Unit> ownedTransport = Matches.unitIsTransport().and(Matches.unitIsOwnedBy(player));
    return Streams.stream(data.getMap())
        .map(Territory::getUnits)
        .mapToInt(c -> c.countMatches(ownedTransport))
        .sum();
  }

  private static int countLandUnits(final GameData data, final PlayerID player) {
    final Predicate<Unit> ownedLandUnit = Matches.unitIsLand().and(Matches.unitIsOwnedBy(player));
    return Streams.stream(data.getMap())
        .map(Territory::getUnits)
        .mapToInt(c -> c.countMatches(ownedLandUnit))
        .sum();
  }

  @Override
  public void purchase(final boolean purchaseForBid, final int pusToSpend, final IPurchaseDelegate purchaseDelegate,
      final GameData data, final PlayerID player) {
    if (purchaseForBid) {
      // bid will only buy land units, due to weak ai placement for bid not being able to handle sea units
      final Resource pus = data.getResourceList().getResource(Constants.PUS);
      int leftToSpend = pusToSpend;
      final List<ProductionRule> rules = player.getProductionFrontier().getRules();
      final IntegerMap<ProductionRule> purchase = new IntegerMap<>();
      int minCost = Integer.MAX_VALUE;
      int i = 0;
      while (((minCost == Integer.MAX_VALUE) || (leftToSpend >= minCost)) && (i < 100000)) {
        i++;
        for (final ProductionRule rule : rules) {
          final NamedAttachable resourceOrUnit = rule.getResults().keySet().iterator().next();
          if (!(resourceOrUnit instanceof UnitType)) {
            continue;
          }
          final UnitType results = (UnitType) resourceOrUnit;
          if (Matches.unitTypeIsSea().test(results) || Matches.unitTypeIsAir().test(results)
              || Matches.unitTypeIsInfrastructure().test(results) || Matches.unitTypeIsAaForAnything().test(results)
              || Matches.unitTypeHasMaxBuildRestrictions().test(results)
              || Matches.unitTypeConsumesUnitsOnCreation().test(results)
              || Matches.unitTypeIsStatic(player).test(results)) {
            continue;
          }
          final int cost = rule.getCosts().getInt(pus);
          if (cost < 1) {
            continue;
          }
          if (minCost == Integer.MAX_VALUE) {
            minCost = cost;
          }
          if (minCost > cost) {
            minCost = cost;
          }
          // give a preference to cheap units
          if ((Math.random() * cost) < 2) {
            if (cost <= leftToSpend) {
              leftToSpend -= cost;
              purchase.add(rule, 1);
            }
          }
        }
      }
      purchaseDelegate.purchase(purchase);
      pause();
      return;
    }
    final boolean isAmphib = isAmphibAttack(player, data);
    final Route amphibRoute = getAmphibRoute(player, data);
    final int transportCount = countTransports(data, player);
    final int landUnitCount = countLandUnits(data, player);
    int defUnitsAtAmpibRoute = 0;
    if (isAmphib && (amphibRoute != null)) {
      defUnitsAtAmpibRoute = amphibRoute.getEnd().getUnits().getUnitCount();
    }
    final Resource pus = data.getResourceList().getResource(Constants.PUS);
    final int totalPu = player.getResources().getQuantity(pus);
    int leftToSpend = totalPu;
    final Territory capitol = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
    final List<ProductionRule> rules = player.getProductionFrontier().getRules();
    final IntegerMap<ProductionRule> purchase = new IntegerMap<>();
    final List<RepairRule> repairRules;
    final Predicate<Unit> ourFactories = Matches.unitIsOwnedBy(player).and(Matches.unitCanProduceUnits());
    final List<Territory> repairFactories =
        CollectionUtils.getMatches(Utils.findUnitTerr(data, ourFactories), Matches.isTerritoryOwnedBy(player));
    // figure out if anything needs to be repaired
    if ((player.getRepairFrontier() != null)
        && Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data)) {
      repairRules = player.getRepairFrontier().getRules();
      final IntegerMap<RepairRule> repairMap = new IntegerMap<>();
      final HashMap<Unit, IntegerMap<RepairRule>> repair = new HashMap<>();
      final Map<Unit, Territory> unitsThatCanProduceNeedingRepair = new HashMap<>();
      final int minimumUnitPrice = 3;
      int diff;
      int capProduction = 0;
      Unit capUnit = null;
      Territory capUnitTerritory = null;
      int currentProduction = 0;
      // we should sort this
      Collections.shuffle(repairFactories);
      for (final Territory fixTerr : repairFactories) {
        if (!Matches.territoryIsOwnedAndHasOwnedUnitMatching(player, Matches.unitCanProduceUnitsAndCanBeDamaged())
            .test(fixTerr)) {
          continue;
        }
        final Unit possibleFactoryNeedingRepair = TripleAUnit.getBiggestProducer(
            CollectionUtils.getMatches(fixTerr.getUnits().getUnits(), ourFactories), fixTerr, player, data, false);
        if (Matches.unitHasTakenSomeBombingUnitDamage().test(possibleFactoryNeedingRepair)) {
          unitsThatCanProduceNeedingRepair.put(possibleFactoryNeedingRepair, fixTerr);
        }
        if (fixTerr == capitol) {
          capProduction =
              TripleAUnit.getHowMuchCanUnitProduce(possibleFactoryNeedingRepair, fixTerr, player, data, true, true);
          capUnit = possibleFactoryNeedingRepair;
          capUnitTerritory = fixTerr;
        }
        currentProduction +=
            TripleAUnit.getHowMuchCanUnitProduce(possibleFactoryNeedingRepair, fixTerr, player, data, true, true);
      }
      repairFactories.remove(capitol);
      unitsThatCanProduceNeedingRepair.remove(capUnit);
      // assume minimum unit price is 3, and that we are buying only that... if we over repair, oh well, that is better
      // than under-repairing
      // goal is to be able to produce all our units, and at least half of that production in the capitol
      //
      // if capitol is super safe, we don't have to do this. and if capitol is under siege, we should repair enough to
      // place all our units here
      int maxUnits = (totalPu - 1) / minimumUnitPrice;
      if (((capProduction <= (maxUnits / 2)) || repairFactories.isEmpty()) && (capUnit != null)) {
        for (final RepairRule rrule : repairRules) {
          if (!capUnit.getType().equals(rrule.getResults().keySet().iterator().next())) {
            continue;
          }
          if (!Matches.territoryIsOwnedAndHasOwnedUnitMatching(player, Matches.unitCanProduceUnitsAndCanBeDamaged())
              .test(capitol)) {
            continue;
          }
          final TripleAUnit taUnit = (TripleAUnit) capUnit;
          diff = taUnit.getUnitDamage();
          final int unitProductionAllowNegative =
              TripleAUnit.getHowMuchCanUnitProduce(capUnit, capUnitTerritory, player, data, false, true) - diff;
          if (!repairFactories.isEmpty()) {
            diff = Math.min(diff, ((maxUnits / 2) - unitProductionAllowNegative) + 1);
          } else {
            diff = Math.min(diff, (maxUnits - unitProductionAllowNegative));
          }
          diff = Math.min(diff, leftToSpend - minimumUnitPrice);
          if (diff > 0) {
            if (unitProductionAllowNegative >= 0) {
              currentProduction += diff;
            } else {
              currentProduction += diff + unitProductionAllowNegative;
            }
            repairMap.add(rrule, diff);
            repair.put(capUnit, repairMap);
            leftToSpend -= diff;
            purchaseDelegate.purchaseRepair(repair);
            repair.clear();
            repairMap.clear();
            // ideally we would adjust this after each single PU spent, then re-evaluate
            // everything.
            maxUnits = (leftToSpend - 1) / minimumUnitPrice;
          }
        }
      }
      int i = 0;
      while ((currentProduction < maxUnits) && (i < 2)) {
        for (final RepairRule rrule : repairRules) {
          for (final Unit fixUnit : unitsThatCanProduceNeedingRepair.keySet()) {
            if ((fixUnit == null) || !fixUnit.getType().equals(rrule.getResults().keySet().iterator().next())) {
              continue;
            }
            if (!Matches.territoryIsOwnedAndHasOwnedUnitMatching(player, Matches.unitCanProduceUnitsAndCanBeDamaged())
                .test(unitsThatCanProduceNeedingRepair.get(fixUnit))) {
              continue;
            }
            // we will repair the first territories in the list as much as we can, until we fulfill the condition, then
            // skip all other
            // territories
            if (currentProduction >= maxUnits) {
              continue;
            }
            final TripleAUnit taUnit = (TripleAUnit) fixUnit;
            diff = taUnit.getUnitDamage();
            final int unitProductionAllowNegative = TripleAUnit.getHowMuchCanUnitProduce(fixUnit,
                unitsThatCanProduceNeedingRepair.get(fixUnit), player, data, false, true) - diff;
            if (i == 0) {
              if (unitProductionAllowNegative < 0) {
                diff = Math.min(diff, (maxUnits - currentProduction) - unitProductionAllowNegative);
              } else {
                diff = Math.min(diff, (maxUnits - currentProduction));
              }
            }
            diff = Math.min(diff, leftToSpend - minimumUnitPrice);
            if (diff > 0) {
              if (unitProductionAllowNegative >= 0) {
                currentProduction += diff;
              } else {
                currentProduction += diff + unitProductionAllowNegative;
              }
              repairMap.add(rrule, diff);
              repair.put(fixUnit, repairMap);
              leftToSpend -= diff;
              purchaseDelegate.purchaseRepair(repair);
              repair.clear();
              repairMap.clear();
              // ideally we would adjust this after each single PU spent, then re-evaluate
              // everything.
              maxUnits = (leftToSpend - 1) / minimumUnitPrice;
            }
          }
        }
        repairFactories.add(capitol);
        if (capUnit != null) {
          unitsThatCanProduceNeedingRepair.put(capUnit, capUnitTerritory);
        }
        i++;
      }
    }
    int minCost = Integer.MAX_VALUE;
    int i = 0;
    while (((minCost == Integer.MAX_VALUE) || (leftToSpend >= minCost)) && (i < 100000)) {
      i++;
      for (final ProductionRule rule : rules) {
        final NamedAttachable resourceOrUnit = rule.getResults().keySet().iterator().next();
        if (!(resourceOrUnit instanceof UnitType)) {
          continue;
        }
        final UnitType results = (UnitType) resourceOrUnit;
        if (Matches.unitTypeIsAir().test(results)
            || Matches.unitTypeIsInfrastructure().test(results)
            || Matches.unitTypeIsAaForAnything().test(results)
            || Matches.unitTypeHasMaxBuildRestrictions().test(results)
            || Matches.unitTypeConsumesUnitsOnCreation().test(results)
            || Matches.unitTypeIsStatic(player).test(results)) {
          continue;
        }
        final int transportCapacity = UnitAttachment.get(results).getTransportCapacity();
        // buy transports if we can be amphibious
        if (Matches.unitTypeIsSea().test(results)) {
          if (!isAmphib || (transportCapacity <= 0)) {
            continue;
          }
        }
        final int cost = rule.getCosts().getInt(pus);
        if (cost < 1) {
          continue;
        }
        if (minCost == Integer.MAX_VALUE) {
          minCost = cost;
        }
        if (minCost > cost) {
          minCost = cost;
        }
        // give a preferene to cheap units, and to transports
        // but dont go overboard with buying transports
        int goodNumberOfTransports = 0;
        final boolean isTransport = transportCapacity > 0;
        if (amphibRoute != null) {
          // 25% transports - can be more if frontier is far away
          goodNumberOfTransports = (landUnitCount / 4);
          // boost for transport production
          if (isTransport && (defUnitsAtAmpibRoute > goodNumberOfTransports) && (landUnitCount > defUnitsAtAmpibRoute)
              && (defUnitsAtAmpibRoute > transportCount)) {
            final int transports = (leftToSpend / cost);
            leftToSpend -= cost * transports;
            purchase.add(rule, transports);
            continue;
          }
        }
        final boolean buyBecauseTransport =
            ((Math.random() < 0.7) && (transportCount < goodNumberOfTransports)) || (Math.random() < 0.10);
        final boolean dontBuyBecauseTooManyTransports = transportCount > (2 * goodNumberOfTransports);
        if ((!isTransport && ((Math.random() * cost) < 2))
            || (isTransport && buyBecauseTransport && !dontBuyBecauseTooManyTransports)) {
          if (cost <= leftToSpend) {
            leftToSpend -= cost;
            purchase.add(rule, 1);
          }
        }
      }
    }
    purchaseDelegate.purchase(purchase);
    pause();
  }

  @Override
  public void place(final boolean bid, final IAbstractPlaceDelegate placeDelegate, final GameData data,
      final PlayerID player) {
    if (player.getUnits().size() == 0) {
      return;
    }
    final Territory capitol = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
    // place in capitol first
    placeAllWeCanOn(data, capitol, placeDelegate, player);
    final List<Territory> randomTerritories = new ArrayList<>(data.getMap().getTerritories());
    Collections.shuffle(randomTerritories);
    for (final Territory t : randomTerritories) {
      if ((t != capitol) && t.getOwner().equals(player) && t.getUnits().anyMatch(Matches.unitCanProduceUnits())) {
        placeAllWeCanOn(data, t, placeDelegate, player);
      }
    }
  }

  private static void placeAllWeCanOn(
      final GameData data,
      final Territory placeAt,
      final IAbstractPlaceDelegate placeDelegate,
      final PlayerID player) {
    final PlaceableUnits pu = placeDelegate.getPlaceableUnits(player.getUnits().getUnits(), placeAt);
    if (pu.getErrorMessage() != null) {
      return;
    }
    int placementLeft = pu.getMaxUnits();
    if (placementLeft == -1) {
      placementLeft = Integer.MAX_VALUE;
    }
    final List<Unit> seaUnits = new ArrayList<>(player.getUnits().getMatches(Matches.unitIsSea()));
    if (seaUnits.size() > 0) {
      final Route amphibRoute = getAmphibRoute(player, data);
      Territory seaPlaceAt = null;
      if (amphibRoute != null) {
        seaPlaceAt = amphibRoute.getAllTerritories().get(1);
      } else {
        final Set<Territory> seaNeighbors = data.getMap().getNeighbors(placeAt, Matches.territoryIsWater());
        if (!seaNeighbors.isEmpty()) {
          seaPlaceAt = seaNeighbors.iterator().next();
        }
      }
      if (seaPlaceAt != null) {
        final int seaPlacement = Math.min(placementLeft, seaUnits.size());
        placementLeft -= seaPlacement;
        final Collection<Unit> toPlace = seaUnits.subList(0, seaPlacement);
        doPlace(seaPlaceAt, toPlace, placeDelegate);
      }
    }
    final List<Unit> landUnits = new ArrayList<>(player.getUnits().getMatches(Matches.unitIsLand()));
    if (!landUnits.isEmpty()) {
      final int landPlaceCount = Math.min(placementLeft, landUnits.size());
      final Collection<Unit> toPlace = landUnits.subList(0, landPlaceCount);
      doPlace(placeAt, toPlace, placeDelegate);
    }
  }

  private static void doPlace(final Territory where, final Collection<Unit> toPlace, final IAbstractPlaceDelegate del) {
    del.placeUnits(new ArrayList<>(toPlace), where, IAbstractPlaceDelegate.BidMode.NOT_BID);
    pause();
  }

  @Override
  public boolean shouldBomberBomb(final Territory territory) {
    return true;
  }
}
