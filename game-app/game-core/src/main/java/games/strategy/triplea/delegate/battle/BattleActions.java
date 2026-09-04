package games.strategy.triplea.delegate.battle;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import java.util.Collection;
import java.util.Optional;

/** Actions that can occur in a battle that require interaction with {@link IDelegateBridge} */
public interface BattleActions {

  void clearWaitingToDieAndDamagedChangesInto(IDelegateBridge bridge, BattleState.Side... sides);

  void endBattle(IBattle.WhoWon whoWon, IDelegateBridge bridge);

  /**
   * Kills the unit and removes it from the battle
   *
   * @param side the side that the killedUnits are on
   */
  void removeUnits(
      Collection<Unit> killedUnits,
      IDelegateBridge bridge,
      Territory battleSite,
      BattleState.Side side);

  Optional<Territory> queryRetreatTerritory(
      BattleState battleState,
      IDelegateBridge bridge,
      GamePlayer retreatingPlayer,
      Collection<Territory> availableTerritories,
      String text);

  Optional<Territory> querySubmergeTerritory(
      BattleState battleState,
      IDelegateBridge bridge,
      GamePlayer retreatingPlayer,
      Collection<Territory> availableTerritories,
      String text);
}
