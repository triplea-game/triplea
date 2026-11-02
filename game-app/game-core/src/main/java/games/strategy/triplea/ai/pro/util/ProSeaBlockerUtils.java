package games.strategy.triplea.ai.pro.util;

import static java.util.function.Predicate.not;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.ai.pro.data.ProBattleResult;
import games.strategy.triplea.ai.pro.data.ProOtherMoveOptions;
import games.strategy.triplea.ai.pro.data.ProTerritory;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.util.TuvUtils;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/** Sea blocker helpers: enemy transport routes to land territories and the zones that cut them. */
public final class ProSeaBlockerUtils {
  private ProSeaBlockerUtils() {}

  /** Enemy transports that could carry land units this turn, with their sea zones. */
  public static Map<Unit, Territory> findEnemyTransportsWithCargo(
      final GameData data, final GamePlayer player) {
    final Map<Unit, Territory> result = new HashMap<>();
    for (final Territory z : data.getMap().getTerritories()) {
      if (!z.isWater()) {
        continue;
      }
      for (final Unit u : z.getUnits()) {
        if (Matches.unitIsSeaTransport().test(u)
            && Matches.isAtWar(player).test(u.getOwner())
            && hasCargo(data, u, z)) {
          result.put(u, z);
        }
      }
    }
    return result;
  }

  private static boolean hasCargo(final GameData data, final Unit transport, final Territory zone) {
    if (!transport.getTransporting(zone).isEmpty()) {
      return true;
    }
    final Predicate<Unit> cargo =
        Matches.unitIsOwnedBy(transport.getOwner())
            .and(Matches.unitIsLand())
            .and(Matches.unitIsInfrastructure().negate());
    final Set<Territory> loadZones = new HashSet<>(data.getMap().getNeighbors(zone));
    loadZones.add(zone);
    for (final Territory z : loadZones) {
      if (!z.isWater()) {
        continue;
      }
      for (final Territory land : data.getMap().getNeighbors(z)) {
        if (!land.isWater() && land.anyUnitsMatch(cargo)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Whether any of the transports can reach (2 moves, through zones without its enemies and not
   * through {@code blocks}) a sea zone next to {@code target}.
   */
  public static boolean canUnloadAt(
      final GameData data,
      final Map<Unit, Territory> transports,
      final Territory target,
      final Set<Territory> blocks) {
    for (final Map.Entry<Unit, Territory> e : transports.entrySet()) {
      final Predicate<Territory> canMove =
          ProMatches.territoryCanMoveSeaUnitsThrough(e.getKey().getOwner(), true)
              .and(not(blocks::contains));
      final Set<Territory> reached = new HashSet<>();
      reached.add(e.getValue());
      Set<Territory> frontier = Set.of(e.getValue());
      for (int step = 0; step < 2; step++) {
        final Set<Territory> next = new HashSet<>();
        for (final Territory z : frontier) {
          for (final Territory n : data.getMap().getNeighbors(z, canMove)) {
            if (n.isWater() && reached.add(n)) {
              next.add(n);
            }
          }
        }
        frontier = next;
      }
      for (final Territory z : reached) {
        if (data.getMap().getNeighbors(z).contains(target)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Land territories in {@code candidates} the enemy's max attack takes with its amphibious units
   * and not without them, mapped to a value (production, factory, capital, defenders' TUV), highest
   * first. {@code defenders} gives the defenders to assume.
   */
  public static Map<Territory, Double> findAmphibOnlyTargets(
      final ProData proData,
      final ProOddsCalculator calc,
      final ProOtherMoveOptions enemyAttackOptions,
      final Collection<Territory> candidates,
      final Function<Territory, Collection<Unit>> defenders) {
    final Map<Territory, Double> values = new HashMap<>();
    for (final Territory t : candidates) {
      final ProTerritory enemyMax = enemyAttackOptions.getMax(t);
      if (t.isWater() || enemyMax == null || enemyMax.getMaxAmphibUnits().isEmpty()) {
        continue;
      }
      final boolean isFactory = ProMatches.territoryHasInfraFactoryAndIsLand().test(t);
      final boolean isCapital =
          TerritoryAttachment.get(t).map(TerritoryAttachment::isCapital).orElse(false);
      final int production = TerritoryAttachment.getProduction(t);
      if (production < 1 && !isFactory) {
        continue;
      }
      final Collection<Unit> myDefenders =
          defenders.apply(t).stream().filter(Matches.unitIsAaForAnything().negate()).toList();
      final Set<Unit> allAttackers = new HashSet<>(enemyMax.getMaxUnits());
      allAttackers.addAll(enemyMax.getMaxAmphibUnits());
      final ProBattleResult withAmphib =
          calc.calculateBattleResults(
              proData, t, allAttackers, myDefenders, enemyMax.getMaxBombardUnits());
      if (withAmphib.getWinPercentage() < proData.getMinWinPercentage()
          || !withAmphib.isHasLandUnitRemaining()) {
        continue;
      }
      final Set<Unit> landAttackers = new HashSet<>(enemyMax.getMaxUnits());
      enemyMax.getMaxAmphibUnits().forEach(landAttackers::remove);
      final ProBattleResult withoutAmphib =
          calc.calculateBattleResults(proData, t, landAttackers, myDefenders, Set.of());
      if (withoutAmphib.isHasLandUnitRemaining()
          && withoutAmphib.getWinPercentage() >= 100 - proData.getMinWinPercentage()) {
        continue;
      }
      values.put(
          t,
          2.0 * production
              + (isFactory ? 10 : 0)
              + (isCapital ? 30 : 0)
              + TuvUtils.getTuv(myDefenders, proData.getUnitValueMap()));
    }
    final Map<Territory, Double> sorted = new LinkedHashMap<>();
    values.entrySet().stream()
        .sorted(Map.Entry.<Territory, Double>comparingByValue().reversed())
        .forEach(e -> sorted.put(e.getKey(), e.getValue()));
    return sorted;
  }

  /**
   * Whether any enemy warship or plane outside {@code attacked} (territories this turn's attacks
   * clear) is within its movement of {@code zone} (map distance, so it errs towards "can attack").
   */
  public static boolean enemyCanAttack(
      final GameData data,
      final GamePlayer player,
      final Territory zone,
      final Set<Territory> attacked) {
    final Predicate<Unit> attacker =
        Matches.enemyUnit(player)
            .and(
                Matches.unitIsAir()
                    .or(Matches.unitIsSea().and(Matches.unitIsSeaTransport().negate())));
    for (final Territory t : data.getMap().getTerritories()) {
      final Collection<Unit> units = t.getMatches(attacker);
      if (units.isEmpty() || attacked.contains(t)) {
        continue;
      }
      final int distance = t.equals(zone) ? 0 : data.getMap().getDistance(t, zone);
      if (distance < 0) {
        continue;
      }
      for (final Unit u : units) {
        if (u.getUnitAttachment().getMovement(u.getOwner()) >= distance) {
          return true;
        }
      }
    }
    return false;
  }

  /** Sea zones a ship of {@code player} in {@code from} can end in this turn (no enemy zones). */
  public static Set<Territory> seaZonesInReach(
      final GameData data, final GamePlayer player, final Territory from, final int movement) {
    final Predicate<Territory> canMove = ProMatches.territoryCanMoveSeaUnitsThrough(player, false);
    final Set<Territory> reached = new HashSet<>();
    reached.add(from);
    Set<Territory> frontier = Set.of(from);
    for (int step = 0; step < movement; step++) {
      final Set<Territory> next = new HashSet<>();
      for (final Territory z : frontier) {
        for (final Territory n : data.getMap().getNeighbors(z, canMove)) {
          if (n.isWater() && reached.add(n)) {
            next.add(n);
          }
        }
      }
      frontier = next;
    }
    return reached;
  }
}
