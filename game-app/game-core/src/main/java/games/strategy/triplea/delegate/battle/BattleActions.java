package games.strategy.triplea.delegate.battle;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.player.Player;
import games.strategy.triplea.ai.weak.WeakAi;
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

  default Player getRemotePlayer(final GamePlayer player, final IDelegateBridge bridge) {
    // if its the null player, return a do nothing proxy
    if (player.isNull()) {
      return new WeakAi(player.getName());
    }
    return bridge.getRemotePlayer(player);
  }
}
