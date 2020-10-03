package games.strategy.triplea.delegate.battle.steps.fire;

import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;
import java.util.function.Function;

/** Converts the BattleState into a list of FiringGroups */
public interface FiringGroupFilter extends Function<BattleState, List<FiringGroup>> {}
