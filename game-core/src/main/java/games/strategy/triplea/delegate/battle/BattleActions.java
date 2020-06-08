package games.strategy.triplea.delegate.battle;

import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import java.util.Collection;

/** Actions that can occur in a battle that require interaction with {@link IDelegateBridge} */
public interface BattleActions {

  void submergeUnits(Collection<Unit> units, boolean defender, IDelegateBridge bridge);
}
