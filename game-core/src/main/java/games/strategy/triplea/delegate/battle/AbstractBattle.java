package games.strategy.triplea.delegate.battle;

import com.google.common.collect.ImmutableList;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.player.Player;
import games.strategy.triplea.ai.weak.WeakAi;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.data.BattleRecord.BattleResultDescription;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

abstract class AbstractBattle implements IBattle {
  private static final long serialVersionUID = 871090498661731337L;

  final UUID battleId = UUID.randomUUID();
  /**
   * In headless mode we should NOT access any Delegates. In headless mode we are just being used to
   * calculate results for an odds calculator so we can skip some steps for efficiency.
   */
  boolean headless = false;

  @Getter final Territory battleSite;
  final GamePlayer attacker;
  GamePlayer defender;
  final BattleTracker battleTracker;
  int round = 1;
  final boolean isBombingRun;
  boolean isAmphibious = false;
  final BattleType battleType;
  boolean isOver = false;
  /**
   * Dependent units, maps unit -> Collection of units, if unit is lost in a battle we are dependent
   * on then we lose the corresponding collection of units.
   */
  final Map<Unit, Collection<Unit>> dependentUnits = new HashMap<>();

  List<Unit> attackingUnits = new ArrayList<>();
  List<Unit> defendingUnits = new ArrayList<>();
  List<Unit> amphibiousLandAttackers = new ArrayList<>();
  List<Unit> bombardingUnits = new ArrayList<>();
  Collection<TerritoryEffect> territoryEffects;
  BattleResultDescription battleResultDescription;
  WhoWon whoWon = WhoWon.NOT_FINISHED;
  int attackerLostTuv = 0;
  int defenderLostTuv = 0;
  final GameData gameData;

  AbstractBattle(
      final Territory battleSite,
      final GamePlayer attacker,
      final BattleTracker battleTracker,
      final boolean isBombingRun,
      final BattleType battleType,
      final GameData data) {
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
  public List<Unit> getDependentUnits(final Collection<Unit> units) {
    final List<Unit> dependentUnits = new ArrayList<>();
    for (final Unit unit : units) {
      final Collection<Unit> dependent = this.dependentUnits.get(unit);
      if (dependent != null) {
        dependentUnits.addAll(dependent);
      }
    }
    return Collections.unmodifiableList(dependentUnits);
  }

  void addDependentTransportingUnits(final Collection<Unit> units) {
    final Map<Unit, Collection<Unit>> addedTransporting = TransportTracker.transporting(units);
    for (final Unit unit : addedTransporting.keySet()) {
      if (dependentUnits.get(unit) != null) {
        dependentUnits.get(unit).addAll(addedTransporting.get(unit));
      } else {
        dependentUnits.put(unit, new ArrayList<>(addedTransporting.get(unit)));
      }
    }
  }

  void clearTransportedBy(final IDelegateBridge bridge) {
    // Clear the transported_by for successfully off loaded units
    final Collection<Unit> transports =
        CollectionUtils.getMatches(attackingUnits, Matches.unitIsTransport());
    if (!transports.isEmpty()) {
      final CompositeChange change = new CompositeChange();
      final Collection<Unit> dependents = getTransportDependents(transports);
      if (!dependents.isEmpty()) {
        for (final Unit unit : dependents) {
          // clear the loaded by ONLY for Combat unloads. NonCombat unloads are handled elsewhere.
          if (Matches.unitWasUnloadedThisTurn().test(unit)) {
            change.add(ChangeFactory.unitPropertyChange(unit, null, Unit.TRANSPORTED_BY));
          }
        }
        bridge.addChange(change);
      }
    }
  }

  /** Figure out what units a transport is transporting and has to unloaded. */
  Collection<Unit> getTransportDependents(final Collection<Unit> targets) {
    if (headless) {
      return List.of();
    } else if (targets.stream().noneMatch(Matches.unitCanTransport())) {
      return List.of();
    }
    return ImmutableList.copyOf(
        targets.stream()
            .map(TransportTracker::transportingAndUnloaded)
            .flatMap(Collection::stream)
            .collect(Collectors.toList()));
  }

  /**
   * Remove any units killed by a previous event (like they died from a strategic bombing raid,
   * rocket attack, etc).
   */
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
    return Collections.unmodifiableCollection(bombardingUnits);
  }

  @Override
  public boolean isAmphibious() {
    return isAmphibious;
  }

  @Override
  public Collection<Unit> getAmphibiousLandAttackers() {
    return Collections.unmodifiableCollection(amphibiousLandAttackers);
  }

  @Override
  public Collection<Unit> getAttackingUnits() {
    return Collections.unmodifiableCollection(attackingUnits);
  }

  @Override
  public Collection<Unit> getDefendingUnits() {
    return Collections.unmodifiableCollection(defendingUnits);
  }

  @Override
  public Collection<Unit> getRemainingAttackingUnits() {
    return Collections.unmodifiableCollection(attackingUnits);
  }

  @Override
  public Collection<Unit> getRemainingDefendingUnits() {
    return Collections.unmodifiableCollection(defendingUnits);
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
  public UUID getBattleId() {
    return battleId;
  }

  @Override
  public final Territory getTerritory() {
    return battleSite;
  }

  @Override
  public GamePlayer getAttacker() {
    return attacker;
  }

  @Override
  public GamePlayer getDefender() {
    return defender;
  }

  public void setHeadless(final boolean headless) {
    this.headless = headless;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(battleSite);
  }

  public Integer getRound() {
    return round;
  }

  /**
   * 2 Battles are equal if they occur in the same territory, and are both of the same type (bombing
   * / not-bombing), and are both of the same sub-type of bombing/normal (ex: MustFightBattle, or
   * StrategicBombingRaidBattle, or StrategicBombingRaidPreBattle, or NonFightingBattle, etc). <br>
   * Equals in the sense that they should never occupy the same Set if these conditions are met.
   */
  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof IBattle)) {
      return false;
    }
    final IBattle other = (IBattle) o;
    return other.getTerritory().equals(this.battleSite)
        && other.isBombingRun() == this.isBombingRun()
        && other.getBattleType() == this.getBattleType();
  }

  @Override
  public String toString() {
    return "Battle in:"
        + battleSite
        + " battle type:"
        + battleType
        + " defender:"
        + defender.getName()
        + " attacked by:"
        + attacker.getName()
        + " attacking with: "
        + attackingUnits;
  }

  static GamePlayer findDefender(
      final Territory battleSite, final GamePlayer attacker, final GameData data) {
    if (battleSite == null) {
      return GamePlayer.NULL_PLAYERID;
    }
    GamePlayer defender = null;
    if (!battleSite.isWater()) {
      defender = battleSite.getOwner();
    }
    if (data == null || attacker == null) {
      // This is needed for many TESTs, so do not delete
      if (defender == null) {
        return GamePlayer.NULL_PLAYERID;
      }
      return defender;
    }
    if (defender == null
        || battleSite.isWater()
        || !data.getRelationshipTracker().isAtWar(attacker, defender)) {
      // if water find the defender based on who has the most units in the territory
      final IntegerMap<GamePlayer> players = battleSite.getUnitCollection().getPlayerUnitCounts();
      int max = -1;
      for (final GamePlayer current : players.keySet()) {
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
      return GamePlayer.NULL_PLAYERID;
    }
    return defender;
  }

  static GamePlayer findPlayerWithMostUnits(final Collection<Unit> units) {
    final IntegerMap<GamePlayer> playerUnitCount = new IntegerMap<>();
    for (final Unit unit : units) {
      playerUnitCount.add(unit.getOwner(), 1);
    }
    int max = -1;
    GamePlayer player = null;
    for (final GamePlayer current : playerUnitCount.keySet()) {
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
    BattleDelegate.markDamaged(damaged, bridge, battleSite);
  }

  protected static Player getRemote(final IDelegateBridge bridge) {
    return bridge.getRemotePlayer();
  }

  protected static Player getRemote(final GamePlayer player, final IDelegateBridge bridge) {
    // if its the null player, return a do nothing proxy
    if (player.isNull()) {
      return new WeakAi(player.getName());
    }
    return bridge.getRemotePlayer(player);
  }
}
