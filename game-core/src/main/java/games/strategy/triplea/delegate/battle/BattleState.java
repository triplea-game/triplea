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
    OFFENSE(IBattle.WhoWon.ATTACKER),
    DEFENSE(IBattle.WhoWon.DEFENDER);

    private final IBattle.WhoWon whoWon;

    Side(final IBattle.WhoWon whoWon) {
      this.whoWon = whoWon;
    }

    public Side getOpposite() {
      return this == OFFENSE ? DEFENSE : OFFENSE;
    }

    public IBattle.WhoWon won() {
      return whoWon;
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

  void retreatUnits(Side side, Collection<Unit> units);

  Collection<Unit> getAa(Side... sides);

  Collection<Unit> getBombardingUnits();

  Collection<Unit> getAmphibiousLandAttackers();

  Collection<Unit> getKilled();

  GamePlayer getAttacker();

  GamePlayer getDefender();

  GameData getGameData();

  boolean isAmphibious();

  boolean isOver();

  boolean isHeadless();

  Collection<Territory> getAttackerRetreatTerritories();

  Collection<Unit> getDependentUnits(Collection<Unit> units);

  Collection<Unit> getTransportDependents(Collection<Unit> units);

  Collection<IBattle> getDependentBattles();
}
