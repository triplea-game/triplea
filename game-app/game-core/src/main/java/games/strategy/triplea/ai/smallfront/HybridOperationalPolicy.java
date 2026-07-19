package games.strategy.triplea.ai.smallfront;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.SupplyTerritoryAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.AirControlTracker;
import games.strategy.triplea.delegate.battle.AirControlTracker.Status;
import games.strategy.triplea.delegate.strategic.simulation.StrategicAction;
import games.strategy.triplea.delegate.strategic.simulation.StrategicActionResolver;
import games.strategy.triplea.delegate.supply.SupplyNetworkResolver;
import games.strategy.triplea.delegate.visibility.VisibilityService;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Scores Small Front moves as parts of one operational plan rather than isolated objective probes.
 *
 * <p>The policy keeps the old objective pressure, but also values air control over intended
 * battles, preserves supply sources and road articulation points, keeps minimum reserves on
 * objectives, and strongly discourages immediately reversing a move made earlier in the phase.
 */
public final class HybridOperationalPolicy implements SmallFrontPolicy {
  private static final int NO_PATH = 99;
  private static final int REVERSAL_PENALTY = 100;

  @Override
  public Optional<StrategicAction> choose(
      final List<StrategicAction> legalActions,
      final GameData data,
      final GamePlayer player) {
    return choose(legalActions, data, player, List.of());
  }

  @Override
  public Optional<StrategicAction> choose(
      final List<StrategicAction> legalActions,
      final GameData data,
      final GamePlayer player,
      final List<StrategicAction> completedActions) {
    final EvaluationContext context = new EvaluationContext(data, player);
    return legalActions.stream()
        .filter(action -> !"end_phase".equals(action.type()))
        .map(action -> new Scored(action, score(action, context, completedActions)))
        .filter(scored -> scored.score() > 0)
        .max(Comparator.comparingInt(Scored::score).thenComparing(scored -> key(scored.action())))
        .map(Scored::action);
  }

  int score(
      final StrategicAction action,
      final GameData data,
      final GamePlayer player,
      final List<StrategicAction> completedActions) {
    return score(action, new EvaluationContext(data, player), completedActions);
  }

  private int score(
      final StrategicAction action,
      final EvaluationContext context,
      final List<StrategicAction> completedActions) {
    final String routeText = action.parameters().get("route");
    final String unitIds = action.parameters().get("unitIds");
    if (routeText == null || unitIds == null) {
      return 0;
    }

    final Territory origin;
    final Territory destination;
    final List<Unit> units;
    try {
      final var route = StrategicActionResolver.resolveRoute(context.data(), routeText);
      origin = route.getStart();
      destination = route.getEnd();
      units = StrategicActionResolver.resolveUnits(unitIds, origin);
    } catch (final RuntimeException e) {
      // A fog probe may no longer resolve against the authoritative state.
      return 0;
    }
    if (units.isEmpty()) {
      return 0;
    }

    final boolean airMove = units.stream().allMatch(Matches.unitIsAir());
    int score = 5 + 2 * units.size();
    score += objectiveProgress(origin, destination, context);
    if (!airMove) {
      score += visibleContactValue(destination, units.size(), context);
    }
    score += airControlValue(action, destination, context);
    score += supplyValue(origin, destination, units, airMove, context);
    score += reserveValue(origin, units, airMove, context);
    score -= reversalPenalty(action, completedActions);
    if (Boolean.parseBoolean(action.parameters().get("uncertain"))) {
      score -= 15;
    }
    return score;
  }

  private static int objectiveProgress(
      final Territory origin, final Territory destination, final EvaluationContext context) {
    final int progress =
        distance(context.data(), origin, context.targets())
            - distance(context.data(), destination, context.targets());
    int score = 12 * progress;
    if (context.targets().contains(destination)) {
      score += 40;
    }
    return score;
  }

  private static int visibleContactValue(
      final Territory destination, final int movingUnits, final EvaluationContext context) {
    if (!context.visible().contains(destination)
        || !isEnemy(destination.getOwner(), context.player(), context.data())) {
      return 0;
    }
    final int defenders =
        (int)
            destination.getUnitCollection().getUnits().stream()
                .filter(Matches.unitIsLand())
                .filter(unit -> isEnemy(unit.getOwner(), context.player(), context.data()))
                .count();
    if (movingUnits <= defenders) {
      return -55;
    }
    return 18 + Math.min(24, 6 * (movingUnits - defenders));
  }

  private static int airControlValue(
      final StrategicAction action,
      final Territory destination,
      final EvaluationContext context) {
    final boolean visible = context.visible().contains(destination);
    final AirControlTracker tracker = AirControlTracker.get(context.data());
    final Status status =
        visible ? tracker.getStatus(destination, context.data()) : Status.UNCONTROLLED;
    final Optional<GamePlayer> controller =
        visible ? tracker.getController(destination, context.data()) : Optional.empty();
    if ("air_assignment".equals(action.type())) {
      int score = 0;
      if (visible) {
        final long enemyGround =
            destination.getUnitCollection().getUnits().stream()
                .filter(Matches.unitIsLand())
                .filter(unit -> isEnemy(unit.getOwner(), context.player(), context.data()))
                .count();
        if (enemyGround > 0) {
          score += 35 + (int) Math.min(20, 5 * enemyGround);
        }
      }
      if (context.targets().contains(destination)) {
        score += 20;
      }
      if (status == Status.CONTESTED) {
        score += 35;
      } else if (status == Status.CONTROLLED && controller.isPresent()) {
        score +=
            isFriendly(controller.orElseThrow(), context.player(), context.data()) ? -20 : 55;
      } else {
        score += 8;
      }
      return score;
    }

    if (!visible || status == Status.UNCONTROLLED) {
      return 0;
    }
    if (status == Status.CONTESTED) {
      return -6;
    }
    return controller
        .map(owner -> isFriendly(owner, context.player(), context.data()) ? 14 : -20)
        .orElse(0);
  }

  private static int supplyValue(
      final Territory origin,
      final Territory destination,
      final List<Unit> units,
      final boolean airMove,
      final EvaluationContext context) {
    if (airMove || !SupplyNetworkResolver.isEnabled(context.data())) {
      return 0;
    }
    int score = 0;
    if (!SupplyNetworkResolver.wouldBeSupplied(destination, context.player(), context.data())) {
      score -= 55;
    }
    if (emptiesFriendlyLand(origin, units, context.player())) {
      final int disconnectedUnits = context.supplyCriticality(origin);
      score -= Math.min(72, 12 * disconnectedUnits);
      if (isSupplySource(origin)) {
        score -= 30;
      }
    }
    return score;
  }

  private static int reserveValue(
      final Territory origin,
      final List<Unit> units,
      final boolean airMove,
      final EvaluationContext context) {
    if (airMove || !emptiesFriendlyLand(origin, units, context.player())) {
      return 0;
    }
    int penalty = 0;
    if (isObjective(origin)) {
      penalty += 35;
    }
    if (context.visibleEnemyNeighbor(origin)) {
      penalty += 20;
    }
    return -penalty;
  }

  private static int reversalPenalty(
      final StrategicAction candidate, final List<StrategicAction> completedActions) {
    final String origin = candidate.parameters().get("origin");
    final String destination = candidate.parameters().get("destination");
    final String unitType = candidate.parameters().get("unitType");
    if (origin == null || destination == null) {
      return 0;
    }
    for (int i = completedActions.size() - 1; i >= 0; i--) {
      final StrategicAction previous = completedActions.get(i);
      if (origin.equals(previous.parameters().get("destination"))
          && destination.equals(previous.parameters().get("origin"))
          && unitType != null
          && unitType.equals(previous.parameters().get("unitType"))) {
        return REVERSAL_PENALTY;
      }
    }
    return 0;
  }

  private static boolean emptiesFriendlyLand(
      final Territory origin, final List<Unit> movingUnits, final GamePlayer player) {
    final Set<Unit> moving = Set.copyOf(movingUnits);
    return origin.getUnitCollection().getUnits().stream()
        .filter(Matches.unitIsLand())
        .filter(unit -> unit.isOwnedBy(player))
        .noneMatch(unit -> !moving.contains(unit));
  }

  private static boolean isObjective(final Territory territory) {
    return TerritoryAttachment.get(territory)
        .map(TerritoryAttachment::getVictoryCity)
        .orElse(0)
        > 0;
  }

  private static boolean isSupplySource(final Territory territory) {
    return SupplyTerritoryAttachment.get(territory)
        .map(SupplyTerritoryAttachment::getSupplySource)
        .orElse(false);
  }

  private static boolean isEnemy(
      final GamePlayer candidate, final GamePlayer player, final GameData data) {
    return !candidate.isNull()
        && !candidate.equals(player)
        && data.getRelationshipTracker().getRelationship(player, candidate) != null
        && data.getRelationshipTracker().isAtWar(player, candidate);
  }

  private static boolean isFriendly(
      final GamePlayer candidate, final GamePlayer player, final GameData data) {
    return !candidate.isNull()
        && (candidate.equals(player)
            || (data.getRelationshipTracker().getRelationship(player, candidate) != null
                && data.getRelationshipTracker().isAllied(player, candidate)));
  }

  private static String key(final StrategicAction action) {
    return action.type() + action.parameters();
  }

  private static int distance(
      final GameData data, final Territory from, final List<Territory> targets) {
    int best = NO_PATH;
    for (final Territory target : targets) {
      final int distance = data.getMap().getDistance(from, target, Matches.territoryIsLand());
      if (distance >= 0 && distance < best) {
        best = distance;
      }
    }
    return best;
  }

  private record Scored(StrategicAction action, int score) {}

  private static final class EvaluationContext {
    private final GameData data;
    private final GamePlayer player;
    private final Set<Territory> visible;
    private final List<Territory> targets;
    private final Map<Territory, Integer> supplyCriticality = new HashMap<>();

    private EvaluationContext(final GameData data, final GamePlayer player) {
      this.data = data;
      this.player = player;
      visible = VisibilityService.getVisibleTerritories(player, data);
      targets =
          data.getMap().getTerritories().stream()
              .filter(HybridOperationalPolicy::isObjective)
              .filter(
                  territory ->
                      !visible.contains(territory) || !player.equals(territory.getOwner()))
              .sorted(Comparator.comparing(Territory::getName))
              .toList();
    }

    private GameData data() {
      return data;
    }

    private GamePlayer player() {
      return player;
    }

    private Set<Territory> visible() {
      return visible;
    }

    private List<Territory> targets() {
      return targets;
    }

    private int supplyCriticality(final Territory territory) {
      return supplyCriticality.computeIfAbsent(territory, this::calculateSupplyCriticality);
    }

    private int calculateSupplyCriticality(final Territory blocked) {
      final Set<Territory> currentlySupplied =
          SupplyNetworkResolver.getSuppliedTerritories(player, data);
      final Set<Territory> suppliedWithout = suppliedWithout(blocked);
      return currentlySupplied.stream()
          .filter(territory -> !territory.equals(blocked))
          .filter(territory -> !suppliedWithout.contains(territory))
          .mapToInt(
              territory ->
                  (int)
                      territory.getUnitCollection().getUnits().stream()
                          .filter(Matches.unitIsLand())
                          .filter(unit -> unit.isOwnedBy(player))
                          .count())
          .sum();
    }

    private Set<Territory> suppliedWithout(final Territory blocked) {
      final Set<Territory> supplied = new LinkedHashSet<>();
      final ArrayDeque<Territory> pending = new ArrayDeque<>();
      SupplyNetworkResolver.getSupplySources(player, data).stream()
          .filter(source -> !source.equals(blocked))
          .forEach(
              source -> {
                if (supplied.add(source)) {
                  pending.addLast(source);
                }
              });
      while (!pending.isEmpty()) {
        final Territory current = pending.removeFirst();
        for (final Territory neighbor : SupplyNetworkResolver.getRoadNeighbors(current, data)) {
          if (!neighbor.equals(blocked)
              && isFriendlyLand(neighbor)
              && supplied.add(neighbor)) {
            pending.addLast(neighbor);
          }
        }
      }
      return supplied;
    }

    private boolean visibleEnemyNeighbor(final Territory territory) {
      return data.getMap().getNeighbors(territory).stream()
          .filter(visible::contains)
          .map(Territory::getOwner)
          .anyMatch(owner -> isEnemy(owner, player, data));
    }

    private boolean isFriendlyLand(final Territory territory) {
      return !territory.isWater() && isFriendly(territory.getOwner(), player, data);
    }
  }
}
