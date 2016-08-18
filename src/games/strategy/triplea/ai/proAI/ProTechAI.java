package games.strategy.triplea.ai.proAI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.IntegerMap;
import games.strategy.util.InverseMatch;
import games.strategy.util.Match;

/**
 * Pro tech AI.
 */
public final class ProTechAI {

  public static void tech(final ITechDelegate techDelegate, final GameData data, final PlayerID player) {
    if (!games.strategy.triplea.Properties.getWW2V3TechModel(data)) {
      return;
    }
    final Territory myCapitol = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
    final float eStrength = getStrengthOfPotentialAttackers(myCapitol, data, player, false, true, null);
    float myStrength = strength(myCapitol.getUnits().getUnits(), false, false, false);
    final List<Territory> areaStrength = getNeighboringLandTerritories(data, player, myCapitol);
    for (final Territory areaTerr : areaStrength) {
      myStrength += strength(areaTerr.getUnits().getUnits(), false, false, false) * 0.75F;
    }
    final boolean capDanger = myStrength < (eStrength * 1.25F + 3.0F);
    final Resource pus = data.getResourceList().getResource(Constants.PUS);
    final int PUs = player.getResources().getQuantity(pus);
    final Resource techtokens = data.getResourceList().getResource(Constants.TECH_TOKENS);
    final int TechTokens = player.getResources().getQuantity(techtokens);
    int TokensToBuy = 0;
    if (!capDanger && TechTokens < 3 && PUs > Math.random() * 160) {
      TokensToBuy = 1;
    }
    if (TechTokens > 0 || TokensToBuy > 0) {
      final List<TechnologyFrontier> cats = TechAdvance.getPlayerTechCategories(data, player);
      // retaining 65% chance of choosing land advances using basic ww2v3 model.
      if (data.getTechnologyFrontier().isEmpty()) {
        if (Math.random() > 0.35) {
          techDelegate.rollTech(TechTokens + TokensToBuy, cats.get(1), TokensToBuy, null);
        } else {
          techDelegate.rollTech(TechTokens + TokensToBuy, cats.get(0), TokensToBuy, null);
        }
      } else {
        final int rand = (int) (Math.random() * cats.size());
        techDelegate.rollTech(TechTokens + TokensToBuy, cats.get(rand), TokensToBuy, null);
      }
    }
  }

  /**
   * Returns the strength of all attackers to a territory
   * differentiates between sea and land attack
   * determines all transports within range of territory
   * determines all air units within range of territory (using 2 for fighters and 3 for bombers)
   * does not check for extended range fighters or bombers
   *
   * @param tFirst
   *        - can transports be killed before other sea units
   * @param ignoreOnlyPlanes
   *        - if true, returns 0.0F if only planes can attack the territory
   */
  private static float getStrengthOfPotentialAttackers(final Territory location, final GameData data,
      final PlayerID player, final boolean tFirst, final boolean ignoreOnlyPlanes, final List<Territory> ignoreTerr) {
    PlayerID ePlayer = null;
    final List<PlayerID> qID = getEnemyPlayers(data, player);
    final HashMap<PlayerID, Float> ePAttackMap = new HashMap<>();
    final Iterator<PlayerID> playerIter = qID.iterator();
    if (location == null) {
      return -1000.0F;
    }
    boolean nonTransportsInAttack = false;
    final boolean onWater = location.isWater();
    if (!onWater) {
      nonTransportsInAttack = true;
    }
    final Set<Territory> waterTerr = data.getMap().getNeighbors(location, Matches.TerritoryIsWater);
    while (playerIter.hasNext()) {
      float seaStrength = 0.0F, firstStrength = 0.0F, secondStrength = 0.0F, blitzStrength = 0.0F, strength = 0.0F,
          airStrength = 0.0F;
      ePlayer = playerIter.next();
      final CompositeMatch<Unit> enemyPlane =
          new CompositeMatchAnd<>(Matches.UnitIsAir, Matches.unitIsOwnedBy(ePlayer), Matches.UnitCanMove);
      final CompositeMatch<Unit> enemyTransport = new CompositeMatchAnd<>(Matches.unitIsOwnedBy(ePlayer),
          Matches.UnitIsSea, Matches.UnitIsTransport, Matches.UnitCanMove);
      final CompositeMatch<Unit> enemyShip =
          new CompositeMatchAnd<>(Matches.unitIsOwnedBy(ePlayer), Matches.UnitIsSea, Matches.UnitCanMove);
      final CompositeMatch<Unit> enemyTransportable = new CompositeMatchAnd<>(Matches.unitIsOwnedBy(ePlayer),
          Matches.UnitCanBeTransported, Matches.UnitIsNotAA, Matches.UnitCanMove);
      final CompositeMatch<Unit> aTransport =
          new CompositeMatchAnd<>(Matches.UnitIsSea, Matches.UnitIsTransport, Matches.UnitCanMove);
      final List<Territory> eFTerrs = findUnitTerr(data, ePlayer, enemyPlane);
      int maxFighterDistance = 0, maxBomberDistance = 0;
      // should change this to read production frontier and tech
      // reality is 99% of time units considered will have full move.
      // and likely player will have at least 1 max move plane.
      for (final Territory eFTerr : eFTerrs) {
        final List<Unit> eFUnits = eFTerr.getUnits().getMatches(enemyPlane);
        maxFighterDistance = Math.max(maxFighterDistance, MoveValidator.getMaxMovement(eFUnits));
      }
      // must be able to land...we will miss fighters who have a Carrier that can reach same sea zone...C'est la vie
      maxFighterDistance--;
      if (maxFighterDistance < 0) {
        maxFighterDistance = 0;
      }
      // must be able to land...won't miss anything here...unless special bombers that can land on carrier per above
      maxBomberDistance--;
      if (maxBomberDistance < 0) {
        maxBomberDistance = 0;
      }
      final List<Territory> eTTerrs = findUnitTerr(data, ePlayer, aTransport);
      int maxTransportDistance = 0;
      for (final Territory eTTerr : eTTerrs) {
        final List<Unit> eTUnits = eTTerr.getUnits().getMatches(aTransport);
        maxTransportDistance = Math.max(maxTransportDistance, MoveValidator.getMaxMovement(eTUnits));
      }
      final List<Unit> alreadyLoaded = new ArrayList<>();
      final List<Route> blitzTerrRoutes = new ArrayList<>();
      final List<Territory> checked = new ArrayList<>();
      final List<Unit> enemyWaterUnits = new ArrayList<>();
      for (final Territory t : data.getMap().getNeighbors(location,
          onWater ? Matches.TerritoryIsWater : Matches.TerritoryIsLand)) {
        if (ignoreTerr != null && ignoreTerr.contains(t)) {
          continue;
        }
        final List<Unit> enemies = t.getUnits().getMatches(Matches.unitIsOwnedBy(ePlayer));
        enemyWaterUnits.addAll(enemies);
        firstStrength += strength(enemies, true, onWater, tFirst);
        checked.add(t);
      }
      if (Matches.TerritoryIsLand.match(location)) {
        blitzStrength = determineEnemyBlitzStrength(location, blitzTerrRoutes, null, data, ePlayer);
      } else
      // get ships attack strength
      { // old assumed fleets won't split up, new lets them. no biggie.
        // assumes max ship movement is 3.
        // note, both old and new implementations
        // allow units to be calculated that are in
        // territories we have already assaulted
        // this can be easily changed
        final HashSet<Integer> ignore = new HashSet<>();
        ignore.add(1);
        final List<Route> r = new ArrayList<>();
        final List<Unit> ships = findAttackers(location, 3, ignore, ePlayer, data, enemyShip,
            Matches.territoryIsBlockedSea(ePlayer, data), ignoreTerr, r, true);
        secondStrength = strength(ships, true, true, tFirst);
        enemyWaterUnits.addAll(ships);
      }
      final List<Unit> attackPlanes =
          findPlaneAttackersThatCanLand(location, maxFighterDistance, ePlayer, data, ignoreTerr, checked);
      airStrength += allairstrength(attackPlanes, true);
      if (Matches.territoryHasWaterNeighbor(data).match(location) && Matches.TerritoryIsLand.match(location)) {
        for (final Territory t4 : data.getMap().getNeighbors(location, maxTransportDistance)) {
          if (!t4.isWater()) {
            continue;
          }
          boolean transportsCounted = false;
          final Iterator<Territory> iterTerr = waterTerr.iterator();
          while (!transportsCounted && iterTerr.hasNext()) {
            final Territory waterCheck = iterTerr.next();
            if (ePlayer == null) {
              continue;
            }
            final List<Unit> transports = t4.getUnits().getMatches(enemyTransport);
            if (transports.isEmpty()) {
              continue;
            }
            if (!t4.equals(waterCheck)) {
              final Route seaRoute = getMaxSeaRoute(data, t4, waterCheck, ePlayer, true, maxTransportDistance);
              if (seaRoute == null || seaRoute.getEnd() == null || seaRoute.getEnd() != waterCheck) {
                continue;
              }
            }
            final List<Unit> loadedUnits = new ArrayList<>();
            int availInf = 0, availOther = 0;
            for (final Unit xTrans : transports) {
              final Collection<Unit> thisTransUnits = TransportTracker.transporting(xTrans);
              if (thisTransUnits == null) {
                availInf += 2;
                availOther += 1;
                continue;
              } else {
                int Inf = 2, Other = 1;
                for (final Unit checkUnit : thisTransUnits) {
                  if (Matches.UnitIsInfantry.match(checkUnit)) {
                    Inf--;
                  }
                  if (Matches.UnitIsNotInfantry.match(checkUnit)) {
                    Inf--;
                    Other--;
                  }
                  loadedUnits.add(checkUnit);
                }
                availInf += Inf;
                availOther += Other;
              }
            }
            final Set<Territory> transNeighbors =
                data.getMap().getNeighbors(t4, Matches.isTerritoryAllied(ePlayer, data));
            for (final Territory xN : transNeighbors) {
              final List<Unit> aTransUnits = xN.getUnits().getMatches(enemyTransportable);
              aTransUnits.removeAll(alreadyLoaded);
              final List<Unit> availTransUnits = sortTransportUnits(aTransUnits);
              for (final Unit aTUnit : availTransUnits) {
                if (availInf > 0 && Matches.UnitIsInfantry.match(aTUnit)) {
                  availInf--;
                  loadedUnits.add(aTUnit);
                  alreadyLoaded.add(aTUnit);
                }
                if (availInf > 0 && availOther > 0 && Matches.UnitIsNotInfantry.match(aTUnit)) {
                  availInf--;
                  availOther--;
                  loadedUnits.add(aTUnit);
                  alreadyLoaded.add(aTUnit);
                }
              }
            }
            seaStrength += strength(loadedUnits, true, false, tFirst);
            transportsCounted = true;
          }
        }
      }
      strength = seaStrength + blitzStrength + firstStrength + secondStrength;
      if (!ignoreOnlyPlanes || strength > 0.0F) {
        strength += airStrength;
      }
      if (onWater) {
        final Iterator<Unit> eWaterIter = enemyWaterUnits.iterator();
        while (eWaterIter.hasNext() && !nonTransportsInAttack) {
          if (Matches.UnitIsNotTransport.match(eWaterIter.next())) {
            nonTransportsInAttack = true;
          }
        }
      }
      if (!nonTransportsInAttack) {
        strength = 0.0F;
      }
      ePAttackMap.put(ePlayer, strength);
    }
    float maxStrength = 0.0F;
    for (final PlayerID xP : qID) {
      if (ePAttackMap.get(xP) > maxStrength) {
        ePlayer = xP;
        maxStrength = ePAttackMap.get(xP);
      }
    }
    for (final PlayerID xP : qID) {
      if (ePlayer != xP) {
        // give 40% of other players...this is will affect a lot of decisions by AI
        maxStrength += ePAttackMap.get(xP) * 0.40F;
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
  private static float strength(final Collection<Unit> units, final boolean attacking, final boolean sea,
      final boolean transportsFirst) {
    float strength = 0.0F;
    if (units.isEmpty()) {
      return strength;
    }
    if (attacking && Match.noneMatch(units, Matches.unitHasAttackValueOfAtLeast(1))) {
      return strength;
    } else if (!attacking && Match.noneMatch(units, Matches.unitHasDefendValueOfAtLeast(1))) {
      return strength;
    }
    for (final Unit u : units) {
      final UnitAttachment unitAttachment = UnitAttachment.get(u.getType());
      if (unitAttachment.getIsInfrastructure()) {
        continue;
      } else if (unitAttachment.getIsSea() == sea) {
        final int unitAttack = unitAttachment.getAttack(u.getOwner());
        // BB = 6.0; AC=2.0/4.0; SUB=3.0; DS=4.0; TR=0.50/2.0; F=4.0/5.0; B=5.0/2.0;
        // played with this value a good bit
        strength += 1.00F;
        if (attacking) {
          strength += unitAttack * unitAttachment.getHitPoints();
        } else {
          strength += unitAttachment.getDefense(u.getOwner()) * unitAttachment.getHitPoints();
        }
        if (attacking) {
          if (unitAttack == 0) {
            strength -= 0.50F;
          }
        }
        if (unitAttack == 0 && unitAttachment.getTransportCapacity() > 0 && !transportsFirst) {
          // only allow transport to have 0.35 on defense; none on attack
          strength -= 0.50F;
        }
      } else if (unitAttachment.getIsAir() == sea) {
        strength += 1.00F;
        if (attacking) {
          strength += unitAttachment.getAttack(u.getOwner()) * unitAttachment.getAttackRolls(u.getOwner());
        } else {
          strength += unitAttachment.getDefense(u.getOwner());
        }
      }
    }
    if (attacking && !sea) {
      final int art = Match.countMatches(units, Matches.UnitIsArtillery);
      final int artSupport = Match.countMatches(units, Matches.UnitIsArtillerySupportable);
      strength += Math.min(art, artSupport);
    }
    return strength;
  }

  /**
   * Returns a list of all enemy players
   */
  private static List<PlayerID> getEnemyPlayers(final GameData data, final PlayerID player) {
    final List<PlayerID> enemyPlayers = new ArrayList<>();
    for (final PlayerID players : data.getPlayerList().getPlayers()) {
      if (!data.getRelationshipTracker().isAllied(player, players)) {
        enemyPlayers.add(players);
      }
    }
    return enemyPlayers;
  }

  /**
   * Determine the enemy potential for blitzing a territory - all enemies are combined
   *
   * @param blitzHere
   *        - Territory expecting to be blitzed
   * @param blitzTerr
   *        - Territory which is being blitzed through (not guaranteed to be all possible route territories!)
   * @param data
   * @param ePlayer
   *        - the enemy Player
   * @return actual strength of enemy units (armor)
   */
  private static float determineEnemyBlitzStrength(final Territory blitzHere, final List<Route> blitzTerrRoutes,
      final List<Territory> blockTerr, final GameData data, final PlayerID ePlayer) {
    final HashSet<Integer> ignore = new HashSet<>();
    ignore.add(1);
    final CompositeMatch<Unit> blitzUnit =
        new CompositeMatchAnd<>(Matches.unitIsOwnedBy(ePlayer), Matches.UnitCanBlitz, Matches.UnitCanMove);
    final CompositeMatch<Territory> validBlitzRoute = new CompositeMatchAnd<>(
        Matches.territoryHasNoEnemyUnits(ePlayer, data), Matches.TerritoryIsNotImpassableToLandUnits(ePlayer, data));
    final List<Route> routes = new ArrayList<>();
    final List<Unit> blitzUnits =
        findAttackers(blitzHere, 2, ignore, ePlayer, data, blitzUnit, validBlitzRoute, blockTerr, routes, false);
    for (final Route r : routes) {
      if (r.numberOfSteps() == 2) {
        blitzTerrRoutes.add(r);
      }
    }
    return strength(blitzUnits, true, false, true);
  }

  private static List<Unit> findAttackers(final Territory start, final int maxDistance,
      final HashSet<Integer> ignoreDistance, final PlayerID player, final GameData data,
      final Match<Unit> unitCondition, final Match<Territory> routeCondition, final List<Territory> blocked,
      final List<Route> routes, final boolean sea) {
    final IntegerMap<Territory> distance = new IntegerMap<>();
    final Map<Territory, Territory> visited = new HashMap<>();
    final List<Unit> units = new ArrayList<>();
    final Queue<Territory> q = new LinkedList<>();
    q.add(start);
    Territory current = null;
    distance.put(start, 0);
    visited.put(start, null);
    while (!q.isEmpty()) {
      current = q.remove();
      if (distance.getInt(current) == maxDistance) {
        break;
      }
      for (final Territory neighbor : data.getMap().getNeighbors(current)) {
        if (!distance.keySet().contains(neighbor)) {
          if (!neighbor.getUnits().someMatch(unitCondition)) {
            if (!routeCondition.match(neighbor)) {
              continue;
            }
          }
          if (sea) {
            final Route r = new Route();
            r.setStart(neighbor);
            r.add(current);
            if (MoveValidator.validateCanal(r, null, player, data) != null) {
              continue;
            }
          }
          distance.put(neighbor, distance.getInt(current) + 1);
          visited.put(neighbor, current);
          if (blocked != null && blocked.contains(neighbor)) {
            continue;
          }
          q.add(neighbor);
          final int dist = distance.getInt(neighbor);
          if (ignoreDistance.contains(dist)) {
            continue;
          }
          for (final Unit u : neighbor.getUnits()) {
            if (unitCondition.match(u) && Matches.UnitHasEnoughMovementForRoutes(routes).match(u)) {
              units.add(u);
            }
          }
        }
      }
    }
    // pain in the ass, should just redesign stop blitz attack
    for (final Territory t : visited.keySet()) {
      final Route r = new Route();
      Territory t2 = t;
      r.setStart(t);
      while (t2 != null) {
        t2 = visited.get(t2);
        if (t2 != null) {
          r.add(t2);
        }
      }
      routes.add(r);
    }
    return units;
  }

  /**
   * does not count planes already in the starting territory
   */
  private static List<Unit> findPlaneAttackersThatCanLand(final Territory start, final int maxDistance,
      final PlayerID player, final GameData data, final List<Territory> ignore, final List<Territory> checked) {
    if (checked.isEmpty()) {
      return new ArrayList<>();
    }
    final IntegerMap<Territory> distance = new IntegerMap<>();
    final IntegerMap<Unit> unitDistance = new IntegerMap<>();
    final List<Unit> units = new ArrayList<>();
    final Queue<Territory> q = new LinkedList<>();
    Territory lz = null, ac = null;
    final CompositeMatch<Unit> enemyPlane =
        new CompositeMatchAnd<>(Matches.UnitIsAir, Matches.unitIsOwnedBy(player), Matches.UnitCanMove);
    final CompositeMatch<Unit> enemyCarrier =
        new CompositeMatchAnd<>(Matches.UnitIsCarrier, Matches.unitIsOwnedBy(player), Matches.UnitCanMove);
    q.add(start);
    Territory current = null;
    distance.put(start, 0);
    while (!q.isEmpty()) {
      current = q.remove();
      if (distance.getInt(current) == maxDistance) {
        break;
      }
      for (final Territory neighbor : data.getMap().getNeighbors(current, TerritoryIsNotImpassableToAirUnits(data))) {
        if (!distance.keySet().contains(neighbor)) {
          q.add(neighbor);
          distance.put(neighbor, distance.getInt(current) + 1);
          if (lz == null && Matches.isTerritoryAllied(player, data).match(neighbor) && !neighbor.isWater()) {
            lz = neighbor;
          }
          if ((ignore != null && ignore.contains(neighbor)) || (checked != null && checked.contains(neighbor))) {
            for (final Unit u : neighbor.getUnits()) {
              if (ac == null && enemyCarrier.match(u)) {
                ac = neighbor;
              }
            }
          } else {
            for (final Unit u : neighbor.getUnits()) {
              if (ac == null && enemyCarrier.match(u)) {
                ac = neighbor;
              }
              if (enemyPlane.match(u)) {
                unitDistance.put(u, distance.getInt(neighbor));
              }
            }
          }
        }
      }
    }
    for (final Unit u : unitDistance.keySet()) {
      if (lz != null && Matches.UnitHasEnoughMovementForRoute(checked).match(u)) {
        units.add(u);
      } else if (ac != null && Matches.UnitCanLandOnCarrier.match(u)
          && Matches.UnitHasEnoughMovementForRoute(checked).match(u)) {
        units.add(u);
      }
    }
    return units;
  }

  /**
   * Determine the strength of a collection of airUnits
   * Caller should guarantee units are all air.
   */
  private static float allairstrength(final Collection<Unit> units, final boolean attacking) {
    float airstrength = 0.0F;
    for (final Unit u : units) {
      final UnitAttachment unitAttachment = UnitAttachment.get(u.getType());
      airstrength += 1.00F;
      if (attacking) {
        airstrength += unitAttachment.getAttack(u.getOwner());
      } else {
        airstrength += unitAttachment.getDefense(u.getOwner());
      }
    }
    return airstrength;
  }

  private static Route getMaxSeaRoute(final GameData data, final Territory start, final Territory destination,
      final PlayerID player, final boolean attacking, final int maxDistance) {
    // note this does not care if subs are submerged or not
    // should it? does submerging affect movement of enemies?
    if (start == null || destination == null || !start.isWater() || !destination.isWater()) {
      return null;
    }
    final CompositeMatch<Unit> ignore =
        new CompositeMatchAnd<>(Matches.UnitIsInfrastructure.invert(), Matches.alliedUnit(player, data).invert());
    final CompositeMatch<Unit> sub = new CompositeMatchAnd<>(Matches.UnitIsSub.invert());
    final CompositeMatch<Unit> transport =
        new CompositeMatchAnd<>(Matches.UnitIsTransport.invert(), Matches.UnitIsLand.invert());
    final CompositeMatch<Unit> unitCond = ignore;
    if (Properties.getIgnoreTransportInMovement(data)) {
      unitCond.add(transport);
    }
    if (Properties.getIgnoreSubInMovement(data)) {
      unitCond.add(sub);
    }
    final CompositeMatch<Territory> routeCond =
        new CompositeMatchAnd<>(Matches.territoryHasUnitsThatMatch(unitCond).invert(), Matches.TerritoryIsWater);
    CompositeMatch<Territory> routeCondition;
    if (attacking) {
      routeCondition = new CompositeMatchOr<>(Matches.territoryIs(destination), routeCond);
    } else {
      routeCondition = routeCond;
    }
    Route r = data.getMap().getRoute(start, destination, routeCondition);
    if (r == null || r.getEnd() == null) {
      return null;
    }
    // cheating because can't do stepwise calculation with canals
    // shouldn't be a huge problem
    // if we fail due to canal, then don't go near any enemy canals
    if (MoveValidator.validateCanal(r, null, player, data) != null) {
      r = data.getMap().getRoute(start, destination,
          new CompositeMatchAnd<>(routeCondition, Matches.territoryHasNonAllowedCanal(player, null, data).invert()));
    }
    if (r == null || r.getEnd() == null) {
      return null;
    }
    final int rDist = r.numberOfSteps();
    Route route2 = new Route();
    if (rDist <= maxDistance) {
      route2 = r;
    } else {
      route2.setStart(start);
      for (int i = 1; i <= maxDistance; i++) {
        route2.add(r.getAllTerritories().get(i));
      }
    }
    return route2;
  }

  /**
   * All Allied Territories which neighbor a territory
   * This duplicates getNeighbors(check, Matches.isTerritoryAllied(player, data))
   */
  private static List<Territory> getNeighboringLandTerritories(final GameData data, final PlayerID player,
      final Territory check) {
    final ArrayList<Territory> rVal = new ArrayList<>();
    final List<Territory> checkList = getExactNeighbors(check, 1, player, data, false);
    for (final Territory t : checkList) {
      if (Matches.isTerritoryAllied(player, data).match(t)
          && Matches.TerritoryIsNotImpassableToLandUnits(player, data).match(t)) {
        rVal.add(t);
      }
    }
    return rVal;
  }

  /**
   * Gets the neighbors which are exactly a certain # of territories away (distance)
   * Removes the inner circle neighbors
   * neutral - whether to include neutral countries
   */
  private static List<Territory> getExactNeighbors(final Territory territory, final int distance, final PlayerID player,
      final GameData data, final boolean neutral) {
    // old functionality retained, i.e. no route condition is imposed.
    // feel free to change, if you are confortable all calls to this function conform.
    final CompositeMatch<Territory> endCond = new CompositeMatchAnd<>(Matches.TerritoryIsImpassable.invert());
    if (!neutral || Properties.getNeutralsImpassable(data)) {
      endCond.add(Matches.TerritoryIsNeutralButNotWater.invert());
    }
    return findFontier(territory, endCond, Match.getAlwaysMatch(), distance, data);
  }

  /**
   * Finds list of territories at exactly distance from the start
   *
   * @param start
   * @param endCondition
   *        condition that all end points must satisfy
   * @param routeCondition
   *        condition that all traversed internal territories must satisy
   * @param distance
   * @param data
   */
  private static List<Territory> findFontier(final Territory start, final Match<Territory> endCondition,
      final Match<Territory> routeCondition, final int distance, final GameData data) {
    final Match<Territory> canGo = new CompositeMatchOr<>(endCondition, routeCondition);
    final IntegerMap<Territory> visited = new IntegerMap<>();
    final Queue<Territory> q = new LinkedList<>();
    final List<Territory> frontier = new ArrayList<>();
    q.addAll(data.getMap().getNeighbors(start, canGo));
    Territory current = null;
    visited.put(start, 0);
    for (final Territory t : q) {
      visited.put(t, 1);
      if (1 == distance && endCondition.match(t)) {
        frontier.add(t);
      }
    }
    while (!q.isEmpty()) {
      current = q.remove();
      if (visited.getInt(current) == distance) {
        break;
      } else {
        for (final Territory neighbor : data.getMap().getNeighbors(current, canGo)) {
          if (!visited.keySet().contains(neighbor)) {
            q.add(neighbor);
            final int dist = visited.getInt(current) + 1;
            visited.put(neighbor, dist);
            if (dist == distance && endCondition.match(neighbor)) {
              frontier.add(neighbor);
            }
          }
        }
      }
    }
    return frontier;
  }

  /**
   * Return Territories containing any unit depending on unitCondition
   * Differs from findCertainShips because it doesn't require the units be owned
   */
  private static List<Territory> findUnitTerr(final GameData data, final PlayerID player,
      final Match<Unit> unitCondition) {
    // Return territories containing a certain unit or set of Units
    final CompositeMatch<Unit> limitShips = new CompositeMatchAnd<>(unitCondition);
    final List<Territory> shipTerr = new ArrayList<>();
    final Collection<Territory> tNeighbors = data.getMap().getTerritories();
    for (final Territory t2 : tNeighbors) {
      if (t2.getUnits().someMatch(limitShips)) {
        shipTerr.add(t2);
      }
    }
    return shipTerr;
  }

  /**
   * Interleave infantry and artillery/armor for loading on transports
   */
  private static List<Unit> sortTransportUnits(final List<Unit> transUnits) {
    final List<Unit> sorted = new ArrayList<>();
    final List<Unit> infantry = new ArrayList<>();
    final List<Unit> artillery = new ArrayList<>();
    final List<Unit> armor = new ArrayList<>();
    final List<Unit> others = new ArrayList<>();
    for (final Unit x : transUnits) {
      if (Matches.UnitIsArtillerySupportable.match(x)) {
        infantry.add(x);
      } else if (Matches.UnitIsArtillery.match(x)) {
        artillery.add(x);
      } else if (Matches.UnitCanBlitz.match(x)) {
        armor.add(x);
      } else {
        others.add(x);
      }
    }
    int artilleryCount = artillery.size();
    int armorCount = armor.size();
    int othersCount = others.size();
    for (Unit anInfantry : infantry) {
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

  private static Match<Territory> TerritoryIsNotImpassableToAirUnits(final GameData data) {
    return new InverseMatch<>(TerritoryIsImpassableToAirUnits(data));
  }

  /**
   * Assumes that water is passable to air units always
   */
  private static Match<Territory> TerritoryIsImpassableToAirUnits(final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        if (Matches.TerritoryIsLand.match(t) && Matches.TerritoryIsImpassable.match(t)) {
          return true;
        }
        return false;
      }
    };
  }

}
