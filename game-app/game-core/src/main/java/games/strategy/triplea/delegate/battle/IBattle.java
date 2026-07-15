package games.strategy.triplea.delegate.battle;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.triplea.java.RemoveOnNextMajorRelease;

/** Represents a battle. */
public interface IBattle extends Serializable {
  /** Identifies the winner of a battle. */
  enum WhoWon {
    NOT_FINISHED,
    DRAW,
    ATTACKER,
    DEFENDER
  }

  /** The combat domain resolved by a battle. */
  enum BattleDomain {
    GROUND,
    AIR,
    RAID
  }

  /** The type of battle. */
  @AllArgsConstructor
  @ToString(of = "type")
  enum BattleType {
    NORMAL("Battle", false, false, BattleDomain.GROUND),
    AIR_BATTLE("Air Battle", false, true, BattleDomain.AIR),
    AIR_RAID("Air Raid", true, true, BattleDomain.RAID),
    BOMBING_RAID("Bombing Raid", true, false, BattleDomain.RAID);

    private final String type;
    @Getter private final boolean bombingRun;
    @Getter private final boolean airBattle;
    @Getter private final BattleDomain domain;

    public static Collection<BattleType> nonBombingBattleTypes() {
      return Arrays.stream(values())
          .filter(Predicate.not(BattleType::isBombingRun))
          .collect(Collectors.toSet());
    }

    public static Collection<BattleType> bombingBattleTypes() {
      return Arrays.stream(values()).filter(BattleType::isBombingRun).collect(Collectors.toSet());
    }

    public boolean isGroundBattle() {
      return domain == BattleDomain.GROUND;
    }

    public boolean isAirDomainBattle() {
      return domain == BattleDomain.AIR;
    }

    public String toDisplayText() {
      return type;
    }
  }

  /**
   * Add a bunch of attacking units to the battle.
   *
   * @param route - attack route
   * @param units - attacking units
   * @param targets Can be NULL if this does not apply. A list of defending units with the
   *     collection of attacking units targeting them mapped to each defending unit.
   * @return attack change object
   */
  Change addAttackChange(Route route, Collection<Unit> units, Map<Unit, Set<Unit>> targets);

  /** The type of battle occurring, example: MustFightBattle, StrategicBombingRaidBattle, etc. */
  BattleType getBattleType();

  /** Returns the territory this battle is occurring in. */
  Territory getTerritory();

  /** Fight this battle. */
  void fight(IDelegateBridge bridge);

  /** Indicates whether this battle is over or not. */
  boolean isOver();

  /**
   * Call this method when units are lost in another battle. This is needed to remove dependent
   * units who have been lost in another battle.
   *
   * @param units - referring units
   */
  void unitsLostInPrecedingBattle(
      Collection<Unit> units, IDelegateBridge bridge, boolean withdrawn);

  /** Add a bombardment unit. */
  void addBombardingUnit(Unit u);

  /** Indicates whether battle is amphibious. */
  boolean isAmphibious();

  /**
   * This occurs when a move has been undone.
   *
   * @param route - attacking route
   * @param units - attacking units
   * @return any changes to be performed as a result.
   */
  Change removeAttack(Route route, Collection<Unit> units);

  /** If we need to cancel the battle, we may need to perform some cleanup. */
  void cancelBattle(IDelegateBridge bridge);

  /** Test-method after an attack has been removed. */
  boolean isEmpty();

  /** Returns an unmodifiable collection of units that are dependent on the given units. */
  Collection<Unit> getDependentUnits(Collection<Unit> units);

  /** Returns an unmodifiable collection of units that are bombarding. */
  Collection<Unit> getBombardingUnits();

  /** Returns what round this battle is in. */
  int getBattleRound();

  /** Returns an unmodifiable collection of the attacking units. */
  Collection<Unit> getAttackingUnits();

  /** Returns an unmodifiable collection of the defending units. */
  Collection<Unit> getDefendingUnits();

  /** Returns an unmodifiable collection of the remaining attacking units. */
  Collection<Unit> getRemainingAttackingUnits();

  /** Returns an unmodifiable collection of the remaining defending units. */
  Collection<Unit> getRemainingDefendingUnits();

  WhoWon getWhoWon();

  GamePlayer getAttacker();

  GamePlayer getDefender();

  @RemoveOnNextMajorRelease
  void fixUpNullPlayer(GamePlayer nullPlayer);

  UUID getBattleId();
}
