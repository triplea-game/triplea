package games.strategy.triplea.odds.calculator.context.seam;

import games.strategy.triplea.odds.calculator.context.model.BattleRound;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.RulesProfile;

/**
 * The hoistable core: derives the per-round firing plan. Re-run each round because composition
 * changes it — eg a dead destroyer restores sub first strike.
 */
public interface RollGroupResolver {
  BattleRound plan(Force attackers, Force defenders, RulesProfile rules, int round);
}
