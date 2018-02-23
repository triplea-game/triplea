package games.strategy.triplea.ai.weakAI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.AiUtils;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.CollectionUtils;

class Utils {

  static List<Unit> getUnitsUpToStrength(final double maxStrength, final Collection<Unit> units,
      final boolean sea) {
    if (AiUtils.strength(units, true, sea) < maxStrength) {
      return new ArrayList<>(units);
    }
    final List<Unit> unitsUpToStrength = new ArrayList<>();
    for (final Unit u : units) {
      unitsUpToStrength.add(u);
      if (AiUtils.strength(unitsUpToStrength, true, sea) > maxStrength) {
        return unitsUpToStrength;
      }
    }
    return unitsUpToStrength;
  }

  static float getStrengthOfPotentialAttackers(final Territory location, final GameData data) {
    float strength = 0;
    for (final Territory t : data.getMap().getNeighbors(location,
        location.isWater() ? Matches.territoryIsWater() : Matches.territoryIsLand())) {
      final List<Unit> enemies = t.getUnits().getMatches(Matches.enemyUnit(location.getOwner(), data));
      strength += AiUtils.strength(enemies, true, location.isWater());
    }
    return strength;
  }

  static Route findNearest(final Territory start, final Predicate<Territory> endCondition,
      final Predicate<Territory> routeCondition, final GameData data) {
    Route shortestRoute = null;
    for (final Territory t : data.getMap().getTerritories()) {
      if (endCondition.test(t)) {
        final Predicate<Territory> routeOrEnd = routeCondition.or(Matches.territoryIs(t));
        final Route r = data.getMap().getRoute(start, t, routeOrEnd);
        if (r != null) {
          if ((shortestRoute == null) || (r.numberOfSteps() < shortestRoute.numberOfSteps())) {
            shortestRoute = r;
          }
        }
      }
    }
    return shortestRoute;
  }

  static boolean hasLandRouteToEnemyOwnedCapitol(final Territory t, final PlayerID us, final GameData data) {
    for (final PlayerID player : CollectionUtils.getMatches(data.getPlayerList().getPlayers(),
        Matches.isAtWar(us, data))) {
      for (final Territory capital : TerritoryAttachment.getAllCurrentlyOwnedCapitals(player, data)) {
        if (data.getMap().getDistance(t, capital, Matches.territoryIsLand()) != -1) {
          return true;
        }
      }
    }
    return false;
  }

  // returns all territories that are water territories (veqryn)
  static List<Territory> onlyWaterTerr(final List<Territory> allTerr) {
    final List<Territory> water = new ArrayList<>(allTerr);
    final Iterator<Territory> waterFactIter = water.iterator();
    while (waterFactIter.hasNext()) {
      final Territory waterFact = waterFactIter.next();
      if (!Matches.territoryIsWater().test(waterFact)) {
        waterFactIter.remove();
      }
    }
    return water;
  }

  /**
   * Return Territories containing any unit depending on unitCondition
   * Differs from findCertainShips because it doesn't require the units be owned.
   */
  static List<Territory> findUnitTerr(final GameData data, final Predicate<Unit> unitCondition) {
    // Return territories containing a certain unit or set of Units
    final List<Territory> shipTerr = new ArrayList<>();
    final Collection<Territory> neighbors = data.getMap().getTerritories();
    for (final Territory t2 : neighbors) {
      if (t2.getUnits().anyMatch(unitCondition)) {
        shipTerr.add(t2);
      }
    }
    return shipTerr;
  }
}
