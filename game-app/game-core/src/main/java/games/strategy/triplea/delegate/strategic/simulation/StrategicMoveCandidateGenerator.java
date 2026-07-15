package games.strategy.triplea.delegate.strategic.simulation;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.data.MoveValidationResult;
import games.strategy.triplea.delegate.move.validation.MoveValidator;
import games.strategy.triplea.delegate.supply.SupplyNetworkResolver;
import games.strategy.triplea.delegate.visibility.VisibilityService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Generates deterministic, visibility-aware local-front movement candidates. */
public final class StrategicMoveCandidateGenerator {
  private static final Comparator<Territory> TERRITORY_ORDER =
      Comparator.comparing(Territory::getName);

  private StrategicMoveCandidateGenerator() {}

  public static List<StrategicAction> generate(
      final GameData data,
      final GamePlayer player,
      final StrategicPhase phase,
      final int maxActions) {
    if (phase != StrategicPhase.COMBAT_MOVE
        && phase != StrategicPhase.AIR_ASSIGNMENT
        && phase != StrategicPhase.REDEPLOYMENT) {
      return List.of();
    }
    final Set<Territory> visible = VisibilityService.getVisibleTerritories(player, data);
    final List<StrategicAction> actions = new ArrayList<>();
    final List<Territory> origins = new ArrayList<>(data.getMap().getTerritories());
    origins.sort(TERRITORY_ORDER);
    for (final Territory origin : origins) {
      if (!visible.contains(origin)) {
        continue;
      }
      for (final List<Unit> group : groups(origin, player, phase)) {
        final List<List<Unit>> selections = selections(group);
        for (final List<Unit> selection : selections) {
          addVisibleDestinations(data, player, phase, visible, origin, selection, actions);
          addHiddenAdjacentAttempts(data, player, phase, visible, origin, selection, actions);
        }
      }
    }
    actions.add(new StrategicAction("end_phase", Map.of("phase", phase.name())));
    final List<StrategicAction> distinct =
        new ArrayList<>(new LinkedHashSet<>(actions))
            .stream()
                .sorted(
                    Comparator.comparing(StrategicAction::type)
                        .thenComparing(action -> action.parameters().toString()))
                .toList();
    if (distinct.size() > maxActions) {
      throw new StrategicActionSpaceOverflow(distinct.size(), maxActions);
    }
    return distinct;
  }

  private static void addVisibleDestinations(
      final GameData data,
      final GamePlayer player,
      final StrategicPhase phase,
      final Set<Territory> visible,
      final Territory origin,
      final List<Unit> units,
      final List<StrategicAction> actions) {
    final BigDecimal movementLeft = minimumMovementLeft(units);
    final List<Territory> destinations =
        data.getMap().getNeighborsByMovementCost(origin, movementLeft, visible::contains).stream()
            .sorted(TERRITORY_ORDER)
            .toList();
    for (final Territory destination : destinations) {
      data.getMap()
          .getRouteForUnits(origin, destination, visible::contains, units, player)
          .filter(route -> isLegal(data, player, phase, origin, units, route))
          .ifPresent(route -> actions.add(moveAction(phase, units, route, false)));
    }
  }

  private static void addHiddenAdjacentAttempts(
      final GameData data,
      final GamePlayer player,
      final StrategicPhase phase,
      final Set<Territory> visible,
      final Territory origin,
      final List<Unit> units,
      final List<StrategicAction> actions) {
    if (units.stream()
        .anyMatch(unit -> !SupplyNetworkResolver.canMove(unit, origin, player, data))) {
      return;
    }
    for (final Territory destination :
        data.getMap().getNeighbors(origin).stream()
            .filter(neighbor -> !visible.contains(neighbor))
            .sorted(TERRITORY_ORDER)
            .toList()) {
      final Route route = new Route(origin, destination);
      if (units.stream()
          .allMatch(unit -> route.getMovementCost(unit).compareTo(unit.getMovementLeft()) <= 0)) {
        actions.add(moveAction(phase, units, route, true));
      }
    }
  }

  private static boolean isLegal(
      final GameData data,
      final GamePlayer player,
      final StrategicPhase phase,
      final Territory origin,
      final List<Unit> units,
      final Route route) {
    if (units.stream()
        .anyMatch(unit -> !SupplyNetworkResolver.canMove(unit, origin, player, data))) {
      return false;
    }
    final MoveValidationResult result =
        new MoveValidator(data, phase == StrategicPhase.REDEPLOYMENT)
            .validateMove(new MoveDescription(units, route), player);
    return !result.hasError() && !result.hasDisallowedUnits() && !result.hasUnresolvedUnits();
  }

  private static StrategicAction moveAction(
      final StrategicPhase phase,
      final List<Unit> units,
      final Route route,
      final boolean uncertain) {
    return new StrategicAction(
        phase == StrategicPhase.AIR_ASSIGNMENT ? "air_assignment" : "move",
        Map.of(
            "origin",
            route.getStart().getName(),
            "destination",
            route.getEnd().getName(),
            "route",
            route.getAllTerritories().stream()
                .map(Territory::getName)
                .collect(Collectors.joining(">")),
            "unitIds",
            units.stream()
                .map(unit -> unit.getId().toString())
                .sorted()
                .collect(Collectors.joining(",")),
            "uncertain",
            Boolean.toString(uncertain)));
  }

  private static List<List<Unit>> groups(
      final Territory territory, final GamePlayer player, final StrategicPhase phase) {
    final Map<GroupKey, List<Unit>> groups = new TreeMap<>();
    for (final Unit unit : territory.getUnitCollection().getUnits()) {
      if (!unit.isOwnedBy(player) || unit.getMovementLeft().compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }
      final boolean air = Matches.unitIsAir().test(unit);
      if (phase == StrategicPhase.AIR_ASSIGNMENT && !air) {
        continue;
      }
      if (phase == StrategicPhase.COMBAT_MOVE && air) {
        continue;
      }
      groups
          .computeIfAbsent(
              new GroupKey(unit.getType().getName(), unit.getMovementLeft().toPlainString()),
              ignored -> new ArrayList<>())
          .add(unit);
    }
    return groups.values().stream()
        .map(
            units ->
                units.stream()
                    .sorted(Comparator.comparing(unit -> unit.getId().toString()))
                    .toList())
        .toList();
  }

  private static List<List<Unit>> selections(final List<Unit> group) {
    if (group.size() == 1) {
      return List.of(group);
    }
    return List.of(List.of(group.getFirst()), group);
  }

  private static BigDecimal minimumMovementLeft(final Collection<Unit> units) {
    return units.stream()
        .map(Unit::getMovementLeft)
        .min(BigDecimal::compareTo)
        .orElse(BigDecimal.ZERO);
  }

  private record GroupKey(String unitType, String movementLeft) implements Comparable<GroupKey> {
    @Override
    public int compareTo(final GroupKey other) {
      return Comparator.comparing(GroupKey::unitType)
          .thenComparing(GroupKey::movementLeft)
          .compare(this, other);
    }
  }
}
