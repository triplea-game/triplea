package games.strategy.triplea.ai.pro.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.ai.pro.data.ProTerritory;
import games.strategy.triplea.ai.pro.logging.ProLogger;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.move.validation.MoveValidator;
import games.strategy.triplea.util.TransportUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import lombok.experimental.UtilityClass;

/**
 * Plans paratrooper airlifts for the combat move phase.
 *
 * <p>Only air transports (eg: bombers) that are already assigned to attack an enemy territory carry
 * passengers. Passengers are unassigned, unmoved, air-transportable units (eg: infantry) that start
 * in the same territory as the air transport. Nothing happens unless the player has the
 * paratroopers tech, since the unit matchers are gated on it.
 */
@UtilityClass
public class ProAirliftPlanner {

  /**
   * Finds airlifts for air transports already attacking in {@code attackMap}.
   *
   * <p>Each air transport that gets passengers is removed from its target's unit list, so that the
   * regular move calculation doesn't also move it, and the returned move (carrying the dependents)
   * replaces it. The passengers are not added to the target's unit list.
   *
   * @param unitTerritory Where each unit currently is.
   * @return Moves to perform before all other combat moves; an air transport must not have moved.
   */
  public static List<MoveDescription> planAirlifts(
      final GameData data,
      final GamePlayer player,
      final Map<Territory, ProTerritory> attackMap,
      final Function<Unit, Territory> unitTerritory) {

    final List<MoveDescription> moves = new ArrayList<>();
    final Set<Unit> assigned = assignedUnits(attackMap);
    final MoveValidator validator = new MoveValidator(data, false);

    for (final ProTerritory patd : new ArrayList<>(attackMap.values())) {
      final Territory target = patd.getTerritory();
      if (!isEnemyLand(player).test(target)) {
        continue;
      }
      for (final Unit airTransport :
          new ArrayList<>(patd.getUnits().stream().filter(Matches.unitIsAirTransport()).toList())) {
        final Optional<MoveDescription> move =
            planAirlift(data, player, patd, airTransport, assigned, validator, unitTerritory);
        if (move.isPresent()) {
          moves.add(move.get());
        } else if (isUndefended(player, target) && patd.getUnits().size() == 1) {
          // An air transport planned as a paradrop alone can't take anything on its own.
          patd.getUnits().remove(airTransport);
        }
      }
    }
    return moves;
  }

  /**
   * Sends idle air transports with paratroopers to take undefended enemy land that no ground unit
   * can reach, by assigning the air transport to that territory in {@code attackMap}; {@link
   * #planAirlifts} then loads the passengers.
   *
   * <p>A transport only goes if it can fly there and back, the territory is worth something, and
   * {@code canHold} accepts the paratroopers staying there.
   *
   * @param canHold Whether the passengers keep the territory against the enemy counter-attack.
   */
  public static void planDrops(
      final GameData data,
      final GamePlayer player,
      final ProData proData,
      final Collection<Territory> myUnitTerritories,
      final Map<Territory, ProTerritory> attackMap,
      final Function<Unit, Territory> unitTerritory,
      final BiPredicate<Territory, List<Unit>> canHold) {

    final Set<Unit> assigned = assignedUnits(attackMap);
    for (final Territory start : myUnitTerritories) {
      final List<Unit> idleTransports =
          start
              .getUnitCollection()
              .getMatches(Matches.unitIsOwnedBy(player).and(isIdleTransport()));
      for (final Unit airTransport : idleTransports) {
        if (assigned.contains(airTransport)) {
          continue;
        }
        final List<Unit> passengers = findPassengers(player, start, airTransport, assigned);
        if (passengers.isEmpty() || loadAirTransport(airTransport, passengers).isEmpty()) {
          continue;
        }
        final Territory target =
            bestDropTarget(data, player, start, airTransport, attackMap, passengers, canHold);
        if (target == null) {
          continue;
        }
        final ProTerritory patd = proData.getProTerritory(attackMap, target);
        patd.addUnit(airTransport);
        patd.setCanHold(true);
        assigned.add(airTransport);
        assigned.addAll(passengers);
        ProLogger.info("Planning paradrop on " + target + " by " + airTransport);
      }
    }
  }

  private static Predicate<Unit> isIdleTransport() {
    return Matches.unitIsAirTransport().and(Matches.unitHasMoved().negate());
  }

  private static @Nullable Territory bestDropTarget(
      final GameData data,
      final GamePlayer player,
      final Territory start,
      final Unit airTransport,
      final Map<Territory, ProTerritory> attackMap,
      final List<Unit> passengers,
      final BiPredicate<Territory, List<Unit>> canHold) {
    final BigDecimal range = airTransport.getMovementLeft();
    final Predicate<Territory> canFly =
        ProMatches.territoryCanMoveAirUnitsAndNoAa(data, player, true);
    Territory best = null;
    int bestProduction = -1;
    for (final Territory target : data.getMap().getNeighborsByMovementCost(start, range, canFly)) {
      final int production = TerritoryAttachment.getProduction(target);
      final boolean isCapital =
          TerritoryAttachment.get(target).map(TerritoryAttachment::isCapital).orElse(false);
      if (!isEnemyLand(player).test(target)
          || ProUtils.isNeutralLand(target)
          || !isUndefended(player, target)
          || isAlreadyTaken(attackMap.get(target))
          || (production <= 0 && !isCapital)
          || production <= bestProduction) {
        continue;
      }
      final var route = data.getMap().getRouteForUnit(start, target, canFly, airTransport, player);
      if (route.isEmpty()
          || route.get().getMovementCost(airTransport).multiply(BigDecimal.TWO).compareTo(range) > 0
          || !canHold.test(target, passengers)) {
        continue;
      }
      best = target;
      bestProduction = production;
    }
    return best;
  }

  private static boolean isAlreadyTaken(final @Nullable ProTerritory patd) {
    return patd != null
        && (!patd.getUnits().isEmpty()
            || !patd.getAmphibAttackMap().isEmpty()
            || !patd.getBombers().isEmpty());
  }

  private static Optional<MoveDescription> planAirlift(
      final GameData data,
      final GamePlayer player,
      final ProTerritory patd,
      final Unit airTransport,
      final Set<Unit> assigned,
      final MoveValidator validator,
      final Function<Unit, Territory> unitTerritory) {
    final Territory target = patd.getTerritory();
    final Territory start = unitTerritory.apply(airTransport);
    if (start == null || start.equals(target)) {
      return Optional.empty();
    }
    final List<Unit> passengers = findPassengers(player, start, airTransport, assigned);
    if (passengers.isEmpty()) {
      return Optional.empty();
    }
    final Map<Unit, Collection<Unit>> dependents = loadAirTransport(airTransport, passengers);
    if (dependents.isEmpty()) {
      return Optional.empty();
    }
    final Collection<Unit> carried = dependents.get(airTransport);
    final var route =
        data.getMap()
            .getRouteForUnit(
                start,
                target,
                ProMatches.territoryCanMoveAirUnitsAndNoAa(data, player, true),
                airTransport,
                player);
    if (route.isEmpty()) {
      return Optional.empty();
    }
    final var units = new ArrayList<Unit>(carried);
    units.add(airTransport);
    final var move = new MoveDescription(units, route.get(), Map.of(), dependents);
    final var result = validator.validateMove(move, player);
    if (result.hasError()) {
      ProLogger.debug("Skipping airlift to " + target + ": " + result.getError());
      return Optional.empty();
    }
    assigned.addAll(carried);
    patd.getUnits().remove(airTransport);
    ProLogger.info("Airlifting " + carried + " with " + airTransport + " to " + target);
    return Optional.of(move);
  }

  /**
   * Whether nothing in the territory fights or shoots: no enemy units except infrastructure that is
   * not anti-aircraft, so a landing there is simply a conquest.
   */
  static boolean isUndefended(final GamePlayer player, final Territory t) {
    return !t.getUnitCollection()
        .anyMatch(
            ProMatches.unitIsEnemyAndNotInfa(player)
                .or(Matches.enemyUnit(player).and(Matches.unitIsAaForAnything())));
  }

  private static Predicate<Territory> isEnemyLand(final GamePlayer player) {
    return Matches.territoryIsLand().and(Matches.isTerritoryEnemy(player));
  }

  /** Everything the regular attack plan already uses, so it is never double-booked. */
  private static Set<Unit> assignedUnits(final Map<Territory, ProTerritory> attackMap) {
    final Set<Unit> assigned = new HashSet<>();
    for (final ProTerritory patd : attackMap.values()) {
      assigned.addAll(patd.getUnits());
      assigned.addAll(patd.getBombers());
      assigned.addAll(patd.getCantMoveUnits());
      patd.getAmphibAttackMap()
          .forEach(
              (transport, loaded) -> {
                assigned.add(transport);
                assigned.addAll(loaded);
              });
      assigned.addAll(patd.getBombardTerritoryMap().keySet());
    }
    return assigned;
  }

  /**
   * Unmoved passengers in the air transport's territory, strongest attackers first. At least one
   * other land unit is left behind so a territory isn't stripped of defenders for a gamble.
   */
  private static List<Unit> findPassengers(
      final GamePlayer player,
      final Territory start,
      final Unit airTransport,
      final Set<Unit> assigned) {
    if (airTransport.hasMoved()) {
      return List.of();
    }
    final List<Unit> candidates =
        start
            .getUnitCollection()
            .getMatches(
                Matches.unitIsOwnedBy(player)
                    .and(Matches.unitIsAirTransportable())
                    .and(Matches.unitHasMoved().negate())
                    .and(u -> !assigned.contains(u)));
    final long otherLandUnits =
        start
            .getUnitCollection()
            .getMatches(Matches.unitIsOwnedBy(player).and(Matches.unitIsLand()))
            .stream()
            .filter(u -> !candidates.contains(u))
            .count();
    candidates.sort(
        Comparator.comparingInt((Unit u) -> u.getUnitAttachment().getAttack(player)).reversed());
    final int spare = (int) Math.max(0, candidates.size() - (otherLandUnits == 0 ? 1 : 0));
    return new ArrayList<>(candidates.subList(0, spare));
  }

  private static Map<Unit, Collection<Unit>> loadAirTransport(
      final Unit airTransport, final List<Unit> passengers) {
    final var candidates = new ArrayList<Unit>(passengers);
    candidates.add(airTransport);
    final Map<Unit, Unit> paratroopToTransport = TransportUtils.mapParatroopers(candidates);
    if (paratroopToTransport.isEmpty()) {
      return Map.of();
    }
    final Map<Unit, Collection<Unit>> dependents = new HashMap<>();
    paratroopToTransport.forEach(
        (paratrooper, transport) ->
            dependents.computeIfAbsent(transport, t -> new ArrayList<>()).add(paratrooper));
    return dependents;
  }
}
