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
      boolean defender,
      RetreatType retreatType,
      IDelegateBridge bridge,
      Collection<Territory> initialAvailableTerritories);

  void fireNavalBombardment(IDelegateBridge bridge);

  void endBattle(IDelegateBridge bridge);

  void attackerWins(IDelegateBridge bridge);
}
