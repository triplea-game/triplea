package games.strategy.triplea.ai.smallfront;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.AirControlTracker;
import games.strategy.triplea.delegate.battle.AirControlTracker.Status;
import games.strategy.triplea.delegate.strategic.simulation.StrategicAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Mutable progress state for a {@link TurnPlan} during one player turn. */
public final class PlanRuntime {
  private TurnPlan plan;
  private final Map<String, ObjectiveState> objectiveStates = new LinkedHashMap<>();
  private final List<StrategicAction> completedActions = new ArrayList<>();
  private int replansThisTurn;

  public PlanRuntime(final TurnPlan plan) {
    setPlan(plan);
  }

  public TurnPlan plan() {
    return plan;
  }

  public List<StrategicAction> completedActions() {
    return List.copyOf(completedActions);
  }

  public ObjectiveState state(final String objectiveId) {
    return objectiveStates.getOrDefault(objectiveId, ObjectiveState.ABANDONED);
  }

  public int replansThisTurn() {
    return replansThisTurn;
  }

  int scoreAdjustment(final StrategicAction action, final GameData data) {
    if ("end_phase".equals(action.type()) || plan.objectives().isEmpty()) {
      return 0;
    }

    int score = formationAlignment(action);
    for (final TurnPlan.Objective objective : plan.objectives()) {
      final ObjectiveState state = objectiveStates.get(objective.id());
      if (state == ObjectiveState.COMPLETED || state == ObjectiveState.ABANDONED) {
        continue;
      }
      score += objectiveAlignment(action, objective, data);
    }
    return score;
  }

  void recordAction(
      final StrategicAction action, final GameData data, final GamePlayer player) {
    completedActions.add(action);
    for (final TurnPlan.Objective objective : plan.objectives()) {
      if (objectiveStates.get(objective.id()) == ObjectiveState.PENDING
          && objectiveAlignment(action, objective, data) > 0) {
        objectiveStates.put(objective.id(), ObjectiveState.ACTIVE);
      }
      if (objective.type() == TurnPlan.ObjectiveType.REDEPLOY
          && objective.targetTerritories().contains(action.parameters().get("destination"))) {
        objectiveStates.put(objective.id(), ObjectiveState.COMPLETED);
      }
    }
    refresh(data, player);
  }

  void refresh(final GameData data, final GamePlayer player) {
    for (final TurnPlan.Objective objective : plan.objectives()) {
      if (objectiveStates.get(objective.id()) == ObjectiveState.ABANDONED) {
        continue;
      }
      final boolean completed =
          switch (objective.type()) {
            case CAPTURE, HOLD ->
                objective.targetTerritories().stream()
                    .map(name -> territory(data, name))
                    .flatMap(Optional::stream)
                    .allMatch(target -> isFriendly(target.getOwner(), player, data));
            case GAIN_AIR_SUPERIORITY ->
                objective.targetTerritories().stream()
                    .map(name -> territory(data, name))
                    .flatMap(Optional::stream)
                    .allMatch(target -> hasFriendlyAirControl(target, player, data));
            case REDEPLOY -> objectiveStates.get(objective.id()) == ObjectiveState.COMPLETED;
          };
      if (completed) {
        objectiveStates.put(objective.id(), ObjectiveState.COMPLETED);
      }
    }
  }

  boolean shouldReplan(final List<StrategicAction> legalActions, final GameData data) {
    if (replansThisTurn > 0 || completedActions.size() < 2) {
      return false;
    }
    final Optional<TurnPlan.Objective> primary = plan.primaryObjective();
    if (primary.isEmpty()
        || objectiveStates.get(primary.orElseThrow().id()) == ObjectiveState.COMPLETED) {
      return false;
    }
    final boolean hasAlignedAction =
        legalActions.stream()
            .filter(action -> !"end_phase".equals(action.type()))
            .anyMatch(action -> objectiveAlignment(action, primary.orElseThrow(), data) > 0);
    if (!hasAlignedAction) {
      objectiveStates.put(primary.orElseThrow().id(), ObjectiveState.BLOCKED);
    }
    return !hasAlignedAction;
  }

  void replacePlan(final TurnPlan replacement) {
    for (final Map.Entry<String, ObjectiveState> entry : objectiveStates.entrySet()) {
      if (entry.getValue() != ObjectiveState.COMPLETED) {
        entry.setValue(ObjectiveState.ABANDONED);
      }
    }
    setPlan(replacement);
    replansThisTurn++;
  }

  private void setPlan(final TurnPlan replacement) {
    plan = replacement;
    objectiveStates.clear();
    replacement.objectives().forEach(objective -> objectiveStates.put(objective.id(), ObjectiveState.PENDING));
  }

  private int formationAlignment(final StrategicAction action) {
    final Set<String> candidateUnitIds = unitIds(action.parameters().get("unitIds"));
    if (candidateUnitIds.isEmpty()) {
      return 0;
    }
    int overlap = 0;
    for (final TurnPlan.FormationAssignment assignment : plan.assignments()) {
      for (final String unitId : assignment.unitIds()) {
        if (candidateUnitIds.contains(unitId)) {
          overlap++;
        }
      }
    }
    return Math.min(30, overlap * 10);
  }

  private static int objectiveAlignment(
      final StrategicAction action, final TurnPlan.Objective objective, final GameData data) {
    final String destination = action.parameters().get("destination");
    if (destination == null) {
      return 0;
    }

    int score = 0;
    if (objective.targetTerritories().contains(destination)) {
      score +=
          switch (objective.type()) {
            case GAIN_AIR_SUPERIORITY -> "air_assignment".equals(action.type()) ? 100 : 0;
            case CAPTURE -> "air_assignment".equals(action.type()) ? 45 : 65;
            case HOLD -> 25;
            case REDEPLOY -> 55;
          };
      score += objective.priority() / 5;
    }

    if (!"air_assignment".equals(action.type())
        && (objective.type() == TurnPlan.ObjectiveType.CAPTURE
            || objective.type() == TurnPlan.ObjectiveType.REDEPLOY)) {
      score += progressTowardObjective(action, objective, data);
    }
    return score;
  }

  private static int progressTowardObjective(
      final StrategicAction action, final TurnPlan.Objective objective, final GameData data) {
    final Optional<Territory> origin = territory(data, action.parameters().get("origin"));
    final Optional<Territory> destination = territory(data, action.parameters().get("destination"));
    if (origin.isEmpty() || destination.isEmpty()) {
      return 0;
    }

    int bestProgress = Integer.MIN_VALUE;
    for (final String targetName : objective.targetTerritories()) {
      final Optional<Territory> target = territory(data, targetName);
      if (target.isEmpty()) {
        continue;
      }
      final int originDistance =
          data.getMap().getDistance(origin.orElseThrow(), target.orElseThrow(), Matches.territoryIsLand());
      final int destinationDistance =
          data
              .getMap()
              .getDistance(destination.orElseThrow(), target.orElseThrow(), Matches.territoryIsLand());
      if (originDistance >= 0 && destinationDistance >= 0) {
        bestProgress = Math.max(bestProgress, originDistance - destinationDistance);
      }
    }
    if (bestProgress == Integer.MIN_VALUE) {
      return 0;
    }
    return Math.max(-30, Math.min(30, bestProgress * 12));
  }

  private static Optional<Territory> territory(final GameData data, final String name) {
    if (name == null) {
      return Optional.empty();
    }
    return data.getMap().getTerritories().stream().filter(value -> value.getName().equals(name)).findFirst();
  }

  private static Set<String> unitIds(final String encoded) {
    if (encoded == null || encoded.isBlank()) {
      return Set.of();
    }
    return new LinkedHashSet<>(Arrays.asList(encoded.split(",")));
  }

  private static boolean hasFriendlyAirControl(
      final Territory territory, final GamePlayer player, final GameData data) {
    final AirControlTracker tracker = AirControlTracker.get(data);
    if (tracker.getStatus(territory, data) != Status.CONTROLLED) {
      return false;
    }
    return tracker
        .getController(territory, data)
        .map(controller -> isFriendly(controller, player, data))
        .orElse(false);
  }

  private static boolean isFriendly(
      final GamePlayer candidate, final GamePlayer player, final GameData data) {
    return !candidate.isNull()
        && (candidate.equals(player)
            || (data.getRelationshipTracker().getRelationship(player, candidate) != null
                && data.getRelationshipTracker().isAllied(player, candidate)));
  }

  public enum ObjectiveState {
    PENDING,
    ACTIVE,
    BLOCKED,
    COMPLETED,
    ABANDONED
  }
}