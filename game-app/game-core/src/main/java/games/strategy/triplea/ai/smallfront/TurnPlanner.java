package games.strategy.triplea.ai.smallfront;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.triplea.delegate.strategic.simulation.StrategicAction;
import games.strategy.triplea.delegate.strategic.simulation.StrategicPhase;
import java.util.List;

/** Produces one operational plan from the visible state and legal action mask. */
public interface TurnPlanner {

  TurnPlan createPlan(
      GameData data,
      GamePlayer player,
      StrategicPhase phase,
      List<StrategicAction> legalActions);

  default TurnPlan replan(
      final GameData data,
      final GamePlayer player,
      final StrategicPhase phase,
      final List<StrategicAction> legalActions,
      final PlanRuntime currentPlan) {
    return createPlan(data, player, phase, legalActions);
  }
}