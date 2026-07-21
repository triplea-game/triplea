package games.strategy.triplea.ai.smallfront;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.triplea.delegate.strategic.simulation.StrategicAction;
import java.util.List;
import java.util.Optional;

/**
 * Chooses one action from the legal mask for a Small Front turn.
 *
 * <p>The same mask the reinforcement-learning environment offers, so a trained policy can replace a
 * hand-written one without touching the AI that drives it.
 */
@FunctionalInterface
public interface SmallFrontPolicy {

  /**
   * Returns the chosen action, or empty to stop acting in this phase. Implementations may assume
   * every action came from {@link
   * games.strategy.triplea.delegate.strategic.simulation.StrategicMoveCandidateGenerator}.
   */
  Optional<StrategicAction> choose(
      List<StrategicAction> legalActions, GameData data, GamePlayer player);

  /**
   * Chooses with the actions already completed in the current phase.
   *
   * <p>Stateless policies may ignore the history. Operational policies can use it to preserve a
   * plan and avoid immediately reversing previous moves.
   */
  default Optional<StrategicAction> choose(
      final List<StrategicAction> legalActions,
      final GameData data,
      final GamePlayer player,
      final List<StrategicAction> completedActions) {
    return choose(legalActions, data, player);
  }

  /** Called after the delegate accepts an action so stateful policies can advance their plan. */
  default void onActionCompleted(
      final StrategicAction action, final GameData data, final GamePlayer player) {}
}
