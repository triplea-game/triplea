package games.strategy.triplea.delegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.net.GUID;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.ai.weak.WeakAi;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.data.BattleRecord.BattleResultDescription;
import games.strategy.triplea.player.ITripleAPlayer;
import games.strategy.triplea.ui.display.ITripleADisplay;

abstract class AbstractBattle implements IBattle {
  private static final long serialVersionUID = 871090498661731337L;

  final GUID battleId = new GUID();
  /**
   * In headless mode we should NOT access any Delegates. In headless mode we are just being used to calculate results
   * for an odds calculator so we can skip some steps for efficiency.
   */
  boolean headless = false;
  final Territory battleSite;
  final PlayerId attacker;
  PlayerId defender;
  final BattleTracker battleTracker;
  int round = 1;
  final boolean isBombingRun;
  boolean isAmphibious = false;
  final BattleType battleType;
  boolean isOver = false;
  /**
   * Dependent units, maps unit -> Collection of units, if unit is lost in a battle we are dependent on
   * then we lose the corresponding collection of units.
   */
  final Map<Unit, Collection<Unit>> dependentUnits = new HashMap<>();
  List<Unit> attackingUnits = new ArrayList<>();
  List<Unit> defendingUnits = new ArrayList<>();
  List<Unit> amphibiousLandAttackers = new ArrayList<>();
  List<Unit> bombardingUnits = new ArrayList<>();
  Collection<TerritoryEffect> territoryEffects;
  BattleResultDescription battleResultDescription;
  WhoWon whoWon = WhoWon.NOTFINISHED;
  int attackerLostTuv = 0;
  int defenderLostTuv = 0;
  final GameData gameData;

  AbstractBattle(final Territory battleSite, final PlayerId attacker, final BattleTracker battleTracker,
      final boolean isBombingRun, final BattleType battleType, final GameData data) {
    this.battleTracker = battleTracker;
    this.attacker = attacker;
    this.battleSite = battleSite;
    territoryEffects = TerritoryEffectHelper.getEffects(battleSite);
    this.isBombingRun = isBombingRun;
    this.battleType = battleType;
    gameData = data;
    defender = findDefender(battleSite, attacker, data);
    // Make sure that if any of the incoming data is null, we are still OK
    // (tests and mockbattle use null for a lot of this stuff)
  }

  @Override
  public Collection<Unit> getDependentUnits(final Collection<Unit> units) {
    final Collection<Unit> dependentUnits = new ArrayList<>();
    for (final Unit unit : units) {
      final Collection<Unit> dependent = this.dependentUnits.get(unit);
      if (dependent != null) {
        dependentUnits.addAll(dependent);
      }
    }
    return dependentUnits;
  }

  void clearTransportedBy(final IDelegateBridge bridge) {
    // Clear the transported_by for successfully off loaded units
    final Collection<Unit> transports = CollectionUtils.getMatches(attackingUnits, Matches.unitIsTransport());
    if (!transports.isEmpty()) {
      final CompositeChange change = new CompositeChange();
      final Collection<Unit> dependents = getTransportDependents(transports);
      if (!dependents.isEmpty()) {
        for (final Unit unit : dependents) {
          // clear the loaded by ONLY for Combat unloads. NonCombat unloads are handled elsewhere.
          if (Matches.unitWasUnloadedThisTurn().test(unit)) {
            change.add(ChangeFactory.unitPropertyChange(unit, null, TripleAUnit.TRANSPORTED_BY));
          }
        }
        bridge.addChange(change);
      }
    }
  }

  /**
   * Figure out what units a transport is transporting and has to unloaded.
   */
  Collection<Unit> getTransportDependents(final Collection<Unit> targets) {
    if (headless) {
      return Collections.emptyList();
    } else if (targets.stream().noneMatch(Matches.unitCanTransport())) {
      return new ArrayList<>();
    }
    return targets.stream()
        .map(TransportTracker::transportingAndUnloaded)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  protected void removeUnitsThatNoLongerExist() {
    if (headless) {
      return;
    }
    // we were having a problem with units that had been killed previously were still part of
    // MFB's variables, so we double check that the stuff still exists here.
    defendingUnits.retainAll(battleSite.getUnits());
    attackingUnits.retainAll(battleSite.getUnits());
  }

  @Override
  public void addBombardingUnit(final Unit unit) {
    bombardingUnits.add(unit);
  }

  @Override
  public Collection<Unit> getBombardingUnits() {
    return new ArrayList<>(bombardingUnits);
  }

  @Override
  public boolean isAmphibious() {
    return isAmphibious;
  }

  @Override
  public Collection<Unit> getAmphibiousLandAttackers() {
    return new ArrayList<>(amphibiousLandAttackers);
  }

  @Override
  public Collection<Unit> getAttackingUnits() {
    return new ArrayList<>(attackingUnits);
  }

  @Override
  public Collection<Unit> getDefendingUnits() {
    return new ArrayList<>(defendingUnits);
  }

  @Override
  public List<Unit> getRemainingAttackingUnits() {
    return new ArrayList<>(attackingUnits);
  }

  @Override
  public List<Unit> getRemainingDefendingUnits() {
    return new ArrayList<>(defendingUnits);
  }

  @Override
  public final boolean isOver() {
    return isOver;
  }

  @Override
  public void cancelBattle(final IDelegateBridge bridge) {}

  @Override
  public boolean isBombingRun() {
    return isBombingRun;
  }

  @Override
  public BattleType getBattleType() {
    return battleType;
  }

  @Override
  public int getBattleRound() {
    return round;
  }

  @Override
  public WhoWon getWhoWon() {
    return whoWon;
  }

  @Override
  public BattleResultDescription getBattleResultDescription() {
    return battleResultDescription;
  }

  @Override
  public GUID getBattleId() {
    return battleId;
  }

  @Override
  public final Territory getTerritory() {
    return battleSite;
  }

  @Override
  public PlayerId getAttacker() {
    return attacker;
  }

  @Override
  public PlayerId getDefender() {
    return defender;
  }

  public void setHeadless(final boolean headless) {
    this.headless = headless;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(battleSite);
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
    if (!(o instanceof IBattle)) {
      return false;
    }
    final IBattle other = (IBattle) o;
    return other.getTerritory().equals(this.battleSite) && other.isBombingRun() == this.isBombingRun()
        && other.getBattleType() == this.getBattleType();
  }

  @Override
  public String toString() {
    return "Battle in:" + battleSite + " battle type:" + battleType + " defender:" + defender.getName()
        + " attacked by:" + attacker.getName() + " attacking with: " + attackingUnits;
  }

  static PlayerId findDefender(final Territory battleSite, final PlayerId attacker, final GameData data) {
    if (battleSite == null) {
      return PlayerId.NULL_PLAYERID;
    }
    PlayerId defender = null;
    if (!battleSite.isWater()) {
      defender = battleSite.getOwner();
    }
    if (data == null || attacker == null) {
      // This is needed for many TESTs, so do not delete
      if (defender == null) {
        return PlayerId.NULL_PLAYERID;
      }
      return defender;
    }
    if (defender == null || battleSite.isWater() || !data.getRelationshipTracker().isAtWar(attacker, defender)) {
      // if water find the defender based on who has the most units in the territory
      final IntegerMap<PlayerId> players = battleSite.getUnitCollection().getPlayerUnitCounts();
      int max = -1;
      for (final PlayerId current : players.keySet()) {
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
      return PlayerId.NULL_PLAYERID;
    }
    return defender;
  }

  static PlayerId findPlayerWithMostUnits(final Collection<Unit> units) {
    final IntegerMap<PlayerId> playerUnitCount = new IntegerMap<>();
    for (final Unit unit : units) {
      playerUnitCount.add(unit.getOwner(), 1);
    }
    int max = -1;
    PlayerId player = null;
    for (final PlayerId current : playerUnitCount.keySet()) {
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

  protected static ITripleAPlayer getRemote(final PlayerId player, final IDelegateBridge bridge) {
    // if its the null player, return a do nothing proxy
    if (player.isNull()) {
      return new WeakAi(player.getName());
    }
    return (ITripleAPlayer) bridge.getRemotePlayer(player);
  }
}
