package games.strategy.triplea.ai.smallfront;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;

/** Produces one machine-readable operational plan at the start of a player's turn. */
@FunctionalInterface
public interface OperationalTurnPlanner {
  OperationalTurnPlan plan(GameData data, GamePlayer player);
}
