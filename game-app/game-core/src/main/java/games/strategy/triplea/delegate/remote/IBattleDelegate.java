package games.strategy.triplea.delegate.remote;

import games.strategy.engine.data.Territory;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.triplea.delegate.battle.IBattle;
import games.strategy.triplea.delegate.battle.IBattle.BattleType;
import games.strategy.triplea.delegate.data.BattleListing;
import java.io.Serializable;
import org.triplea.java.RemoveOnNextMajorRelease;

/** Logic for querying and fighting pending battles. */
public interface IBattleDelegate extends IRemote, IDelegate {
  /** Returns the battles currently waiting to be fought. */
  @RemoteActionCode(3)
  BattleListing getBattleListing();

  /**
   * Fight the battle in the given country.
   *
   * @param where - where to fight
   * @param bombing - fight a bombing raid
   * @return an error string if the battle could not be fought or an error occurred, null otherwise
   */
  @RemoveOnNextMajorRelease("Remove 'boolean bombing' parameter")
  @RemoteActionCode(2)
  String fightBattle(Territory where, boolean bombing, BattleType type);

  /**
   * Returns the current battle if there is one, or null if there is no current battle in progress.
   */
  @RemoteActionCode(5)
  IBattle getCurrentBattle();

  @RemoteActionCode(9)
  @Override
  void initialize(String name, String displayName);

  @RemoteActionCode(12)
  @Override
  void setDelegateBridgeAndPlayer(IDelegateBridge delegateBridge);

  @RemoteActionCode(13)
  @Override
  void start();

  @RemoteActionCode(1)
  @Override
  void end();

  @RemoteActionCode(7)
  @Override
  String getName();

  @RemoteActionCode(6)
  @Override
  String getDisplayName();

  @RemoteActionCode(4)
  @Override
  IDelegateBridge getBridge();

  @RemoteActionCode(11)
  @Override
  Serializable saveState();

  @RemoteActionCode(10)
  @Override
  void loadState(Serializable state);

  @RemoteActionCode(8)
  @Override
  Class<? extends IRemote> getRemoteType();

  @RemoteActionCode(0)
  @Override
  boolean delegateCurrentlyRequiresUserInput();
}
