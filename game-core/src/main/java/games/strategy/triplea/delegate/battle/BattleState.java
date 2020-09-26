package games.strategy.triplea.delegate.battle;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import java.util.Collection;
import java.util.UUID;
import lombok.Getter;
import lombok.Value;
import org.triplea.java.ChangeOnNextMajorRelease;

/** Exposes the battle state and allows updates to it */
public interface BattleState {

  @Getter
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
  }

  enum UnitBattleStatus {
    // units that are either undamaged or damaged
    ALIVE,
    // either ALIVE or CASUALTY
    ACTIVE,
    // units that are dead but can still act
    // (such as defending units that haven't had their turn to return fire)
    CASUALTY,
    // units that are no longer in the game
    DEAD,
  }

  @Value(staticConstructor = "of")
  class BattleStatus {
    int round;
    int maxRounds;
    boolean isOver;
    boolean isAmphibious;
    boolean isHeadless;

    public boolean isLastRound() {
      return maxRounds > 0 && maxRounds <= round;
    }

    public boolean isFirstRound() {
      return round == 1;
    }
  }

  BattleStatus getStatus();

  Territory getBattleSite();

  Collection<TerritoryEffect> getTerritoryEffects();

  @ChangeOnNextMajorRelease("Use a BattleId class instead of UUID")
  UUID getBattleId();

  Collection<Unit> getUnits(UnitBattleStatus status, Side... sides);

  void clearWaitingToDie(Side... sides);

  void retreatUnits(Side side, Collection<Unit> units);

  Collection<Unit> getAa(Side... sides);

  Collection<Unit> getBombardingUnits();

  GamePlayer getPlayer(Side side);

  GameData getGameData();

  Collection<Territory> getAttackerRetreatTerritories();

  Collection<Unit> getDependentUnits(Collection<Unit> units);

  Collection<Unit> getTransportDependents(Collection<Unit> units);

  Collection<IBattle> getDependentBattles();
}
