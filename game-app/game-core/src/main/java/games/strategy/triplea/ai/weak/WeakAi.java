package games.strategy.triplea.ai.weak;

import com.google.common.collect.Streams;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.UnitUtils;
import games.strategy.triplea.ai.AbstractAi;
import games.strategy.triplea.ai.AiUtils;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.battle.BattleDelegate;
import games.strategy.triplea.delegate.data.PlaceableUnits;
import games.strategy.triplea.delegate.move.validation.MoveValidator;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

/** A very weak ai, based on some simple rules. */
public class WeakAi extends AbstractAi {

  public WeakAi(final String name, final String playerLabel) {
    super(name, playerLabel);
  }

  public WeakAi(final String name) {
    // This class may be used as fallback implementation
    // for Player. If this is the case assign the "Temporary" label
    super(name, "Temporary");
  }

  @Override
  protected void tech(
      final ITechDelegate techDelegate, final GameData data, final GamePlayer player) {}

  private static Optional<Route> getAmphibRoute(final GamePlayer player, final GameState data) {
    if (!isAmphibAttack(player, data)) {
      return Optional.empty();
    }
    final Optional<Territory> optionalCapital =
        TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data.getMap());
    if (optionalCapital.isEmpty()) {
      return Optional.empty();
    }
    final Territory ourCapitol = optionalCapital.get();
    final Predicate<Territory> endMatch =
        o -> {
          final boolean impassable =
              TerritoryAttachment.get(o).map(TerritoryAttachment::getIsImpassable).orElse(false);
          return !impassable
              && !o.isWater()
              && Utils.hasLandRouteToEnemyOwnedCapitol(o, player, data);
        };
    final Predicate<Territory> routeCond =
        Matches.territoryIsWater().and(Matches.territoryHasNoEnemyUnits(player));
    final Optional<Route> optionalWithNoEnemy =
        Utils.findNearest(ourCapitol, endMatch, routeCond, data);
    if (optionalWithNoEnemy.isPresent() && optionalWithNoEnemy.get().hasSteps()) {
      return optionalWithNoEnemy;
    }
    // this will fail if our capitol is not next to water, c'est la vie.
    final Optional<Route> optionalRoute =
        Utils.findNearest(ourCapitol, endMatch, Matches.territoryIsWater(), data);
    if (optionalRoute.isPresent() && optionalRoute.get().hasSteps()) {
      return optionalRoute;
    }
    return Optional.empty();
  }

  private static boolean isAmphibAttack(final GamePlayer player, final GameState data) {
    final Optional<Territory> optionalCapital =
        TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data.getMap());
    if (optionalCapital.isEmpty() || !optionalCapital.get().isOwnedBy(player)) {
      return false;
    }
    // find a land route to an enemy territory from our capitol
    final Optional<Route> invasionRoute =
        Utils.findNearest(
            optionalCapital.get(),
            Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassableOrRestricted(player),
            Matches.territoryIsLand().and(Matches.territoryIsNeutralButNotWater().negate()),
            data);
    return invasionRoute.isEmpty();
  }

  @Override
  protected void move(
      final boolean nonCombat,
      final IMoveDelegate moveDel,
      final GameData data,
      final GamePlayer player) {
    if (nonCombat) {
      doNonCombatMove(moveDel, player, data);
    } else {
      doCombatMove(moveDel, player, data);
    }
    movePause();
  }

  private void doNonCombatMove(
      final IMoveDelegate moveDel, final GamePlayer player, final GameData data) {
    // load the transports first
    // they may be able to move farther
    doMove(calculateTransportLoad(data, player), moveDel);
    // do the rest of the moves
    doMove(calculateNonCombat(data, player), moveDel);
    doMove(calculateNonCombatSea(true, data, player), moveDel);
    // load the transports again if we can
    // they may be able to move farther
    doMove(calculateTransportLoad(data, player), moveDel);
    // unload the transports that can be unloaded
    doMove(calculateTransportUnloadNonCombat(data, player), moveDel);
  }

  private void doCombatMove(
      final IMoveDelegate moveDel, final GamePlayer player, final GameData data) {
    // load the transports first
    // they may be able to take part in a battle
    doMove(calculateTransportLoad(data, player), moveDel);
    doMove(calculateCombatSea(data, player), moveDel);

    // fight
    doMove(calculateCombatMove(data, player), moveDel);
    doMove(calculateCombatMoveSea(data, player), moveDel);
  }

  private List<MoveDescription> calculateTransportLoad(
      final GameState data, final GamePlayer player) {
    if (!isAmphibAttack(player, data)) {
      return List.of();
    }
    final Optional<Territory> optionalCapital =
        TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data.getMap());
    if (optionalCapital.isEmpty() || !optionalCapital.get().isOwnedBy(player)) {
      return List.of();
    }
    final Territory capitol = optionalCapital.get();
    final var moves = new ArrayList<MoveDescription>();
    final List<Unit> unitsToLoad =
        capitol.getMatches(
            Matches.unitIsInfrastructure()
                .negate()
                .and(Matches.unitIsOwnedBy(this.getGamePlayer())));
    for (final Territory neighbor : data.getMap().getNeighbors(capitol)) {
      if (!neighbor.isWater()) {
        continue;
      }
      final List<Unit> units = new ArrayList<>();
      final Map<Unit, Unit> unitsToSeaTransports = new HashMap<>();
      for (final Unit transport : neighbor.getMatches(Matches.unitIsOwnedBy(player))) {
        int free = TransportTracker.getAvailableCapacity(transport);
        if (free <= 0) {
          continue;
        }
        final Iterator<Unit> iter = unitsToLoad.iterator();
        while (iter.hasNext() && free > 0) {
          final Unit current = iter.next();
          final UnitAttachment ua = current.getUnitAttachment();
          if (ua.isAir()) {
            continue;
          }
          if (ua.getTransportCost() <= free) {
            iter.remove();
            free -= ua.getTransportCost();
            units.add(current);
            unitsToSeaTransports.put(current, transport);
          }
        }
      }
      if (!units.isEmpty()) {
        final Route route = new Route(capitol, neighbor);
        moves.add(new MoveDescription(units, route, unitsToSeaTransports, Map.of()));
      }
    }
    return moves;
  }

  private static List<MoveDescription> calculateTransportUnloadNonCombat(
      final GameState data, final GamePlayer player) {
    final Optional<Route> optionalAmphibRoute = getAmphibRoute(player, data);
    if (optionalAmphibRoute.isEmpty()) {
      return List.of();
    }
    final Route amphibRoute = optionalAmphibRoute.get();
    final Territory lastSeaZoneOnAmphib =
        amphibRoute.getAllTerritories().get(amphibRoute.numberOfSteps() - 1);
    final Territory landOn = amphibRoute.getEnd();
    final Predicate<Unit> landAndOwned = Matches.unitIsLand().and(Matches.unitIsOwnedBy(player));
    final List<Unit> units = lastSeaZoneOnAmphib.getMatches(landAndOwned);
    if (units.isEmpty()) {
      return List.of();
    }
    final Route route = new Route(lastSeaZoneOnAmphib, landOn);
    return List.of(new MoveDescription(units, route));
  }

  private static List<Unit> load2Transports(final List<Unit> transportsToLoad) {
    final List<Unit> units = new ArrayList<>();
    for (final Unit transport : transportsToLoad) {
      final Collection<Unit> landunits = transport.getTransporting();
      units.addAll(landunits);
    }
    return units;
  }

  private static void doMove(final List<MoveDescription> moves, final IMoveDelegate moveDel) {
    for (final MoveDescription move : moves) {
      moveDel.performMove(move);
      movePause();
    }
  }

  private static List<MoveDescription> calculateCombatSea(
      final GameData data, final GamePlayer player) {
    // we want to move loaded transports before we try to fight our battles
    final List<MoveDescription> moves = calculateNonCombatSea(false, data, player);
    // find second amphib target
    final Optional<Route> optionalAmphibRoute = getAlternativeAmphibRoute(player, data);
    if (optionalAmphibRoute.isEmpty()) {
      return moves;
    }
    final Route amphibRoute = optionalAmphibRoute.get();
    // TODO workaround - should check if amphibRoute is in moves
    if (moves.size() == 2) {
      moves.remove(1);
    }
    final Territory firstSeaZoneOnAmphib = amphibRoute.getAllTerritories().get(0);
    final Territory lastSeaZoneOnAmphib =
        amphibRoute.getAllTerritories().get(amphibRoute.numberOfSteps() - 1);
    final Predicate<Unit> ownedAndNotMoved =
        Matches.unitIsOwnedBy(player).and(Matches.unitHasNotMoved());
    final List<Unit> unitsToMove = new ArrayList<>();
    final List<Unit> transports =
        firstSeaZoneOnAmphib.getMatches(
            ownedAndNotMoved.and(u -> !u.getTransporting(firstSeaZoneOnAmphib).isEmpty()));
    if (transports.size() <= 1) {
      unitsToMove.addAll(transports);
    } else {
      unitsToMove.addAll(transports.subList(0, 1));
    }
    final List<Unit> landUnits = load2Transports(unitsToMove);
    getMaxSeaRoute(data, firstSeaZoneOnAmphib, lastSeaZoneOnAmphib, player)
        .ifPresent(
            route -> {
              unitsToMove.addAll(landUnits);
              moves.add(new MoveDescription(unitsToMove, route));
            });
    return moves;
  }

  /** prepares moves for transports. */
  private static List<MoveDescription> calculateNonCombatSea(
      final boolean nonCombat, final GameData data, final GamePlayer player) {
    final Optional<Route> optionalAmphibRoute = getAmphibRoute(player, data);
    @Nullable Territory firstSeaZoneOnAmphib = null;
    @Nullable Territory lastSeaZoneOnAmphib = null;
    if (optionalAmphibRoute.isPresent()) {
      final Route amphibRoute = optionalAmphibRoute.get();
      firstSeaZoneOnAmphib = amphibRoute.getAllTerritories().get(1);
      lastSeaZoneOnAmphib = amphibRoute.getAllTerritories().get(amphibRoute.numberOfSteps() - 1);
    }
    final Predicate<Unit> ownedAndNotMoved =
        Matches.unitIsOwnedBy(player).and(Matches.unitHasNotMoved());
    final var moves = new ArrayList<MoveDescription>();
    for (final Territory t : data.getMap()) {
      // move sea units to the capitol, unless they are loaded transports
      if (t.isWater()) {
        // land units, move all towards the end point
        // and move along amphib route
        if (t.anyUnitsMatch(Matches.unitIsLand()) && lastSeaZoneOnAmphib != null) {
          // two move route to end
          getMaxSeaRoute(data, t, lastSeaZoneOnAmphib, player)
              .ifPresent(
                  route -> {
                    final List<Unit> unitsToMove = t.getMatches(Matches.unitIsOwnedBy(player));
                    moves.add(new MoveDescription(unitsToMove, route));
                  });
        }
        // move toward the start of the amphib route
        if (nonCombat && t.anyUnitsMatch(ownedAndNotMoved) && firstSeaZoneOnAmphib != null) {
          getMaxSeaRoute(data, t, firstSeaZoneOnAmphib, player)
              .ifPresent(
                  route -> moves.add(new MoveDescription(t.getMatches(ownedAndNotMoved), route)));
        }
      }
    }
    return moves;
  }

  private static Optional<Route> getMaxSeaRoute(
      final GameData data,
      final Territory start,
      final Territory destination,
      final GamePlayer player) {
    final Predicate<Territory> routeCond =
        Matches.territoryIsWater()
            .and(Matches.territoryHasEnemyUnits(player).negate())
            .and(territoryHasNonAllowedCanal(player, data).negate());
    final Optional<Route> optionalRoute = data.getMap().getRoute(start, destination, routeCond);
    if (optionalRoute.isEmpty()
        || optionalRoute.get().hasNoSteps()
        || !routeCond.test(destination)) {
      return Optional.empty();
    }
    if (optionalRoute.get().numberOfSteps() > 2) {
      final List<Territory> allRouteTerritories = optionalRoute.get().getAllTerritories();
      return Optional.of(new Route(start, allRouteTerritories.get(1), allRouteTerritories.get(2)));
    }
    return optionalRoute;
  }

  private static Predicate<Territory> territoryHasNonAllowedCanal(
      final GamePlayer player, final GameData gameData) {
    return t ->
        new MoveValidator(gameData, false).validateCanal(new Route(t), null, player) != null;
  }

  private static List<MoveDescription> calculateCombatMoveSea(
      final GameState data, final GamePlayer player) {
    final var moves = new ArrayList<MoveDescription>();
    final Collection<Unit> unitsAlreadyMoved = new HashSet<>();
    for (final Territory t : data.getMap()) {
      if (!t.isWater()) {
        continue;
      }
      if (!t.anyUnitsMatch(Matches.enemyUnit(player))) {
        continue;
      }
      final float enemyStrength = AiUtils.strength(t.getUnits(), false, true);
      if (enemyStrength > 0) {
        final Predicate<Unit> attackable =
            Matches.unitIsOwnedBy(player).and(o -> !unitsAlreadyMoved.contains(o));
        final Set<Territory> dontMoveFrom = new HashSet<>();
        // find our strength that we can attack with
        float ourStrength = 0;
        final Collection<Territory> attackFrom =
            data.getMap().getNeighbors(t, Matches.territoryIsWater());
        for (final Territory owned : attackFrom) {
          // dont risk units we are carrying
          if (owned.anyUnitsMatch(Matches.unitIsLand())) {
            dontMoveFrom.add(owned);
            continue;
          }
          ourStrength += AiUtils.strength(owned.getMatches(attackable), true, true);
        }
        if (ourStrength > 1.32 * enemyStrength) {
          for (final Territory owned : attackFrom) {
            if (dontMoveFrom.contains(owned)) {
              continue;
            }
            final List<Unit> units = owned.getMatches(attackable);
            unitsAlreadyMoved.addAll(units);
            moves.add(new MoveDescription(units, new Route(owned, t)));
          }
        }
      }
    }
    return moves;
  }

  // searches for amphibious attack on empty territory
  private static Optional<Route> getAlternativeAmphibRoute(
      final GamePlayer player, final GameState data) {
    if (!isAmphibAttack(player, data)) {
      return null;
    }
    final Predicate<Territory> routeCondition =
        Matches.territoryIsWater().and(Matches.territoryHasNoEnemyUnits(player));
    // should select all territories with loaded transports
    final Predicate<Territory> transportOnSea =
        Matches.territoryIsWater().and(Matches.territoryHasLandUnitsOwnedBy(player));
    final Predicate<Unit> ownedTransports =
        Matches.unitCanTransport()
            .and(Matches.unitIsOwnedBy(player))
            .and(Matches.unitHasNotMoved());
    final Predicate<Territory> enemyTerritory =
        Matches.isTerritoryEnemy(player)
            .and(Matches.territoryIsLand())
            .and(Matches.territoryIsNeutralButNotWater().negate())
            .and(Matches.territoryIsEmpty());
    Optional<Route> altRoute = Optional.empty();
    final int length = Integer.MAX_VALUE;
    for (final Territory t : data.getMap()) {
      if (!transportOnSea.test(t)) {
        continue;
      }
      final int trans = t.getUnitCollection().countMatches(ownedTransports);
      if (trans > 0) {
        final Optional<Route> optionalNewRoute =
            Utils.findNearest(t, enemyTerritory, routeCondition, data);
        if (optionalNewRoute.isPresent() && length > optionalNewRoute.get().numberOfSteps()) {
          altRoute = optionalNewRoute;
        }
      }
    }
    return altRoute;
  }

  private List<MoveDescription> calculateNonCombat(final GameData data, final GamePlayer player) {
    final Collection<Territory> territories = data.getMap().getTerritories();
    final List<MoveDescription> moves = movePlanesHomeNonCombat(player, data);
    // these are the units we can move
    final Predicate<Unit> moveOfType =
        Matches.unitIsOwnedBy(player)
            .and(Matches.unitIsNotAa())
            // we can never move factories
            .and(Matches.unitCanMove())
            .and(Matches.unitIsNotInfrastructure())
            .and(Matches.unitIsLand());
    final Predicate<Territory> moveThrough =
        Matches.territoryIsImpassable()
            .negate()
            .and(Matches.territoryIsNeutralButNotWater().negate())
            .and(Matches.territoryIsLand());
    // move our units toward the nearest enemy capitol
    for (final Territory t : territories) {
      if (t.isWater()) {
        continue;
      }
      if (TerritoryAttachment.get(t).map(TerritoryAttachment::isCapital).orElse(false)) {
        // if they are a threat to take our capitol, dont move
        // compare the strength of units we can place
        final float ourStrength = AiUtils.strength(player.getUnits(), false, false);
        final float attackerStrength = Utils.getStrengthOfPotentialAttackers(t, data);
        if (attackerStrength > ourStrength) {
          continue;
        }
      }
      final List<Unit> units = t.getMatches(moveOfType);
      if (units.isEmpty()) {
        continue;
      }
      int minDistance = Integer.MAX_VALUE;
      Optional<Territory> to = Optional.empty();
      // find the nearest enemy owned capital
      for (final GamePlayer otherPlayer : data.getPlayerList().getPlayers()) {
        final Optional<Territory> optionalCapital =
            TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(
                otherPlayer, data.getMap());
        if (optionalCapital.isPresent()) {
          final Territory capital = optionalCapital.get();
          if (!player.isAllied(capital.getOwner()) && moveThrough.test(capital)) {
            Optional<Route> optionalRoute = data.getMap().getRoute(t, capital, moveThrough);
            if (optionalRoute.isPresent()) {
              final int distance = optionalRoute.get().numberOfSteps();
              if (distance != 0 && distance < minDistance) {
                minDistance = distance;
                to = Optional.of(capital);
              }
            }
          }
        }
      }
      final Optional<Route> optionalRouteToCapitol;
      if (to.isPresent()) {
        optionalRouteToCapitol = data.getMap().getRoute(t, to.get(), moveThrough);
      } else {
        optionalRouteToCapitol = Optional.empty();
      }
      if (optionalRouteToCapitol.isPresent()) {
        final Territory firstStep = optionalRouteToCapitol.get().getAllTerritories().get(1);
        final Route route = new Route(t, firstStep);
        moves.add(new MoveDescription(units, route));
      } else { // if we cant move to a capitol, move towards the enemy
        final Predicate<Territory> routeCondition =
            Matches.territoryIsLand().and(Matches.territoryIsImpassable().negate());
        Optional<Route> optionalNewRoute =
            Utils.findNearest(t, Matches.territoryHasEnemyLandUnits(player), routeCondition, data);
        // move to any enemy territory
        if (optionalNewRoute.isEmpty()) {
          optionalNewRoute =
              Utils.findNearest(t, Matches.isTerritoryEnemy(player), routeCondition, data);
        }
        if (optionalNewRoute.isPresent() && optionalNewRoute.get().hasSteps()) {
          final Territory firstStep = optionalNewRoute.get().getAllTerritories().get(1);
          final Route route = new Route(t, firstStep);
          moves.add(new MoveDescription(units, route));
        }
      }
    }
    return moves;
  }

  private List<MoveDescription> movePlanesHomeNonCombat(
      final GamePlayer player, final GameData data) {
    // the preferred way to get the delegate
    final IMoveDelegate delegateRemote = (IMoveDelegate) getPlayerBridge().getRemoteDelegate();
    // this works because we are on the server
    final BattleDelegate delegate = data.getBattleDelegate();
    final Predicate<Territory> canLand =
        Matches.isTerritoryAllied(player).and(o -> !delegate.getBattleTracker().wasConquered(o));
    final Predicate<Territory> routeCondition =
        Matches.territoryHasEnemyAaForFlyOver(player)
            .negate()
            .and(Matches.territoryIsImpassable().negate());
    final var moves = new ArrayList<MoveDescription>();
    for (final Territory t : delegateRemote.getTerritoriesWhereAirCantLand()) {
      final Optional<Route> noAaRoute = Utils.findNearest(t, canLand, routeCondition, data);
      final Optional<Route> aaRoute =
          Utils.findNearest(t, canLand, Matches.territoryIsImpassable().negate(), data);
      final Collection<Unit> airToLand =
          t.getMatches(Matches.unitIsAir().and(Matches.unitIsOwnedBy(player)));
      // don't bother to see if all the air units have enough movement points to move without aa
      // guns firing
      // simply move first over no aa, then with aa one (but hopefully not both) will be rejected
      noAaRoute.ifPresent(route -> moves.add(new MoveDescription(airToLand, route)));
      aaRoute.ifPresent(route -> moves.add(new MoveDescription(airToLand, route)));
    }
    return moves;
  }

  private List<MoveDescription> calculateCombatMove(final GameState data, final GamePlayer player) {
    final List<MoveDescription> moves = calculateBomberCombat(data, player);
    final Collection<Unit> unitsAlreadyMoved = new HashSet<>();
    // find the territories we can just walk into
    final Predicate<Territory> walkInto =
        Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassableOrRestricted(player)
            .or(Matches.isTerritoryFreeNeutral(data.getProperties()));
    final List<Territory> enemyOwned =
        CollectionUtils.getMatches(data.getMap().getTerritories(), walkInto);
    Collections.shuffle(enemyOwned);
    enemyOwned.sort(
        (o1, o2) -> {
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
          final Optional<TerritoryAttachment> optionalTerritoryAttachment1 =
              TerritoryAttachment.get(o1);
          final Optional<TerritoryAttachment> optionalTerritoryAttachment2 =
              TerritoryAttachment.get(o2);
          if (optionalTerritoryAttachment1.isEmpty() && optionalTerritoryAttachment2.isEmpty()) {
            return 0;
          }
          if (optionalTerritoryAttachment1.isEmpty()) {
            return 1;
          }
          if (optionalTerritoryAttachment2.isEmpty()) {
            return -1;
          }
          final boolean ta1IsCapital = optionalTerritoryAttachment1.get().isCapital();
          final boolean ta2IsCapital = optionalTerritoryAttachment2.get().isCapital();
          // take capitols first if we can
          if (ta1IsCapital && !ta2IsCapital) {
            return -1;
          }
          if (!ta1IsCapital && ta2IsCapital) {
            return 1;
          }
          final boolean factoryInT1 = o1.anyUnitsMatch(Matches.unitCanProduceUnits());
          final boolean factoryInT2 = o2.anyUnitsMatch(Matches.unitCanProduceUnits());
          // next take territories which can produce
          if (factoryInT1 && !factoryInT2) {
            return -1;
          }
          if (!factoryInT1 && factoryInT2) {
            return 1;
          }
          final boolean infrastructureInT1 = o1.anyUnitsMatch(Matches.unitIsInfrastructure());
          final boolean infrastructureInT2 = o2.anyUnitsMatch(Matches.unitIsInfrastructure());
          // next take territories with infrastructure
          if (infrastructureInT1 && !infrastructureInT2) {
            return -1;
          }
          if (!infrastructureInT1 && infrastructureInT2) {
            return 1;
          }
          // next take territories with the largest PU value
          return optionalTerritoryAttachment2.get().getProduction()
              - optionalTerritoryAttachment1.get().getProduction();
        });
    final List<Territory> isWaterTerr = Utils.onlyWaterTerr(enemyOwned);
    enemyOwned.removeAll(isWaterTerr);
    // first find the territories we can just walk into
    for (final Territory enemy : enemyOwned) {
      if (AiUtils.strength(enemy.getUnits(), false, false) == 0) {
        // only take it with 1 unit
        boolean taken = false;
        for (final Territory attackFrom :
            data.getMap().getNeighbors(enemy, Matches.territoryHasLandUnitsOwnedBy(player))) {
          if (taken) {
            break;
          }
          // get the cheapest unit to move in
          final List<Unit> unitsSortedByCost = new ArrayList<>(attackFrom.getUnits());
          unitsSortedByCost.sort(AiUtils.getCostComparator());
          for (final Unit unit : unitsSortedByCost) {
            final Predicate<Unit> match =
                Matches.unitIsOwnedBy(player)
                    .and(Matches.unitIsLand())
                    .and(Matches.unitIsNotInfrastructure())
                    .and(Matches.unitCanMove())
                    .and(Matches.unitIsNotAa())
                    .and(Matches.unitCanNotMoveDuringCombatMove().negate());
            if (!unitsAlreadyMoved.contains(unit) && match.test(unit)) {
              final Route route = new Route(attackFrom, enemy);
              // if unloading units, unload all of them, otherwise we wont be able to unload them
              // in non com, for land moves we want to move the minimal
              // number of units, to leave units free to move elsewhere
              if (attackFrom.isWater()) {
                final List<Unit> units =
                    attackFrom.getMatches(Matches.unitIsLandAndOwnedBy(player));
                moves.add(
                    new MoveDescription(
                        CollectionUtils.difference(units, unitsAlreadyMoved), route));
                unitsAlreadyMoved.addAll(units);
              } else {
                moves.add(new MoveDescription(List.of(unit), route));
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
      final float enemyStrength = AiUtils.strength(enemy.getUnits(), false, false);
      if (enemyStrength > 0) {
        final Predicate<Unit> attackable =
            Matches.unitIsOwnedBy(player)
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
          if (TerritoryAttachment.get(owned).map(TerritoryAttachment::isCapital).orElse(false)
              && (Utils.getStrengthOfPotentialAttackers(owned, data)
                  > AiUtils.strength(owned.getUnits(), false, false))) {
            dontMoveFrom.add(owned);
            continue;
          }
          ourStrength += AiUtils.strength(owned.getMatches(attackable), true, false);
        }
        // prevents 2 infantry from attacking 1 infantry
        if (ourStrength > 1.37 * enemyStrength) {
          // this is all we need to take it, dont go overboard, since we may be able to use the
          // units to attack
          // somewhere else
          double remainingStrengthNeeded = (2.5 * enemyStrength) + 4;
          for (final Territory owned : attackFrom) {
            if (dontMoveFrom.contains(owned)) {
              continue;
            }
            List<Unit> units = owned.getMatches(attackable);
            // only take the units we need if
            // 1) we are not an amphibious attack
            // 2) we can potentially attack another territory
            if (!owned.isWater()
                && data.getMap()
                        .getNeighbors(owned, Matches.territoryHasEnemyLandUnits(player))
                        .size()
                    > 1) {
              units = Utils.getUnitsUpToStrength(remainingStrengthNeeded, units);
            }
            remainingStrengthNeeded -= AiUtils.strength(units, true, false);
            if (!units.isEmpty()) {
              unitsAlreadyMoved.addAll(units);
              moves.add(new MoveDescription(units, new Route(owned, enemy)));
            }
          }
        }
      }
    }
    return moves;
  }

  private static List<MoveDescription> calculateBomberCombat(
      final GameState data, final GamePlayer player) {
    final Predicate<Territory> enemyFactory =
        Matches.territoryIsEnemyNonNeutralAndHasEnemyUnitMatching(
            player, Matches.unitCanProduceUnitsAndCanBeDamaged());
    final Predicate<Unit> ownBomber =
        Matches.unitIsStrategicBomber().and(Matches.unitIsOwnedBy(player));
    final var moves = new ArrayList<MoveDescription>();
    for (final Territory t : data.getMap().getTerritories()) {
      final Collection<Unit> bombers = t.getMatches(ownBomber);
      if (bombers.isEmpty()) {
        continue;
      }
      final Predicate<Territory> routeCond = Matches.territoryHasEnemyAaForFlyOver(player).negate();
      Utils.findNearest(t, enemyFactory, routeCond, data)
          .ifPresent(route -> moves.add(new MoveDescription(bombers, route)));
    }
    return moves;
  }

  private static int countTransports(final GameState data, final GamePlayer player) {
    final Predicate<Unit> ownedTransport =
        Matches.unitIsSeaTransport().and(Matches.unitIsOwnedBy(player));
    return Streams.stream(data.getMap())
        .map(Territory::getUnitCollection)
        .mapToInt(c -> c.countMatches(ownedTransport))
        .sum();
  }

  private static int countLandUnits(final GameState data, final GamePlayer player) {
    final Predicate<Unit> ownedLandUnit = Matches.unitIsLand().and(Matches.unitIsOwnedBy(player));
    return Streams.stream(data.getMap())
        .map(Territory::getUnitCollection)
        .mapToInt(c -> c.countMatches(ownedLandUnit))
        .sum();
  }

  @Override
  public void purchase(
      final boolean purchaseForBid,
      final int pusToSpend,
      final IPurchaseDelegate purchaseDelegate,
      final GameData data,
      final GamePlayer player) {
    if (purchaseForBid) {
      // bid will only buy land units, due to weak ai placement for bid not being able to handle sea
      // units
      final Resource pus = data.getResourceList().getResourceOrThrow(Constants.PUS);
      int leftToSpend = pusToSpend;
      final List<ProductionRule> rules = player.getProductionFrontier().getRules();
      final IntegerMap<ProductionRule> purchase = new IntegerMap<>();
      int minCost = Integer.MAX_VALUE;
      int i = 0;
      while ((minCost == Integer.MAX_VALUE || leftToSpend >= minCost) && i < 100000) {
        i++;
        for (final ProductionRule rule : rules) {
          final NamedAttachable resourceOrUnit = rule.getAnyResultKey();
          if (!(resourceOrUnit instanceof UnitType)) {
            continue;
          }
          final UnitType results = (UnitType) resourceOrUnit;
          if (Matches.unitTypeIsSea().test(results)
              || Matches.unitTypeIsAir().test(results)
              || Matches.unitTypeIsInfrastructure().test(results)
              || Matches.unitTypeIsAaForAnything().test(results)
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
          if (Math.random() * cost < 2 && cost <= leftToSpend) {
            leftToSpend -= cost;
            purchase.add(rule, 1);
          }
        }
      }
      purchaseDelegate.purchase(purchase);
      movePause();
      return;
    }
    final boolean isAmphib = isAmphibAttack(player, data);
    final int transportCount = countTransports(data, player);
    final int landUnitCount = countLandUnits(data, player);
    int defUnitsAtAmpibRoute = 0;
    final Optional<Route> optionalAmphibRoute;
    if (!isAmphib) {
      optionalAmphibRoute = Optional.empty();
    } else {
      optionalAmphibRoute = getAmphibRoute(player, data);
      if (optionalAmphibRoute.isPresent()) {
        defUnitsAtAmpibRoute =
            optionalAmphibRoute.get().getEnd().getUnitCollection().getUnitCount();
      }
    }
    final Resource pus = data.getResourceList().getResourceOrThrow(Constants.PUS);
    final int totalPu = player.getResources().getQuantity(pus);
    int leftToSpend = totalPu;
    final List<ProductionRule> rules = player.getProductionFrontier().getRules();
    final IntegerMap<ProductionRule> purchase = new IntegerMap<>();
    final List<RepairRule> repairRules;
    final Predicate<Unit> ourFactories =
        Matches.unitIsOwnedBy(player).and(Matches.unitCanProduceUnits());
    final List<Territory> repairFactories =
        CollectionUtils.getMatches(
            Utils.findUnitTerr(data, ourFactories), Matches.isTerritoryOwnedBy(player));
    // figure out if anything needs to be repaired
    if (player.getRepairFrontier() != null
        && Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data.getProperties())) {
      repairRules = player.getRepairFrontier().getRules();
      final IntegerMap<RepairRule> repairMap = new IntegerMap<>();
      final Map<Unit, IntegerMap<RepairRule>> repair = new HashMap<>();
      final Map<Unit, Territory> unitsThatCanProduceNeedingRepair = new HashMap<>();
      final int minimumUnitPrice = 3;
      int diff;
      int capProduction = 0;
      Unit capUnit = null;
      Territory capUnitTerritory = null;
      int currentProduction = 0;
      final Optional<Territory> optionalCapitol =
          TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data.getMap());
      // we should sort this
      Collections.shuffle(repairFactories);
      for (final Territory fixTerr : repairFactories) {
        if (!Matches.territoryIsOwnedAndHasOwnedUnitMatching(
                player, Matches.unitCanProduceUnitsAndCanBeDamaged())
            .test(fixTerr)) {
          continue;
        }
        final Optional<Unit> optionalFactoryNeedingRepair =
            UnitUtils.getBiggestProducer(
                CollectionUtils.getMatches(fixTerr.getUnits(), ourFactories),
                fixTerr,
                player,
                false);
        if (optionalFactoryNeedingRepair.isPresent()) {
          final Unit factoryNeedingRepair = optionalFactoryNeedingRepair.get();
          if (Matches.unitHasTakenSomeBombingUnitDamage().test(factoryNeedingRepair)) {
            unitsThatCanProduceNeedingRepair.put(factoryNeedingRepair, fixTerr);
          }
          if (optionalCapitol.isPresent() && fixTerr.equals(optionalCapitol.get())) {
            capProduction =
                UnitUtils.getHowMuchCanUnitProduce(factoryNeedingRepair, fixTerr, true, true);
            capUnit = factoryNeedingRepair;
            capUnitTerritory = fixTerr;
          }
          currentProduction +=
              UnitUtils.getHowMuchCanUnitProduce(factoryNeedingRepair, fixTerr, true, true);
        }
      }
      optionalCapitol.ifPresent(repairFactories::remove);
      unitsThatCanProduceNeedingRepair.remove(capUnit);
      final var territoryIsOwnedAndHasOwnedUnitMatching =
          Matches.territoryIsOwnedAndHasOwnedUnitMatching(
              player, Matches.unitCanProduceUnitsAndCanBeDamaged());
      // assume minimum unit price is 3, and that we are buying only that... if we over repair, oh
      // well, that is better than under-repairing
      // goal is to be able to produce all our units, and at least half of that production in the
      // capitol
      //
      // if capitol is super safe, we don't have to do this. and if capitol is under siege, we
      // should repair enough to place all our units here
      int maxUnits = (totalPu - 1) / minimumUnitPrice;
      final @Nullable Territory capitol = optionalCapitol.orElse(null);
      if ((capProduction <= maxUnits / 2 || repairFactories.isEmpty()) && capUnit != null) {
        for (final RepairRule rrule : repairRules) {
          if (!capUnit.getType().equals(rrule.getAnyResultKey())) {
            continue;
          }
          if (!territoryIsOwnedAndHasOwnedUnitMatching.test(capitol)) {
            continue;
          }
          diff = capUnit.getUnitDamage();
          final int unitProductionAllowNegative =
              UnitUtils.getHowMuchCanUnitProduce(capUnit, capUnitTerritory, false, true) - diff;
          if (!repairFactories.isEmpty()) {
            diff = Math.min(diff, (maxUnits / 2 - unitProductionAllowNegative) + 1);
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
            // ideally we would adjust this after each single PU spent, then re-evaluate everything.
            maxUnits = (leftToSpend - 1) / minimumUnitPrice;
          }
        }
      }
      int i = 0;
      while (currentProduction < maxUnits && i < 2) {
        for (final RepairRule rrule : repairRules) {
          for (final Unit fixUnit : unitsThatCanProduceNeedingRepair.keySet()) {
            if (fixUnit == null || !fixUnit.getType().equals(rrule.getAnyResultKey())) {
              continue;
            }
            if (!territoryIsOwnedAndHasOwnedUnitMatching.test(
                unitsThatCanProduceNeedingRepair.get(fixUnit))) {
              continue;
            }
            // we will repair the first territories in the list as much as we can, until we fulfill
            // the condition, then
            // skip all other territories
            if (currentProduction >= maxUnits) {
              continue;
            }
            diff = fixUnit.getUnitDamage();
            final int unitProductionAllowNegative =
                UnitUtils.getHowMuchCanUnitProduce(
                        fixUnit, unitsThatCanProduceNeedingRepair.get(fixUnit), false, true)
                    - diff;
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
    while ((minCost == Integer.MAX_VALUE || leftToSpend >= minCost) && i < 100000) {
      i++;
      for (final ProductionRule rule : rules) {
        final NamedAttachable resourceOrUnit = rule.getAnyResultKey();
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
        final int transportCapacity = results.getUnitAttachment().getTransportCapacity();
        // buy transports if we can be amphibious
        if (Matches.unitTypeIsSea().test(results) && (!isAmphib || transportCapacity <= 0)) {
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
        // give a preference to cheap units, and to transports but don't go overboard with buying
        // transports
        int goodNumberOfTransports = 0;
        final boolean isTransport = transportCapacity > 0;
        if (optionalAmphibRoute.isPresent()) {
          // 25% transports - can be more if frontier is far away
          goodNumberOfTransports = (landUnitCount / 4);
          // boost for transport production
          if (isTransport
              && defUnitsAtAmpibRoute > goodNumberOfTransports
              && landUnitCount > defUnitsAtAmpibRoute
              && defUnitsAtAmpibRoute > transportCount) {
            final int transports = (leftToSpend / cost);
            leftToSpend -= cost * transports;
            purchase.add(rule, transports);
            continue;
          }
        }
        final boolean buyBecauseTransport =
            (Math.random() < 0.7 && transportCount < goodNumberOfTransports)
                || Math.random() < 0.10;
        final boolean dontBuyBecauseTooManyTransports = transportCount > 2 * goodNumberOfTransports;
        if (((!isTransport && Math.random() * cost < 2)
                || (isTransport && buyBecauseTransport && !dontBuyBecauseTooManyTransports))
            && cost <= leftToSpend) {
          leftToSpend -= cost;
          purchase.add(rule, 1);
        }
      }
    }
    purchaseDelegate.purchase(purchase);
    movePause();
  }

  @Override
  public void place(
      final boolean bid,
      final IAbstractPlaceDelegate placeDelegate,
      final GameState data,
      final GamePlayer player) {
    if (player.getUnitCollection().isEmpty()) {
      return;
    }
    final Optional<Territory> optionalCapitol =
        TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data.getMap());
    // place in capitol first
    optionalCapitol.ifPresent(capitol -> placeAllWeCanOn(data, capitol, placeDelegate, player));
    final List<Territory> randomTerritories = new ArrayList<>(data.getMap().getTerritories());
    Collections.shuffle(randomTerritories);
    final @Nullable Territory capitol = optionalCapitol.orElse(null);
    for (final Territory t : randomTerritories) {
      if (!t.equals(capitol)
          && t.isOwnedBy(player)
          && t.anyUnitsMatch(Matches.unitCanProduceUnits())) {
        placeAllWeCanOn(data, t, placeDelegate, player);
      }
    }
  }

  private static void placeAllWeCanOn(
      final GameState data,
      final Territory placeAt,
      final IAbstractPlaceDelegate placeDelegate,
      final GamePlayer player) {
    final PlaceableUnits pu = placeDelegate.getPlaceableUnits(player.getUnits(), placeAt);
    if (pu.getErrorMessage() != null) {
      return;
    }
    int placementLeft = pu.getMaxUnits();
    if (placementLeft == -1) {
      placementLeft = Integer.MAX_VALUE;
    }
    final List<Unit> seaUnits = new ArrayList<>(player.getMatches(Matches.unitIsSea()));
    if (!seaUnits.isEmpty()) {
      Territory seaPlaceAt = null;
      final Optional<Route> optionalAmphibRoute = getAmphibRoute(player, data);
      if (optionalAmphibRoute.isPresent()) {
        seaPlaceAt = optionalAmphibRoute.get().getAllTerritories().get(1);
      } else {
        final Set<Territory> seaNeighbors =
            data.getMap().getNeighbors(placeAt, Matches.territoryIsWater());
        if (!seaNeighbors.isEmpty()) {
          seaPlaceAt = CollectionUtils.getAny(seaNeighbors);
        }
      }
      if (seaPlaceAt != null) {
        final int seaPlacement = Math.min(placementLeft, seaUnits.size());
        placementLeft -= seaPlacement;
        final Collection<Unit> toPlace = seaUnits.subList(0, seaPlacement);
        doPlace(seaPlaceAt, toPlace, placeDelegate);
      }
    }
    final List<Unit> landUnits = new ArrayList<>(player.getMatches(Matches.unitIsLand()));
    if (!landUnits.isEmpty()) {
      final int landPlaceCount = Math.min(placementLeft, landUnits.size());
      final Collection<Unit> toPlace = landUnits.subList(0, landPlaceCount);
      doPlace(placeAt, toPlace, placeDelegate);
    }
  }

  private static void doPlace(
      final Territory where, final Collection<Unit> toPlace, final IAbstractPlaceDelegate del) {
    del.placeUnits(new ArrayList<>(toPlace), where, IAbstractPlaceDelegate.BidMode.NOT_BID);
    movePause();
  }

  @Override
  public boolean shouldBomberBomb(final Territory territory) {
    return true;
  }
}
