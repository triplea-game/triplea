package games.strategy.triplea.delegate.remote;

import games.strategy.engine.data.Territory;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.delegate.IBattle;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.delegate.dataObjects.BattleListing;

public interface IBattleDelegate extends IRemote, IDelegate {
  /**
   * @return the battles currently waiting to be fought
   */
  BattleListing getBattles();

  /**
   * Fight the battle in the given country
   *
   * @param where
   *        - where to fight
   * @param bombing
   *        - fight a bombing raid
   * @return an error string if the battle could not be fought or an error occurred, null otherwise
   */
  String fightBattle(Territory where, boolean bombing, BattleType type);

  /**
   * Finish the current battle
   *
   * @return an error string if the battle could not be fought or an error occurred, null otherwise
   */
  String fightCurrentBattle();

  /**
   * @return The location of the currently being fought battle, or null if no battle is in progress.
   */
  Territory getCurrentBattleTerritory();

  /**
   * @return The current battle if there is one, or null if there is no current battle in progress.
   */
  IBattle getCurrentBattle();
}
