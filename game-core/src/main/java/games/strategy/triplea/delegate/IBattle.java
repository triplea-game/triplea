package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.data.BattleRecord.BattleResultDescription;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Represents a battle. */
public interface IBattle extends Serializable {
  /** Identifies the winner of a battle. */
  enum WhoWon {
    NOT_FINISHED,
    DRAW,
    ATTACKER,
    DEFENDER
  }

  /** The type of a battle. */
  enum BattleType {
    NORMAL("Battle"),
    AIR_BATTLE("Air Battle"),
    AIR_RAID("Air Raid"),
    BOMBING_RAID("Bombing Raid");

    private final String type;

    BattleType(final String type) {
      this.type = type;
    }

    @Override
    public String toString() {
      return type;
    }

    // if it has the word "Raid" in it, then it is a bombing battle
    public boolean isBombingRun() {
      return type.contains("Raid");
    }

    // if it has the word "Air" in it, then it is an air battle
    public boolean isAirPreBattleOrPreRaid() {
      return type.contains("Air");
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

  /**
   * There are two distinct super-types of battles: Bombing battles, and Fighting battles. There may
   * be sub-types of each of these.
   *
   * @return whether this battle is a bombing run
   */
  boolean isBombingRun();

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

  /**
   * Add a bombardment unit.
   *
   * @param u - unit to add
   */
  void addBombardingUnit(Unit u);

  /** Indicates whether battle is amphibious. */
  boolean isAmphibious();

  /**
   * This occurs when a move has been undone.
   *
   * @param route - attacking route
   * @param units - attacking units
   */
  void removeAttack(Route route, Collection<Unit> units);

  /** If we need to cancel the battle, we may need to perform some cleanup. */
  void cancelBattle(IDelegateBridge bridge);

  /**
   * Test-method after an attack has been removed.
   *
   * @return whether there are still units left to fight
   */
  boolean isEmpty();

  /** @return Unmodifiable collection of units that are dependent on the given units. */
  Collection<Unit> getDependentUnits(Collection<Unit> units);

  /** @return Unmodifiable collection of units that are assaulting amphibiously. */
  Collection<Unit> getAmphibiousLandAttackers();

  /** @return Unmodifiable collection of units that are bombarding. */
  Collection<Unit> getBombardingUnits();

  /** @return What round this battle is in. Read-only. */
  int getBattleRound();

  /** @return Unmodifiable collection of the attacking units. */
  Collection<Unit> getAttackingUnits();

  /** @return Unmodifiable collection of the defending units. */
  Collection<Unit> getDefendingUnits();

  /** @return Unmodifiable collection of the remaining attacking units. */
  Collection<Unit> getRemainingAttackingUnits();

  /** @return Unmodifiable collection of the remaining defending units. */
  Collection<Unit> getRemainingDefendingUnits();

  WhoWon getWhoWon();

  BattleResultDescription getBattleResultDescription();

  PlayerId getAttacker();

  PlayerId getDefender();

  UUID getBattleId();
}
