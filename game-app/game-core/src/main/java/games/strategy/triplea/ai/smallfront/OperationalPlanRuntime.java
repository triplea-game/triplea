package games.strategy.triplea.ai.smallfront;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.AirControlTracker;
import games.strategy.triplea.delegate.battle.AirControlTracker.Status;
import games.strategy.triplea.delegate.strategic.simulation.StrategicAction;
import games.strategy.triplea.delegate.strategic.simulation.StrategicActionResolver;
import games.strategy.triplea.delegate.supply.SupplyNetworkResolver;
import games.strategy.triplea.delegate.visibility.VisibilityService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Mutable progress state for one immutable {@link OperationalTurnPlan}. */
public final class OperationalPlanRuntime {
  private final OperationalTurnPlan plan;
  private final Set<String> completedObjectiveIds = new LinkedHashSet<>();
  private final Set<String> recordedActionKeys = new LinkedHashSet<>();
  private int completedActionCount;

  public OperationalPlanRuntime(final OperationalTurnPlan plan) {
    this.plan = plan;
  }

  public OperationalTurnPlan plan() {
    return plan;
  }

  public int completedActionCount() {
    return completedActionCount;
  }

  public Set<String> completedObjectiveIds() {
    return Set.copyOf(completedObjectiveIds);
  }

  public boolean matches(final GameData data, final GamePlayer player) {
    return plan.playerName().equals(player.getName())
        && plan.round() == data.getSequence().getRound();
  }

  public void recordCompletedAction(
      final StrategicAction action, final GameData data, final GamePlayer player) {
    if (recordedActionKeys.add(key(action))) {
      completedActionCount++;
    }
    refresh(data, player);
  }

  public void recordCompletedActions(
      final List<StrategicAction> actions, final GameData data, final GamePlayer player) {
    actions.forEach(action -> recordCompletedAction(action, data, player));
  }

  public void refresh(final GameData data, final GamePlayer player) {
    final Set<Territory> visible = VisibilityService.getVisibleTerritories(player, data);
    for (final OperationalTurnPlan.Objective objective : plan.objectives()) {
      final Territory territory = data.getMap().getTerritoryOrNull(objective.territoryName());
      if (territory != null
          && visible.contains(territory)
          && isCompleted(objective, territory, data, player)) {
        completedObjectiveIds.add(objective.objectiveId());
      }
    }
  }

  public int scoreAlignment(
      final StrategicAction action, final GameData data, final GamePlayer player) {
    if ("end_phase".equals(action.type())) {
      return 0;
    }
    final ResolvedAction resolved = resolve(action, data);
    if (resolved == null) {
      return 0;
    }

    refresh(data, player);
    int score = 0;
    boolean contributesToActiveObjective = false;
    for (final OperationalTurnPlan.Objective objective : plan.objectives()) {
      if (!isActionable(objective)
          || !objective.territoryName().equals(resolved.destination().getName())) {
        continue;
      }
      final int contribution = directContribution(action, objective);
      if (contribution > 0) {
        contributesToActiveObjective = true;
        score += contribution;
      }
    }

    final Optional<OperationalTurnPlan.Objective> primary =
        plan.objectives().stream().filter(this::isActive).findFirst();
    if (primary.isPresent() && !resolved.units().stream().allMatch(Matches.unitIsAir())) {
      final Territory primaryTerritory =
          data.getMap().getTerritoryOrNull(primary.orElseThrow().territoryName());
      if (primaryTerritory != null) {
        final int before = distance(data, resolved.origin(), primaryTerritory);
        final int after = distance(data, resolved.destination(), primaryTerritory);
        if (before >= 0 && after >= 0) {
          final int progress = before - after;
          score += 18 * progress;
          contributesToActiveObjective |= progress > 0;
        }
      }
    }

    if (plan.protectedTerritories().contains(resolved.origin().getName())
        && emptiesFriendlyLand(resolved.origin(), resolved.units(), player)) {
      score -= 120;
    }
    if (plan.protectedTerritories().contains(resolved.destination().getName())) {
      score += 12;
    }
    if (completedActionCount > 0 && !contributesToActiveObjective) {
      score -= 20;
    }
    return score;
  }

  private boolean isActive(final OperationalTurnPlan.Objective objective) {
    return !completedObjectiveIds.contains(objective.objectiveId())
        && completedObjectiveIds.containsAll(objective.prerequisiteObjectiveIds());
  }

  private boolean isActionable(final OperationalTurnPlan.Objective objective) {
    // Ground moves are declared before air assignment. A capture may therefore be staged while its
    // supporting air objective is still pending, but all other dependencies are strict.
    return !completedObjectiveIds.contains(objective.objectiveId())
        && (objective.type() == OperationalObjectiveType.CAPTURE
            || completedObjectiveIds.containsAll(objective.prerequisiteObjectiveIds()));
  }

  private boolean isCompleted(
      final OperationalTurnPlan.Objective objective,
      final Territory territory,
      final GameData data,
      final GamePlayer player) {
    return switch (objective.type()) {
      case CAPTURE, HOLD -> isFriendly(territory.getOwner(), player, data);
      case PROTECT_SUPPLY ->
          isFriendly(territory.getOwner(), player, data)
              && (!SupplyNetworkResolver.isEnabled(data)
                  || SupplyNetworkResolver.wouldBeSupplied(territory, player, data));
      case GAIN_AIR_SUPERIORITY -> friendlyAirControl(territory, data, player);
      case REDEPLOY_RESERVE, SCREEN ->
          territory.getUnitCollection().getUnits().stream()
              .filter(Matches.unitIsLand())
              .anyMatch(unit -> unit.isOwnedBy(player));
    };
  }

  private static int directContribution(
      final StrategicAction action, final OperationalTurnPlan.Objective objective) {
    return switch (objective.type()) {
      case GAIN_AIR_SUPERIORITY ->
          "air_assignment".equals(action.type()) ? 2 * objective.priority() : 0;
      case CAPTURE ->
          "air_assignment".equals(action.type()) ? objective.priority() / 2 : objective.priority();
      case HOLD, PROTECT_SUPPLY, REDEPLOY_RESERVE, SCREEN -> objective.priority();
    };
  }

  private static boolean friendlyAirControl(
      final Territory territory, final GameData data, final GamePlayer player) {
    final AirControlTracker tracker = AirControlTracker.get(data);
    if (tracker.getStatus(territory, data) != Status.CONTROLLED) {
      return false;
    }
    return tracker
        .getController(territory, data)
        .map(controller -> isFriendly(controller, player, data))
        .orElse(false);
  }

  private static ResolvedAction resolve(final StrategicAction action, final GameData data) {
    final String routeText = action.parameters().get("route");
    final String unitIds = action.parameters().get("unitIds");
    if (routeText == null || unitIds == null) {
      return null;
    }
    try {
      final var route = StrategicActionResolver.resolveRoute(data, routeText);
      return new ResolvedAction(
          route.getStart(),
          route.getEnd(),
          StrategicActionResolver.resolveUnits(unitIds, route.getStart()));
    } catch (final RuntimeException e) {
      return null;
    }
  }

  private static int distance(
      final GameData data, final Territory origin, final Territory destination) {
    return data.getMap().getDistance(origin, destination, Matches.territoryIsLand());
  }

  private static boolean emptiesFriendlyLand(
      final Territory origin, final List<Unit> movingUnits, final GamePlayer player) {
    final Set<Unit> moving = Set.copyOf(movingUnits);
    return origin.getUnitCollection().getUnits().stream()
        .filter(Matches.unitIsLand())
        .filter(unit -> unit.isOwnedBy(player))
        .noneMatch(unit -> !moving.contains(unit));
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

  private record ResolvedAction(Territory origin, Territory destination, List<Unit> units) {}
}
