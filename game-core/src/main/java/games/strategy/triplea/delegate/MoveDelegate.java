package games.strategy.triplea.delegate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.AutoSave;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.MapSupport;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.AbstractTriggerAttachment;
import games.strategy.triplea.attachments.ICondition;
import games.strategy.triplea.attachments.TriggerAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.dataObjects.MoveValidationResult;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;
import games.strategy.util.PredicateBuilder;
import games.strategy.util.Tuple;

/**
 * Responsible for moving units on the board.
 *
 * <p>
 * Responsible for checking the validity of a move, and for moving the units.
 * </p>
 */
@MapSupport
@AutoSave(afterStepEnd = true)
public class MoveDelegate extends AbstractMoveDelegate {

  public static final String CLEANING_UP_DURING_MOVEMENT_PHASE = "Cleaning up during movement phase";

  // needToInitialize means we only do certain things once, so that if a game is saved then
  // loaded, they aren't done again
  private boolean needToInitialize = true;
  private boolean needToDoRockets = true;
  private IntegerMap<Territory> pusLost = new IntegerMap<>();

  public MoveDelegate() {}

  @Override
  public void setDelegateBridgeAndPlayer(final IDelegateBridge delegateBridge) {
    super.setDelegateBridgeAndPlayer(new GameDelegateBridge(delegateBridge));
  }

  @Override
  public void start() {
    super.start();
    final GameData data = getData();
    if (needToInitialize) {

      // territory property changes triggered at beginning of combat move
      // TODO create new delegate called "start of turn" and move them there.
      // First set up a match for what we want to have fire as a default in this delegate. List out as a composite match
      // OR. use 'null, null' because this is the Default firing location for any trigger that does NOT have 'when' set.
      HashMap<ICondition, Boolean> testedConditions = null;
      final Predicate<TriggerAttachment> moveCombatDelegateBeforeBonusTriggerMatch =
          AbstractTriggerAttachment.availableUses
              .and(AbstractTriggerAttachment.whenOrDefaultMatch(null, null))
              .and(AbstractTriggerAttachment.notificationMatch()
                  .or(TriggerAttachment.playerPropertyMatch())
                  .or(TriggerAttachment.relationshipTypePropertyMatch())
                  .or(TriggerAttachment.territoryPropertyMatch())
                  .or(TriggerAttachment.territoryEffectPropertyMatch())
                  .or(TriggerAttachment.removeUnitsMatch())
                  .or(TriggerAttachment.changeOwnershipMatch()));
      final Predicate<TriggerAttachment> moveCombatDelegateAfterBonusTriggerMatch =
          AbstractTriggerAttachment.availableUses
              .and(AbstractTriggerAttachment.whenOrDefaultMatch(null, null))
              .and(TriggerAttachment.placeMatch());
      final Predicate<TriggerAttachment> moveCombatDelegateAllTriggerMatch = moveCombatDelegateBeforeBonusTriggerMatch
          .or(moveCombatDelegateAfterBonusTriggerMatch);
      if (GameStepPropertiesHelper.isCombatMove(data) && Properties.getTriggers(data)) {
        final HashSet<TriggerAttachment> toFirePossible = TriggerAttachment.collectForAllTriggersMatching(
            new HashSet<>(Collections.singleton(player)), moveCombatDelegateAllTriggerMatch);
        if (!toFirePossible.isEmpty()) {

          // collect conditions and test them for ALL triggers, both those that we will fire before and those we will
          // fire after.
          testedConditions = TriggerAttachment.collectTestsForAllTriggers(toFirePossible, bridge);
          final HashSet<TriggerAttachment> toFireBeforeBonus =
              TriggerAttachment.collectForAllTriggersMatching(new HashSet<>(Collections.singleton(player)),
                  moveCombatDelegateBeforeBonusTriggerMatch);
          if (!toFireBeforeBonus.isEmpty()) {

            // get all triggers that are satisfied based on the tested conditions.
            final Set<TriggerAttachment> toFireTestedAndSatisfied = new HashSet<>(CollectionUtils
                .getMatches(toFireBeforeBonus, AbstractTriggerAttachment.isSatisfiedMatch(testedConditions)));

            // now list out individual types to fire, once for each of the matches above.
            TriggerAttachment.triggerNotifications(toFireTestedAndSatisfied, bridge, null, null, true, true, true,
                true);
            TriggerAttachment.triggerPlayerPropertyChange(toFireTestedAndSatisfied, bridge, null, null, true, true,
                true, true);
            TriggerAttachment.triggerRelationshipTypePropertyChange(toFireTestedAndSatisfied, bridge, null, null,
                true, true, true, true);
            TriggerAttachment.triggerTerritoryPropertyChange(toFireTestedAndSatisfied, bridge, null, null, true,
                true, true, true);
            TriggerAttachment.triggerTerritoryEffectPropertyChange(toFireTestedAndSatisfied, bridge, null, null,
                true, true, true, true);
            TriggerAttachment.triggerChangeOwnership(toFireTestedAndSatisfied, bridge, null, null, true, true, true,
                true);
            TriggerAttachment.triggerUnitRemoval(toFireTestedAndSatisfied, bridge, null, null, true, true, true,
                true);
          }
        }
      }

      // repair 2-hit units at beginning of turn (some maps have combat move before purchase, so i think it is better to
      // do this at beginning of combat move)
      if (GameStepPropertiesHelper.isRepairUnits(data)) {
        MoveDelegate.repairMultipleHitPointUnits(bridge, player);
      }

      // reset any bonus of units, and give movement to units which begin the turn in the same territory as units with
      // giveMovement (like air and naval bases)
      if (GameStepPropertiesHelper.isGiveBonusMovement(data)) {
        resetAndGiveBonusMovement();
      }

      // take away all movement from allied fighters sitting on damaged carriers
      removeMovementFromAirOnDamagedAlliedCarriers(bridge, player);

      // placing triggered units at beginning of combat move, but after bonuses and repairing, etc, have been done.
      if (GameStepPropertiesHelper.isCombatMove(data) && Properties.getTriggers(data)) {
        final HashSet<TriggerAttachment> toFireAfterBonus = TriggerAttachment.collectForAllTriggersMatching(
            new HashSet<>(Collections.singleton(player)), moveCombatDelegateAfterBonusTriggerMatch);
        if (!toFireAfterBonus.isEmpty()) {

          // get all triggers that are satisfied based on the tested conditions.
          final Set<TriggerAttachment> toFireTestedAndSatisfied = new HashSet<>(CollectionUtils
              .getMatches(toFireAfterBonus, AbstractTriggerAttachment.isSatisfiedMatch(testedConditions)));

          // now list out individual types to fire, once for each of the matches above.
          TriggerAttachment.triggerUnitPlacement(toFireTestedAndSatisfied, bridge, null, null, true, true, true,
              true);
        }
      }
      if (GameStepPropertiesHelper.isResetUnitStateAtStart(data)) {
        resetUnitStateAndDelegateState();
      }
      needToInitialize = false;
    }
  }

  private void resetAndGiveBonusMovement() {
    boolean addedHistoryEvent = false;
    final Change changeReset = resetBonusMovement();
    if (!changeReset.isEmpty()) {
      bridge.getHistoryWriter().startEvent("Resetting and Giving Bonus Movement to Units");
      bridge.addChange(changeReset);
      addedHistoryEvent = true;
    }
    Change changeBonus = null;
    if (Properties.getUnitsMayGiveBonusMovement(getData())) {
      changeBonus = giveBonusMovement(bridge, player);
    }
    if ((changeBonus != null) && !changeBonus.isEmpty()) {
      if (!addedHistoryEvent) {
        bridge.getHistoryWriter().startEvent("Resetting and Giving Bonus Movement to Units");
      }
      bridge.addChange(changeBonus);
    }
  }

  @Override
  public void end() {
    super.end();
    final GameData data = getData();
    if (GameStepPropertiesHelper.isRemoveAirThatCanNotLand(data)) {
      removeAirThatCantLand();
    }

    // WW2V2/WW2V3, fires at end of combat move
    // WW2V1, fires at end of non combat move
    if (GameStepPropertiesHelper.isFireRockets(data)) {
      if (needToDoRockets && TechTracker.hasRocket(bridge.getPlayerId())) {
        RocketsFireHelper.fireRockets(bridge, bridge.getPlayerId());
        needToDoRockets = false;
      }
    }

    // do at the end of the round, if we do it at the start of non combat, then we may do it in the middle of the round,
    // while loading.
    if (GameStepPropertiesHelper.isResetUnitStateAtEnd(data)) {
      resetUnitStateAndDelegateState();
    } else {

      // Only air units can move during both CM and NCM in the same turn so moved units are set to no moves left
      final List<Unit> alreadyMovedNonAirUnits =
          CollectionUtils.getMatches(data.getUnits().getUnits(), Matches.unitHasMoved().and(Matches.unitIsNotAir()));
      bridge.addChange(ChangeFactory.markNoMovementChange(alreadyMovedNonAirUnits));
    }
    needToInitialize = true;
    needToDoRockets = true;
  }

  @Override
  public Serializable saveState() {
    final MoveExtendedDelegateState state = new MoveExtendedDelegateState();
    state.superState = super.saveState();
    state.m_needToInitialize = needToInitialize;
    state.m_needToDoRockets = needToDoRockets;
    state.m_PUsLost = pusLost;
    return state;
  }

  @Override
  public void loadState(final Serializable state) {
    final MoveExtendedDelegateState s = (MoveExtendedDelegateState) state;
    super.loadState(s.superState);
    needToInitialize = s.m_needToInitialize;
    needToDoRockets = s.m_needToDoRockets;
    pusLost = s.m_PUsLost;
  }

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    final Predicate<Unit> moveableUnitOwnedByMe = PredicateBuilder.of(Matches.unitIsOwnedBy(player))
        // right now, land units on transports have movement taken away when they their transport moves
        .and(Matches.unitHasMovementLeft()
            .or(Matches.unitIsLand().and(Matches.unitIsBeingTransported())))
        // if not non combat, cannot move aa units
        .andIf(GameStepPropertiesHelper.isCombatMove(getData()), Matches.unitCanNotMoveDuringCombatMove().negate())
        .build();
    return !getData().getMap().getTerritories().isEmpty()
        && getData().getMap().getTerritories().stream()
            .map(Territory::getUnits)
            .anyMatch(units -> units.anyMatch(moveableUnitOwnedByMe));
  }

  private Change resetBonusMovement() {
    final GameData data = getData();
    final CompositeChange change = new CompositeChange();
    for (final Unit u : data.getUnits()) {
      if (TripleAUnit.get(u).getBonusMovement() != 0) {
        change.add(ChangeFactory.unitPropertyChange(u, 0, TripleAUnit.BONUS_MOVEMENT));
      }
    }
    return change;
  }

  private void resetUnitStateAndDelegateState() {
    // while not a 'unit state', this is fine here for now. since we only have one instance of this delegate, as long as
    // it gets cleared once per player's turn block, we are fine.
    pusLost.clear();
    final Change change = getResetUnitStateChange(getData());
    if (!change.isEmpty()) {
      // if no non-combat occurred, we may have cleanup left from combat
      // that we need to spawn an event for
      bridge.getHistoryWriter().startEvent(CLEANING_UP_DURING_MOVEMENT_PHASE);
      bridge.addChange(change);
    }
  }

  static Change getResetUnitStateChange(final GameData data) {
    final CompositeChange change = new CompositeChange();
    for (final Unit u : data.getUnits()) {
      final TripleAUnit taUnit = TripleAUnit.get(u);
      if (taUnit.getAlreadyMoved() != 0) {
        change.add(ChangeFactory.unitPropertyChange(u, 0, TripleAUnit.ALREADY_MOVED));
      }
      if (taUnit.getWasInCombat()) {
        change.add(ChangeFactory.unitPropertyChange(u, false, TripleAUnit.WAS_IN_COMBAT));
      }
      if (taUnit.getSubmerged()) {
        change.add(ChangeFactory.unitPropertyChange(u, false, TripleAUnit.SUBMERGED));
      }
      if (taUnit.getAirborne()) {
        change.add(ChangeFactory.unitPropertyChange(u, false, TripleAUnit.AIRBORNE));
      }
      if (taUnit.getLaunched() != 0) {
        change.add(ChangeFactory.unitPropertyChange(u, 0, TripleAUnit.LAUNCHED));
      }
      if (!taUnit.getUnloaded().isEmpty()) {
        change.add(ChangeFactory.unitPropertyChange(u, Collections.EMPTY_LIST, TripleAUnit.UNLOADED));
      }
      if (taUnit.getWasLoadedThisTurn()) {
        change.add(ChangeFactory.unitPropertyChange(u, Boolean.FALSE, TripleAUnit.LOADED_THIS_TURN));
      }
      if (taUnit.getUnloadedTo() != null) {
        change.add(ChangeFactory.unitPropertyChange(u, null, TripleAUnit.UNLOADED_TO));
      }
      if (taUnit.getWasUnloadedInCombatPhase()) {
        change.add(ChangeFactory.unitPropertyChange(u, Boolean.FALSE, TripleAUnit.UNLOADED_IN_COMBAT_PHASE));
      }
      if (taUnit.getWasAmphibious()) {
        change.add(ChangeFactory.unitPropertyChange(u, Boolean.FALSE, TripleAUnit.UNLOADED_AMPHIBIOUS));
      }
    }
    return change;
  }

  private static void removeMovementFromAirOnDamagedAlliedCarriers(final IDelegateBridge bridge,
      final PlayerID player) {
    final GameData data = bridge.getData();
    final Predicate<Unit> crippledAlliedCarriersMatch = Matches.isUnitAllied(player, data)
        .and(Matches.unitIsOwnedBy(player).negate())
        .and(Matches.unitIsCarrier())
        .and(Matches.unitHasWhenCombatDamagedEffect(UnitAttachment.UNITSMAYNOTLEAVEALLIEDCARRIER));
    final Predicate<Unit> ownedFightersMatch = Matches.unitIsOwnedBy(player)
        .and(Matches.unitIsAir())
        .and(Matches.unitCanLandOnCarrier())
        .and(Matches.unitHasMovementLeft());
    final CompositeChange change = new CompositeChange();
    for (final Territory t : data.getMap().getTerritories()) {
      final Collection<Unit> ownedFighters = t.getUnits().getMatches(ownedFightersMatch);
      if (ownedFighters.isEmpty()) {
        continue;
      }
      final Collection<Unit> crippledAlliedCarriers =
          CollectionUtils.getMatches(t.getUnits().getUnits(), crippledAlliedCarriersMatch);
      if (crippledAlliedCarriers.isEmpty()) {
        continue;
      }
      for (final Unit fighter : ownedFighters) {
        final TripleAUnit taUnit = (TripleAUnit) fighter;
        if (taUnit.getTransportedBy() != null) {
          if (crippledAlliedCarriers.contains(taUnit.getTransportedBy())) {
            change.add(ChangeFactory.markNoMovementChange(fighter));
          }
        }
      }
    }
    if (!change.isEmpty()) {
      bridge.addChange(change);
    }
  }

  private static Change giveBonusMovement(final IDelegateBridge bridge, final PlayerID player) {
    final GameData data = bridge.getData();
    final CompositeChange change = new CompositeChange();
    for (final Territory t : data.getMap().getTerritories()) {
      change.add(giveBonusMovementToUnits(player, data, t));
    }
    return change;
  }

  static Change giveBonusMovementToUnits(final PlayerID player, final GameData data, final Territory t) {
    final CompositeChange change = new CompositeChange();
    for (final Unit u : t.getUnits().getUnits()) {
      if (Matches.unitCanBeGivenBonusMovementByFacilitiesInItsTerritory(t, player, data).test(u)) {
        if (!Matches.isUnitAllied(player, data).test(u)) {
          continue;
        }
        int bonusMovement = Integer.MIN_VALUE;
        final Collection<Unit> givesBonusUnits = new ArrayList<>();
        final Predicate<Unit> givesBonusUnit = Matches.alliedUnit(player, data)
            .and(Matches.unitCanGiveBonusMovementToThisUnit(u));
        givesBonusUnits.addAll(CollectionUtils.getMatches(t.getUnits().getUnits(), givesBonusUnit));
        if (Matches.unitIsSea().test(u)) {
          final Predicate<Unit> givesBonusUnitLand = givesBonusUnit.and(Matches.unitIsLand());
          final Set<Territory> neighbors = new HashSet<>(data.getMap().getNeighbors(t, Matches.territoryIsLand()));
          for (final Territory current : neighbors) {
            givesBonusUnits.addAll(CollectionUtils.getMatches(current.getUnits().getUnits(), givesBonusUnitLand));
          }
        } else if (Matches.unitIsLand().test(u)) {
          final Predicate<Unit> givesBonusUnitSea = givesBonusUnit.and(Matches.unitIsSea());
          final Set<Territory> neighbors = new HashSet<>(data.getMap().getNeighbors(t, Matches.territoryIsWater()));
          for (final Territory current : neighbors) {
            givesBonusUnits.addAll(CollectionUtils.getMatches(current.getUnits().getUnits(), givesBonusUnitSea));
          }
        }
        for (final Unit bonusGiver : givesBonusUnits) {
          final int tempBonus = UnitAttachment.get(bonusGiver.getType()).getGivesMovement().getInt(u.getType());
          if (tempBonus > bonusMovement) {
            bonusMovement = tempBonus;
          }
        }
        if ((bonusMovement != Integer.MIN_VALUE) && (bonusMovement != 0)) {
          bonusMovement = Math.max(bonusMovement, (UnitAttachment.get(u.getType()).getMovement(player) * -1));
          change.add(ChangeFactory.unitPropertyChange(u, bonusMovement, TripleAUnit.BONUS_MOVEMENT));
        }
      }
    }
    return change;
  }

  static void repairMultipleHitPointUnits(final IDelegateBridge bridge, final PlayerID player) {
    final GameData data = bridge.getData();
    final boolean repairOnlyOwn =
        Properties.getBattleshipsRepairAtBeginningOfRound(bridge.getData());
    final Predicate<Unit> damagedUnits = Matches.unitHasMoreThanOneHitPointTotal()
        .and(Matches.unitHasTakenSomeDamage());
    final Predicate<Unit> damagedUnitsOwned = damagedUnits.and(Matches.unitIsOwnedBy(player));
    final Map<Territory, Set<Unit>> damagedMap = new HashMap<>();
    for (final Territory current : data.getMap().getTerritories()) {
      final Set<Unit> damaged;
      if (!Properties.getTwoHitPointUnitsRequireRepairFacilities(data)) {
        damaged = new HashSet<>(current.getUnits().getMatches(repairOnlyOwn ? damagedUnitsOwned : damagedUnits));
      } else {
        damaged = new HashSet<>(current.getUnits().getMatches(damagedUnitsOwned
            .and(Matches.unitCanBeRepairedByFacilitiesInItsTerritory(current, player, data))));
      }
      if (!damaged.isEmpty()) {
        damagedMap.put(current, damaged);
      }
    }
    if (damagedMap.isEmpty()) {
      return;
    }
    final Map<Unit, Territory> fullyRepaired = new HashMap<>();
    final IntegerMap<Unit> newHitsMap = new IntegerMap<>();
    for (final Entry<Territory, Set<Unit>> entry : damagedMap.entrySet()) {
      for (final Unit u : entry.getValue()) {
        final int repairAmount = getLargestRepairRateForThisUnit(u, entry.getKey(), data);
        final int currentHits = u.getHits();
        final int newHits = Math.max(0, Math.min(currentHits, (currentHits - repairAmount)));
        if (newHits != currentHits) {
          newHitsMap.put(u, newHits);
        }
        if (newHits <= 0) {
          fullyRepaired.put(u, entry.getKey());
        }
      }
    }
    bridge.getHistoryWriter().startEvent(
        newHitsMap.size() + " " + MyFormatter.pluralize("unit", newHitsMap.size()) + " repaired.",
        new HashSet<>(newHitsMap.keySet()));
    bridge.addChange(ChangeFactory.unitsHit(newHitsMap));

    // now if damaged includes any carriers that are repairing, and have damaged abilities set for not allowing air
    // units to leave while damaged, we need to remove those air units now
    final Collection<Unit> damagedCarriers = CollectionUtils.getMatches(fullyRepaired.keySet(),
        Matches.unitHasWhenCombatDamagedEffect(UnitAttachment.UNITSMAYNOTLEAVEALLIEDCARRIER));

    // now cycle through those now-repaired carriers, and remove allied air from being dependent
    final CompositeChange clearAlliedAir = new CompositeChange();
    for (final Unit carrier : damagedCarriers) {
      final CompositeChange change = MustFightBattle.clearTransportedByForAlliedAirOnCarrier(
          Collections.singleton(carrier), fullyRepaired.get(carrier), carrier.getOwner(), data);
      if (!change.isEmpty()) {
        clearAlliedAir.add(change);
      }
    }
    if (!clearAlliedAir.isEmpty()) {
      bridge.addChange(clearAlliedAir);
    }

    // Check if any repaired units change into different unit types
    for (final Territory territory : damagedMap.keySet()) {
      repairedChangeInto(damagedMap.get(territory), territory, bridge);
    }
  }

  private static void repairedChangeInto(final Set<Unit> units, final Territory territory,
      final IDelegateBridge bridge) {
    final List<Unit> changesIntoUnits =
        CollectionUtils.getMatches(units, Matches.unitWhenHitPointsRepairedChangesInto());
    final CompositeChange changes = new CompositeChange();
    final List<Unit> unitsToRemove = new ArrayList<>();
    final List<Unit> unitsToAdd = new ArrayList<>();
    for (final Unit unit : changesIntoUnits) {
      final Map<Integer, Tuple<Boolean, UnitType>> map =
          UnitAttachment.get(unit.getType()).getWhenHitPointsRepairedChangesInto();
      if (map.containsKey(unit.getHits())) {
        final boolean translateAttributes = map.get(unit.getHits()).getFirst();
        final UnitType unitType = map.get(unit.getHits()).getSecond();
        final List<Unit> toAdd = unitType.create(1, unit.getOwner());
        if (translateAttributes) {
          final Change translate = TripleAUnit.translateAttributesToOtherUnits(unit, toAdd, territory);
          changes.add(translate);
        }
        unitsToRemove.add(unit);
        unitsToAdd.addAll(toAdd);
      }
    }
    if (!unitsToRemove.isEmpty()) {
      bridge.addChange(changes);
      final String removeText = MyFormatter.unitsToText(unitsToRemove) + " removed in " + territory.getName();
      bridge.getHistoryWriter().addChildToEvent(removeText, new ArrayList<>(unitsToRemove));
      bridge.addChange(ChangeFactory.removeUnits(territory, unitsToRemove));
      final String addText = MyFormatter.unitsToText(unitsToAdd) + " added in " + territory.getName();
      bridge.getHistoryWriter().addChildToEvent(addText, new ArrayList<>(unitsToAdd));
      bridge.addChange(ChangeFactory.addUnits(territory, unitsToAdd));
    }
  }

  /**
   * This has to be the exact same as Matches.UnitCanBeRepairedByFacilitiesInItsTerritory()
   */
  private static int getLargestRepairRateForThisUnit(final Unit unitToBeRepaired, final Territory territoryUnitIsIn,
      final GameData data) {
    if (!Properties.getTwoHitPointUnitsRequireRepairFacilities(data)) {
      return 1;
    }
    final Set<Unit> repairUnitsForThisUnit = new HashSet<>();
    final PlayerID owner = unitToBeRepaired.getOwner();
    final Predicate<Unit> repairUnit = Matches.alliedUnit(owner, data)
        .and(Matches.unitCanRepairOthers())
        .and(Matches.unitCanRepairThisUnit(unitToBeRepaired, territoryUnitIsIn));
    repairUnitsForThisUnit.addAll(territoryUnitIsIn.getUnits().getMatches(repairUnit));
    if (Matches.unitIsSea().test(unitToBeRepaired)) {
      final List<Territory> neighbors =
          new ArrayList<>(data.getMap().getNeighbors(territoryUnitIsIn, Matches.territoryIsLand()));
      for (final Territory current : neighbors) {
        final Predicate<Unit> repairUnitLand = Matches.alliedUnit(owner, data)
            .and(Matches.unitCanRepairOthers())
            .and(Matches.unitCanRepairThisUnit(unitToBeRepaired, current))
            .and(Matches.unitIsLand());
        repairUnitsForThisUnit.addAll(current.getUnits().getMatches(repairUnitLand));
      }
    } else if (Matches.unitIsLand().test(unitToBeRepaired)) {
      final List<Territory> neighbors =
          new ArrayList<>(data.getMap().getNeighbors(territoryUnitIsIn, Matches.territoryIsWater()));
      for (final Territory current : neighbors) {
        final Predicate<Unit> repairUnitSea = Matches.alliedUnit(owner, data)
            .and(Matches.unitCanRepairOthers())
            .and(Matches.unitCanRepairThisUnit(unitToBeRepaired, current))
            .and(Matches.unitIsSea());
        repairUnitsForThisUnit.addAll(current.getUnits().getMatches(repairUnitSea));
      }
    }
    int largest = 0;
    for (final Unit u : repairUnitsForThisUnit) {
      final int repair = UnitAttachment.get(u.getType()).getRepairsUnits().getInt(unitToBeRepaired.getType());
      if (largest < repair) {
        largest = repair;
      }
    }
    return largest;
  }

  @Override
  public String move(final Collection<Unit> units, final Route route, final Collection<Unit> transportsThatCanBeLoaded,
      final Map<Unit, Collection<Unit>> newDependents) {
    final GameData data = getData();

    // the reason we use this, is if we are in edit mode, we may have a different unit owner than the current player
    final PlayerID player = getUnitsOwner(units);
    final MoveValidationResult result = MoveValidator.validateMove(units, route, player, transportsThatCanBeLoaded,
        newDependents, GameStepPropertiesHelper.isNonCombatMove(data, false), movesToUndo, data);
    final StringBuilder errorMsg = new StringBuilder(100);
    final int numProblems = result.getTotalWarningCount() - (result.hasError() ? 0 : 1);
    final String numErrorsMsg = (numProblems > 0)
        ? ("; " + numProblems + " " + MyFormatter.pluralize("error", numProblems) + " not shown")
        : "";
    if (result.hasError()) {
      return errorMsg.append(result.getError()).append(numErrorsMsg).toString();
    }
    if (result.hasDisallowedUnits()) {
      return errorMsg.append(result.getDisallowedUnitWarning(0)).append(numErrorsMsg).toString();
    }
    boolean isKamikaze = false;
    final boolean getKamikazeAir = Properties.getKamikazeAirplanes(data);
    Collection<Unit> kamikazeUnits = new ArrayList<>();

    // confirm kamikaze moves, and remove them from unresolved units
    if (getKamikazeAir || units.stream().anyMatch(Matches.unitIsKamikaze())) {
      kamikazeUnits = result.getUnresolvedUnits(MoveValidator.NOT_ALL_AIR_UNITS_CAN_LAND);
      if ((kamikazeUnits.size() > 0) && getRemotePlayer().confirmMoveKamikaze()) {
        for (final Unit unit : kamikazeUnits) {
          if (getKamikazeAir || Matches.unitIsKamikaze().test(unit)) {
            result.removeUnresolvedUnit(MoveValidator.NOT_ALL_AIR_UNITS_CAN_LAND, unit);
            isKamikaze = true;
          }
        }
      }
    }
    if (result.hasUnresolvedUnits()) {
      return errorMsg.append(result.getUnresolvedUnitWarning(0)).append(numErrorsMsg).toString();
    }

    // allow user to cancel move if aa guns will fire
    final AAInMoveUtil aaInMoveUtil = new AAInMoveUtil();
    aaInMoveUtil.initialize(bridge);
    final Collection<Territory> aaFiringTerritores = aaInMoveUtil.getTerritoriesWhereAaWillFire(route, units);
    if (!aaFiringTerritores.isEmpty()) {
      if (!getRemotePlayer().confirmMoveInFaceOfAa(aaFiringTerritores)) {
        return null;
      }
    }

    // do the move
    final UndoableMove currentMove = new UndoableMove(units, route);
    final String transcriptText = MyFormatter.unitsToTextNoOwner(units) + " moved from " + route.getStart().getName()
        + " to " + route.getEnd().getName();
    bridge.getHistoryWriter().startEvent(transcriptText, currentMove.getDescriptionObject());
    if (isKamikaze) {
      bridge.getHistoryWriter().addChildToEvent("This was a kamikaze move, for at least some of the units",
          kamikazeUnits);
    }
    tempMovePerformer = new MovePerformer();
    tempMovePerformer.initialize(this);
    tempMovePerformer.moveUnits(units, route, player, transportsThatCanBeLoaded, newDependents, currentMove);
    tempMovePerformer = null;
    return null;
  }

  static Collection<Territory> getEmptyNeutral(final Route route) {
    final Predicate<Territory> emptyNeutral = Matches.territoryIsEmpty().and(Matches.territoryIsNeutralButNotWater());
    final Collection<Territory> neutral = route.getMatches(emptyNeutral);
    return neutral;
  }

  private void removeAirThatCantLand() {
    final GameData data = getData();
    final boolean lhtrCarrierProd = AirThatCantLandUtil.isLhtrCarrierProduction(data)
        || AirThatCantLandUtil.isLandExistingFightersOnNewCarriers(data);
    boolean hasProducedCarriers = false;
    for (final PlayerID p : GameStepPropertiesHelper.getCombinedTurns(data, player)) {
      if (p.getUnits().anyMatch(Matches.unitIsCarrier())) {
        hasProducedCarriers = true;
        break;
      }
    }
    final AirThatCantLandUtil util = new AirThatCantLandUtil(bridge);
    util.removeAirThatCantLand(player, lhtrCarrierProd && hasProducedCarriers);

    // if edit mode has been on, we need to clean up after all players
    for (final PlayerID player : data.getPlayerList()) {

      // Check if player still has units to place
      if (!player.equals(this.player)) {
        util.removeAirThatCantLand(player,
            ((player.getUnits().anyMatch(Matches.unitIsCarrier()) || hasProducedCarriers) && lhtrCarrierProd));
      }
    }
  }

  @Override
  public int pusAlreadyLost(final Territory t) {
    return pusLost.getInt(t);
  }

  @Override
  public void pusLost(final Territory t, final int amt) {
    pusLost.add(t, amt);
  }
}
