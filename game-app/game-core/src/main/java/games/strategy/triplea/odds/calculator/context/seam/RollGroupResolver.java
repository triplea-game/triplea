package games.strategy.triplea.odds.calculator.context.seam;

import games.strategy.triplea.odds.calculator.context.model.BattleRound;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.RulesProfile;
import games.strategy.triplea.odds.calculator.context.model.SupportRule;
import java.util.List;

/**
 * The hoistable core: derives the per-round firing plan. Re-run each round because composition
 * changes it — eg a dead destroyer restores sub first strike.
 */
public interface RollGroupResolver {
  BattleRound plan(
      Force attackers, Force defenders, RulesProfile rules, List<SupportRule> support, int round);
}
