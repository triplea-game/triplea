package games.strategy.triplea.ai.smallfront;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.triplea.delegate.strategic.simulation.StrategicAction;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Executes a stable turn plan with the existing deterministic operational action evaluator. */
public final class PlanAwareOperationalPolicy implements SmallFrontPolicy {
  private final HybridOperationalPolicy basePolicy;
  private final OperationalTurnPlanner planner;
  private OperationalPlanRuntime runtime;
  private int replansThisTurn;

  public PlanAwareOperationalPolicy() {
    this(new HeuristicOperationalPlanner(), new HybridOperationalPolicy());
  }

  PlanAwareOperationalPolicy(
      final OperationalTurnPlanner planner, final HybridOperationalPolicy basePolicy) {
    this.planner = Objects.requireNonNull(planner);
    this.basePolicy = Objects.requireNonNull(basePolicy);
  }

  @Override
  public Optional<StrategicAction> choose(
      final List<StrategicAction> legalActions, final GameData data, final GamePlayer player) {
    return choose(legalActions, data, player, List.of());
  }

  @Override
  public synchronized Optional<StrategicAction> choose(
      final List<StrategicAction> legalActions,
      final GameData data,
      final GamePlayer player,
      final List<StrategicAction> completedActions) {
    ensureRuntime(data, player);
    runtime.recordCompletedActions(completedActions, data, player);
    Optional<StrategicAction> choice =
        chooseWithCurrentPlan(legalActions, data, player, completedActions);
    if (choice.isEmpty()
        && replansThisTurn < runtime.plan().maximumReplans()
        && runtime.completedActionCount() >= 2) {
      replansThisTurn++;
      runtime = new OperationalPlanRuntime(planner.plan(data, player));
      choice = chooseWithCurrentPlan(legalActions, data, player, completedActions);
    }
    return choice;
  }

  @Override
  public synchronized void onActionCompleted(
      final StrategicAction action, final GameData data, final GamePlayer player) {
    ensureRuntime(data, player);
    runtime.recordCompletedAction(action, data, player);
  }

  synchronized OperationalTurnPlan currentPlan(final GameData data, final GamePlayer player) {
    ensureRuntime(data, player);
    return runtime.plan();
  }

  synchronized int score(
      final StrategicAction action,
      final GameData data,
      final GamePlayer player,
      final List<StrategicAction> completedActions) {
    ensureRuntime(data, player);
    return basePolicy.score(action, data, player, completedActions)
        + runtime.scoreAlignment(action, data, player);
  }

  private Optional<StrategicAction> chooseWithCurrentPlan(
      final List<StrategicAction> legalActions,
      final GameData data,
      final GamePlayer player,
      final List<StrategicAction> completedActions) {
    return legalActions.stream()
        .filter(action -> !"end_phase".equals(action.type()))
        .map(action -> new Scored(action, score(action, data, player, completedActions)))
        .filter(scored -> scored.score() > 0)
        .max(Comparator.comparingInt(Scored::score).thenComparing(scored -> key(scored.action())))
        .map(Scored::action);
  }

  private void ensureRuntime(final GameData data, final GamePlayer player) {
    if (runtime == null || !runtime.matches(data, player)) {
      runtime = new OperationalPlanRuntime(planner.plan(data, player));
      replansThisTurn = 0;
    }
  }

  private static String key(final StrategicAction action) {
    return action.type() + action.parameters();
  }

  private record Scored(StrategicAction action, int score) {}
}
