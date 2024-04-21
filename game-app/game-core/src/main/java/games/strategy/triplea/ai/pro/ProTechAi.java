package games.strategy.triplea.ai.pro;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.pro.util.ProMatches;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.move.validation.MoveValidator;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.triplea.java.PredicateBuilder;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

/** Pro tech AI. */
final class ProTechAi {
  private ProTechAi() {}

  static void tech(final ITechDelegate techDelegate, final GameData data, final GamePlayer player) {
    if (!Properties.getWW2V3TechModel(data.getProperties())) {
      return;
    }
    final Territory myCapitol =
        TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data.getMap());
    final float enemyStrength = getStrengthOfPotentialAttackers(myCapitol, data, player);
    float myStrength =
        (myCapitol == null) ? 0.0F : strength(myCapitol.getUnits(), false, false, false);
    final List<Territory> areaStrength = getNeighboringLandTerritories(data, player, myCapitol);
    for (final Territory areaTerr : areaStrength) {
      myStrength += strength(areaTerr.getUnits(), false, false, false) * 0.75F;
    }
    final boolean capDanger = myStrength < (enemyStrength * 1.25F + 3.0F);
    final Resource pus = data.getResourceList().getResource(Constants.PUS);
    final int pusRemaining = player.getResources().getQuantity(pus);
    final Resource techTokens = data.getResourceList().getResource(Constants.TECH_TOKENS);
    final int techTokensQuantity = player.getResources().getQuantity(techTokens);
    int tokensToBuy = 0;
    if (!capDanger && techTokensQuantity < 3 && pusRemaining > Math.random() * 160) {
      tokensToBuy = 1;
    }
    if (techTokensQuantity > 0 || tokensToBuy > 0) {
      final List<TechnologyFrontier> cats = TechAdvance.getPlayerTechCategories(player);
      // retaining 65% chance of choosing land advances using basic ww2v3 model.
      if (data.getTechnologyFrontier().isEmpty()) {
        if (Math.random() > 0.35) {
          techDelegate.rollTech(techTokensQuantity + tokensToBuy, cats.get(1), tokensToBuy, null);
        } else {
          techDelegate.rollTech(techTokensQuantity + tokensToBuy, cats.get(0), tokensToBuy, null);
        }
      } else {
        final int rand = (int) (Math.random() * cats.size());
        techDelegate.rollTech(techTokensQuantity + tokensToBuy, cats.get(rand), tokensToBuy, null);
      }
    }
  }

  /**
   * Returns the strength of all attackers to a territory. Differentiates between sea and land
   * attack Determines all transports within range of territory Determines all air units within
   * range of territory (using 2 for fighters and 3 for bombers) Does not check for extended range
   * fighters or bombers
   */
  private static float getStrengthOfPotentialAttackers(
      final Territory location, final GameData data, final GamePlayer player) {
    final boolean transportsFirst = false;

    @Nullable GamePlayer enemyPlayer = null;
    final List<GamePlayer> enemyPlayers = getEnemyPlayers(data, player);
    final Map<GamePlayer, Float> enemyPlayerAttackMap = new HashMap<>();
    final Iterator<GamePlayer> playerIter = enemyPlayers.iterator();
    if (location == null) {
      return -1000.0F;
    }
    boolean nonTransportsInAttack = false;
    final boolean onWater = location.isWater();
    if (!onWater) {
      nonTransportsInAttack = true;
    }
    final Set<Territory> waterTerr =
        data.getMap().getNeighbors(location, Matches.territoryIsWater());
    while (playerIter.hasNext()) {
      float seaStrength = 0.0F;
      float firstStrength = 0.0F;
      float secondStrength = 0.0F;
      float blitzStrength = 0.0F;
      float strength;
      enemyPlayer = playerIter.next();
      final Predicate<Unit> enemyPlane =
          Matches.unitIsAir().and(Matches.unitIsOwnedBy(enemyPlayer)).and(Matches.unitCanMove());
      final Predicate<Unit> enemyTransport =
          Matches.unitIsOwnedBy(enemyPlayer)
              .and(Matches.unitIsSea())
              .and(Matches.unitIsSeaTransport())
              .and(Matches.unitCanMove());
      final Predicate<Unit> enemyShip =
          Matches.unitIsOwnedBy(enemyPlayer).and(Matches.unitIsSea()).and(Matches.unitCanMove());
      final Predicate<Unit> enemyTransportable =
          Matches.unitIsOwnedBy(enemyPlayer)
              .and(Matches.unitCanBeTransported())
              .and(Matches.unitIsNotAa())
              .and(Matches.unitCanMove());
      final Predicate<Unit> transport =
          Matches.unitIsSea().and(Matches.unitIsSeaTransport()).and(Matches.unitCanMove());
      final List<Territory> enemyFighterTerritories = findUnitTerr(data, enemyPlane);
      int maxFighterDistance = 0;
      // should change this to read production frontier and tech
      // reality is 99% of time units considered will have full move.
      // and likely player will have at least 1 max move plane.
      for (final Territory enemyFighterTerritory : enemyFighterTerritories) {
        final List<Unit> enemyFighterUnits = enemyFighterTerritory.getMatches(enemyPlane);
        maxFighterDistance =
            Math.max(
                maxFighterDistance, MoveValidator.getMaxMovement(enemyFighterUnits).intValue());
      }
      // must be able to land...we will miss fighters who have a Carrier that can reach same sea
      // zone...C'est la vie
      maxFighterDistance--;
      if (maxFighterDistance < 0) {
        maxFighterDistance = 0;
      }
      final List<Territory> enemyTransportTerritories = findUnitTerr(data, transport);
      int maxTransportDistance = 0;
      for (final Territory enemyTransportTerritory : enemyTransportTerritories) {
        final List<Unit> enemyTransportUnits = enemyTransportTerritory.getMatches(transport);
        maxTransportDistance =
            Math.max(
                maxTransportDistance, MoveValidator.getMaxMovement(enemyTransportUnits).intValue());
      }
      final List<Unit> alreadyLoaded = new ArrayList<>();
      final List<Route> blitzTerrRoutes = new ArrayList<>();
      final List<Territory> checked = new ArrayList<>();
      final List<Unit> enemyWaterUnits = new ArrayList<>();
      for (final Territory t :
          data.getMap()
              .getNeighbors(
                  location, onWater ? Matches.territoryIsWater() : Matches.territoryIsLand())) {
        final List<Unit> enemies = t.getMatches(Matches.unitIsOwnedBy(enemyPlayer));
        enemyWaterUnits.addAll(enemies);
        firstStrength += strength(enemies, true, onWater, transportsFirst);
        checked.add(t);
      }
      if (Matches.territoryIsLand().test(location)) {
        blitzStrength = determineEnemyBlitzStrength(location, blitzTerrRoutes, data, enemyPlayer);
      } else { // get ships attack strength
        // old assumed fleets won't split up, new lets them. no biggie.
        // assumes max ship movement is 3.
        // note, both old and new implementations allow units to be calculated that are in
        // territories we have already assaulted
        // this can be easily changed
        final Set<Integer> ignore = new HashSet<>();
        ignore.add(1);
        final List<Route> r = new ArrayList<>();
        final List<Unit> ships =
            findAttackers(
                location,
                3,
                ignore,
                enemyPlayer,
                data,
                enemyShip,
                ProMatches.territoryIsBlockedSea(enemyPlayer),
                r,
                true);
        secondStrength = strength(ships, true, true, transportsFirst);
        enemyWaterUnits.addAll(ships);
      }
      final List<Unit> attackPlanes =
          findPlaneAttackersThatCanLand(location, maxFighterDistance, enemyPlayer, data, checked);
      final float airStrength = allAirStrength(attackPlanes);
      if (Matches.territoryHasWaterNeighbor(data.getMap()).test(location)
          && Matches.territoryIsLand().test(location)) {
        for (final Territory t4 : data.getMap().getNeighbors(location, maxTransportDistance)) {
          if (!t4.isWater()) {
            continue;
          }
          for (final Territory waterCheck : waterTerr) {
            if (enemyPlayer == null) {
              continue;
            }
            final List<Unit> transports = t4.getMatches(enemyTransport);
            if (transports.isEmpty()) {
              continue;
            }
            if (!t4.equals(waterCheck)) {
              final Route seaRoute =
                  getMaxSeaRoute(
                      data, t4, waterCheck, transports, enemyPlayer, maxTransportDistance);
              if (seaRoute == null || !seaRoute.getEnd().equals(waterCheck)) {
                continue;
              }
            }
            final List<Unit> loadedUnits = new ArrayList<>();
            int availInf = 0;
            int availOther = 0;
            for (final Unit candidateTransport : transports) {
              final Collection<Unit> thisTransUnits = candidateTransport.getTransporting();
              int inf = 2;
              int other = 1;
              for (final Unit checkUnit : thisTransUnits) {
                if (Matches.unitIsLandTransportable().test(checkUnit)) {
                  inf--;
                } else {
                  inf--;
                  other--;
                }
                loadedUnits.add(checkUnit);
              }
              availInf += inf;
              availOther += other;
            }
            final Set<Territory> transNeighbors =
                data.getMap().getNeighbors(t4, Matches.isTerritoryAllied(enemyPlayer));
            for (final Territory transNeighbor : transNeighbors) {
              final List<Unit> transUnits = transNeighbor.getMatches(enemyTransportable);
              transUnits.removeAll(alreadyLoaded);
              final List<Unit> availTransUnits = sortTransportUnits(transUnits);
              for (final Unit transUnit : availTransUnits) {
                boolean landTransportable = Matches.unitIsLandTransportable().test(transUnit);
                if (availInf > 0 && landTransportable) {
                  availInf--;
                  loadedUnits.add(transUnit);
                  alreadyLoaded.add(transUnit);
                }
                if (availInf > 0 && availOther > 0 && !landTransportable) {
                  availInf--;
                  availOther--;
                  loadedUnits.add(transUnit);
                  alreadyLoaded.add(transUnit);
                }
              }
            }
            seaStrength += strength(loadedUnits, true, false, transportsFirst);
            break;
          }
        }
      }
      strength = seaStrength + blitzStrength + firstStrength + secondStrength;
      if (strength > 0.0F) {
        strength += airStrength;
      }
      if (onWater) {
        final Iterator<Unit> enemyWaterUnitsIter = enemyWaterUnits.iterator();
        while (enemyWaterUnitsIter.hasNext() && !nonTransportsInAttack) {
          if (Matches.unitIsNotSeaTransport().test(enemyWaterUnitsIter.next())) {
            nonTransportsInAttack = true;
          }
        }
      }
      if (!nonTransportsInAttack) {
        strength = 0.0F;
      }
      enemyPlayerAttackMap.put(enemyPlayer, strength);
    }
    float maxStrength = 0.0F;
    for (final GamePlayer enemyPlayerCandidate : enemyPlayers) {
      if (enemyPlayerAttackMap.get(enemyPlayerCandidate) > maxStrength) {
        enemyPlayer = enemyPlayerCandidate;
        maxStrength = enemyPlayerAttackMap.get(enemyPlayerCandidate);
      }
    }
    for (final GamePlayer enemyPlayerCandidate : enemyPlayers) {
      if (!Objects.equals(enemyPlayer, enemyPlayerCandidate)) {
        // give 40% of other players...this is will affect a lot of decisions by AI
        maxStrength += enemyPlayerAttackMap.get(enemyPlayerCandidate) * 0.40F;
      }
    }
    return maxStrength;
  }

  /**
   * Get a quick and dirty estimate of the strength of some units in a battle.
   *
   * @param units - the units to measure
   * @param attacking - are the units on attack or defense
   * @param sea - calculate the strength of the units in a sea or land battle?
   */
  private static float strength(
      final Collection<Unit> units,
      final boolean attacking,
      final boolean sea,
      final boolean transportsFirst) {
    float strength = 0.0F;
    if (units.isEmpty()) {
      return strength;
    }
    if ((attacking && units.stream().noneMatch(Matches.unitHasAttackValueOfAtLeast(1)))
        || (!attacking && units.stream().noneMatch(Matches.unitHasDefendValueOfAtLeast(1)))) {
      return strength;
    }
    for (final Unit u : units) {
      final UnitAttachment unitAttachment = u.getUnitAttachment();
      if (unitAttachment.getIsInfrastructure()) {
        if (unitAttachment.getIsSea() == sea) {
          final int unitAttack = unitAttachment.getAttack(u.getOwner());
          // BB = 6.0; AC=2.0/4.0; SUB=3.0; DS=4.0; TR=0.50/2.0; F=4.0/5.0; B=5.0/2.0;
          // played with this value a good bit
          strength += 1.00F;
          if (attacking) {
            strength += (float) unitAttack * unitAttachment.getHitPoints();
          } else {
            strength +=
                (float) unitAttachment.getDefense(u.getOwner()) * unitAttachment.getHitPoints();
          }
          if (attacking && unitAttack == 0) {
            strength -= 0.50F;
          }
          if (unitAttack == 0 && unitAttachment.getTransportCapacity() > 0 && !transportsFirst) {
            // only allow transport to have 0.35 on defense; none on attack
            strength -= 0.50F;
          }
        } else if (unitAttachment.getIsAir() == sea) {
          strength += 1.00F;
          if (attacking) {
            strength +=
                (float) unitAttachment.getAttack(u.getOwner())
                    * unitAttachment.getAttackRolls(u.getOwner());
          } else {
            strength += unitAttachment.getDefense(u.getOwner());
          }
        }
      }
    }
    if (attacking && !sea) {
      final int art = CollectionUtils.countMatches(units, Matches.unitIsArtillery());
      final int artSupport =
          CollectionUtils.countMatches(units, Matches.unitIsArtillerySupportable());
      strength += Math.min(art, artSupport);
    }
    return strength;
  }

  /** Returns a list of all enemy players. */
  private static List<GamePlayer> getEnemyPlayers(final GameState data, final GamePlayer player) {
    final List<GamePlayer> enemyPlayers = new ArrayList<>();
    for (final GamePlayer players : data.getPlayerList().getPlayers()) {
      if (!player.isAllied(players)) {
        enemyPlayers.add(players);
      }
    }
    return enemyPlayers;
  }

  /**
   * Determine the enemy potential for blitzing a territory - all enemies are combined.
   *
   * @param blitzHere - Territory expecting to be blitzed
   * @return actual strength of enemy units (armor)
   */
  private static float determineEnemyBlitzStrength(
      final Territory blitzHere,
      final List<Route> blitzTerrRoutes,
      final GameData data,
      final GamePlayer enemyPlayer) {
    final Set<Integer> ignore = new HashSet<>();
    ignore.add(1);
    final Predicate<Unit> blitzUnit =
        Matches.unitIsOwnedBy(enemyPlayer).and(Matches.unitCanBlitz()).and(Matches.unitCanMove());
    final Predicate<Territory> validBlitzRoute =
        Matches.territoryHasNoEnemyUnits(enemyPlayer)
            .and(Matches.territoryIsNotImpassableToLandUnits(enemyPlayer));
    final List<Route> routes = new ArrayList<>();
    final List<Unit> blitzUnits =
        findAttackers(
            blitzHere, 2, ignore, enemyPlayer, data, blitzUnit, validBlitzRoute, routes, false);
    for (final Route r : routes) {
      if (r.numberOfSteps() == 2) {
        blitzTerrRoutes.add(r);
      }
    }
    return strength(blitzUnits, true, false, true);
  }

  private static List<Unit> findAttackers(
      final Territory start,
      final int maxDistance,
      final Set<Integer> ignoreDistance,
      final GamePlayer player,
      final GameData data,
      final Predicate<Unit> unitCondition,
      final Predicate<Territory> routeCondition,
      final List<Route> routes,
      final boolean sea) {

    final IntegerMap<Territory> distance = new IntegerMap<>();
    final Map<Territory, Territory> visited = new HashMap<>();
    final List<Unit> units = new ArrayList<>();
    final Queue<Territory> q = new ArrayDeque<>();
    q.add(start);
    Territory current;
    distance.put(start, 0);
    visited.put(start, null);
    MoveValidator moveValidator = new MoveValidator(data, false);
    while (!q.isEmpty()) {
      current = q.remove();
      if (distance.getInt(current) == maxDistance) {
        break;
      }
      for (final Territory neighbor : data.getMap().getNeighbors(current)) {
        if (!distance.keySet().contains(neighbor)) {
          if (!neighbor.anyUnitsMatch(unitCondition) && !routeCondition.test(neighbor)) {
            continue;
          }
          if (sea
              && moveValidator.validateCanal(new Route(neighbor, current), null, player) != null) {
            continue;
          }
          distance.put(neighbor, distance.getInt(current) + 1);
          visited.put(neighbor, current);
          q.add(neighbor);
          final int dist = distance.getInt(neighbor);
          if (ignoreDistance.contains(dist)) {
            continue;
          }
          units.addAll(neighbor.getMatches(unitCondition));
        }
      }
    }
    // pain in the ass, should just redesign stop blitz attack
    for (final Territory t : visited.keySet()) {
      final List<Territory> territories = new ArrayList<>();
      territories.add(t);
      Territory t2 = t;
      while (t2 != null) {
        t2 = visited.get(t2);
        if (t2 != null) {
          territories.add(t2);
        }
      }
      final Route r = new Route(territories);
      routes.add(r);
    }
    return units;
  }

  /** does not count planes already in the starting territory. */
  private static List<Unit> findPlaneAttackersThatCanLand(
      final Territory start,
      final int maxDistance,
      final GamePlayer player,
      final GameState data,
      final List<Territory> checked) {

    if (checked.isEmpty()) {
      return new ArrayList<>();
    }
    final IntegerMap<Territory> distance = new IntegerMap<>();
    final IntegerMap<Unit> unitDistance = new IntegerMap<>();
    final List<Unit> units = new ArrayList<>();
    final Queue<Territory> q = new ArrayDeque<>();
    Territory lz = null;
    Territory ac = null;
    final Predicate<Unit> enemyPlane =
        Matches.unitIsAir().and(Matches.unitIsOwnedBy(player)).and(Matches.unitCanMove());
    final Predicate<Unit> enemyCarrier =
        Matches.unitIsCarrier().and(Matches.unitIsOwnedBy(player)).and(Matches.unitCanMove());
    q.add(start);
    Territory current;
    distance.put(start, 0);
    while (!q.isEmpty()) {
      current = q.remove();
      if (distance.getInt(current) == maxDistance) {
        break;
      }
      for (final Territory neighbor :
          data.getMap().getNeighbors(current, territoryIsNotImpassableToAirUnits())) {
        if (!distance.keySet().contains(neighbor)) {
          q.add(neighbor);
          distance.put(neighbor, distance.getInt(current) + 1);
          if (lz == null
              && Matches.isTerritoryAllied(player).test(neighbor)
              && !neighbor.isWater()) {
            lz = neighbor;
          }
          if (checked.contains(neighbor)) {
            for (final Unit u : neighbor.getUnitCollection()) {
              if (ac == null && enemyCarrier.test(u)) {
                ac = neighbor;
              }
            }
          } else {
            for (final Unit u : neighbor.getUnitCollection()) {
              if (ac == null && enemyCarrier.test(u)) {
                ac = neighbor;
              }
              if (enemyPlane.test(u)) {
                unitDistance.put(u, distance.getInt(neighbor));
              }
            }
          }
        }
      }
    }
    for (final Unit u : unitDistance.keySet()) {
      if ((lz != null) || (ac != null && Matches.unitCanLandOnCarrier().test(u))) {
        units.add(u);
      }
    }
    return units;
  }

  /**
   * Determine the strength of a collection of airUnits Caller should guarantee units are all air.
   */
  private static float allAirStrength(final Collection<Unit> units) {
    float airStrength = 0.0F;
    for (final Unit u : units) {
      final UnitAttachment unitAttachment = u.getUnitAttachment();
      airStrength += 1.00F;
      airStrength += unitAttachment.getAttack(u.getOwner());
    }
    return airStrength;
  }

  private static Route getMaxSeaRoute(
      final GameState data,
      final Territory start,
      final Territory destination,
      final Collection<Unit> units,
      final GamePlayer player,
      final int maxDistance) {
    // note this does not care if subs are submerged or not
    // should it? does submerging affect movement of enemies?
    if (start == null || destination == null || !start.isWater() || !destination.isWater()) {
      return null;
    }
    final Predicate<Unit> transport =
        Matches.unitIsSeaTransport().negate().and(Matches.unitIsLand().negate());
    final Predicate<Unit> unitCond =
        PredicateBuilder.of(Matches.unitIsInfrastructure().negate())
            .and(Matches.alliedUnit(player).negate())
            .and(Matches.unitCanBeMovedThroughByEnemies().negate())
            .andIf(Properties.getIgnoreTransportInMovement(data.getProperties()), transport)
            .build();
    final Predicate<Territory> routeCond =
        Matches.territoryHasUnitsThatMatch(unitCond).negate().and(Matches.territoryIsWater());
    Route r = data.getMap().getRouteForUnits(start, destination, routeCond, units, player);
    if (r == null) {
      return null;
    }
    final int routeDistance = r.numberOfSteps();
    if (routeDistance > maxDistance) {
      final List<Territory> territories = new ArrayList<>();
      territories.add(start);
      territories.addAll(r.getSteps().subList(0, maxDistance));
      r = new Route(territories);
    }
    return r;
  }

  /**
   * All Allied Territories which neighbor a territory This duplicates getNeighbors(check,
   * Matches.isTerritoryAllied(player, data))
   */
  private static List<Territory> getNeighboringLandTerritories(
      final GameState data, final GamePlayer player, final Territory check) {
    final List<Territory> territories = new ArrayList<>();
    final List<Territory> checkList = getExactNeighbors(check, data);
    for (final Territory t : checkList) {
      if (Matches.isTerritoryAllied(player).test(t)
          && Matches.territoryIsNotImpassableToLandUnits(player).test(t)) {
        territories.add(t);
      }
    }
    return territories;
  }

  /** Gets the neighbors which are one territory away. */
  private static List<Territory> getExactNeighbors(
      final Territory territory, final GameState data) {
    // old functionality retained, i.e. no route condition is imposed.
    // feel free to change, if you are comfortable all calls to this function conform.
    final Predicate<Territory> endCond =
        PredicateBuilder.of(Matches.territoryIsImpassable().negate())
            .andIf(
                Properties.getNeutralsImpassable(data.getProperties()),
                Matches.territoryIsNeutralButNotWater().negate())
            .build();
    return findFrontier(territory, endCond, it -> true, data);
  }

  /**
   * Finds list of territories at exactly distance from the start.
   *
   * @param endCondition condition that all end points must satisfy
   * @param routeCondition condition that all traversed internal territories must satisfied
   */
  private static List<Territory> findFrontier(
      final Territory start,
      final Predicate<Territory> endCondition,
      final Predicate<Territory> routeCondition,
      final GameState data) {
    final Predicate<Territory> canGo = endCondition.or(routeCondition);
    final IntegerMap<Territory> visited = new IntegerMap<>();
    final List<Territory> frontier = new ArrayList<>();
    final Queue<Territory> q = new ArrayDeque<>(data.getMap().getNeighbors(start, canGo));
    Territory current;
    visited.put(start, 0);
    for (final Territory t : q) {
      visited.put(t, 1);
      if (endCondition.test(t)) {
        frontier.add(t);
      }
    }
    while (!q.isEmpty()) {
      current = q.remove();
      if (visited.getInt(current) == 1) {
        break;
      }

      for (final Territory neighbor : data.getMap().getNeighbors(current, canGo)) {
        if (!visited.keySet().contains(neighbor)) {
          q.add(neighbor);
          final int dist = visited.getInt(current) + 1;
          visited.put(neighbor, dist);
          if (dist == 1 && endCondition.test(neighbor)) {
            frontier.add(neighbor);
          }
        }
      }
    }
    return frontier;
  }

  /**
   * Return Territories containing any unit depending on unitCondition Differs from findCertainShips
   * because it doesn't require the units be owned.
   */
  private static List<Territory> findUnitTerr(
      final GameState data, final Predicate<Unit> unitCondition) {
    // Return territories containing a certain unit or set of Units
    final List<Territory> shipTerr = new ArrayList<>();
    final Collection<Territory> neighbors = data.getMap().getTerritories();
    for (final Territory t2 : neighbors) {
      if (t2.anyUnitsMatch(unitCondition)) {
        shipTerr.add(t2);
      }
    }
    return shipTerr;
  }

  /** Interleave infantry and artillery/armor for loading on transports. */
  private static List<Unit> sortTransportUnits(final List<Unit> transUnits) {
    final List<Unit> sorted = new ArrayList<>();
    final List<Unit> infantry = new ArrayList<>();
    final List<Unit> artillery = new ArrayList<>();
    final List<Unit> armor = new ArrayList<>();
    final List<Unit> others = new ArrayList<>();
    for (final Unit x : transUnits) {
      if (Matches.unitIsArtillerySupportable().test(x)) {
        infantry.add(x);
      } else if (Matches.unitIsArtillery().test(x)) {
        artillery.add(x);
      } else if (Matches.unitCanBlitz().test(x)) {
        armor.add(x);
      } else {
        others.add(x);
      }
    }
    int artilleryCount = artillery.size();
    int armorCount = armor.size();
    int othersCount = others.size();
    for (final Unit anInfantry : infantry) {
      sorted.add(anInfantry);
      // this should be based on combined attack and defense powers, not on attachments like blitz
      if (armorCount > 0) {
        sorted.add(armor.get(armorCount - 1));
        armorCount--;
      } else if (artilleryCount > 0) {
        sorted.add(artillery.get(artilleryCount - 1));
        artilleryCount--;
      } else if (othersCount > 0) {
        sorted.add(others.get(othersCount - 1));
        othersCount--;
      }
    }
    if (artilleryCount > 0) {
      for (int j2 = 0; j2 < artilleryCount; j2++) {
        sorted.add(artillery.get(j2));
      }
    }
    if (othersCount > 0) {
      for (int j4 = 0; j4 < othersCount; j4++) {
        sorted.add(others.get(j4));
      }
    }
    if (armorCount > 0) {
      for (int j3 = 0; j3 < armorCount; j3++) {
        sorted.add(armor.get(j3));
      }
    }
    return sorted;
  }

  private static Predicate<Territory> territoryIsNotImpassableToAirUnits() {
    return territoryIsImpassableToAirUnits().negate();
  }

  /** Assumes that water is passable to air units always. */
  private static Predicate<Territory> territoryIsImpassableToAirUnits() {
    return Matches.territoryIsLand().and(Matches.territoryIsImpassable());
  }
}
