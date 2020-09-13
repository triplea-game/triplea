package games.strategy.triplea.delegate.battle;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import java.util.Collection;
import java.util.UUID;

/** Exposes the battle state and allows updates to it */
public interface BattleState {

  enum Side {
    OFFENSE,
    DEFENSE;

    public Side getOpposite() {
      return this == OFFENSE ? DEFENSE : OFFENSE;
    }
  }

  int getBattleRound();

  Territory getBattleSite();

  UUID getBattleId();

  Collection<Unit> getUnits(Side... sides);

  Collection<Unit> getWaitingToDie(Side... sides);

  void clearWaitingToDie(Side... sides);

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
