package games.strategy.triplea.ai.smallfront;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.triplea.delegate.strategic.simulation.StrategicAction;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Combines the existing operational evaluator with one stable turn-level plan. */
public final class PlanAwareOperationalPolicy implements SmallFrontPolicy {
  private final HybridOperationalPolicy evaluator;

  public PlanAwareOperationalPolicy() {
    this(new HybridOperationalPolicy());
  }

  PlanAwareOperationalPolicy(final HybridOperationalPolicy evaluator) {
    this.evaluator = evaluator;
  }

  @Override
  public Optional<StrategicAction> choose(
      final List<StrategicAction> legalActions,
      final GameData data,
      final GamePlayer player) {
    return evaluator.choose(legalActions, data, player);
  }

  @Override
  public Optional<StrategicAction> choose(
      final List<StrategicAction> legalActions,
      final GameData data,
      final GamePlayer player,
      final List<StrategicAction> completedActions) {
    return evaluator.choose(legalActions, data, player, completedActions);
  }

  @Override
  public Optional<StrategicAction> choose(
      final List<StrategicAction> legalActions,
      final GameData data,
      final GamePlayer player,
      final List<StrategicAction> completedActions,
      final PlanRuntime planRuntime) {
    if (planRuntime == null || planRuntime.plan().objectives().isEmpty()) {
      return evaluator.choose(legalActions, data, player, completedActions);
    }
    return legalActions.stream()
        .filter(action -> !"end_phase".equals(action.type()))
        .map(
            action ->
                new ScoredAction(
                    action,
                    evaluator.score(action, data, player, completedActions)
                        + planRuntime.scoreAdjustment(action, data)))
        .filter(scored -> scored.score() > 0)
        .max(
            Comparator.comparingInt(ScoredAction::score)
                .thenComparing(scored -> key(scored.action())))
        .map(ScoredAction::action);
  }

  private static String key(final StrategicAction action) {
    return action.type() + action.parameters();
  }

  private record ScoredAction(StrategicAction action, int score) {}
}