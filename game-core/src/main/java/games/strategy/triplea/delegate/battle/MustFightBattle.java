package games.strategy.triplea.delegate.battle;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.AirMovementValidator;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.IExecutable;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.data.BattleRecord;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.util.TuvUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.java.Log;
import org.triplea.java.PredicateBuilder;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.sound.SoundPath;
import org.triplea.sound.SoundUtils;
import org.triplea.util.Tuple;

/** Handles logic for battles in which fighting actually occurs. */
@Log
public class MustFightBattle extends DependentBattle implements BattleStepStrings {

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

  /**
   * An action representing attacking subs firing during a battle.
   *
   * <p>NOTE: This type exists solely for tests to interrogate the execution stack looking for an
   * action of this type.
   */
  public abstract static class FirstStrikeAttackersFire implements IExecutable {
    private static final long serialVersionUID = 4872551667582174716L;
  }

  /**
   * An action representing defending subs firing during a battle.
   *
   * <p>NOTE: This type exists solely for tests to interrogate the execution stack looking for an
   * action of this type.
   */
  public abstract static class FirstStrikeDefendersFire implements IExecutable {
    private static final long serialVersionUID = 3768066729336520095L;
  }

  private static final long serialVersionUID = 5879502298361231540L;

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
  private List<String> stepStrings;
  private List<Unit> defendingAa;
  private List<Unit> offensiveAa;
  private List<String> defendingAaTypes;
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
            ? Properties.getSeaBattleRounds(data)
            : Properties.getLandBattleRounds(data);
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
      final Collection<Unit> amphibious,
      final GamePlayer defender,
      final Collection<TerritoryEffect> territoryEffects) {
    defendingUnits = new ArrayList<>(defending);
    attackingUnits = new ArrayList<>(attacking);
    bombardingUnits = new ArrayList<>(bombarding);
    amphibiousLandAttackers = new ArrayList<>(amphibious);
    isAmphibious = !amphibiousLandAttackers.isEmpty();
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
      if (!route.getEnd().isWater() && units.stream().anyMatch(Matches.unitIsLand())) {
        amphibiousLandAttackers.removeAll(CollectionUtils.getMatches(units, Matches.unitIsLand()));
      }
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
        isWW2V2() ? CollectionUtils.getMatches(units, ownedBy) : units;
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
      amphibiousLandAttackers.addAll(
          CollectionUtils.getMatches(attackingUnits, Matches.unitIsLand()));
      isAmphibious = true;
    }
    final Map<Unit, Collection<Unit>> dependencies =
        new HashMap<>(TransportTracker.transporting(units));
    if (!Properties.getAlliedAirIndependent(gameData)) {
      dependencies.putAll(MoveValidator.carrierMustMoveWith(units, units, gameData, attacker));
      for (final Unit carrier : dependencies.keySet()) {
        final UnitAttachment ua = UnitAttachment.get(carrier.getType());
        if (ua.getCarrierCapacity() == -1) {
          continue;
        }
        final Collection<Unit> fighters = dependencies.get(carrier);
        // Dependencies count both land and air units. Land units could be allied or owned, while
        // air is just allied
        // since owned already launched at beginning of turn
        fighters.retainAll(CollectionUtils.getMatches(fighters, Matches.unitIsAir()));
        for (final Unit fighter : fighters) {
          // Set transportedBy for fighter
          change.add(
              ChangeFactory.unitPropertyChange(fighter, carrier, TripleAUnit.TRANSPORTED_BY));
        }
        // remove transported fighters from battle display
        this.attackingUnits.removeAll(fighters);
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
    if (MoveValidator.onlyIgnoredUnitsOnPath(route, attacker, false)) {
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

  @Override
  public List<Unit> getRemainingAttackingUnits() {
    final List<Unit> remaining = new ArrayList<>(attackingUnitsRetreated);
    final Collection<Unit> unitsLeftInTerritory = battleSite.getUnits();
    unitsLeftInTerritory.removeAll(killed);
    remaining.addAll(
        CollectionUtils.getMatches(
            unitsLeftInTerritory,
            getWhoWon() != WhoWon.DEFENDER
                ? Matches.unitOwnedBy(attacker)
                : Matches.unitOwnedBy(attacker)
                    .and(Matches.unitIsAir())
                    .and(Matches.unitIsNotInfrastructure())));
    return remaining;
  }

  @Override
  public List<Unit> getRemainingDefendingUnits() {
    final List<Unit> remaining = new ArrayList<>(defendingUnitsRetreated);
    if (getWhoWon() != WhoWon.ATTACKER || attackingUnits.stream().allMatch(Matches.unitIsAir())) {
      final Collection<Unit> unitsLeftInTerritory = battleSite.getUnits();
      unitsLeftInTerritory.removeAll(killed);
      remaining.addAll(
          CollectionUtils.getMatches(unitsLeftInTerritory, Matches.enemyUnit(attacker, gameData)));
    }
    return remaining;
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
    amphibiousLandAttackers.removeAll(lost);
    if (amphibiousLandAttackers.isEmpty()) {
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

  private void clearWaitingToDieAndDamagedChangesInto(final IDelegateBridge bridge) {
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

  private void damagedChangeInto(
      final GamePlayer player,
      final List<Unit> units,
      final List<Unit> killedUnits,
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
              TripleAUnit.translateAttributesToOtherUnits(unit, toAdd, battleSite);
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

  void removeSuicideOnHitCasualties(
      final Collection<Unit> firingUnits,
      final int hits,
      final boolean defender,
      final IDelegateBridge bridge) {
    if (firingUnits.stream().anyMatch(Matches.unitIsSuicideOnHit()) && hits > 0) {
      final List<Unit> units = firingUnits.stream().limit(hits).collect(Collectors.toList());
      bridge
          .getDisplayChannelBroadcaster()
          .deadUnitNotification(
              battleId, defender ? this.defender : attacker, units, dependentUnits);
      remove(units, bridge, battleSite, defender);
    }
  }

  void removeCasualties(
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

  private void remove(
      final Collection<Unit> killed,
      final IDelegateBridge bridge,
      final Territory battleSite,
      final Boolean defenderDying) {
    if (killed.isEmpty()) {
      return;
    }
    final Collection<Unit> dependent = getDependentUnits(killed);
    killed.addAll(dependent);

    // Set max damage for any units that will change into another unit
    final IntegerMap<Unit> lethallyDamagedMap = new IntegerMap<>();
    for (final Unit unit :
        CollectionUtils.getMatches(killed, Matches.unitAtMaxHitPointDamageChangesInto())) {
      lethallyDamagedMap.put(unit, unit.getUnitAttachment().getHitPoints());
    }
    final Change lethallyDamagedChange = ChangeFactory.unitsHit(lethallyDamagedMap);
    bridge.addChange(lethallyDamagedChange);

    // Remove units
    final Change killedChange = ChangeFactory.removeUnits(battleSite, killed);
    this.killed.addAll(killed);
    killedDuringCurrentRound.addAll(killed);
    final String transcriptText =
        MyFormatter.unitsToText(killed) + " lost in " + battleSite.getName();
    bridge.getHistoryWriter().addChildToEvent(transcriptText, new ArrayList<>(killed));
    bridge.addChange(killedChange);
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
    if (stack.isExecuting()) {
      final IDisplay display = bridge.getDisplayChannelBroadcaster();
      display.showBattle(
          battleId,
          battleSite,
          getBattleTitle(),
          removeNonCombatants(attackingUnits, true, false),
          removeNonCombatants(defendingUnits, false, false),
          killed,
          attackingWaitingToDie,
          defendingWaitingToDie,
          dependentUnits,
          attacker,
          defender,
          isAmphibious(),
          getBattleType(),
          amphibiousLandAttackers);
      display.listBattleSteps(battleId, stepStrings);
      stack.execute(bridge);
      return;
    }
    bridge.getHistoryWriter().startEvent("Battle in " + battleSite, battleSite);
    removeAirNoLongerInTerritory();
    writeUnitsToHistory(bridge);
    if (CollectionUtils.getMatches(attackingUnits, Matches.unitIsNotInfrastructure()).isEmpty()) {
      endBattle(bridge);
      defenderWins(bridge);
      return;
    }
    if (CollectionUtils.getMatches(defendingUnits, Matches.unitIsNotInfrastructure()).isEmpty()) {
      endBattle(bridge);
      attackerWins(bridge);
      return;
    }
    addDependentUnits(TransportTracker.transporting(defendingUnits));
    addDependentUnits(TransportTracker.transporting(attackingUnits));
    updateOffensiveAaUnits();
    updateDefendingAaUnits();
    stepStrings = determineStepStrings(true);
    final IDisplay display = bridge.getDisplayChannelBroadcaster();
    display.showBattle(
        battleId,
        battleSite,
        getBattleTitle(),
        removeNonCombatants(attackingUnits, true, false),
        removeNonCombatants(defendingUnits, false, false),
        killed,
        attackingWaitingToDie,
        defendingWaitingToDie,
        dependentUnits,
        attacker,
        defender,
        isAmphibious(),
        getBattleType(),
        amphibiousLandAttackers);
    display.listBattleSteps(battleId, stepStrings);
    if (!headless) {
      // take the casualties with least movement first
      if (isAmphibious()) {
        CasualtySelector.sortAmphib(attackingUnits, amphibiousLandAttackers);
      } else {
        CasualtySelector.sortPreBattle(attackingUnits);
      }
      CasualtySelector.sortPreBattle(defendingUnits);
      SoundUtils.playBattleType(attacker, attackingUnits, defendingUnits, bridge);
    }
    // push on stack in opposite order of execution
    pushFightLoopOnStack(true);
    stack.execute(bridge);
  }

  private String getBattleTitle() {
    return attacker.getName() + " attack " + defender.getName() + " in " + battleSite.getName();
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

  private void writeUnitsToHistory(final IDelegateBridge bridge) {
    if (headless) {
      return;
    }
    final Set<GamePlayer> playerSet = battleSite.getUnitCollection().getPlayersWithUnits();
    // find all attacking players (unsorted)
    final Collection<GamePlayer> attackers = new ArrayList<>();
    for (final GamePlayer current : playerSet) {
      if (gameData.getRelationshipTracker().isAllied(attacker, current)
          || current.equals(attacker)) {
        attackers.add(current);
      }
    }
    final StringBuilder transcriptText = new StringBuilder();
    // find all attacking units (unsorted)
    final Collection<Unit> allAttackingUnits = new ArrayList<>();
    for (final Iterator<GamePlayer> attackersIter = attackers.iterator();
        attackersIter.hasNext(); ) {
      final GamePlayer current = attackersIter.next();
      final String delim;
      if (attackersIter.hasNext()) {
        delim = "; ";
      } else {
        delim = "";
      }
      final Collection<Unit> attackingUnits =
          CollectionUtils.getMatches(this.attackingUnits, Matches.unitIsOwnedBy(current));
      final String verb = current.equals(attacker) ? "attack" : "loiter and taunt";
      transcriptText
          .append(current.getName())
          .append(" ")
          .append(verb)
          .append(
              attackingUnits.isEmpty()
                  ? ""
                  : " with " + MyFormatter.unitsToTextNoOwner(attackingUnits))
          .append(delim);
      allAttackingUnits.addAll(attackingUnits);
      // If any attacking transports are in the battle, set their status to later restrict
      // load/unload
      if (current.equals(attacker)) {
        final CompositeChange change = new CompositeChange();
        final Collection<Unit> transports =
            CollectionUtils.getMatches(attackingUnits, Matches.unitCanTransport());
        for (final Unit unit : transports) {
          change.add(ChangeFactory.unitPropertyChange(unit, true, TripleAUnit.WAS_IN_COMBAT));
        }
        bridge.addChange(change);
      }
    }
    // write attacking units to history
    if (!attackingUnits.isEmpty()) {
      bridge.getHistoryWriter().addChildToEvent(transcriptText.toString(), allAttackingUnits);
    }
    // find all defending players (unsorted)
    final Collection<GamePlayer> defenders = new ArrayList<>();
    for (final GamePlayer current : playerSet) {
      if (gameData.getRelationshipTracker().isAllied(defender, current)
          || current.equals(defender)) {
        defenders.add(current);
      }
    }
    final StringBuilder transcriptBuilder = new StringBuilder();
    // find all defending units (unsorted)
    final Collection<Unit> allDefendingUnits = new ArrayList<>();
    for (final Iterator<GamePlayer> defendersIter = defenders.iterator();
        defendersIter.hasNext(); ) {
      final GamePlayer current = defendersIter.next();
      final String delim;
      if (defendersIter.hasNext()) {
        delim = "; ";
      } else {
        delim = "";
      }
      final Collection<Unit> defendingUnits =
          CollectionUtils.getMatches(this.defendingUnits, Matches.unitIsOwnedBy(current));
      transcriptBuilder
          .append(current.getName())
          .append(" defend with ")
          .append(MyFormatter.unitsToTextNoOwner(defendingUnits))
          .append(delim);
      allDefendingUnits.addAll(defendingUnits);
    }
    // write defending units to history
    if (!defendingUnits.isEmpty()) {
      bridge.getHistoryWriter().addChildToEvent(transcriptBuilder.toString(), allDefendingUnits);
    }
  }

  private void updateOffensiveAaUnits() {
    final Collection<Unit> canFire = new ArrayList<>(attackingUnits);
    canFire.addAll(attackingWaitingToDie);
    // no airborne targets for offensive aa
    offensiveAa =
        CollectionUtils.getMatches(
            canFire,
            Matches.unitIsAaThatCanFire(
                defendingUnits,
                new HashMap<>(),
                defender,
                Matches.unitIsAaForCombatOnly(),
                round,
                false,
                gameData));
    // comes ordered alphabetically
    offensiveAaTypes = UnitAttachment.getAllOfTypeAas(offensiveAa);
    // stacks are backwards
    Collections.reverse(offensiveAaTypes);
  }

  private void updateDefendingAaUnits() {
    final Collection<Unit> canFire = new ArrayList<>(defendingUnits);
    canFire.addAll(defendingWaitingToDie);
    final Map<String, Set<UnitType>> airborneTechTargetsAllowed =
        TechAbilityAttachment.getAirborneTargettedByAa(attacker, gameData);
    defendingAa =
        CollectionUtils.getMatches(
            canFire,
            Matches.unitIsAaThatCanFire(
                attackingUnits,
                airborneTechTargetsAllowed,
                attacker,
                Matches.unitIsAaForCombatOnly(),
                round,
                true,
                gameData));
    // comes ordered alphabetically
    defendingAaTypes = UnitAttachment.getAllOfTypeAas(defendingAa);
    // stacks are backwards
    Collections.reverse(defendingAaTypes);
  }

  @VisibleForTesting
  public List<String> determineStepStrings(final boolean showFirstRun) {
    final List<String> steps = new ArrayList<>();
    if (canFireOffensiveAa()) {
      for (final String typeAa : UnitAttachment.getAllOfTypeAas(offensiveAa)) {
        steps.add(attacker.getName() + " " + typeAa + AA_GUNS_FIRE_SUFFIX);
        steps.add(defender.getName() + SELECT_PREFIX + typeAa + CASUALTIES_SUFFIX);
        steps.add(defender.getName() + REMOVE_PREFIX + typeAa + CASUALTIES_SUFFIX);
      }
    }
    if (canFireDefendingAa()) {
      for (final String typeAa : UnitAttachment.getAllOfTypeAas(defendingAa)) {
        steps.add(defender.getName() + " " + typeAa + AA_GUNS_FIRE_SUFFIX);
        steps.add(attacker.getName() + SELECT_PREFIX + typeAa + CASUALTIES_SUFFIX);
        steps.add(attacker.getName() + REMOVE_PREFIX + typeAa + CASUALTIES_SUFFIX);
      }
    }
    if (showFirstRun) {
      if (!battleSite.isWater() && !getBombardingUnits().isEmpty()) {
        steps.add(NAVAL_BOMBARDMENT);
        steps.add(SELECT_NAVAL_BOMBARDMENT_CASUALTIES);
      }
      if (!battleSite.isWater() && TechAttachment.isAirTransportable(attacker)) {
        final Collection<Unit> bombers =
            CollectionUtils.getMatches(battleSite.getUnits(), Matches.unitIsAirTransport());
        if (!bombers.isEmpty()) {
          final Collection<Unit> dependents = getDependentUnits(bombers);
          if (!dependents.isEmpty()) {
            steps.add(LAND_PARATROOPS);
          }
        }
      }
    }
    // Check if defending subs can submerge before battle
    if (isSubRetreatBeforeBattle()) {
      if (defendingUnits.stream().noneMatch(Matches.unitIsDestroyer())
          && attackingUnits.stream().anyMatch(Matches.unitCanEvade())) {
        steps.add(attacker.getName() + SUBS_SUBMERGE);
      }
      if (attackingUnits.stream().noneMatch(Matches.unitIsDestroyer())
          && defendingUnits.stream().anyMatch(Matches.unitCanEvade())) {
        steps.add(defender.getName() + SUBS_SUBMERGE);
      }
    }
    // See if there any unescorted transports
    if (battleSite.isWater() && isTransportCasualtiesRestricted()) {
      if (attackingUnits.stream().anyMatch(Matches.unitIsTransport())
          || defendingUnits.stream().anyMatch(Matches.unitIsTransport())) {
        steps.add(REMOVE_UNESCORTED_TRANSPORTS);
      }
    }
    // if attacker has no sneak attack subs, then defender sneak attack subs fire first and remove
    // casualties
    final boolean defenderSubsFireFirst = defenderSubsFireFirst();
    if (defenderSubsFireFirst && defendingUnits.stream().anyMatch(Matches.unitIsFirstStrike())) {
      steps.add(defender.getName() + FIRST_STRIKE_UNITS_FIRE);
      steps.add(attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES);
      steps.add(REMOVE_SNEAK_ATTACK_CASUALTIES);
    }
    final boolean onlyAttackerSneakAttack =
        !defenderSubsFireFirst
            && returnFireAgainstAttackingSubs() == ReturnFire.NONE
            && returnFireAgainstDefendingSubs() == ReturnFire.ALL;
    // attacker subs sneak attack, no sneak attack if destroyers are present
    if (attackingUnits.stream().anyMatch(Matches.unitIsFirstStrike())) {
      steps.add(attacker.getName() + FIRST_STRIKE_UNITS_FIRE);
      steps.add(defender.getName() + SELECT_FIRST_STRIKE_CASUALTIES);
      if (onlyAttackerSneakAttack) {
        steps.add(REMOVE_SNEAK_ATTACK_CASUALTIES);
      }
    }
    // ww2v2 rules, all subs fire FIRST in combat, regardless of presence of destroyers.
    final boolean defendingSubsFireWithAllDefenders =
        !defenderSubsFireFirst
            && !Properties.getWW2V2(gameData)
            && returnFireAgainstDefendingSubs() == ReturnFire.ALL;
    // defender subs sneak attack, no sneak attack in Pacific/Europe Theaters or if destroyers are
    // present
    final boolean defendingSubsFireWithAllDefendersAlways = !defendingSubsSneakAttack();
    if (!defendingSubsFireWithAllDefendersAlways
        && !defendingSubsFireWithAllDefenders
        && !defenderSubsFireFirst
        && defendingUnits.stream().anyMatch(Matches.unitIsFirstStrike())) {
      steps.add(defender.getName() + FIRST_STRIKE_UNITS_FIRE);
      steps.add(attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES);
    }
    if ((attackingUnits.stream().anyMatch(Matches.unitIsFirstStrike())
            || defendingUnits.stream().anyMatch(Matches.unitIsFirstStrike()))
        && !defenderSubsFireFirst
        && !onlyAttackerSneakAttack
        && (returnFireAgainstDefendingSubs() != ReturnFire.ALL
            || returnFireAgainstAttackingSubs() != ReturnFire.ALL)) {
      steps.add(REMOVE_SNEAK_ATTACK_CASUALTIES);
    }
    // Air units can't attack subs without Destroyers present
    if (attackingUnits.stream().anyMatch(Matches.unitIsAir())
        && defendingUnits.stream().anyMatch(Matches.unitCanNotBeTargetedByAll())
        && !canAirAttackSubs(defendingUnits, attackingUnits)) {
      steps.add(SUBMERGE_SUBS_VS_AIR_ONLY);
      steps.add(AIR_ATTACK_NON_SUBS);
    }
    if (attackingUnits.stream().anyMatch(Matches.unitIsFirstStrike().negate())) {
      steps.add(attacker.getName() + FIRE);
      steps.add(defender.getName() + SELECT_CASUALTIES);
    }
    // classic rules, subs fire with all defenders
    // also, ww2v3/global rules, defending subs without sneak attack fire with all defenders
    final Collection<Unit> units = new ArrayList<>(defendingUnits);
    units.addAll(defendingWaitingToDie);
    if (units.stream().anyMatch(Matches.unitCanNotTargetAll())
        && !defenderSubsFireFirst
        && (defendingSubsFireWithAllDefenders || defendingSubsFireWithAllDefendersAlways)) {
      steps.add(defender.getName() + FIRST_STRIKE_UNITS_FIRE);
      steps.add(attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES);
    }
    // Air Units can't attack subs without Destroyers present
    if (defendingUnits.stream().anyMatch(Matches.unitIsAir())
        && attackingUnits.stream().anyMatch(Matches.unitCanNotBeTargetedByAll())
        && !canAirAttackSubs(attackingUnits, units)) {
      steps.add(AIR_DEFEND_NON_SUBS);
    }
    if (defendingUnits.stream().anyMatch(Matches.unitIsFirstStrike().negate())) {
      steps.add(defender.getName() + FIRE);
      steps.add(attacker.getName() + SELECT_CASUALTIES);
    }
    // remove casualties
    steps.add(REMOVE_CASUALTIES);
    // retreat attacking subs
    if (attackingUnits.stream().anyMatch(Matches.unitCanEvade())) {
      if (canSubsSubmerge()) {
        if (!isSubRetreatBeforeBattle()) {
          steps.add(attacker.getName() + SUBS_SUBMERGE);
        }
      } else {
        if (canAttackerRetreatSubs()) {
          steps.add(attacker.getName() + SUBS_WITHDRAW);
        }
      }
    }
    // if we are a sea zone, then we may not be able to retreat
    // (ie a sub traveled under another unit to get to the battle site)
    // or an enemy sub retreated to our sea zone
    // however, if all our sea units die, then the air units can still retreat, so if we have any
    // air units attacking in
    // a sea zone, we always have to have the retreat option shown
    // later, if our sea units die, we may ask the user to retreat
    final boolean someAirAtSea =
        battleSite.isWater() && attackingUnits.stream().anyMatch(Matches.unitIsAir());
    if (canAttackerRetreat()
        || someAirAtSea
        || canAttackerRetreatPartialAmphib()
        || canAttackerRetreatPlanes()) {
      steps.add(attacker.getName() + ATTACKER_WITHDRAW);
    }
    // retreat defending subs
    if (defendingUnits.stream().anyMatch(Matches.unitCanEvade())) {
      if (canSubsSubmerge()) {
        if (!isSubRetreatBeforeBattle()) {
          steps.add(defender.getName() + SUBS_SUBMERGE);
        }
      } else {
        if (canDefenderRetreatSubs()) {
          steps.add(defender.getName() + SUBS_WITHDRAW);
        }
      }
    }
    return steps;
  }

  private boolean canFireOffensiveAa() {
    if (offensiveAa == null) {
      updateOffensiveAaUnits();
    }
    return !offensiveAa.isEmpty();
  }

  private boolean canFireDefendingAa() {
    if (defendingAa == null) {
      updateDefendingAaUnits();
    }
    return !defendingAa.isEmpty();
  }

  private boolean defenderSubsFireFirst() {
    return returnFireAgainstAttackingSubs() == ReturnFire.ALL
        && returnFireAgainstDefendingSubs() == ReturnFire.NONE;
  }

  private ReturnFire returnFireAgainstAttackingSubs() {
    final boolean attackingSubsSneakAttack =
        defendingUnits.stream().noneMatch(Matches.unitIsDestroyer());
    final boolean defendingSubsSneakAttack = defendingSubsSneakAttackAndNoAttackingDestroyers();
    final ReturnFire returnFireAgainstAttackingSubs;
    if (!attackingSubsSneakAttack) {
      returnFireAgainstAttackingSubs = ReturnFire.ALL;
    } else if (defendingSubsSneakAttack || isWW2V2()) {
      returnFireAgainstAttackingSubs = ReturnFire.SUBS;
    } else {
      returnFireAgainstAttackingSubs = ReturnFire.NONE;
    }
    return returnFireAgainstAttackingSubs;
  }

  private ReturnFire returnFireAgainstDefendingSubs() {
    final boolean attackingSubsSneakAttack =
        defendingUnits.stream().noneMatch(Matches.unitIsDestroyer());
    final boolean defendingSubsSneakAttack = defendingSubsSneakAttackAndNoAttackingDestroyers();
    final ReturnFire returnFireAgainstDefendingSubs;
    if (!defendingSubsSneakAttack) {
      returnFireAgainstDefendingSubs = ReturnFire.ALL;
    } else if (attackingSubsSneakAttack || isWW2V2()) {
      returnFireAgainstDefendingSubs = ReturnFire.SUBS;
    } else {
      returnFireAgainstDefendingSubs = ReturnFire.NONE;
    }
    return returnFireAgainstDefendingSubs;
  }

  private boolean defendingSubsSneakAttackAndNoAttackingDestroyers() {
    return attackingUnits.stream().noneMatch(Matches.unitIsDestroyer())
        && defendingSubsSneakAttack();
  }

  private boolean defendingSubsSneakAttack() {
    return isWW2V2() || Properties.getDefendingSubsSneakAttack(gameData);
  }

  private static boolean canAirAttackSubs(
      final Collection<Unit> firedAt, final Collection<Unit> firing) {
    return firedAt.stream().noneMatch(Matches.unitCanNotBeTargetedByAll())
        || firing.stream().anyMatch(Matches.unitIsDestroyer());
  }

  private boolean canAttackerRetreatSubs() {
    if (defendingUnits.stream().anyMatch(Matches.unitIsDestroyer())) {
      return false;
    }
    return defendingWaitingToDie.stream().noneMatch(Matches.unitIsDestroyer())
        && (canAttackerRetreat() || canSubsSubmerge());
  }

  private boolean canAttackerRetreat() {
    if (onlyDefenselessDefendingTransportsLeft()) {
      return false;
    }
    if (isAmphibious) {
      return false;
    }
    return !getAttackerRetreatTerritories().isEmpty();
  }

  private boolean onlyDefenselessDefendingTransportsLeft() {
    return isTransportCasualtiesRestricted()
        && !defendingUnits.isEmpty()
        && defendingUnits.stream().allMatch(Matches.unitIsTransportButNotCombatTransport());
  }

  @VisibleForTesting
  public Collection<Territory> getAttackerRetreatTerritories() {
    // TODO: when attacking with paratroopers (air + carried land), there are several bugs in
    // retreating.
    // TODO: air should always be able to retreat. paratroopers can only retreat if there are other
    // non-paratrooper non-amphibious land units.

    // If attacker is all planes, just return collection of current territory
    if (headless
        || (!attackingUnits.isEmpty() && attackingUnits.stream().allMatch(Matches.unitIsAir()))
        || Properties.getRetreatingUnitsRemainInPlace(gameData)) {
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
                Properties.getIgnoreTransportInMovement(gameData),
                Matches.unitIsNotTransportButCouldBeCombatTransport())
            .build();
    Collection<Territory> possible =
        CollectionUtils.getMatches(
            attackingFrom,
            Matches.territoryHasUnitsThatMatch(enemyUnitsThatPreventRetreat).negate());
    // In WW2V2 and WW2V3 we need to filter out territories where only planes
    // came from since planes cannot define retreat paths
    if (isWW2V2() || isWW2V3()) {
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

  private boolean canAttackerRetreatPlanes() {
    return (isWW2V2()
            || Properties.getAttackerRetreatPlanes(gameData)
            || isPartialAmphibiousRetreat())
        && isAmphibious
        && attackingUnits.stream().anyMatch(Matches.unitIsAir());
  }

  private boolean canAttackerRetreatPartialAmphib() {
    if (isAmphibious && isPartialAmphibiousRetreat()) {
      // Only include land units when checking for allow amphibious retreat
      final List<Unit> landUnits = CollectionUtils.getMatches(attackingUnits, Matches.unitIsLand());
      for (final Unit unit : landUnits) {
        final TripleAUnit taUnit = (TripleAUnit) unit;
        if (!taUnit.getWasAmphibious()) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean canDefenderRetreatSubs() {
    if (attackingUnits.stream().anyMatch(Matches.unitIsDestroyer())) {
      return false;
    }
    return attackingWaitingToDie.stream().noneMatch(Matches.unitIsDestroyer())
        && (getEmptyOrFriendlySeaNeighbors(
                        defender,
                        CollectionUtils.getMatches(defendingUnits, Matches.unitCanEvade()))
                    .size()
                != 0
            || canSubsSubmerge());
  }

  private Collection<Territory> getEmptyOrFriendlySeaNeighbors(
      final GamePlayer player, final Collection<Unit> unitsToRetreat) {
    Collection<Territory> possible = gameData.getMap().getNeighbors(battleSite);
    if (headless) {
      return possible;
    }
    // make sure we can move through the any canals
    final Predicate<Territory> canalMatch =
        t -> {
          final Route r = new Route(battleSite, t);
          return MoveValidator.validateCanal(r, unitsToRetreat, defender) == null;
        };
    final Predicate<Territory> match =
        Matches.territoryIsWater()
            .and(Matches.territoryHasNoEnemyUnits(player, gameData))
            .and(canalMatch);
    possible = CollectionUtils.getMatches(possible, match);
    return possible;
  }

  private void pushFightLoopOnStack(final boolean firstRun) {
    if (isOver) {
      return;
    }
    final List<IExecutable> steps = getBattleExecutables(firstRun);
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
  public List<IExecutable> getBattleExecutables(final boolean firstRun) {
    final List<IExecutable> steps = new ArrayList<>();
    addFightStartSteps(firstRun, steps);
    addFightSteps(steps);
    addCheckEndBattleAndRetreatingSteps(steps);
    return steps;
  }

  private void addFightStartSteps(final boolean firstRun, final List<IExecutable> steps) {
    final boolean offensiveAa = canFireOffensiveAa();
    final boolean defendingAa = canFireDefendingAa();
    if (offensiveAa) {
      steps.add(
          new IExecutable() {
            private static final long serialVersionUID = 3802352588499530533L;

            @Override
            public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
              fireOffensiveAaGuns();
            }
          });
    }
    if (defendingAa) {
      steps.add(
          new IExecutable() {
            private static final long serialVersionUID = -1370090785540214199L;

            @Override
            public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
              fireDefensiveAaGuns();
            }
          });
    }
    if (offensiveAa || defendingAa) {
      steps.add(
          new IExecutable() {
            private static final long serialVersionUID = 8762796262264296436L;

            @Override
            public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
              clearWaitingToDieAndDamagedChangesInto(bridge);
            }
          });
    }
    if (round > 1) {
      steps.add(
          new IExecutable() {
            private static final long serialVersionUID = 2781652892457063082L;

            @Override
            public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
              removeNonCombatants(bridge);
            }
          });
    }
    if (firstRun) {
      steps.add(
          new IExecutable() {
            private static final long serialVersionUID = -2255284529092427441L;

            @Override
            public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
              fireNavalBombardment(bridge);
            }
          });
      steps.add(
          new IExecutable() {
            private static final long serialVersionUID = 3389635558184415797L;

            @Override
            public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
              removeNonCombatants(bridge);
            }
          });
      steps.add(
          new IExecutable() {
            private static final long serialVersionUID = 7193353768857658286L;

            @Override
            public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
              landParatroops(bridge);
            }
          });
      steps.add(
          new IExecutable() {
            private static final long serialVersionUID = -6676316363537467594L;

            @Override
            public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
              markNoMovementLeft(bridge);
            }
          });
    }
  }

  private void fireOffensiveAaGuns() {
    final List<Unit> allFriendlyUnitsAliveOrWaitingToDie = new ArrayList<>(attackingUnits);
    allFriendlyUnitsAliveOrWaitingToDie.addAll(attackingWaitingToDie);
    final List<Unit> allEnemyUnitsAliveOrWaitingToDie = new ArrayList<>(defendingUnits);
    allEnemyUnitsAliveOrWaitingToDie.addAll(defendingWaitingToDie);
    stack.push(
        new FireAa(
            defendingUnits,
            attacker,
            defender,
            offensiveAa,
            this,
            false,
            dependentUnits,
            headless,
            battleSite,
            territoryEffects,
            allFriendlyUnitsAliveOrWaitingToDie,
            allEnemyUnitsAliveOrWaitingToDie,
            offensiveAaTypes));
  }

  private void fireDefensiveAaGuns() {
    final List<Unit> allFriendlyUnitsAliveOrWaitingToDie = new ArrayList<>(defendingUnits);
    allFriendlyUnitsAliveOrWaitingToDie.addAll(defendingWaitingToDie);
    final List<Unit> allEnemyUnitsAliveOrWaitingToDie = new ArrayList<>(attackingUnits);
    allEnemyUnitsAliveOrWaitingToDie.addAll(attackingWaitingToDie);
    stack.push(
        new FireAa(
            attackingUnits,
            defender,
            attacker,
            defendingAa,
            this,
            true,
            dependentUnits,
            headless,
            battleSite,
            territoryEffects,
            allFriendlyUnitsAliveOrWaitingToDie,
            allEnemyUnitsAliveOrWaitingToDie,
            defendingAaTypes));
  }

  private void fireNavalBombardment(final IDelegateBridge bridge) {
    final Collection<Unit> bombard = getBombardingUnits();
    final Collection<Unit> attacked =
        CollectionUtils.getMatches(
            defendingUnits,
            Matches.unitIsNotInfrastructureAndNotCapturedOnEntering(
                attacker, battleSite, gameData));
    // bombarding units can't move after bombarding
    if (!headless) {
      final Change change = ChangeFactory.markNoMovementChange(bombard);
      bridge.addChange(change);
    }
    final boolean canReturnFire = Properties.getNavalBombardCasualtiesReturnFire(gameData);
    if (!bombard.isEmpty() && !attacked.isEmpty()) {
      if (!headless) {
        bridge
            .getSoundChannelBroadcaster()
            .playSoundForAll(SoundPath.CLIP_BATTLE_BOMBARD, attacker);
      }
      final List<Unit> allEnemyUnitsAliveOrWaitingToDie = new ArrayList<>(defendingUnits);
      allEnemyUnitsAliveOrWaitingToDie.addAll(defendingWaitingToDie);
      fire(
          SELECT_NAVAL_BOMBARDMENT_CASUALTIES,
          bombard,
          attacked,
          allEnemyUnitsAliveOrWaitingToDie,
          bombard,
          false,
          canReturnFire ? ReturnFire.ALL : ReturnFire.NONE,
          "Bombard");
    }
  }

  private void fire(
      final String stepName,
      final Collection<Unit> firingUnits,
      final Collection<Unit> attackableUnits,
      final Collection<Unit> allEnemyUnitsAliveOrWaitingToDie,
      final Collection<Unit> allFriendlyUnitsAliveOrWaitingToDie,
      final boolean defender,
      final ReturnFire returnFire,
      final String text) {

    final Collection<Unit> targetUnits =
        CollectionUtils.getMatches(
            attackableUnits,
            PredicateBuilder.of(Matches.unitIsNotInfrastructure())
                .andIf(defender, Matches.unitIsSuicideOnAttack().negate())
                .andIf(!defender, Matches.unitIsSuicideOnDefense().negate())
                .build());
    if (firingUnits.isEmpty() || targetUnits.isEmpty()) {
      return;
    }
    final GamePlayer firingPlayer = defender ? this.defender : attacker;
    final GamePlayer hitPlayer = !defender ? this.defender : attacker;

    // Fire each type of suicide on hit unit separately and then remaining units
    final List<Collection<Unit>> firingGroups = newFiringUnitGroups(firingUnits);
    for (final Collection<Unit> units : firingGroups) {
      stack.push(
          new Fire(
              targetUnits,
              returnFire,
              firingPlayer,
              hitPlayer,
              units,
              stepName,
              text,
              this,
              defender,
              dependentUnits,
              headless,
              battleSite,
              territoryEffects,
              allEnemyUnitsAliveOrWaitingToDie,
              allFriendlyUnitsAliveOrWaitingToDie));
    }
  }

  /**
   * Breaks list of units into groups of non suicide on hit units and each type of suicide on hit
   * units since each type of suicide on hit units need to roll separately to know which ones get
   * hits.
   */
  static List<Collection<Unit>> newFiringUnitGroups(final Collection<Unit> units) {

    // Sort suicide on hit units by type
    final Map<UnitType, Collection<Unit>> map = new HashMap<>();
    for (final Unit unit : CollectionUtils.getMatches(units, Matches.unitIsSuicideOnHit())) {
      final UnitType type = unit.getType();
      if (map.containsKey(type)) {
        map.get(type).add(unit);
      } else {
        final Collection<Unit> unitList = new ArrayList<>();
        unitList.add(unit);
        map.put(type, unitList);
      }
    }

    // Add all suicide on hit groups and the remaining units
    final List<Collection<Unit>> result = new ArrayList<>(map.values());
    final Collection<Unit> remainingUnits =
        CollectionUtils.getMatches(units, Matches.unitIsSuicideOnHit().negate());
    if (!remainingUnits.isEmpty()) {
      result.add(remainingUnits);
    }
    return result;
  }

  private void removeNonCombatants(final IDelegateBridge bridge) {
    final List<Unit> notRemovedDefending = removeNonCombatants(defendingUnits, false, true);
    final List<Unit> notRemovedAttacking = removeNonCombatants(attackingUnits, true, true);
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
      final Collection<Unit> units, final boolean attacking, final boolean removeForNextRound) {
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
                    false)
                .negate()));
    // remove any disabled units from combat
    unitList.removeAll(CollectionUtils.getMatches(unitList, Matches.unitIsDisabled()));
    // remove capturableOnEntering units (veqryn)
    unitList.removeAll(
        CollectionUtils.getMatches(
            unitList,
            Matches.unitCanBeCapturedOnEnteringToInThisTerritory(attacker, battleSite, gameData)));
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

  private void landParatroops(final IDelegateBridge bridge) {
    if (TechAttachment.isAirTransportable(attacker)) {
      final Collection<Unit> airTransports =
          CollectionUtils.getMatches(battleSite.getUnits(), Matches.unitIsAirTransport());
      if (!airTransports.isEmpty()) {
        final Collection<Unit> dependents = getDependentUnits(airTransports);
        if (!dependents.isEmpty()) {
          final CompositeChange change = new CompositeChange();
          // remove dependency from paratroopers by unloading the air transports
          for (final Unit unit : dependents) {
            change.add(
                TransportTracker.unloadAirTransportChange((TripleAUnit) unit, battleSite, false));
          }
          bridge.addChange(change);
          // remove bombers from dependentUnits
          for (final Unit unit : airTransports) {
            dependentUnits.remove(unit);
          }
        }
      }
    }
  }

  private void markNoMovementLeft(final IDelegateBridge bridge) {
    if (headless) {
      return;
    }
    final Collection<Unit> attackingNonAir =
        CollectionUtils.getMatches(attackingUnits, Matches.unitIsAir().negate());
    final Change noMovementChange = ChangeFactory.markNoMovementChange(attackingNonAir);
    if (!noMovementChange.isEmpty()) {
      bridge.addChange(noMovementChange);
    }
  }

  private void addFightSteps(final List<IExecutable> steps) {
    // Ask to retreat defending subs before battle
    if (isSubRetreatBeforeBattle()) {
      steps.add(
          new IExecutable() {
            private static final long serialVersionUID = 6775880082912594489L;

            @Override
            public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
              if (!isOver) {
                attackerRetreatSubs(bridge);
              }
            }
          });
      steps.add(
          new IExecutable() {
            private static final long serialVersionUID = 7056448091800764539L;

            @Override
            public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
              if (!isOver) {
                defenderRetreatSubs(bridge);
              }
            }
          });
    }
    // Remove undefended transports
    if (isTransportCasualtiesRestricted()) {
      steps.add(
          new IExecutable() {
            private static final long serialVersionUID = 99989L;

            @Override
            public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
              checkUndefendedTransports(bridge, defender);
              checkUndefendedTransports(bridge, attacker);
              checkForUnitsThatCanRollLeft(bridge, true);
              checkForUnitsThatCanRollLeft(bridge, false);
            }
          });
    }
    // Submerge subs if -vs air only & air restricted from attacking subs
    steps.add(
        new IExecutable() {
          private static final long serialVersionUID = 99990L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            submergeSubsVsOnlyAir(bridge);
          }
        });
    final ReturnFire returnFireAgainstAttackingSubs = returnFireAgainstAttackingSubs();
    final ReturnFire returnFireAgainstDefendingSubs = returnFireAgainstDefendingSubs();
    if (defenderSubsFireFirst()) {
      steps.add(
          new FirstStrikeDefendersFire() {
            private static final long serialVersionUID = 99992L;

            @Override
            public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
              firstStrikeDefendersFire(returnFireAgainstDefendingSubs);
            }
          });
    }
    steps.add(
        new FirstStrikeAttackersFire() {
          private static final long serialVersionUID = 99991L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            firstStrikeAttackersFire(returnFireAgainstAttackingSubs);
          }
        });
    final boolean defendingSubsFireWithAllDefenders =
        !defenderSubsFireFirst()
            && !Properties.getWW2V2(gameData)
            && returnFireAgainstDefendingSubs() == ReturnFire.ALL;
    if (defendingSubsSneakAttack()
        && !defenderSubsFireFirst()
        && !defendingSubsFireWithAllDefenders) {
      steps.add(
          new FirstStrikeDefendersFire() {
            private static final long serialVersionUID = 99992L;

            @Override
            public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
              firstStrikeDefendersFire(returnFireAgainstDefendingSubs);
            }
          });
    }
    steps.add(
        new IExecutable() {
          private static final long serialVersionUID = -7634700553071456768L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            removeFirstStrikeSuicideUnits(bridge);
          }
        });
    // Attacker fire remaining units
    steps.add(
        new IExecutable() {
          private static final long serialVersionUID = 99994L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            standardAttackersFire();
          }
        });
    if (!defenderSubsFireFirst()
        && (!defendingSubsSneakAttack() || defendingSubsFireWithAllDefenders)) {
      steps.add(
          new FirstStrikeDefendersFire() {
            private static final long serialVersionUID = 999921L;

            @Override
            public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
              firstStrikeDefendersFire(returnFireAgainstDefendingSubs);
            }
          });
    }
    steps.add(
        new IExecutable() {
          private static final long serialVersionUID = 1560702114917865290L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            standardDefendersFire();
          }
        });
  }

  private void attackerRetreatSubs(final IDelegateBridge bridge) {
    if (!canAttackerRetreatSubs()) {
      return;
    }
    if (attackingUnits.stream().anyMatch(Matches.unitCanEvade())) {
      queryRetreat(false, RetreatType.SUBS, bridge, getAttackerRetreatTerritories());
    }
  }

  private void defenderRetreatSubs(final IDelegateBridge bridge) {
    if (!canDefenderRetreatSubs()) {
      return;
    }
    if (!isOver && defendingUnits.stream().anyMatch(Matches.unitCanEvade())) {
      queryRetreat(
          true,
          RetreatType.SUBS,
          bridge,
          getEmptyOrFriendlySeaNeighbors(
              defender, CollectionUtils.getMatches(defendingUnits, Matches.unitCanEvade())));
    }
  }

  /** Check for unescorted transports and kill them immediately. */
  private void checkUndefendedTransports(final IDelegateBridge bridge, final GamePlayer player) {
    // if we are the attacker, we can retreat instead of dying
    if (player.equals(attacker)
        && (!getAttackerRetreatTerritories().isEmpty()
            || attackingUnits.stream().anyMatch(Matches.unitIsAir()))) {
      return;
    }
    // Get all allied transports in the territory
    final Predicate<Unit> matchAllied =
        Matches.unitIsTransport()
            .and(Matches.unitIsNotCombatTransport())
            .and(Matches.isUnitAllied(player, gameData))
            .and(Matches.unitIsSea());
    final List<Unit> alliedTransports =
        CollectionUtils.getMatches(battleSite.getUnits(), matchAllied);
    // If no transports, just return
    if (alliedTransports.isEmpty()) {
      return;
    }
    // Get all ALLIED, sea & air units in the territory (that are NOT submerged)
    final Predicate<Unit> alliedUnitsMatch =
        Matches.isUnitAllied(player, gameData)
            .and(Matches.unitIsNotLand())
            .and(Matches.unitIsSubmerged().negate());
    final Collection<Unit> alliedUnits =
        CollectionUtils.getMatches(battleSite.getUnits(), alliedUnitsMatch);
    // If transports are unescorted, check opposing forces to see if the Trns die automatically
    if (alliedTransports.size() == alliedUnits.size()) {
      // Get all the ENEMY sea and air units (that can attack) in the territory
      final Predicate<Unit> enemyUnitsMatch =
          Matches.unitIsNotLand()
              .and(Matches.unitIsSubmerged().negate())
              .and(Matches.unitCanAttack(player));
      final Collection<Unit> enemyUnits =
          CollectionUtils.getMatches(battleSite.getUnits(), enemyUnitsMatch);
      // If there are attackers set their movement to 0 and kill the transports
      if (!enemyUnits.isEmpty()) {
        final Change change =
            ChangeFactory.markNoMovementChange(
                CollectionUtils.getMatches(enemyUnits, Matches.unitIsSea()));
        bridge.addChange(change);
        final boolean defender = player.equals(this.defender);
        remove(alliedTransports, bridge, battleSite, defender);
      }
    }
  }

  private void checkForUnitsThatCanRollLeft(final IDelegateBridge bridge, final boolean attacker) {
    // if we are the attacker, we can retreat instead of dying
    if (attacker
        && (!getAttackerRetreatTerritories().isEmpty()
            || attackingUnits.stream().anyMatch(Matches.unitIsAir()))) {
      return;
    }
    if (attackingUnits.isEmpty() || defendingUnits.isEmpty()) {
      return;
    }
    final Predicate<Unit> notSubmergedAndType =
        Matches.unitIsSubmerged()
            .negate()
            .and(
                Matches.territoryIsLand().test(battleSite)
                    ? Matches.unitIsSea().negate()
                    : Matches.unitIsLand().negate());
    final Collection<Unit> unitsToKill;
    final boolean hasUnitsThatCanRollLeft;
    if (attacker) {
      hasUnitsThatCanRollLeft =
          attackingUnits.stream()
              .anyMatch(
                  notSubmergedAndType.and(Matches.unitIsSupporterOrHasCombatAbility(attacker)));
      unitsToKill =
          CollectionUtils.getMatches(
              attackingUnits, notSubmergedAndType.and(Matches.unitIsNotInfrastructure()));
    } else {
      hasUnitsThatCanRollLeft =
          defendingUnits.stream()
              .anyMatch(
                  notSubmergedAndType.and(Matches.unitIsSupporterOrHasCombatAbility(attacker)));
      unitsToKill =
          CollectionUtils.getMatches(
              defendingUnits, notSubmergedAndType.and(Matches.unitIsNotInfrastructure()));
    }
    final boolean enemy = !attacker;
    final boolean enemyHasUnitsThatCanRollLeft;
    if (enemy) {
      enemyHasUnitsThatCanRollLeft =
          attackingUnits.stream()
              .anyMatch(notSubmergedAndType.and(Matches.unitIsSupporterOrHasCombatAbility(enemy)));
    } else {
      enemyHasUnitsThatCanRollLeft =
          defendingUnits.stream()
              .anyMatch(notSubmergedAndType.and(Matches.unitIsSupporterOrHasCombatAbility(enemy)));
    }
    if (!hasUnitsThatCanRollLeft && enemyHasUnitsThatCanRollLeft) {
      remove(unitsToKill, bridge, battleSite, !attacker);
    }
  }

  /** Submerge attacking/defending subs if they're alone OR with transports against only air. */
  private void submergeSubsVsOnlyAir(final IDelegateBridge bridge) {
    // if All attackers are AIR, submerge any defending subs
    final Predicate<Unit> subMatch =
        Matches.unitCanEvade().and(Matches.unitCanNotBeTargetedByAll());
    if (!attackingUnits.isEmpty()
        && attackingUnits.stream().allMatch(Matches.unitIsAir())
        && defendingUnits.stream().anyMatch(subMatch)) {
      // Get all defending subs (including allies) in the territory
      final List<Unit> defendingSubs = CollectionUtils.getMatches(defendingUnits, subMatch);
      // submerge defending subs
      submergeUnits(defendingSubs, true, bridge);
      // checking defending air on attacking subs
    } else if (!defendingUnits.isEmpty()
        && defendingUnits.stream().allMatch(Matches.unitIsAir())
        && attackingUnits.stream().anyMatch(subMatch)) {
      // Get all attacking subs in the territory
      final List<Unit> attackingSubs = CollectionUtils.getMatches(attackingUnits, subMatch);
      // submerge attacking subs
      submergeUnits(attackingSubs, false, bridge);
    }
  }

  private void firstStrikeDefendersFire(final ReturnFire returnFire) {
    findTargetGroupsAndFire(
        returnFire,
        attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
        true,
        defender,
        Matches.unitIsFirstStrike(),
        defendingUnits,
        defendingWaitingToDie,
        attackingUnits,
        attackingWaitingToDie);
  }

  private void firstStrikeAttackersFire(final ReturnFire returnFire) {
    findTargetGroupsAndFire(
        returnFire,
        defender.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
        false,
        attacker,
        Matches.unitIsFirstStrike(),
        attackingUnits,
        attackingWaitingToDie,
        defendingUnits,
        defendingWaitingToDie);
  }

  private void standardAttackersFire() {
    findTargetGroupsAndFire(
        ReturnFire.ALL,
        defender.getName() + SELECT_CASUALTIES,
        false,
        attacker,
        Matches.unitIsFirstStrike().negate(),
        attackingUnits,
        attackingWaitingToDie,
        defendingUnits,
        defendingWaitingToDie);
  }

  private void standardDefendersFire() {
    findTargetGroupsAndFire(
        ReturnFire.ALL,
        attacker.getName() + SELECT_CASUALTIES,
        true,
        defender,
        Matches.unitIsFirstStrike().negate(),
        defendingUnits,
        defendingWaitingToDie,
        attackingUnits,
        attackingWaitingToDie);
  }

  private void findTargetGroupsAndFire(
      final ReturnFire returnFire,
      final String stepName,
      final boolean defending,
      final GamePlayer firingPlayer,
      final Predicate<Unit> firingUnitPredicate,
      final Collection<Unit> firingUnits,
      final Collection<Unit> firingUnitsWaitingToDie,
      final Collection<Unit> enemyUnits,
      final Collection<Unit> enemyUnitsWaitingToDie) {

    Collection<Unit> firing = new ArrayList<>(firingUnits);
    firing.addAll(firingUnitsWaitingToDie);
    firing = CollectionUtils.getMatches(firing, firingUnitPredicate);
    // See if allied air can participate in combat
    if (!defending && !Properties.getAlliedAirIndependent(gameData)) {
      firing = CollectionUtils.getMatches(firing, Matches.unitIsOwnedBy(attacker));
    }
    final List<Unit> allEnemyUnitsAliveOrWaitingToDie = new ArrayList<>(enemyUnits);
    allEnemyUnitsAliveOrWaitingToDie.addAll(enemyUnitsWaitingToDie);
    final List<Unit> allFriendlyUnitsAliveOrWaitingToDie = new ArrayList<>(firingUnits);
    allFriendlyUnitsAliveOrWaitingToDie.addAll(firingUnitsWaitingToDie);
    for (final TargetGroup firingGroup : TargetGroup.newTargetGroups(firing, enemyUnits)) {
      fire(
          stepName,
          firingGroup.getFiringUnits(firing),
          firingGroup.getTargetUnits(enemyUnits),
          allEnemyUnitsAliveOrWaitingToDie,
          allFriendlyUnitsAliveOrWaitingToDie,
          defending,
          returnFire,
          firingPlayer.getName() + " fire, ");
    }
  }

  private void addCheckEndBattleAndRetreatingSteps(final List<IExecutable> steps) {
    steps.add(
        new IExecutable() {
          private static final long serialVersionUID = 8611067962952500496L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            clearWaitingToDieAndDamagedChangesInto(bridge);
          }
        });
    steps.add(
        new IExecutable() {
          private static final long serialVersionUID = 6387198382888361848L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            removeStandardSuicideUnits(bridge);
          }
        });
    steps.add(
        new IExecutable() {
          private static final long serialVersionUID = 5259103822937067667L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            if (CollectionUtils.getMatches(attackingUnits, Matches.unitIsNotInfrastructure()).size()
                == 0) {
              if (!isTransportCasualtiesRestricted()) {
                endBattle(bridge);
                defenderWins(bridge);
              } else {
                // Get all allied transports in the territory
                final Predicate<Unit> matchAllied =
                    Matches.unitIsTransport()
                        .and(Matches.unitIsNotCombatTransport())
                        .and(Matches.isUnitAllied(attacker, gameData));
                final List<Unit> alliedTransports =
                    CollectionUtils.getMatches(battleSite.getUnits(), matchAllied);
                // If no transports, just end the battle
                if (alliedTransports.isEmpty()) {
                  endBattle(bridge);
                  defenderWins(bridge);
                } else if (round <= 1) {
                  attackingUnits =
                      CollectionUtils.getMatches(
                          battleSite.getUnits(), Matches.unitIsOwnedBy(attacker));
                } else {
                  endBattle(bridge);
                  defenderWins(bridge);
                }
              }
            } else if (CollectionUtils.getMatches(defendingUnits, Matches.unitIsNotInfrastructure())
                    .size()
                == 0) {
              if (isTransportCasualtiesRestricted()) {
                // If there are undefended attacking transports, determine if they automatically die
                checkUndefendedTransports(bridge, defender);
              }
              checkForUnitsThatCanRollLeft(bridge, false);
              endBattle(bridge);
              attackerWins(bridge);
            } else if (maxRounds > 0 && maxRounds <= round) {
              endBattle(bridge);
              nobodyWins(bridge);
            } else {
              final int attackPower =
                  DiceRoll.getTotalPower(
                      DiceRoll.getUnitPowerAndRollsForNormalBattles(
                          attackingUnits,
                          defendingUnits,
                          attackingUnits,
                          false,
                          gameData,
                          battleSite,
                          territoryEffects,
                          isAmphibious,
                          amphibiousLandAttackers),
                      gameData);
              final int defensePower =
                  DiceRoll.getTotalPower(
                      DiceRoll.getUnitPowerAndRollsForNormalBattles(
                          defendingUnits,
                          attackingUnits,
                          defendingUnits,
                          true,
                          gameData,
                          battleSite,
                          territoryEffects,
                          isAmphibious,
                          amphibiousLandAttackers),
                      gameData);
              if (attackPower == 0 && defensePower == 0) {
                // Check if both sides have only transports remaining and are in a sea battle.
                // See: https://github.com/triplea-game/triplea/issues/2367
                // Rule: "In a sea battle, if both sides have only transports remaining, the
                // attacker’s transports can remain in the contested sea zone or retreat per the
                // rules in Condition B below, if possible.
                if (Matches.territoryIsWater().test(battleSite)
                    && defendingUnits.stream().allMatch(Matches.unitIsTransport())
                    && attackingUnits.stream().allMatch(Matches.unitIsTransport())) {
                  attackerRetreatStalemate(bridge);
                }
                endBattle(bridge);
                nobodyWins(bridge);
              }
            }
          }
        });
    steps.add(
        new IExecutable() {
          private static final long serialVersionUID = 6775880082912594489L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            if (!isOver && canAttackerRetreatSubs() && !isSubRetreatBeforeBattle()) {
              attackerRetreatSubs(bridge);
            }
          }
        });
    steps.add(
        new IExecutable() {
          private static final long serialVersionUID = -1150863964807721395L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            if (!isOver && canAttackerRetreatPlanes() && !canAttackerRetreatPartialAmphib()) {
              attackerRetreatPlanes(bridge);
            }
          }
        });
    steps.add(
        new IExecutable() {
          private static final long serialVersionUID = -1150863964807721395L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            if (!isOver && canAttackerRetreatPartialAmphib()) {
              attackerRetreatNonAmphibUnits(bridge);
            }
          }
        });
    steps.add(
        new IExecutable() {
          private static final long serialVersionUID = 669349383898975048L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            if (!isOver) {
              attackerRetreat(bridge);
            }
          }
        });
    steps.add(
        new IExecutable() {
          private static final long serialVersionUID = -1544916305666912480L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            if (!isOver) {
              if (canDefenderRetreatSubs() && !isSubRetreatBeforeBattle()) {
                defenderRetreatSubs(bridge);
              }
              // If no defenders left, then battle is over. The reason we test a "second" time here,
              // is because otherwise
              // the attackers can retreat even though the battle is over (illegal).
              if (defendingUnits.isEmpty()) {
                endBattle(bridge);
                attackerWins(bridge);
              }
            }
          }
        });
    final IExecutable loop =
        new IExecutable() {
          private static final long serialVersionUID = 3118458517320468680L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            pushFightLoopOnStack(false);
          }
        };
    steps.add(
        new IExecutable() {
          private static final long serialVersionUID = -3993599528368570254L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            if (!isOver) {
              round++;
              // determine any AA
              updateOffensiveAaUnits();
              updateDefendingAaUnits();
              stepStrings = determineStepStrings(false);
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

  private void removeFirstStrikeSuicideUnits(final IDelegateBridge bridge) {
    removeSuicideUnits(bridge, Matches.unitIsFirstStrike());
  }

  private void removeStandardSuicideUnits(final IDelegateBridge bridge) {
    removeSuicideUnits(bridge, Matches.unitIsFirstStrike().negate());
  }

  private void removeSuicideUnits(final IDelegateBridge bridge, final Predicate<Unit> unitMatch) {
    final Collection<Unit> deadAttackers =
        CollectionUtils.getMatches(attackingUnits, unitMatch.and(Matches.unitIsSuicideOnAttack()));
    final Collection<Unit> deadDefenders =
        CollectionUtils.getMatches(defendingUnits, unitMatch.and(Matches.unitIsSuicideOnDefense()));
    bridge
        .getDisplayChannelBroadcaster()
        .deadUnitNotification(battleId, attacker, deadAttackers, dependentUnits);
    bridge
        .getDisplayChannelBroadcaster()
        .deadUnitNotification(battleId, defender, deadDefenders, dependentUnits);
    final List<Unit> deadUnits = new ArrayList<>(deadAttackers);
    deadUnits.addAll(deadDefenders);
    remove(deadUnits, bridge, battleSite, null);
  }

  private void attackerRetreatPlanes(final IDelegateBridge bridge) {
    // planes retreat to the same square the battle is in, and then should move during non combat to
    // their landing site, or be scrapped if they can't find one.
    if (attackingUnits.stream().anyMatch(Matches.unitIsAir())) {
      queryRetreat(false, RetreatType.PLANES, bridge, Set.of(battleSite));
    }
  }

  private void attackerRetreatNonAmphibUnits(final IDelegateBridge bridge) {
    final Collection<Territory> possible = getAttackerRetreatTerritories();
    queryRetreat(false, RetreatType.PARTIAL_AMPHIB, bridge, possible);
  }

  private void attackerRetreatStalemate(final IDelegateBridge bridge) {
    attackerRetreat(bridge, true);
  }

  private void attackerRetreat(final IDelegateBridge bridge) {
    attackerRetreat(bridge, false);
  }

  private void attackerRetreat(
      final IDelegateBridge bridge, final boolean forceAllowAttackerRetreat) {
    if (!forceAllowAttackerRetreat && !canAttackerRetreat()) {
      return;
    }
    final Collection<Territory> possible = getAttackerRetreatTerritories();
    if (!isOver) {
      if (isAmphibious) {
        queryRetreat(false, RetreatType.PARTIAL_AMPHIB, bridge, possible);
      } else {
        queryRetreat(false, RetreatType.DEFAULT, bridge, possible);
      }
    }
  }

  private void queryRetreat(
      final boolean defender,
      final RetreatType retreatType,
      final IDelegateBridge bridge,
      final Collection<Territory> initialAvailableTerritories) {
    final boolean planes = retreatType == RetreatType.PLANES;
    final boolean subs = retreatType == RetreatType.SUBS;
    final boolean canSubsSubmerge = canSubsSubmerge();
    final boolean canDefendingSubsSubmergeOrRetreat =
        subs && defender && Properties.getSubmarinesDefendingMaySubmergeOrRetreat(gameData);
    final boolean partialAmphib = retreatType == RetreatType.PARTIAL_AMPHIB;
    final boolean submerge = subs && canSubsSubmerge;
    if (initialAvailableTerritories.isEmpty() && !(submerge || canDefendingSubsSubmergeOrRetreat)) {
      return;
    }

    // If attacker then add all owned units at battle site as some might have been removed from
    // battle (infra)
    Collection<Unit> units = defender ? defendingUnits : attackingUnits;
    if (!defender) {
      units = new HashSet<>(units);
      units.addAll(battleSite.getUnitCollection().getMatches(Matches.unitIsOwnedBy(attacker)));
      units.removeAll(killed);
    }
    if (subs) {
      units = CollectionUtils.getMatches(units, Matches.unitCanEvade());
    } else if (planes) {
      units = CollectionUtils.getMatches(units, Matches.unitIsAir());
    } else if (partialAmphib) {
      units = CollectionUtils.getMatches(units, Matches.unitWasNotAmphibious());
    }
    final Collection<Territory> availableTerritories =
        units.stream().anyMatch(Matches.unitIsSea())
            ? CollectionUtils.getMatches(initialAvailableTerritories, Matches.territoryIsWater())
            : new ArrayList<>(initialAvailableTerritories);
    if (canDefendingSubsSubmergeOrRetreat) {
      availableTerritories.add(battleSite);
    } else if (submerge) {
      availableTerritories.clear();
      availableTerritories.add(battleSite);
    }
    if (planes) {
      availableTerritories.clear();
      availableTerritories.add(battleSite);
    }
    if (units.isEmpty()) {
      return;
    }
    final GamePlayer retreatingPlayer = defender ? this.defender : attacker;
    final String text;
    if (subs) {
      text = retreatingPlayer.getName() + " retreat subs?";
    } else if (planes) {
      text = retreatingPlayer.getName() + " retreat planes?";
    } else if (partialAmphib) {
      text = retreatingPlayer.getName() + " retreat non-amphibious units?";
    } else {
      text = retreatingPlayer.getName() + " retreat?";
    }
    final String step;
    if (defender) {
      step = this.defender.getName() + (canSubsSubmerge ? SUBS_SUBMERGE : SUBS_WITHDRAW);
    } else {
      if (subs) {
        step = attacker.getName() + (canSubsSubmerge ? SUBS_SUBMERGE : SUBS_WITHDRAW);
      } else {
        step = attacker.getName() + ATTACKER_WITHDRAW;
      }
    }
    bridge.getDisplayChannelBroadcaster().gotoBattleStep(battleId, step);
    final Territory retreatTo =
        getRemote(retreatingPlayer, bridge)
            .retreatQuery(
                battleId,
                (submerge || canDefendingSubsSubmergeOrRetreat),
                battleSite,
                availableTerritories,
                text);
    if (retreatTo != null && !availableTerritories.contains(retreatTo) && !subs) {
      log.severe(
          "Invalid retreat selection: "
              + retreatTo
              + " not in "
              + MyFormatter.defaultNamedToTextList(availableTerritories));
      return;
    }
    if (retreatTo != null) {
      // if attacker retreating non subs then its all over
      if (!defender && !subs && !planes && !partialAmphib) {
        // this is illegal in ww2v2 revised and beyond (the fighters should die). still checking if
        // illegal in classic.
        isOver = true;
      }
      if (!headless) {
        SoundUtils.playRetreatType(attacker, units, retreatType, bridge);
      }
      if (subs && battleSite.equals(retreatTo) && (submerge || canDefendingSubsSubmergeOrRetreat)) {
        submergeUnits(units, defender, bridge);
        final String messageShort = retreatingPlayer.getName() + " submerges subs";
        bridge
            .getDisplayChannelBroadcaster()
            .notifyRetreat(messageShort, messageShort, step, retreatingPlayer);
      } else if (planes) {
        retreatPlanes(units, defender, bridge);
        final String messageShort = retreatingPlayer.getName() + " retreats planes";
        bridge
            .getDisplayChannelBroadcaster()
            .notifyRetreat(messageShort, messageShort, step, retreatingPlayer);
      } else if (partialAmphib) {
        // remove amphib units from those retreating
        units = CollectionUtils.getMatches(units, Matches.unitWasNotAmphibious());
        retreatUnitsAndPlanes(units, retreatTo, defender, bridge);
        final String messageShort = retreatingPlayer.getName() + " retreats non-amphibious units";
        bridge
            .getDisplayChannelBroadcaster()
            .notifyRetreat(messageShort, messageShort, step, retreatingPlayer);
      } else {
        retreatUnits(units, retreatTo, defender, bridge);
        final String messageShort = retreatingPlayer.getName() + " retreats";
        final String messageLong;
        if (subs) {
          messageLong = retreatingPlayer.getName() + " retreats subs to " + retreatTo.getName();
        } else {
          messageLong =
              retreatingPlayer.getName() + " retreats all units to " + retreatTo.getName();
        }
        bridge
            .getDisplayChannelBroadcaster()
            .notifyRetreat(messageShort, messageLong, step, retreatingPlayer);
      }
    }
  }

  private void submergeUnits(
      final Collection<Unit> submerging, final boolean defender, final IDelegateBridge bridge) {
    final String transcriptText = MyFormatter.unitsToText(submerging) + " Submerged";
    final Collection<Unit> units = defender ? defendingUnits : attackingUnits;
    final Collection<Unit> unitsRetreated =
        defender ? defendingUnitsRetreated : attackingUnitsRetreated;
    final CompositeChange change = new CompositeChange();
    for (final Unit u : submerging) {
      change.add(ChangeFactory.unitPropertyChange(u, true, TripleAUnit.SUBMERGED));
    }
    bridge.addChange(change);
    units.removeAll(submerging);
    unitsRetreated.addAll(submerging);
    if (!units.isEmpty() && !isOver) {
      bridge.getDisplayChannelBroadcaster().notifyRetreat(battleId, submerging);
    }
    bridge.getHistoryWriter().addChildToEvent(transcriptText, new ArrayList<>(submerging));
  }

  private void retreatPlanes(
      final Collection<Unit> retreating, final boolean defender, final IDelegateBridge bridge) {
    final String transcriptText = MyFormatter.unitsToText(retreating) + " retreated";
    final Collection<Unit> units = defender ? defendingUnits : attackingUnits;
    final Collection<Unit> unitsRetreated =
        defender ? defendingUnitsRetreated : attackingUnitsRetreated;
    units.removeAll(retreating);
    unitsRetreated.removeAll(retreating);
    if (units.isEmpty() || isOver) {
      endBattle(bridge);
      if (defender) {
        attackerWins(bridge);
      } else {
        defenderWins(bridge);
      }
    } else {
      bridge.getDisplayChannelBroadcaster().notifyRetreat(battleId, retreating);
    }
    bridge.getHistoryWriter().addChildToEvent(transcriptText, new ArrayList<>(retreating));
  }

  private void retreatUnitsAndPlanes(
      final Collection<Unit> retreating,
      final Territory to,
      final boolean defender,
      final IDelegateBridge bridge) {
    // Remove air from battle
    final Collection<Unit> units = defender ? defendingUnits : attackingUnits;
    final Collection<Unit> unitsRetreated =
        defender ? defendingUnitsRetreated : attackingUnitsRetreated;
    units.removeAll(CollectionUtils.getMatches(units, Matches.unitIsAir()));
    // add all land units' dependents
    retreating.addAll(getDependentUnits(units));
    // our own air units don't retreat with land units
    final Predicate<Unit> notMyAir =
        Matches.unitIsNotAir().or(Matches.unitIsOwnedBy(attacker).negate());
    final Collection<Unit> nonAirRetreating = CollectionUtils.getMatches(retreating, notMyAir);
    final String transcriptText =
        MyFormatter.unitsToTextNoOwner(nonAirRetreating) + " retreated to " + to.getName();
    bridge.getHistoryWriter().addChildToEvent(transcriptText, new ArrayList<>(nonAirRetreating));
    final CompositeChange change = new CompositeChange();
    change.add(ChangeFactory.moveUnits(battleSite, to, nonAirRetreating));
    if (isOver) {
      final Collection<IBattle> dependentBattles = battleTracker.getBlocked(this);
      // If there are no dependent battles, check landings in allied territories
      if (dependentBattles.isEmpty()) {
        change.add(retreatFromNonCombat(nonAirRetreating, to));
        // Else retreat the units from combat when their transport retreats
      } else {
        change.add(retreatFromDependents(nonAirRetreating, to, dependentBattles));
      }
    }
    bridge.addChange(change);
    units.removeAll(nonAirRetreating);
    unitsRetreated.addAll(nonAirRetreating);
    if (units.isEmpty() || isOver) {
      endBattle(bridge);
      if (defender) {
        attackerWins(bridge);
      } else {
        defenderWins(bridge);
      }
    } else {
      bridge.getDisplayChannelBroadcaster().notifyRetreat(battleId, retreating);
    }
  }

  /** Added for test case calls. */
  @VisibleForTesting
  public void externalRetreat(
      final Collection<Unit> retreaters,
      final Territory retreatTo,
      final boolean defender,
      final IDelegateBridge bridge) {
    isOver = true;
    retreatUnits(retreaters, retreatTo, defender, bridge);
  }

  private void retreatUnits(
      final Collection<Unit> initialRetreating,
      final Territory to,
      final boolean defender,
      final IDelegateBridge bridge) {
    Collection<Unit> retreating = initialRetreating;
    retreating.addAll(getDependentUnits(retreating));
    // our own air units don't retreat with land units
    final Predicate<Unit> notMyAir =
        Matches.unitIsNotAir().or(Matches.unitIsOwnedBy(attacker).negate());
    retreating = CollectionUtils.getMatches(retreating, notMyAir);
    final String transcriptText;
    // in WW2V1, defending subs can retreat so show owner
    if (isWW2V2()) {
      transcriptText = MyFormatter.unitsToTextNoOwner(retreating) + " retreated to " + to.getName();
    } else {
      transcriptText = MyFormatter.unitsToText(retreating) + " retreated to " + to.getName();
    }
    bridge.getHistoryWriter().addChildToEvent(transcriptText, new ArrayList<>(retreating));
    final CompositeChange change = new CompositeChange();
    change.add(ChangeFactory.moveUnits(battleSite, to, retreating));
    if (isOver) {
      final Collection<IBattle> dependentBattles = battleTracker.getBlocked(this);
      // If there are no dependent battles, check landings in allied territories
      if (dependentBattles.isEmpty()) {
        change.add(retreatFromNonCombat(retreating, to));
        // Else retreat the units from combat when their transport retreats
      } else {
        change.add(retreatFromDependents(retreating, to, dependentBattles));
      }
    }
    bridge.addChange(change);
    final Collection<Unit> units = defender ? defendingUnits : attackingUnits;
    final Collection<Unit> unitsRetreated =
        defender ? defendingUnitsRetreated : attackingUnitsRetreated;
    units.removeAll(retreating);
    unitsRetreated.addAll(retreating);
    if (units.isEmpty() || isOver) {
      endBattle(bridge);
      if (defender) {
        attackerWins(bridge);
      } else {
        defenderWins(bridge);
      }
    } else {
      bridge.getDisplayChannelBroadcaster().notifyRetreat(battleId, retreating);
    }
  }

  /** Retreat landed units from allied territory when their transport retreats. */
  private Change retreatFromNonCombat(final Collection<Unit> units, final Territory retreatTo) {
    final CompositeChange change = new CompositeChange();
    final Collection<Unit> transports =
        CollectionUtils.getMatches(units, Matches.unitIsTransport());
    final Collection<Unit> retreated = getTransportDependents(transports);
    if (!retreated.isEmpty()) {
      for (final Unit unit : transports) {
        final Territory retreatedFrom = TransportTracker.getTerritoryTransportHasUnloadedTo(unit);
        if (retreatedFrom != null) {
          TransportTracker.reloadTransports(transports, change);
          change.add(ChangeFactory.moveUnits(retreatedFrom, retreatTo, retreated));
        }
      }
    }
    return change;
  }

  private Change retreatFromDependents(
      final Collection<Unit> units,
      final Territory retreatTo,
      final Collection<IBattle> dependentBattles) {
    final CompositeChange change = new CompositeChange();
    for (final IBattle dependent : dependentBattles) {
      final Route route = new Route(battleSite, dependent.getTerritory());
      final Collection<Unit> retreatedUnits = dependent.getDependentUnits(units);
      dependent.removeAttack(route, retreatedUnits);
      TransportTracker.reloadTransports(units, change);
      change.add(ChangeFactory.moveUnits(dependent.getTerritory(), retreatTo, retreatedUnits));
    }
    return change;
  }

  private void defenderWins(final IDelegateBridge bridge) {
    whoWon = WhoWon.DEFENDER;
    bridge.getDisplayChannelBroadcaster().battleEnd(battleId, defender.getName() + " win");
    if (Properties.getAbandonedTerritoriesMayBeTakenOverImmediately(gameData)) {
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

  private boolean isWW2V2() {
    return Properties.getWW2V2(gameData);
  }

  private boolean isWW2V3() {
    return Properties.getWW2V3(gameData);
  }

  private boolean isPartialAmphibiousRetreat() {
    return Properties.getPartialAmphibiousRetreat(gameData);
  }

  private boolean canSubsSubmerge() {
    return Properties.getSubmersibleSubs(gameData);
  }

  private boolean isSubRetreatBeforeBattle() {
    return Properties.getSubRetreatBeforeBattle(gameData);
  }

  private boolean isTransportCasualtiesRestricted() {
    return Properties.getTransportCasualtiesRestricted(gameData);
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
