package games.strategy.triplea.delegate;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.net.GUID;
import games.strategy.triplea.delegate.dataObjects.BattleRecord.BattleResultDescription;

/**
 * Represents a battle.
 */
public interface IBattle extends java.io.Serializable {
  enum WhoWon {
    NOTFINISHED, DRAW, ATTACKER, DEFENDER
  }

  enum BattleType {
    NORMAL("Battle"), AIR_BATTLE("Air Battle"), AIR_RAID("Air Raid"), BOMBING_RAID("Bombing Raid");
    private final String m_type;

    BattleType(final String type) {
      m_type = type;
    }

    @Override
    public String toString() {
      return m_type;
    }

    // if it has the word "Raid" in it, then it is a bombing battle
    public boolean isBombingRun() {
      return m_type.contains("Raid");
    }

    // if it has the word "Air" in it, then it is an air battle
    public boolean isAirPreBattleOrPreRaid() {
      return m_type.contains("Air");
    }
  }

  /**
   * Add a bunch of attacking units to the battle.
   *
   * @param route
   *        - attack route
   * @param units
   *        - attacking units
   * @param targets
   *        - Can be NULL if this does not apply. A list of defending units with the collection of attacking units
   *        targetting them mapped to
   *        each defending unit.
   * @return attack change object
   */
  Change addAttackChange(Route route, Collection<Unit> units, HashMap<Unit, HashSet<Unit>> targets);

  /**
   * There are two distinct super-types of battles: Bombing battles, and Fighting battles.
   * There may be sub-types of each of these.
   *
   * @return whether this battle is a bombing run
   */
  boolean isBombingRun();

  /**
   * The type of battle occurring, example: MustFightBattle, StrategicBombingRaidBattle, etc.
   */
  BattleType getBattleType();

  /**
   * @return territory this battle is occurring in.
   */
  Territory getTerritory();

  /**
   * Fight this battle.
   *
   * @param bridge
   *        - IDelegateBridge
   */
  void fight(IDelegateBridge bridge);

  /**
   * @return whether this battle is over or not.
   */
  boolean isOver();

  /**
   * Call this method when units are lost in another battle.
   * This is needed to remove dependent units who have been
   * lost in another battle.
   *
   * @param battle
   *        - other battle
   * @param units
   *        - referring units
   * @param bridge
   *        - IDelegateBridge
   */
  void unitsLostInPrecedingBattle(IBattle battle, Collection<Unit> units, IDelegateBridge bridge,
      boolean withdrawn);

  /**
   * Add a bombardment unit.
   *
   * @param u
   *        - unit to add
   */
  void addBombardingUnit(Unit u);

  /**
   * @return whether battle is amphibious.
   */
  boolean isAmphibious();

  /**
   * This occurs when a move has been undone.
   *
   * @param route
   *        - attacking route
   * @param units
   *        - attacking units
   */
  void removeAttack(Route route, Collection<Unit> units);

  /**
   * If we need to cancel the battle, we may need to perform some cleanup.
   */
  void cancelBattle(IDelegateBridge bridge);

  /**
   * Test-method after an attack has been removed.
   *
   * @return whether there are still units left to fight
   */
  boolean isEmpty();

  /**
   * @return units which are dependent on the given units.
   */
  Collection<Unit> getDependentUnits(Collection<Unit> units);

  /**
   * @return units which are actually assaulting amphibiously.
   */
  Collection<Unit> getAmphibiousLandAttackers();

  /**
   * @return units which are actually bombarding.
   */
  Collection<Unit> getBombardingUnits();

  /**
   * @return what round this battle is in.
   */
  int getBattleRound();

  /**
   * @return units which are attacking.
   */
  Collection<Unit> getAttackingUnits();

  /**
   * @return units which are defending.
   */
  Collection<Unit> getDefendingUnits();

  List<Unit> getRemainingAttackingUnits();

  List<Unit> getRemainingDefendingUnits();

  WhoWon getWhoWon();

  BattleResultDescription getBattleResultDescription();

  PlayerID getAttacker();

  PlayerID getDefender();

  GUID getBattleID();
}
