package games.strategy.triplea.delegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.net.GUID;
import games.strategy.triplea.TripleA;
import games.strategy.triplea.ai.weakAI.WeakAi;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.dataObjects.BattleRecord.BattleResultDescription;
import games.strategy.triplea.player.ITripleAPlayer;
import games.strategy.triplea.ui.display.ITripleADisplay;
import games.strategy.util.IntegerMap;

abstract class AbstractBattle implements IBattle {
  private static final long serialVersionUID = 871090498661731337L;
  final GUID m_battleID = new GUID();
  /**
   * In headless mode we should NOT access any Delegates. In headless mode we are just being used to calculate results
   * for an odds
   * calculator so we can skip some steps for efficiency.
   */
  boolean m_headless = false;
  final Territory m_battleSite;
  final PlayerID m_attacker;
  PlayerID m_defender;
  final BattleTracker m_battleTracker;
  int m_round = 1;
  final boolean m_isBombingRun;
  boolean m_isAmphibious = false;
  BattleType m_battleType;
  boolean m_isOver = false;
  /**
   * Dependent units, maps unit -> Collection of units, if unit is lost in a battle we are dependent on
   * then we lose the corresponding collection of units.
   */
  final Map<Unit, Collection<Unit>> m_dependentUnits = new HashMap<>();
  List<Unit> m_attackingUnits = new ArrayList<>();
  List<Unit> m_defendingUnits = new ArrayList<>();
  List<Unit> m_amphibiousLandAttackers = new ArrayList<>();
  List<Unit> m_bombardingUnits = new ArrayList<>();
  Collection<TerritoryEffect> m_territoryEffects;
  BattleResultDescription m_battleResultDescription;
  WhoWon m_whoWon = WhoWon.NOTFINISHED;
  int m_attackerLostTUV = 0;
  int m_defenderLostTUV = 0;

  protected final GameData m_data;

  AbstractBattle(final Territory battleSite, final PlayerID attacker, final BattleTracker battleTracker,
      final boolean isBombingRun, final BattleType battleType, final GameData data) {
    m_battleTracker = battleTracker;
    m_attacker = attacker;
    m_battleSite = battleSite;
    m_territoryEffects = TerritoryEffectHelper.getEffects(battleSite);
    m_isBombingRun = isBombingRun;
    m_battleType = battleType;
    m_data = data;
    m_defender = findDefender(battleSite, attacker, data);
    // Make sure that if any of the incoming data is null, we are still OK
    // (tests and mockbattle use null for a lot of this stuff)
  }

  @Override
  public Collection<Unit> getDependentUnits(final Collection<Unit> units) {
    final Collection<Unit> dependentUnits = new ArrayList<>();
    for (final Unit unit : units) {
      final Collection<Unit> dependent = m_dependentUnits.get(unit);
      if (dependent != null) {
        dependentUnits.addAll(dependent);
      }
    }
    return dependentUnits;
  }

  protected void removeUnitsThatNoLongerExist() {
    if (m_headless) {
      return;
    }
    // we were having a problem with units that had been killed previously were still part of
    // MFB's variables, so we double check that the stuff still exists here.
    m_defendingUnits.retainAll(m_battleSite.getUnits().getUnits());
    m_attackingUnits.retainAll(m_battleSite.getUnits().getUnits());
  }

  @Override
  public void addBombardingUnit(final Unit unit) {
    m_bombardingUnits.add(unit);
  }

  @Override
  public Collection<Unit> getBombardingUnits() {
    return new ArrayList<>(m_bombardingUnits);
  }

  @Override
  public boolean isAmphibious() {
    return m_isAmphibious;
  }

  @Override
  public Collection<Unit> getAmphibiousLandAttackers() {
    return new ArrayList<>(m_amphibiousLandAttackers);
  }

  @Override
  public Collection<Unit> getAttackingUnits() {
    return new ArrayList<>(m_attackingUnits);
  }

  @Override
  public Collection<Unit> getDefendingUnits() {
    return new ArrayList<>(m_defendingUnits);
  }

  @Override
  public List<Unit> getRemainingAttackingUnits() {
    return new ArrayList<>(m_attackingUnits);
  }

  @Override
  public List<Unit> getRemainingDefendingUnits() {
    return new ArrayList<>(m_defendingUnits);
  }

  @Override
  public abstract boolean isEmpty();

  @Override
  public final boolean isOver() {
    return m_isOver;
  }

  @Override
  public void cancelBattle(final IDelegateBridge bridge) {}

  @Override
  public boolean isBombingRun() {
    return m_isBombingRun;
  }

  @Override
  public BattleType getBattleType() {
    return m_battleType;
  }

  @Override
  public int getBattleRound() {
    return m_round;
  }

  @Override
  public WhoWon getWhoWon() {
    return m_whoWon;
  }

  @Override
  public BattleResultDescription getBattleResultDescription() {
    return m_battleResultDescription;
  }

  @Override
  public GUID getBattleId() {
    return m_battleID;
  }

  @Override
  public final Territory getTerritory() {
    return m_battleSite;
  }

  @Override
  public PlayerID getAttacker() {
    return m_attacker;
  }

  @Override
  public PlayerID getDefender() {
    return m_defender;
  }

  public void setHeadless(final boolean headless) {
    m_headless = headless;
  }

  @Override
  public abstract void fight(IDelegateBridge bridge);

  @Override
  public abstract Change addAttackChange(final Route route, final Collection<Unit> units,
      final HashMap<Unit, HashSet<Unit>> targets);

  @Override
  public abstract void removeAttack(Route route, Collection<Unit> units);

  @Override
  public abstract void unitsLostInPrecedingBattle(IBattle battle, Collection<Unit> units, IDelegateBridge bridge,
      boolean withdrawn);

  @Override
  public int hashCode() {
    return Objects.hashCode(m_battleSite);
  }

  /**
   * 2 Battles are equal if they occur in the same territory,
   * and are both of the same type (bombing / not-bombing),
   * and are both of the same sub-type of bombing/normal
   * (ex: MustFightBattle, or StrategicBombingRaidBattle, or StrategicBombingRaidPreBattle, or NonFightingBattle, etc).
   * <br>
   * Equals in the sense that they should never occupy the same Set if these conditions are met.
   */
  @Override
  public boolean equals(final Object o) {
    if ((o == null) || !(o instanceof IBattle)) {
      return false;
    }
    final IBattle other = (IBattle) o;
    return other.getTerritory().equals(this.m_battleSite) && (other.isBombingRun() == this.isBombingRun())
        && (other.getBattleType() == this.getBattleType());
  }

  @Override
  public String toString() {
    return "Battle in:" + m_battleSite + " battle type:" + m_battleType + " defender:" + m_defender.getName()
        + " attacked by:" + m_attacker.getName() + " attacking with: " + m_attackingUnits;
  }

  static PlayerID findDefender(final Territory battleSite, final PlayerID attacker, final GameData data) {
    if (battleSite == null) {
      return PlayerID.NULL_PLAYERID;
    }
    PlayerID defender = null;
    if (!battleSite.isWater()) {
      defender = battleSite.getOwner();
    }
    if ((data == null) || (attacker == null)) {
      // This is needed for many TESTs, so do not delete
      if (defender == null) {
        return PlayerID.NULL_PLAYERID;
      }
      return defender;
    }
    if ((defender == null) || battleSite.isWater() || !data.getRelationshipTracker().isAtWar(attacker, defender)) {
      // if water find the defender based on who has the most units in the territory
      final IntegerMap<PlayerID> players = battleSite.getUnits().getPlayerUnitCounts();
      int max = -1;
      for (final PlayerID current : players.keySet()) {
        if (current.equals(attacker) || !data.getRelationshipTracker().isAtWar(attacker, current)) {
          continue;
        }
        final int count = players.getInt(current);
        if (count > max) {
          max = count;
          defender = current;
        }
      }
    }
    if (defender == null) {
      return PlayerID.NULL_PLAYERID;
    }
    return defender;
  }

  static PlayerID findPlayerWithMostUnits(final Collection<Unit> units) {
    final IntegerMap<PlayerID> playerUnitCount = new IntegerMap<>();
    for (final Unit unit : units) {
      playerUnitCount.add(unit.getOwner(), 1);
    }
    int max = -1;
    PlayerID player = null;
    for (final PlayerID current : playerUnitCount.keySet()) {
      final int count = playerUnitCount.getInt(current);
      if (count > max) {
        max = count;
        player = current;
      }
    }
    return player;
  }

  /**
   * The maximum number of hits that this collection of units can sustain, taking into account units
   * with two hits, and accounting for existing damage.
   */
  static int getMaxHits(final Collection<Unit> units) {
    int count = 0;
    for (final Unit unit : units) {
      count += UnitAttachment.get(unit.getType()).getHitPoints();
      count -= unit.getHits();
    }
    return count;
  }

  void markDamaged(final Collection<Unit> damaged, final IDelegateBridge bridge) {
    BattleDelegate.markDamaged(damaged, bridge);
  }

  protected static ITripleADisplay getDisplay(final IDelegateBridge bridge) {
    return (ITripleADisplay) bridge.getDisplayChannelBroadcaster();
  }

  protected static ITripleAPlayer getRemote(final IDelegateBridge bridge) {
    return (ITripleAPlayer) bridge.getRemotePlayer();
  }

  protected static ITripleAPlayer getRemote(final PlayerID player, final IDelegateBridge bridge) {
    // if its the null player, return a do nothing proxy
    if (player.isNull()) {
      return new WeakAi(player.getName(), TripleA.WEAK_COMPUTER_PLAYER_TYPE);
    }
    return (ITripleAPlayer) bridge.getRemotePlayer(player);
  }
}
