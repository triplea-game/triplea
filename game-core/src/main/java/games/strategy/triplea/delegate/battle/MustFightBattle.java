package games.strategy.triplea.delegate.battle;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleStatus.ALIVE;
import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.SUB_DEFENSIVE_RETREAT_AFTER_BATTLE;
import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.SUB_DEFENSIVE_RETREAT_BEFORE_BATTLE;
import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.SUB_OFFENSIVE_RETREAT_AFTER_BATTLE;
import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.SUB_OFFENSIVE_RETREAT_BEFORE_BATTLE;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.engine.player.Player;
import games.strategy.triplea.Properties;
import games.strategy.triplea.UnitUtils;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.IExecutable;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.battle.casualty.CasualtySortingUtil;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import games.strategy.triplea.delegate.battle.steps.BattleSteps;
import games.strategy.triplea.delegate.battle.steps.change.CheckGeneralBattleEndOld;
import games.strategy.triplea.delegate.battle.steps.change.ClearAaCasualties;
import games.strategy.triplea.delegate.battle.steps.change.ClearGeneralCasualties;
import games.strategy.triplea.delegate.battle.steps.change.LandParatroopers;
import games.strategy.triplea.delegate.battle.steps.change.MarkNoMovementLeft;
import games.strategy.triplea.delegate.battle.steps.change.RemoveNonCombatants;
import games.strategy.triplea.delegate.battle.steps.change.RemoveUnprotectedUnits;
import games.strategy.triplea.delegate.battle.steps.change.suicide.RemoveFirstStrikeSuicide;
import games.strategy.triplea.delegate.battle.steps.change.suicide.RemoveGeneralSuicide;
import games.strategy.triplea.delegate.battle.steps.fire.NavalBombardment;
import games.strategy.triplea.delegate.battle.steps.fire.aa.DefensiveAaFire;
import games.strategy.triplea.delegate.battle.steps.fire.aa.OffensiveAaFire;
import games.strategy.triplea.delegate.battle.steps.fire.firststrike.DefensiveFirstStrike;
import games.strategy.triplea.delegate.battle.steps.fire.firststrike.OffensiveFirstStrike;
import games.strategy.triplea.delegate.battle.steps.fire.general.DefensiveGeneral;
import games.strategy.triplea.delegate.battle.steps.fire.general.OffensiveGeneral;
import games.strategy.triplea.delegate.battle.steps.retreat.DefensiveSubsRetreat;
import games.strategy.triplea.delegate.battle.steps.retreat.OffensiveGeneralRetreat;
import games.strategy.triplea.delegate.battle.steps.retreat.OffensiveSubsRetreat;
import games.strategy.triplea.delegate.battle.steps.retreat.sub.SubmergeSubsVsOnlyAirStep;
import games.strategy.triplea.delegate.data.BattleRecord;
import games.strategy.triplea.delegate.move.validation.AirMovementValidator;
import games.strategy.triplea.delegate.move.validation.MoveValidator;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.util.TuvUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.extern.java.Log;
import org.triplea.java.PredicateBuilder;
import org.triplea.java.RemoveOnNextMajorRelease;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.sound.SoundPath;
import org.triplea.sound.SoundUtils;
import org.triplea.util.Tuple;

/** Handles logic for battles in which fighting actually occurs. */
@Log
public class MustFightBattle extends DependentBattle
    implements BattleStepStrings, BattleActions, BattleState {

  /** Determines whether casualties can return fire for various battle phases. */
  public enum ReturnFire {
    ALL,
    SUBS,
    NONE
  }

  /** Determines the type of retreat. */
  public enum RetreatType {
    DEFAULT,
    SUBS,
    PLANES,
    PARTIAL_AMPHIB
  }

  private static final long serialVersionUID = 5879502298361231540L;

  private static final long MAX_ROUNDS = 10000;

  private final Collection<Unit> attackingWaitingToDie = new ArrayList<>();

  private final Collection<Unit> defendingWaitingToDie = new ArrayList<>();
  // keep track of all the units that die in the battle to show in the history window
  private final Collection<Unit> killed = new ArrayList<>();
  // keep track of all the units that die this round to see if they change into another unit
  private final List<Unit> killedDuringCurrentRound = new ArrayList<>();
  // Our current execution state, we keep a stack of executables, this allows us to save our state
  // and resume while in
  // the middle of a battle.
  private final ExecutionStack stack = new ExecutionStack();

  @Getter(onMethod = @__({@Override}))
  private List<String> stepStrings;

  @RemoveOnNextMajorRelease
  @SuppressWarnings("unused")
  private List<Unit> defendingAa;

  @RemoveOnNextMajorRelease
  @SuppressWarnings("unused")
  private List<Unit> offensiveAa;

  @RemoveOnNextMajorRelease
  @SuppressWarnings("unused")
  private List<String> defendingAaTypes;

  @RemoveOnNextMajorRelease
  @SuppressWarnings("unused")
  private List<String> offensiveAaTypes;

  private final List<Unit> attackingUnitsRetreated = new ArrayList<>();
  private final List<Unit> defendingUnitsRetreated = new ArrayList<>();
  // -1 would mean forever until one side is eliminated (the default is infinite)
  private final int maxRounds;

  public MustFightBattle(
      final Territory battleSite,
      final GamePlayer attacker,
      final GameData data,
      final BattleTracker battleTracker) {
    super(battleSite, attacker, battleTracker, data);
    defendingUnits.addAll(
        this.battleSite.getUnitCollection().getMatches(Matches.enemyUnit(attacker, data)));
    maxRounds =
        battleSite.isWater()
            ? Properties.getSeaBattleRounds(data.getProperties())
            : Properties.getLandBattleRounds(data.getProperties());
  }

  void resetDefendingUnits(final GamePlayer attacker, final GameData data) {
    defendingUnits.clear();
    defendingUnits.addAll(
        battleSite.getUnitCollection().getMatches(Matches.enemyUnit(attacker, data)));
  }

  /** Used for head-less battles. */
  public void setUnits(
      final Collection<Unit> defending,
      final Collection<Unit> attacking,
      final Collection<Unit> bombarding,
      final GamePlayer defender,
      final Collection<TerritoryEffect> territoryEffects) {
    defendingUnits = new ArrayList<>(defending);
    attackingUnits = new ArrayList<>(attacking);
    bombardingUnits = new ArrayList<>(bombarding);
    this.defender = defender;
    this.territoryEffects = territoryEffects;
  }

  @Override
  public void removeAttack(final Route route, final Collection<Unit> units) {
    attackingUnits.removeAll(units);
    // the route could be null, in the case of a unit in a territory where a sub is submerged.
    if (route == null) {
      return;
    }
    final Territory attackingFrom = route.getTerritoryBeforeEnd();
    Collection<Unit> attackingFromMapUnits = attackingFromMap.get(attackingFrom);
    // handle possible null pointer
    if (attackingFromMapUnits == null) {
      attackingFromMapUnits = new ArrayList<>();
    }
    attackingFromMapUnits.removeAll(units);
    if (attackingFromMapUnits.isEmpty()) {
      this.attackingFrom.remove(attackingFrom);
    }
    // deal with amphibious assaults
    if (attackingFrom.isWater()) {
      // if none of the units is a land unit, the attack from that territory is no longer an
      // amphibious assault
      if (attackingFromMapUnits.stream().noneMatch(Matches.unitIsLand())) {
        getAmphibiousAttackTerritories().remove(attackingFrom);
        // do we have any amphibious attacks left?
        isAmphibious = !getAmphibiousAttackTerritories().isEmpty();
      }
    }
    for (final Collection<Unit> dependents : dependentUnits.values()) {
      dependents.removeAll(units);
    }
  }

  @Override
  public boolean isEmpty() {
    return attackingUnits.isEmpty() && attackingWaitingToDie.isEmpty();
  }

  @Override
  public Change addAttackChange(
      final Route route, final Collection<Unit> units, final Map<Unit, Set<Unit>> targets) {
    final CompositeChange change = new CompositeChange();
    // Filter out allied units if WW2V2
    final Predicate<Unit> ownedBy = Matches.unitIsOwnedBy(attacker);
    final Collection<Unit> attackingUnits =
        Properties.getWW2V2(gameData.getProperties())
            ? CollectionUtils.getMatches(units, ownedBy)
            : units;
    final Territory attackingFrom = route.getTerritoryBeforeEnd();
    this.attackingFrom.add(attackingFrom);
    this.attackingUnits.addAll(attackingUnits);
    attackingFromMap.computeIfAbsent(attackingFrom, k -> new ArrayList<>());
    final Collection<Unit> attackingFromMapUnits = attackingFromMap.get(attackingFrom);
    attackingFromMapUnits.addAll(attackingUnits);
    // are we amphibious
    if (route.getStart().isWater()
        && !route.getEnd().isWater()
        && attackingUnits.stream().anyMatch(Matches.unitIsLand())) {
      getAmphibiousAttackTerritories().add(route.getTerritoryBeforeEnd());
      isAmphibious = true;
    }
    final Map<Unit, Collection<Unit>> dependencies =
        new HashMap<>(TransportTracker.transporting(units));
    if (!Properties.getAlliedAirIndependent(gameData.getProperties())) {
      dependencies.putAll(MoveValidator.carrierMustMoveWith(units, units, gameData, attacker));
      for (final Unit carrier : dependencies.keySet()) {
        final UnitAttachment ua = UnitAttachment.get(carrier.getType());
        if (ua.getCarrierCapacity() == -1) {
          continue;
        }

        // set transported by on each figher and remove each one from battle display
        dependencies.get(carrier).stream()
            .filter(Matches.unitIsAir())
            .forEach(
                fighter -> {
                  change.add(
                      ChangeFactory.unitPropertyChange(fighter, carrier, Unit.TRANSPORTED_BY));
                  this.attackingUnits.remove(fighter);
                });
      }
    }
    addDependentUnits(dependencies);
    // mark units with no movement for all but air
    Collection<Unit> nonAir = CollectionUtils.getMatches(attackingUnits, Matches.unitIsNotAir());
    // we don't want to change the movement of transported land units if this is a sea battle
    // so restrict non air to remove land units
    if (battleSite.isWater()) {
      nonAir = CollectionUtils.getMatches(nonAir, Matches.unitIsNotLand());
    }
    // TODO: This checks for ignored sub/trns and skips the set of the attackers to 0 movement left
    // If attacker stops in an occupied territory, movement stops (battle is optional)
    if (new MoveValidator(gameData).onlyIgnoredUnitsOnPath(route, attacker, false)) {
      return change;
    }
    change.add(ChangeFactory.markNoMovementChange(nonAir));
    return change;
  }

  void addDependentUnits(final Map<Unit, Collection<Unit>> dependencies) {
    for (final Unit holder : dependencies.keySet()) {
      final Collection<Unit> transporting = dependencies.get(holder);
      if (dependentUnits.get(holder) != null) {
        dependentUnits.get(holder).addAll(transporting);
      } else {
        dependentUnits.put(holder, new LinkedHashSet<>(transporting));
      }
    }
  }

  /**
   * Used by battle results to get the remaining attackers after the battle is completed. It
   * includes remaining attackers, retreated attackers, and if attacker won/draw then any owned
   * units left in the territory.
   */
  @Override
  public List<Unit> getRemainingAttackingUnits() {
    final Set<Unit> remaining = new HashSet<>(attackingUnitsRetreated);
    final Collection<Unit> unitsLeftInTerritory = new ArrayList<>(battleSite.getUnits());
    unitsLeftInTerritory.removeAll(killed);
    remaining.addAll(
        CollectionUtils.getMatches(
            unitsLeftInTerritory,
            getWhoWon() != WhoWon.DEFENDER
                ? Matches.unitOwnedBy(attacker)
                : Matches.unitOwnedBy(attacker)
                    .and(Matches.unitIsAir())
                    .and(Matches.unitIsNotInfrastructure())));
    return new ArrayList<>(remaining);
  }

  /**
   * Used by battle results to get the remaining defenders after the battle is completed. It
   * includes remaining defenders, retreated defenders, and if defender won/draw then any owned
   * units and enemy units of the attacker left in the territory.
   */
  @Override
  public List<Unit> getRemainingDefendingUnits() {
    final Set<Unit> remaining = new HashSet<>(defendingUnitsRetreated);
    remaining.addAll(defendingUnits);
    if (getWhoWon() != WhoWon.ATTACKER || attackingUnits.stream().allMatch(Matches.unitIsAir())) {
      final Collection<Unit> unitsLeftInTerritory = new ArrayList<>(battleSite.getUnits());
      unitsLeftInTerritory.removeAll(killed);
      remaining.addAll(
          CollectionUtils.getMatches(
              unitsLeftInTerritory,
              Matches.unitIsOwnedBy(defender).or(Matches.enemyUnit(attacker, gameData))));
    }
    return new ArrayList<>(remaining);
  }

  @Override
  public BattleStatus getStatus() {
    return BattleStatus.of(round, maxRounds, isOver, isAmphibious, headless);
  }

  @Override
  public @Nullable Territory queryRetreatTerritory(
      final BattleState battleState,
      final IDelegateBridge bridge,
      final GamePlayer retreatingPlayer,
      final Collection<Territory> availableTerritories,
      final String text) {
    return retreatQuery(
        battleState, getRemote(retreatingPlayer, bridge), availableTerritories, false, text);
  }

  private @Nullable Territory retreatQuery(
      final BattleState battleState,
      final Player remotePlayer,
      final Collection<Territory> availableTerritories,
      final boolean submerge,
      final String text) {
    final Territory retreatTo =
        remotePlayer.retreatQuery(
            battleState.getBattleId(),
            submerge,
            battleState.getBattleSite(),
            availableTerritories,
            text);
    if (retreatTo != null && !availableTerritories.contains(retreatTo)) {
      log.severe(
          "Invalid retreat selection: "
              + retreatTo
              + " not in "
              + MyFormatter.defaultNamedToTextList(availableTerritories));
      return null;
    }
    return retreatTo;
  }

  @Override
  public @Nullable Territory querySubmergeTerritory(
      final BattleState battleState,
      final IDelegateBridge bridge,
      final GamePlayer retreatingPlayer,
      final Collection<Territory> availableTerritories,
      final String text) {
    return retreatQuery(
        battleState, getRemote(retreatingPlayer, bridge), availableTerritories, true, text);
  }

  @Override
  public Collection<IBattle> getDependentBattles() {
    return battleTracker.getBlocked(this);
  }

  @Override
  public GamePlayer getPlayer(final Side side) {
    return side == OFFENSE ? getAttacker() : getDefender();
  }

  @Override
  public Collection<Unit> filterUnits(final UnitBattleFilter filter, final Side... sides) {
    return filter.getFilter().stream()
        .flatMap(status -> getUnits(status, sides).stream())
        .collect(Collectors.toList());
  }

  private Collection<Unit> getUnits(final UnitBattleStatus status, final Side... sides) {
    switch (status) {
      case ALIVE:
        return Collections.unmodifiableCollection(getUnits(sides));
      case CASUALTY:
        return Collections.unmodifiableCollection(getWaitingToDie(sides));
      case REMOVED_CASUALTY:
        return Collections.unmodifiableCollection(killed);
      default:
        return List.of();
    }
  }

  private Collection<Unit> getUnits(final Side... sides) {
    final Collection<Unit> units = new ArrayList<>();
    for (final Side side : sides) {
      switch (side) {
        case OFFENSE:
          units.addAll(attackingUnits);
          break;
        case DEFENSE:
          units.addAll(defendingUnits);
          break;
        default:
          break;
      }
    }
    return units;
  }

  private Collection<Unit> getWaitingToDie(final Side... sides) {
    final Collection<Unit> waitingToDie = new ArrayList<>();
    for (final Side side : sides) {
      switch (side) {
        case OFFENSE:
          waitingToDie.addAll(attackingWaitingToDie);
          break;
        case DEFENSE:
          waitingToDie.addAll(defendingWaitingToDie);
          break;
        default:
          break;
      }
    }
    return waitingToDie;
  }

  @Override
  public void clearWaitingToDie(final Side... sides) {
    for (final Side side : sides) {
      switch (side) {
        case OFFENSE:
          attackingWaitingToDie.clear();
          break;
        case DEFENSE:
          defendingWaitingToDie.clear();
          break;
        default:
          break;
      }
    }
  }

  @Override
  public void retreatUnits(final Side side, final Collection<Unit> retreatingUnits) {
    final Collection<Unit> units = side == DEFENSE ? defendingUnits : attackingUnits;
    final Collection<Unit> unitsRetreated =
        side == DEFENSE ? defendingUnitsRetreated : attackingUnitsRetreated;
    units.removeAll(retreatingUnits);
    unitsRetreated.addAll(retreatingUnits);
  }

  @Override
  public void removeDependentUnits(final Collection<Unit> unitsWithDependents) {
    for (final Unit unit : unitsWithDependents) {
      dependentUnits.remove(unit);
    }
  }

  /**
   * Used for setting stuff when we make a scrambling battle when there was no previous battle
   * there, and we need retreat spaces.
   */
  void setAttackingFromAndMap(final Map<Territory, Collection<Unit>> attackingFromMap) {
    this.attackingFromMap = attackingFromMap;
    attackingFrom = new HashSet<>(attackingFromMap.keySet());
  }

  @Override
  public void unitsLostInPrecedingBattle(
      final Collection<Unit> units, final IDelegateBridge bridge, final boolean withdrawn) {
    Collection<Unit> lost = new ArrayList<>(getDependentUnits(units));
    lost.addAll(CollectionUtils.intersection(units, attackingUnits));
    // if all the amphibious attacking land units are lost, then we are no longer a naval invasion
    if (getUnits(ALIVE, OFFENSE).stream().noneMatch(Matches.unitWasAmphibious())) {
      isAmphibious = false;
      bombardingUnits.clear();
    }
    attackingUnits.removeAll(lost);
    // now that they are definitely removed from our attacking list, make sure that they were not
    // already removed from
    // the territory by the previous battle's remove method
    lost = CollectionUtils.getMatches(lost, Matches.unitIsInTerritory(battleSite));
    if (!withdrawn) {
      remove(lost, bridge, battleSite, false);
    }
    if (attackingUnits.isEmpty()) {
      final IntegerMap<UnitType> costs = TuvUtils.getCostsForTuv(attacker, gameData);
      final int tuvLostAttacker =
          (withdrawn ? 0 : TuvUtils.getTuv(lost, attacker, costs, gameData));
      attackerLostTuv += tuvLostAttacker;
      whoWon = WhoWon.DEFENDER;
      if (!headless) {
        battleTracker
            .getBattleRecords()
            .addResultToBattle(
                attacker,
                battleId,
                defender,
                attackerLostTuv,
                defenderLostTuv,
                BattleRecord.BattleResultDescription.LOST,
                new BattleResults(this, gameData));
      }
      battleTracker.removeBattle(this, gameData);
    }
  }

  @Override
  public void cancelBattle(final IDelegateBridge bridge) {
    endBattle(bridge);
  }

  @Override
  public void endBattle(final WhoWon whoWon, final IDelegateBridge bridge) {
    switch (whoWon) {
      case ATTACKER:
        attackerWins(bridge);
        break;
      case DEFENDER:
        defenderWins(bridge);
        break;
      case DRAW:
        nobodyWins(bridge);
        break;
      default:
        throw new IllegalStateException("WhoWon? " + whoWon);
    }
  }

  private void endBattle(final IDelegateBridge bridge) {
    clearWaitingToDieAndDamagedChangesInto(bridge);
    isOver = true;
    battleTracker.removeBattle(this, bridge.getData());

    // Must clear transportedby for allied air on carriers for both attacking units and retreating
    // units
    final CompositeChange clearAlliedAir =
        TransportTracker.clearTransportedByForAlliedAirOnCarrier(
            attackingUnits, battleSite, attacker, gameData);
    if (!clearAlliedAir.isEmpty()) {
      bridge.addChange(clearAlliedAir);
    }
    final CompositeChange clearAlliedAirRetreated =
        TransportTracker.clearTransportedByForAlliedAirOnCarrier(
            attackingUnitsRetreated, battleSite, attacker, gameData);
    if (!clearAlliedAirRetreated.isEmpty()) {
      bridge.addChange(clearAlliedAirRetreated);
    }
  }

  @Override
  public void clearWaitingToDieAndDamagedChangesInto(final IDelegateBridge bridge) {
    final Collection<Unit> unitsToRemove = new ArrayList<>();
    unitsToRemove.addAll(attackingWaitingToDie);
    unitsToRemove.addAll(defendingWaitingToDie);
    remove(unitsToRemove, bridge, battleSite, null);
    defendingWaitingToDie.clear();
    attackingWaitingToDie.clear();
    damagedChangeInto(
        attacker,
        attackingUnits,
        CollectionUtils.getMatches(killedDuringCurrentRound, Matches.unitIsOwnedBy(attacker)),
        bridge);
    damagedChangeInto(
        defender,
        defendingUnits,
        CollectionUtils.getMatches(
            killedDuringCurrentRound, Matches.unitIsOwnedBy(attacker).negate()),
        bridge);
    killedDuringCurrentRound.clear();
  }

  @Override
  public void damagedChangeInto(
      final GamePlayer player,
      final Collection<Unit> units,
      final Collection<Unit> killedUnits,
      final IDelegateBridge bridge) {
    final List<Unit> damagedUnits =
        CollectionUtils.getMatches(
            units,
            Matches.unitWhenHitPointsDamagedChangesInto().and(Matches.unitHasTakenSomeDamage()));
    damagedUnits.addAll(
        CollectionUtils.getMatches(killedUnits, Matches.unitAtMaxHitPointDamageChangesInto()));
    final CompositeChange changes = new CompositeChange();
    final List<Unit> unitsToRemove = new ArrayList<>();
    final List<Unit> unitsToAdd = new ArrayList<>();
    for (final Unit unit : damagedUnits) {
      final Map<Integer, Tuple<Boolean, UnitType>> map =
          UnitAttachment.get(unit.getType()).getWhenHitPointsDamagedChangesInto();
      if (map.containsKey(unit.getHits())) {
        final boolean translateAttributes = map.get(unit.getHits()).getFirst();
        final UnitType unitType = map.get(unit.getHits()).getSecond();
        final List<Unit> toAdd = unitType.create(1, unit.getOwner());
        if (translateAttributes) {
          final Change translate =
              UnitUtils.translateAttributesToOtherUnits(unit, toAdd, battleSite);
          changes.add(translate);
        }
        unitsToAdd.addAll(toAdd);
        if (!killedUnits.contains(unit)) {
          unitsToRemove.add(unit);
        }
      }
    }
    if (!unitsToAdd.isEmpty()) {
      bridge.addChange(changes);
      remove(unitsToRemove, bridge, battleSite, null);
      final String transcriptText =
          MyFormatter.unitsToText(unitsToAdd) + " added in " + battleSite.getName();
      bridge.getHistoryWriter().addChildToEvent(transcriptText, new ArrayList<>(unitsToAdd));
      bridge.addChange(ChangeFactory.addUnits(battleSite, unitsToAdd));
      bridge.addChange(ChangeFactory.markNoMovementChange(unitsToAdd));
      units.addAll(unitsToAdd);
      bridge
          .getDisplayChannelBroadcaster()
          .changedUnitsNotification(battleId, player, unitsToRemove, unitsToAdd, null);
    }
  }

  @Override
  public void removeCasualties(
      final Collection<Unit> killed,
      final ReturnFire returnFire,
      final boolean defender,
      final IDelegateBridge bridge) {
    if (killed.isEmpty()) {
      return;
    }
    if (returnFire == ReturnFire.ALL) {
      // move to waiting to die
      if (defender) {
        defendingWaitingToDie.addAll(killed);
      } else {
        attackingWaitingToDie.addAll(killed);
      }
    } else if (returnFire == ReturnFire.SUBS) {
      // move to waiting to die
      if (defender) {
        defendingWaitingToDie.addAll(
            CollectionUtils.getMatches(killed, Matches.unitIsFirstStrike()));
      } else {
        attackingWaitingToDie.addAll(
            CollectionUtils.getMatches(killed, Matches.unitIsFirstStrike()));
      }
      remove(
          CollectionUtils.getMatches(killed, Matches.unitIsFirstStrike().negate()),
          bridge,
          battleSite,
          defender);
    } else if (returnFire == ReturnFire.NONE) {
      remove(killed, bridge, battleSite, defender);
    }
    // remove from the active fighting
    if (defender) {
      defendingUnits.removeAll(killed);
    } else {
      attackingUnits.removeAll(killed);
    }
  }

  @Override
  public void remove(
      final Collection<Unit> killedUnits,
      final IDelegateBridge bridge,
      final Territory battleSite,
      final Boolean defenderDying) {
    if (killedUnits.isEmpty()) {
      return;
    }
    final Collection<Unit> killed = getUnitsWithDependents(killedUnits);

    // Remove units
    final Change killedChange = ChangeFactory.removeUnits(battleSite, killed);
    this.killed.addAll(killed);
    killedDuringCurrentRound.addAll(killed);
    final String transcriptText =
        MyFormatter.unitsToText(killed) + " lost in " + battleSite.getName();
    bridge.getHistoryWriter().addChildToEvent(transcriptText, new ArrayList<>(killed));
    bridge.addChange(killedChange);

    // Set max damage for any units that will change into another unit
    final IntegerMap<Unit> lethallyDamagedMap = new IntegerMap<>();
    for (final Unit unit :
        CollectionUtils.getMatches(killed, Matches.unitAtMaxHitPointDamageChangesInto())) {
      lethallyDamagedMap.put(unit, unit.getUnitAttachment().getHitPoints());
    }
    final Change lethallyDamagedChange =
        ChangeFactory.unitsHit(lethallyDamagedMap, List.of(battleSite));
    bridge.addChange(lethallyDamagedChange);

    final Collection<IBattle> dependentBattles = battleTracker.getBlocked(this);
    // If there are NO dependent battles, check for unloads in allied territories
    if (dependentBattles.isEmpty()) {
      removeFromNonCombatLandings(killed, bridge);
      // otherwise remove them and the units involved
    } else {
      removeFromDependents(killed, bridge, dependentBattles);
    }

    // Remove them from the battle display
    if (defenderDying == null || defenderDying) {
      defendingUnits.removeAll(killed);
    }
    if (defenderDying == null || !defenderDying) {
      attackingUnits.removeAll(killed);
    }
  }

  // Remove landed units from allied territory when their transport sinks
  private void removeFromNonCombatLandings(
      final Collection<Unit> units, final IDelegateBridge bridge) {
    for (final Unit transport : CollectionUtils.getMatches(units, Matches.unitIsTransport())) {
      final Collection<Unit> lost = getTransportDependents(Set.of(transport));
      if (lost.isEmpty()) {
        continue;
      }
      final Territory landedTerritory =
          TransportTracker.getTerritoryTransportHasUnloadedTo(transport);
      if (landedTerritory == null) {
        throw new IllegalStateException("not unloaded?:" + units);
      }
      remove(lost, bridge, landedTerritory, false);
    }
  }

  private static void removeFromDependents(
      final Collection<Unit> units,
      final IDelegateBridge bridge,
      final Collection<IBattle> dependents) {
    for (final IBattle dependent : dependents) {
      dependent.unitsLostInPrecedingBattle(units, bridge, false);
    }
  }

  @Override
  public void fight(final IDelegateBridge bridge) {
    removeUnitsThatNoLongerExist();
    removeDisabledUnits();
    if (stack.isExecuting()) {
      final IDisplay display = bridge.getDisplayChannelBroadcaster();
      display.showBattle(
          battleId,
          battleSite,
          getBattleTitle(),
          removeNonCombatants(attackingUnits, defendingUnits, true, false),
          removeNonCombatants(defendingUnits, attackingUnits, false, false),
          killed,
          attackingWaitingToDie,
          defendingWaitingToDie,
          dependentUnits,
          attacker,
          defender,
          false,
          getBattleType(),
          List.of());
      display.listBattleSteps(battleId, stepStrings);
      stack.execute(bridge);
      return;
    }
    bridge.getHistoryWriter().startEvent("Battle in " + battleSite, battleSite);
    removeAirNoLongerInTerritory();
    markAttackingTransports(bridge);
    writeUnitsToHistory(bridge);
    if (CollectionUtils.getMatches(attackingUnits, Matches.unitIsNotInfrastructure()).isEmpty()) {
      endBattle(WhoWon.DEFENDER, bridge);
      return;
    }
    if (CollectionUtils.getMatches(defendingUnits, Matches.unitIsNotInfrastructure()).isEmpty()) {
      endBattle(WhoWon.ATTACKER, bridge);
      return;
    }
    addDependentUnits(TransportTracker.transporting(defendingUnits));
    addDependentUnits(TransportTracker.transporting(attackingUnits));
    stepStrings = determineStepStrings();
    final IDisplay display = bridge.getDisplayChannelBroadcaster();
    display.showBattle(
        battleId,
        battleSite,
        getBattleTitle(),
        removeNonCombatants(attackingUnits, defendingUnits, true, false),
        removeNonCombatants(defendingUnits, attackingUnits, false, false),
        killed,
        attackingWaitingToDie,
        defendingWaitingToDie,
        dependentUnits,
        attacker,
        defender,
        false,
        getBattleType(),
        List.of());
    display.listBattleSteps(battleId, stepStrings);
    if (!headless) {
      // take the casualties with least movement first
      CasualtySortingUtil.sortPreBattle(attackingUnits);
      CasualtySortingUtil.sortPreBattle(defendingUnits);
      SoundUtils.playBattleType(attacker, attackingUnits, defendingUnits, bridge);
    }
    // push on stack in opposite order of execution
    pushFightLoopOnStack();
    stack.execute(bridge);
  }

  private String getBattleTitle() {
    return attacker.getName() + " attack " + defender.getName() + " in " + battleSite.getName();
  }

  private void removeDisabledUnits() {
    defendingUnits = CollectionUtils.getMatches(defendingUnits, Matches.unitIsNotDisabled());
    attackingUnits = CollectionUtils.getMatches(attackingUnits, Matches.unitIsNotDisabled());
  }

  private void removeAirNoLongerInTerritory() {
    if (headless) {
      return;
    }
    // remove any air units that were once in this attack, but have now
    // moved out of the territory this is an inelegant way to handle this bug
    final Predicate<Unit> airNotInTerritory = Matches.unitIsInTerritory(battleSite).negate();
    attackingUnits.removeAll(CollectionUtils.getMatches(attackingUnits, airNotInTerritory));
  }

  private void markAttackingTransports(final IDelegateBridge bridge) {
    if (headless) {
      return;
    }
    // If any attacking transports are in the battle, set their status to later restrict
    // load/unload
    final Collection<Unit> transports =
        CollectionUtils.getMatches(
            attackingUnits, Matches.unitCanTransport().and(Matches.unitIsOwnedBy(attacker)));
    if (!transports.isEmpty()) {
      final CompositeChange change = new CompositeChange();
      for (final Unit unit : transports) {
        change.add(ChangeFactory.unitPropertyChange(unit, true, Unit.WAS_IN_COMBAT));
      }
      bridge.addChange(change);
    }
  }

  private void writeUnitsToHistory(final IDelegateBridge bridge) {
    if (headless) {
      return;
    }
    final Set<GamePlayer> playersWithUnits = battleSite.getUnitCollection().getPlayersWithUnits();
    final var relationshipTracker = gameData.getRelationshipTracker();

    final Collection<GamePlayer> attackers =
        findAllies(playersWithUnits, attacker, relationshipTracker);
    addPlayerCombatHistoryText(attackers, attackingUnits, true, bridge.getHistoryWriter());
    final Collection<GamePlayer> defenders =
        findAllies(playersWithUnits, defender, relationshipTracker);
    addPlayerCombatHistoryText(defenders, defendingUnits, false, bridge.getHistoryWriter());
  }

  private static Collection<GamePlayer> findAllies(
      final Collection<GamePlayer> candidatePlayers,
      final GamePlayer player,
      final RelationshipTracker relationshipTracker) {
    final Collection<GamePlayer> allies = new ArrayList<>();
    for (final GamePlayer current : candidatePlayers) {
      if (current.equals(player) || relationshipTracker.isAllied(player, current)) {
        allies.add(current);
      }
    }
    return allies;
  }

  private void addPlayerCombatHistoryText(
      final Collection<GamePlayer> players,
      final Collection<Unit> units,
      final boolean attacking,
      final IDelegateHistoryWriter historyWriter) {
    final StringBuilder sb = new StringBuilder();
    final Collection<Unit> allUnits = new ArrayList<>();
    for (final GamePlayer current : players) {
      if (sb.length() > 0) {
        sb.append("; ");
      }
      final Collection<Unit> filteredUnits =
          CollectionUtils.getMatches(units, Matches.unitIsOwnedBy(current));
      final String verb =
          (!attacking ? "defend" : current.equals(attacker) ? "attack" : "loiter and taunt");
      sb.append(current.getName()).append(" ").append(verb);
      if (!filteredUnits.isEmpty()) {
        sb.append(" with ").append(MyFormatter.unitsToTextNoOwner(filteredUnits));
      }
      allUnits.addAll(filteredUnits);
    }
    if (!allUnits.isEmpty()) {
      historyWriter.addChildToEvent(sb.toString(), allUnits);
    }
  }

  @VisibleForTesting
  public List<String> determineStepStrings() {
    return BattleSteps.builder().battleState(this).battleActions(this).build().get();
  }

  @Override
  public Collection<Territory> getAttackerRetreatTerritories() {
    // TODO: when attacking with paratroopers (air + carried land), there are several bugs in
    // retreating.
    // TODO: air should always be able to retreat. paratroopers can only retreat if there are other
    // non-paratrooper non-amphibious land units.

    // If attacker is all planes, just return collection of current territory
    if (headless
        || (!attackingUnits.isEmpty() && attackingUnits.stream().allMatch(Matches.unitIsAir()))
        || Properties.getRetreatingUnitsRemainInPlace(gameData.getProperties())) {
      return Set.of(battleSite);
    }
    // its possible that a sub retreated to a territory we came from, if so we can no longer retreat
    // there
    // or if we are moving out of a territory containing enemy units, we cannot retreat back there
    final Predicate<Unit> enemyUnitsThatPreventRetreat =
        PredicateBuilder.of(Matches.enemyUnit(attacker, gameData))
            .and(Matches.unitIsNotInfrastructure())
            .and(Matches.unitIsBeingTransported().negate())
            .and(Matches.unitIsSubmerged().negate())
            .and(Matches.unitCanBeMovedThroughByEnemies().negate())
            .andIf(
                Properties.getIgnoreTransportInMovement(gameData.getProperties()),
                Matches.unitIsNotTransportButCouldBeCombatTransport())
            .build();
    Collection<Territory> possible =
        CollectionUtils.getMatches(
            attackingFrom,
            Matches.territoryHasUnitsThatMatch(enemyUnitsThatPreventRetreat).negate());
    // In WW2V2 and WW2V3 we need to filter out territories where only planes
    // came from since planes cannot define retreat paths
    if (Properties.getWW2V2(gameData.getProperties())
        || Properties.getWW2V3(gameData.getProperties())) {
      possible =
          CollectionUtils.getMatches(
              possible,
              t -> {
                final Collection<Unit> units = attackingFromMap.get(t);
                return units.isEmpty() || !units.stream().allMatch(Matches.unitIsAir());
              });
    }

    // the air unit may have come from a conquered or enemy territory, don't allow retreating
    final Predicate<Territory> conqueuredOrEnemy =
        Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassableOrRestricted(attacker, gameData)
            .or(Matches.territoryIsWater().and(Matches.territoryWasFoughtOver(battleTracker)));
    possible.removeAll(CollectionUtils.getMatches(possible, conqueuredOrEnemy));

    // the battle site is in the attacking from if sea units are fighting a submerged sub
    possible.remove(battleSite);
    if (attackingUnits.stream().anyMatch(Matches.unitIsLand()) && !battleSite.isWater()) {
      possible = CollectionUtils.getMatches(possible, Matches.territoryIsLand());
    }
    if (attackingUnits.stream().anyMatch(Matches.unitIsSea())) {
      possible = CollectionUtils.getMatches(possible, Matches.territoryIsWater());
    }
    return possible;
  }

  private void pushFightLoopOnStack() {
    if (isOver) {
      return;
    }
    final List<IExecutable> steps = getBattleExecutables();
    // add in the reverse order we create them
    Collections.reverse(steps);
    for (final IExecutable step : steps) {
      stack.push(step);
    }
  }

  /**
   * The code here is a bit odd to read but basically, we need to break the code into separate
   * atomic pieces. If there is a network error, or some other unfortunate event, then we need to
   * keep track of what pieces we have executed, and what is left to do. Each atomic step is in its
   * own IExecutable with the definition of atomic is that either:
   *
   * <ol>
   *   <li>The code does not call to an IDisplay, IPlayer, or IRandomSource
   *   <li>If the code calls to an IDisplay, IPlayer, IRandomSource, and an exception is called from
   *       one of those methods, the exception will be propagated out of execute() and the execute
   *       method can be called again.
   * </ol>
   *
   * It is allowed for an IExecutable to add other IExecutables to the stack. If you read the code
   * in linear order, ignore wrapping stuff in anonymous IExecutables, then the code can be read as
   * it will execute. The steps are added to the stack and then reversed at the end.
   */
  @VisibleForTesting
  public List<IExecutable> getBattleExecutables() {
    final List<IExecutable> steps =
        BattleStep.getAll(this, this).stream()
            .sorted(Comparator.comparing(BattleStep::getOrder))
            .collect(Collectors.toList());

    addRoundResetStep(steps);
    return steps;
  }

  /*
   *
   * <p> Save Game Compatibility Note:
   *
   * <p>Because of saved game compatibility issues, the original steps are left behind as inner
   * anonymous classes. The reason for this is that their class name is defined by the order in
   * which they are defined. As an example, the first inner anonymous class is MustFightBattle$0 and
   * the next one is MustFightBattle$1 and so on. When a saved game is deserialized, it will match
   * the step by the class name and so if the order of inner anonymous classes change, it will not
   * deserialize. So even though these old steps aren't being added to the steps array, they are
   * still needed. They can be safely removed once save compatibility can be broken.
   */
  {
    // Removed in 2.0
    new IExecutable() {
      private static final long serialVersionUID = 3802352588499530533L;

      @Override
      @RemoveOnNextMajorRelease
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        final BattleStep offensiveAaStep =
            new OffensiveAaFire(MustFightBattle.this, MustFightBattle.this);
        offensiveAaStep.execute(stack, bridge);
      }
    };
    // Removed in 2.0
    new IExecutable() {
      private static final long serialVersionUID = -1370090785540214199L;

      @Override
      @RemoveOnNextMajorRelease
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        final BattleStep defensiveAaStep =
            new DefensiveAaFire(MustFightBattle.this, MustFightBattle.this);
        defensiveAaStep.execute(stack, bridge);
      }
    };
    // Removed in 2.1
    new IExecutable() {
      private static final long serialVersionUID = 8762796262264296436L;

      @Override
      @RemoveOnNextMajorRelease
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        final BattleStep clearAaCasualties =
            new ClearAaCasualties(MustFightBattle.this, MustFightBattle.this);
        clearAaCasualties.execute(stack, bridge);
      }
    };
    // Removed in 2.1
    new IExecutable() {
      private static final long serialVersionUID = 2781652892457063082L;

      @Override
      @RemoveOnNextMajorRelease
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        final BattleStep removeNonCombatants =
            new RemoveNonCombatants(MustFightBattle.this, MustFightBattle.this);
        removeNonCombatants.execute(stack, bridge);
      }
    };
    // Removed in 2.0
    new IExecutable() {
      private static final long serialVersionUID = -2255284529092427441L;

      @Override
      @RemoveOnNextMajorRelease
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        final BattleStep navalBombardment =
            new NavalBombardment(MustFightBattle.this, MustFightBattle.this);
        navalBombardment.execute(stack, bridge);
      }
    };
    // Removed in 2.1
    new IExecutable() {
      private static final long serialVersionUID = 3389635558184415797L;

      @Override
      @RemoveOnNextMajorRelease
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        final BattleStep removeNonCombatants =
            new RemoveNonCombatants(MustFightBattle.this, MustFightBattle.this);
        removeNonCombatants.execute(stack, bridge);
      }
    };
    // Removed in 2.1
    new IExecutable() {
      private static final long serialVersionUID = 7193353768857658286L;

      @Override
      @RemoveOnNextMajorRelease
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        final BattleStep landParatroopers =
            new LandParatroopers(MustFightBattle.this, MustFightBattle.this);
        landParatroopers.execute(stack, bridge);
      }
    };
    // Removed in 2.1
    new IExecutable() {
      private static final long serialVersionUID = -6676316363537467594L;

      @Override
      @RemoveOnNextMajorRelease
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        final BattleStep markNoMovementLeft =
            new MarkNoMovementLeft(MustFightBattle.this, MustFightBattle.this);
        markNoMovementLeft.execute(stack, bridge);
      }
    };
    new IExecutable() {
      private static final long serialVersionUID = 6775880082912594489L;

      @Override
      @RemoveOnNextMajorRelease
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        final BattleStep offensiveSubsRetreat =
            new OffensiveSubsRetreat(MustFightBattle.this, MustFightBattle.this);
        if (offensiveSubsRetreat.getOrder() == SUB_OFFENSIVE_RETREAT_BEFORE_BATTLE) {
          offensiveSubsRetreat.execute(stack, bridge);
        }
      }
    };
    new IExecutable() {
      private static final long serialVersionUID = 7056448091800764539L;

      @Override
      @RemoveOnNextMajorRelease
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        final BattleStep defensiveSubsRetreat =
            new DefensiveSubsRetreat(MustFightBattle.this, MustFightBattle.this);
        if (defensiveSubsRetreat.getOrder() == SUB_DEFENSIVE_RETREAT_BEFORE_BATTLE) {
          defensiveSubsRetreat.execute(stack, bridge);
        }
      }
    };
    new IExecutable() {
      private static final long serialVersionUID = 99989L;

      @Override
      @RemoveOnNextMajorRelease
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        new RemoveUnprotectedUnits(MustFightBattle.this, MustFightBattle.this)
            .execute(stack, bridge);
      }
    };
    new IExecutable() {
      private static final long serialVersionUID = 99990L;

      @Override
      @RemoveOnNextMajorRelease
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        final BattleStep submergeSubsVsOnlyAir =
            new SubmergeSubsVsOnlyAirStep(MustFightBattle.this, MustFightBattle.this);
        submergeSubsVsOnlyAir.execute(stack, bridge);
      }
    };
    new IExecutable() {
      private static final long serialVersionUID = 99992L;

      @Override
      @RemoveOnNextMajorRelease
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        new DefensiveFirstStrike(MustFightBattle.this, MustFightBattle.this, ReturnFire.NONE)
            .execute(stack, bridge);
      }
    };
    // these two variables are needed for save compatibility
    // the value of the variables aren't important now, but they must
    // be defined in the same scope as the IExecutables so that when
    // the save is loaded, it will correctly populate the saved value
    // of these variables.
    @RemoveOnNextMajorRelease final ReturnFire returnFireAgainstAttackingSubs = ReturnFire.ALL;
    @RemoveOnNextMajorRelease final ReturnFire returnFireAgainstDefendingSubs = ReturnFire.ALL;
    new IExecutable() {
      private static final long serialVersionUID = 99991L;

      @Override
      @RemoveOnNextMajorRelease
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        new OffensiveFirstStrike(
                MustFightBattle.this,
                MustFightBattle.this,
                // can either be NONE, SUBS, or ALL
                returnFireAgainstAttackingSubs)
            .execute(stack, bridge);
      }
    };
    new IExecutable() {
      private static final long serialVersionUID = 99992L;

      @Override
      @RemoveOnNextMajorRelease
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        new DefensiveFirstStrike(
                MustFightBattle.this,
                MustFightBattle.this,
                // can either be SUBS or (if WW2V2) ALL
                returnFireAgainstDefendingSubs)
            .execute(stack, bridge);
      }
    };
    new IExecutable() {
      private static final long serialVersionUID = -7634700553071456768L;

      @Override
      @RemoveOnNextMajorRelease
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        new RemoveFirstStrikeSuicide(MustFightBattle.this, MustFightBattle.this)
            .execute(stack, bridge);
      }
    };
    new IExecutable() {
      private static final long serialVersionUID = 99994L;

      @Override
      @RemoveOnNextMajorRelease
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        new OffensiveGeneral(MustFightBattle.this, MustFightBattle.this).execute(stack, bridge);
      }
    };
    new IExecutable() {
      private static final long serialVersionUID = 999921L;

      @Override
      @RemoveOnNextMajorRelease
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        new DefensiveFirstStrike(MustFightBattle.this, MustFightBattle.this, ReturnFire.ALL)
            .execute(stack, bridge);
      }
    };
    new IExecutable() {
      private static final long serialVersionUID = 1560702114917865290L;

      @Override
      @RemoveOnNextMajorRelease
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        new DefensiveGeneral(MustFightBattle.this, MustFightBattle.this).execute(stack, bridge);
      }
    };
    new IExecutable() {
      private static final long serialVersionUID = 8611067962952500496L;

      @Override
      @RemoveOnNextMajorRelease
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        new ClearGeneralCasualties(MustFightBattle.this, MustFightBattle.this)
            .execute(stack, bridge);
      }
    };
    new IExecutable() {
      private static final long serialVersionUID = 6387198382888361848L;

      @Override
      @RemoveOnNextMajorRelease
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        new RemoveGeneralSuicide(MustFightBattle.this, MustFightBattle.this).execute(stack, bridge);
      }
    };
    new IExecutable() {
      private static final long serialVersionUID = 5259103822937067667L;

      @Override
      @RemoveOnNextMajorRelease
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        new CheckGeneralBattleEndOld(MustFightBattle.this, MustFightBattle.this)
            .execute(stack, bridge);
      }
    };
    new IExecutable() {
      private static final long serialVersionUID = 6775880082912594489L;

      @Override
      @RemoveOnNextMajorRelease
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        final BattleStep offensiveSubsRetreat =
            new OffensiveSubsRetreat(MustFightBattle.this, MustFightBattle.this);
        if (offensiveSubsRetreat.getOrder() == SUB_OFFENSIVE_RETREAT_AFTER_BATTLE) {
          offensiveSubsRetreat.execute(stack, bridge);
        }
      }
    };
    new IExecutable() {
      private static final long serialVersionUID = -1150863964807721395L;

      @Override
      @RemoveOnNextMajorRelease
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        // Intentionally left blank
        // Old saves will fall through to the IExecutable that instantiates
        // OffensiveGeneralRetreat which does the work that previously was here
      }
    };
    new IExecutable() {
      private static final long serialVersionUID = -1150863964807721395L;

      @Override
      @RemoveOnNextMajorRelease
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        // Intentionally left blank
        // Old saves will fall through to the IExecutable that instantiates
        // OffensiveGeneralRetreat which does the work that previously was here
      }
    };
    new IExecutable() {
      private static final long serialVersionUID = 669349383898975048L;

      @Override
      @RemoveOnNextMajorRelease
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        new OffensiveGeneralRetreat(MustFightBattle.this, MustFightBattle.this)
            .execute(stack, bridge);
      }
    };
    new IExecutable() {
      private static final long serialVersionUID = -1544916305666912480L;

      @Override
      @RemoveOnNextMajorRelease
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        final BattleStep defensiveSubsRetreat =
            new DefensiveSubsRetreat(MustFightBattle.this, MustFightBattle.this);
        if (defensiveSubsRetreat.getOrder() == SUB_DEFENSIVE_RETREAT_AFTER_BATTLE) {
          defensiveSubsRetreat.execute(stack, bridge);
        }
      }
    };
  }

  @Override
  public void removeNonCombatants(final IDelegateBridge bridge) {
    final List<Unit> notRemovedDefending =
        removeNonCombatants(defendingUnits, attackingUnits, false, true);
    final List<Unit> notRemovedAttacking =
        removeNonCombatants(attackingUnits, defendingUnits, true, true);
    final Collection<Unit> toRemoveDefending =
        CollectionUtils.difference(defendingUnits, notRemovedDefending);
    final Collection<Unit> toRemoveAttacking =
        CollectionUtils.difference(attackingUnits, notRemovedAttacking);
    defendingUnits = notRemovedDefending;
    attackingUnits = notRemovedAttacking;
    if (!headless) {
      if (!toRemoveDefending.isEmpty()) {
        bridge
            .getDisplayChannelBroadcaster()
            .changedUnitsNotification(battleId, defender, toRemoveDefending, null, null);
      }
      if (!toRemoveAttacking.isEmpty()) {
        bridge
            .getDisplayChannelBroadcaster()
            .changedUnitsNotification(battleId, attacker, toRemoveAttacking, null, null);
      }
    }
  }

  /**
   * Returns only the relevant non-combatant units present in the specified collection.
   *
   * @return a collection containing all the combatants in units non-combatants include such things
   *     as factories, aa guns, land units in a water battle.
   */
  private List<Unit> removeNonCombatants(
      final Collection<Unit> units,
      final Collection<Unit> enemyUnits,
      final boolean attacking,
      final boolean removeForNextRound) {
    final List<Unit> unitList = new ArrayList<>(units);
    if (battleSite.isWater()) {
      unitList.removeAll(CollectionUtils.getMatches(unitList, Matches.unitIsLand()));
    }
    // still allow infrastructure type units that can provide support have combat abilities
    // remove infrastructure units that can't take part in combat (air/naval bases, etc...)
    unitList.removeAll(
        CollectionUtils.getMatches(
            unitList,
            Matches.unitCanBeInBattle(
                    attacking,
                    !battleSite.isWater(),
                    (removeForNextRound ? round + 1 : round),
                    false,
                    enemyUnits.stream().map(Unit::getType).collect(Collectors.toSet()))
                .negate()));
    // remove capturableOnEntering units (veqryn)
    unitList.removeAll(
        CollectionUtils.getMatches(
            unitList,
            Matches.unitCanBeCapturedOnEnteringToInThisTerritory(
                attacker, battleSite, gameData.getProperties())));
    // remove any allied air units that are stuck on damaged carriers (veqryn)
    unitList.removeAll(
        CollectionUtils.getMatches(
            unitList,
            Matches.unitIsBeingTransported()
                .and(Matches.unitIsAir())
                .and(Matches.unitCanLandOnCarrier())));
    // remove any units that were in air combat (veqryn)
    unitList.removeAll(CollectionUtils.getMatches(unitList, Matches.unitWasInAirBattle()));
    return unitList;
  }

  private void addRoundResetStep(final List<IExecutable> steps) {
    final IExecutable loop =
        new IExecutable() {
          private static final long serialVersionUID = 3118458517320468680L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            pushFightLoopOnStack();
          }
        };
    steps.add(
        new IExecutable() {
          private static final long serialVersionUID = -3993599528368570254L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            if (!isOver) {
              round++;
              if (round > MAX_ROUNDS) {
                // the battle appears to be in an infinite loop
                throw new IllegalStateException(
                    "Round 10,000 reached in a battle. Something must be wrong."
                        + " Please report this to TripleA.\n"
                        + " Attacking unit types: "
                        + attackingUnits.stream()
                            .map(Unit::getType)
                            .collect(Collectors.toSet())
                            .stream()
                            .map(UnitType::getName)
                            .collect(Collectors.joining(","))
                        + ", Defending unit types: "
                        + defendingUnits.stream()
                            .map(Unit::getType)
                            .collect(Collectors.toSet())
                            .stream()
                            .map(UnitType::getName)
                            .collect(Collectors.joining(",")));
              }
              stepStrings = determineStepStrings();
              final IDisplay display = bridge.getDisplayChannelBroadcaster();
              display.listBattleSteps(battleId, stepStrings);
              // continue fighting the recursive steps
              // this should always be the base of the stack
              // when we execute the loop, it will populate the stack with the battle steps
              if (!MustFightBattle.this.stack.isEmpty()) {
                throw new IllegalStateException("Stack not empty:" + MustFightBattle.this.stack);
              }
              MustFightBattle.this.stack.push(loop);
            }
          }
        });
  }

  private void defenderWins(final IDelegateBridge bridge) {
    endBattle(bridge);
    whoWon = WhoWon.DEFENDER;
    bridge.getDisplayChannelBroadcaster().battleEnd(battleId, defender.getName() + " win");
    if (Properties.getAbandonedTerritoriesMayBeTakenOverImmediately(gameData.getProperties())) {
      if (CollectionUtils.getMatches(defendingUnits, Matches.unitIsNotInfrastructure()).size()
          == 0) {
        final List<Unit> allyOfAttackerUnits =
            battleSite.getUnitCollection().getMatches(Matches.unitIsNotInfrastructure());
        if (!allyOfAttackerUnits.isEmpty()) {
          final GamePlayer abandonedToPlayer =
              AbstractBattle.findPlayerWithMostUnits(allyOfAttackerUnits);
          bridge
              .getHistoryWriter()
              .addChildToEvent(
                  abandonedToPlayer.getName()
                      + " takes over "
                      + battleSite.getName()
                      + " as there are no defenders left",
                  allyOfAttackerUnits);
          // should we create a new battle records to show the ally capturing the territory (in the
          // case where they
          // didn't already own/allied it)?
          battleTracker.takeOver(battleSite, abandonedToPlayer, bridge, null, allyOfAttackerUnits);
        }
      } else {
        // should we create a new battle records to show the defender capturing the territory (in
        // the case where they
        // didn't already own/allied it)?
        battleTracker.takeOver(battleSite, defender, bridge, null, defendingUnits);
      }
    }
    bridge
        .getHistoryWriter()
        .addChildToEvent(defender.getName() + " win", new ArrayList<>(defendingUnits));
    battleResultDescription = BattleRecord.BattleResultDescription.LOST;
    showCasualties(bridge);
    if (!headless) {
      battleTracker
          .getBattleRecords()
          .addResultToBattle(
              attacker,
              battleId,
              defender,
              attackerLostTuv,
              defenderLostTuv,
              battleResultDescription,
              new BattleResults(this, gameData));
    }
    checkDefendingPlanesCanLand();
    BattleTracker.captureOrDestroyUnits(battleSite, defender, defender, bridge, null);
    if (!headless) {
      bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_FAILURE, attacker);
    }
  }

  private void nobodyWins(final IDelegateBridge bridge) {
    endBattle(bridge);
    whoWon = WhoWon.DRAW;
    bridge.getDisplayChannelBroadcaster().battleEnd(battleId, "Stalemate");
    bridge
        .getHistoryWriter()
        .addChildToEvent(defender.getName() + " and " + attacker.getName() + " reach a stalemate");
    battleResultDescription = BattleRecord.BattleResultDescription.STALEMATE;
    showCasualties(bridge);
    if (!headless) {
      battleTracker
          .getBattleRecords()
          .addResultToBattle(
              attacker,
              battleId,
              defender,
              attackerLostTuv,
              defenderLostTuv,
              battleResultDescription,
              new BattleResults(this, gameData));
      bridge
          .getSoundChannelBroadcaster()
          .playSoundForAll(SoundPath.CLIP_BATTLE_STALEMATE, attacker);
    }
    checkDefendingPlanesCanLand();
  }

  private void attackerWins(final IDelegateBridge bridge) {
    endBattle(bridge);
    whoWon = WhoWon.ATTACKER;
    bridge.getDisplayChannelBroadcaster().battleEnd(battleId, attacker.getName() + " win");
    if (headless) {
      return;
    }

    // do we need to change ownership
    if (attackingUnits.stream().anyMatch(Matches.unitIsNotAir())) {
      if (Matches.isTerritoryEnemyAndNotUnownedWater(attacker, gameData).test(battleSite)) {
        battleTracker.addToConquered(battleSite);
      }
      battleTracker.takeOver(battleSite, attacker, bridge, null, attackingUnits);
      battleResultDescription = BattleRecord.BattleResultDescription.CONQUERED;
    } else {
      battleResultDescription = BattleRecord.BattleResultDescription.WON_WITHOUT_CONQUERING;
    }

    clearTransportedBy(bridge);
    bridge
        .getHistoryWriter()
        .addChildToEvent(attacker.getName() + " win", new ArrayList<>(attackingUnits));
    showCasualties(bridge);
    battleTracker
        .getBattleRecords()
        .addResultToBattle(
            attacker,
            battleId,
            defender,
            attackerLostTuv,
            defenderLostTuv,
            battleResultDescription,
            new BattleResults(this, gameData));
    SoundUtils.playAttackerWinsAirOrSea(attacker, attackingUnits, battleSite.isWater(), bridge);
  }

  /**
   * The defender has won, but there may be defending fighters that can't stay in the sea zone due
   * to insufficient carriers.
   */
  private void checkDefendingPlanesCanLand() {
    if (headless) {
      return;
    }
    // not water, not relevant.
    if (!battleSite.isWater()) {
      return;
    }
    final Predicate<Unit> unscrambledAir =
        Matches.unitIsAir().and(Matches.unitWasScrambled().negate());
    final Collection<Unit> defendingAir =
        CollectionUtils.getMatches(defendingUnits, unscrambledAir);
    if (defendingAir.isEmpty()) {
      return;
    }
    int carrierCost = AirMovementValidator.carrierCost(defendingAir);
    final int carrierCapacity = AirMovementValidator.carrierCapacity(defendingUnits, battleSite);
    // add dependent air to carrier cost
    carrierCost +=
        AirMovementValidator.carrierCost(
            CollectionUtils.getMatches(getDependentUnits(defendingUnits), unscrambledAir));
    // all planes can land, exit
    if (carrierCapacity >= carrierCost) {
      return;
    }
    // find out what we must remove by removing all the air that can land on carriers from
    // defendingAir
    carrierCost = 0;
    carrierCost +=
        AirMovementValidator.carrierCost(
            CollectionUtils.getMatches(getDependentUnits(defendingUnits), unscrambledAir));
    for (final Unit currentUnit : new ArrayList<>(defendingAir)) {
      if (!Matches.unitCanLandOnCarrier().test(currentUnit)) {
        defendingAir.remove(currentUnit);
        continue;
      }
      carrierCost += UnitAttachment.get(currentUnit.getType()).getCarrierCost();
      if (carrierCapacity >= carrierCost) {
        defendingAir.remove(currentUnit);
      }
    }
    // Moved this choosing to after all battles, as we legally should be able to land in a territory
    // if we win there.
    battleTracker.addToDefendingAirThatCanNotLand(defendingAir, battleSite);
  }

  private void showCasualties(final IDelegateBridge bridge) {
    if (killed.isEmpty()) {
      return;
    }
    // a handy summary of all the units killed
    IntegerMap<UnitType> costs = TuvUtils.getCostsForTuv(attacker, gameData);
    final int tuvLostAttacker = TuvUtils.getTuv(killed, attacker, costs, gameData);
    costs = TuvUtils.getCostsForTuv(defender, gameData);
    final int tuvLostDefender = TuvUtils.getTuv(killed, defender, costs, gameData);
    final int tuvChange = tuvLostDefender - tuvLostAttacker;
    bridge
        .getHistoryWriter()
        .addChildToEvent(
            "Battle casualty summary: Battle score (TUV change) for attacker is " + tuvChange,
            new ArrayList<>(killed));
    attackerLostTuv += tuvLostAttacker;
    defenderLostTuv += tuvLostDefender;
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
        + " from:"
        + attackingFrom
        + " attacking with: "
        + attackingUnits;
  }
}
