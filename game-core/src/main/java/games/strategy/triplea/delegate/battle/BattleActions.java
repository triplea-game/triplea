package games.strategy.triplea.delegate.battle;

import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import java.util.Collection;

/** Exposes actions that occur in a battle and affect the game state */
public interface BattleActions {

  void submergeUnits(Collection<Unit> units, boolean defender, IDelegateBridge bridge);
}
