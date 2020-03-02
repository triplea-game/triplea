package games.strategy.triplea.delegate.remote;

import games.strategy.engine.data.Territory;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.delegate.battle.IBattle;
import games.strategy.triplea.delegate.battle.IBattle.BattleType;
import games.strategy.triplea.delegate.data.BattleListing;

/** Logic for querying and fighting pending battles. */
public interface IBattleDelegate extends IRemote, IDelegate {
  /** Returns the battles currently waiting to be fought. */
@RemoteActionCode(3)
  BattleListing getBattles();

  /**
   * Fight the battle in the given country.
   *
   * @param where - where to fight
   * @param bombing - fight a bombing raid
   * @return an error string if the battle could not be fought or an error occurred, null otherwise
   */
  String fightBattle(Territory where, boolean bombing, BattleType type);

  /**
   * Returns the current battle if there is one, or null if there is no current battle in progress.
   */
@RemoteActionCode(5)
  IBattle getCurrentBattle();
}
