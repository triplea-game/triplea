package games.strategy.triplea.delegate.battle;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import java.util.Collection;

/** Exposes the battle state and allows updates to it */
public interface BattleState {

  int getBattleRound();

  Territory getBattleSite();

  Collection<Unit> getAttackingUnits();

  Collection<Unit> getAttackingWaitingToDie();

  void clearAttackingWaitingToDie();

  Collection<Unit> getDefendingUnits();

  Collection<Unit> getDefendingWaitingToDie();

  void clearDefendingWaitingToDie();

  Collection<Unit> getOffensiveAa();

  Collection<Unit> getDefendingAa();

  Collection<Unit> getBombardingUnits();

  GamePlayer getAttacker();

  GamePlayer getDefender();

  GameData getGameData();

  boolean isAmphibious();

  boolean isOver();

  Collection<Territory> getAttackerRetreatTerritories();

  Collection<Territory> getEmptyOrFriendlySeaNeighbors(Collection<Unit> units);

  Collection<Unit> getDependentUnits(Collection<Unit> units);
}
