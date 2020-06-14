package games.strategy.triplea.delegate.battle;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.battle.MustFightBattle.RetreatType;
import java.util.Collection;

/** Actions that can occur in a battle that require interaction with {@link IDelegateBridge} */
public interface BattleActions {

  void fireOffensiveAaGuns();

  void fireDefensiveAaGuns();

  void submergeUnits(Collection<Unit> units, boolean defender, IDelegateBridge bridge);

  void queryRetreat(
      final boolean defender,
      final RetreatType retreatType,
      final IDelegateBridge bridge,
      final Collection<Territory> initialAvailableTerritories);
}
