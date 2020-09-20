package games.strategy.triplea.delegate.battle;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import java.util.Collection;
import java.util.UUID;
import lombok.Value;
import org.triplea.java.ChangeOnNextMajorRelease;

/** Exposes the battle state and allows updates to it */
public interface BattleState {

  enum Side {
    OFFENSE,
    DEFENSE;

    public Side getOpposite() {
      return this == OFFENSE ? DEFENSE : OFFENSE;
    }
  }

  @Value(staticConstructor = "of")
  class BattleRound {
    int round;
    int maxRounds;

    public boolean isLastRound() {
      return maxRounds > 0 && maxRounds <= round;
    }

    public boolean isFirstRound() {
      return round == 1;
    }
  }

  BattleRound getBattleRoundState();

  Territory getBattleSite();

  Collection<TerritoryEffect> getTerritoryEffects();

  @ChangeOnNextMajorRelease("Use a BattleId class instead of UUID")
  UUID getBattleId();

  Collection<Unit> getUnits(Side... sides);

  Collection<Unit> getWaitingToDie(Side... sides);

  void clearWaitingToDie(Side... sides);

  Collection<Unit> getAa(Side... sides);

  Collection<Unit> getBombardingUnits();

  Collection<Unit> getAmphibiousLandAttackers();

  GamePlayer getAttacker();

  GamePlayer getDefender();

  GameData getGameData();

  boolean isAmphibious();

  boolean isOver();

  boolean isHeadless();

  Collection<Territory> getAttackerRetreatTerritories();

  Collection<Unit> getDependentUnits(Collection<Unit> units);
}
