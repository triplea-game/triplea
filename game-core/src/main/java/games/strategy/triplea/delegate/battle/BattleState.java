package games.strategy.triplea.delegate.battle;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import java.util.Collection;
import java.util.EnumSet;

/** Exposes the battle state and allows updates to it */
public interface BattleState {

  enum Side {
    OFFENSE,
    DEFENSE;

    public Side getOpposite() {
      if (this == OFFENSE) {
        return DEFENSE;
      } else {
        return OFFENSE;
      }
    }
  }

  int getBattleRound();

  Territory getBattleSite();

  Collection<Unit> getAttackingUnits();

  Collection<Unit> getDefendingUnits();

  Collection<Unit> getUnits(EnumSet<Side> sides);

  Collection<Unit> getAttackingWaitingToDie();

  Collection<Unit> getDefendingWaitingToDie();

  Collection<Unit> getWaitingToDie(EnumSet<Side> sides);

  void clearWaitingToDie(EnumSet<Side> sides);

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
