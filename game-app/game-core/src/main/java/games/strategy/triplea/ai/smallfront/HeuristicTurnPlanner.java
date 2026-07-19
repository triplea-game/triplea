package games.strategy.triplea.ai.smallfront;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.triplea.delegate.strategic.simulation.StrategicAction;
import games.strategy.triplea.delegate.strategic.simulation.StrategicPhase;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Builds a compact turn plan from the strongest currently legal operational action. */
public final class HeuristicTurnPlanner implements TurnPlanner {
  private final HybridOperationalPolicy evaluator;

  public HeuristicTurnPlanner() {
    this(new HybridOperationalPolicy());
  }

  HeuristicTurnPlanner(final HybridOperationalPolicy evaluator) {
    this.evaluator = evaluator;
  }

  @Override
  public TurnPlan createPlan(
      final GameData data,
      final GamePlayer player,
      final StrategicPhase phase,
      final List<StrategicAction> legalActions) {
    final Optional<StrategicAction> leadAction =
        legalActions.stream()
            .filter(action -> !"end_phase".equals(action.type()))
            .filter(action -> action.parameters().get("destination") != null)
            .max(
                Comparator.comparingInt(
                        (StrategicAction action) -> evaluator.score(action, data, player, List.of()))
                    .thenComparing(HeuristicTurnPlanner::key));
    if (leadAction.isEmpty()) {
      return TurnPlan.standPat(player.getName());
    }

    final StrategicAction lead = leadAction.orElseThrow();
    final String target = lead.parameters().get("destination");
    final String planId =
        player.getName() + "-" + phase.name().toLowerCase() + "-" + Integer.toHexString(key(lead).hashCode());

    final List<TurnPlan.Objective> objectives;
    final String primaryObjectiveId;
    final String commanderIntent;
    if (phase == StrategicPhase.AIR_ASSIGNMENT) {
      primaryObjectiveId = "gain-air-superiority";
      objectives =
          List.of(
              new TurnPlan.Objective(
                  primaryObjectiveId,
                  TurnPlan.ObjectiveType.GAIN_AIR_SUPERIORITY,
                  Set.of(target),
                  100));
      commanderIntent = "Establish air superiority over " + target + ".";
    } else if (phase == StrategicPhase.REDEPLOYMENT) {
      primaryObjectiveId = "redeploy-to-" + target;
      objectives =
          List.of(
              new TurnPlan.Objective(
                  primaryObjectiveId, TurnPlan.ObjectiveType.REDEPLOY, Set.of(target), 100));
      commanderIntent = "Redeploy the selected formation toward " + target + ".";
    } else {
      primaryObjectiveId = "capture-" + target;
      objectives =
          List.of(
              new TurnPlan.Objective(
                  primaryObjectiveId, TurnPlan.ObjectiveType.CAPTURE, Set.of(target), 100),
              new TurnPlan.Objective(
                  "air-over-" + target,
                  TurnPlan.ObjectiveType.GAIN_AIR_SUPERIORITY,
                  Set.of(target),
                  90));
      commanderIntent =
          "Concentrate the main effort on "
              + target
              + " while preserving supply and establishing supporting air control.";
    }

    final Set<String> unitIds = unitIds(lead.parameters().get("unitIds"));
    final List<TurnPlan.FormationAssignment> assignments =
        unitIds.isEmpty()
            ? List.of()
            : List.of(
                new TurnPlan.FormationAssignment(
                    "lead-formation", unitIds, primaryObjectiveId));
    return new TurnPlan(
        planId, player.getName(), commanderIntent, objectives, assignments);
  }

  private static Set<String> unitIds(final String encoded) {
    if (encoded == null || encoded.isBlank()) {
      return Set.of();
    }
    return new LinkedHashSet<>(Arrays.asList(encoded.split(",")));
  }

  private static String key(final StrategicAction action) {
    return action.type() + action.parameters();
  }
}