package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.AutoSave;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.UnitUtils;
import games.strategy.triplea.attachments.AbstractTriggerAttachment;
import games.strategy.triplea.attachments.FireTriggerParams;
import games.strategy.triplea.attachments.ICondition;
import games.strategy.triplea.attachments.TriggerAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.data.MoveValidationResult;
import games.strategy.triplea.delegate.move.validation.AirMovementValidator;
import games.strategy.triplea.delegate.move.validation.MoveValidator;
import games.strategy.triplea.formatter.MyFormatter;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.triplea.java.PredicateBuilder;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Tuple;

/**
 * Responsible for moving units on the board.
 *
 * <p>Responsible for checking the validity of a move, and for moving the units.
 */
@AutoSave(afterStepEnd = true)
public class MoveDelegate extends AbstractMoveDelegate {

  public static final String CLEANING_UP_DURING_MOVEMENT_PHASE =
      "Cleaning up during movement phase";

  // needToInitialize means we only do certain things once, so that if a game is saved then
  // loaded, they aren't done again
  private boolean needToInitialize = true;
  private boolean needToDoRockets = true;
  private IntegerMap<Territory> pusLost = new IntegerMap<>();

  @Override
  public void start() {
    super.start();
    final GameData data = getData();
    if (needToInitialize) {

      // territory property changes triggered at beginning of combat move
      // TODO create new delegate called "start of turn" and move them there.
      // First set up a match for what we want to have fire as a default in this delegate. List out
      // as a composite match
      // OR. use 'null, null' because this is the Default firing location for any trigger that does
      // NOT have 'when' set.
      Map<ICondition, Boolean> testedConditions = null;
      final Predicate<TriggerAttachment> moveCombatDelegateBeforeBonusTriggerMatch =
          AbstractTriggerAttachment.availableUses
              .and(AbstractTriggerAttachment.whenOrDefaultMatch(null, null))
              .and(
                  AbstractTriggerAttachment.notificationMatch()
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
      final Predicate<TriggerAttachment> moveCombatDelegateAllTriggerMatch =
          moveCombatDelegateBeforeBonusTriggerMatch.or(moveCombatDelegateAfterBonusTriggerMatch);
      if (GameStepPropertiesHelper.isCombatMove(data)
          && Properties.getTriggers(data.getProperties())) {
        final Set<TriggerAttachment> toFirePossible =
            TriggerAttachment.collectForAllTriggersMatching(
                Set.of(player), moveCombatDelegateAllTriggerMatch);
        if (!toFirePossible.isEmpty()) {

          // collect conditions and test them for ALL triggers, both those that we will fire before
          // and those we will
          // fire after.
          testedConditions = TriggerAttachment.collectTestsForAllTriggers(toFirePossible, bridge);
          final Set<TriggerAttachment> toFireBeforeBonus =
              TriggerAttachment.collectForAllTriggersMatching(
                  Set.of(player), moveCombatDelegateBeforeBonusTriggerMatch);
          if (!toFireBeforeBonus.isEmpty()) {

            // get all triggers that are satisfied based on the tested conditions.
            final Set<TriggerAttachment> toFireTestedAndSatisfied =
                new HashSet<>(
                    CollectionUtils.getMatches(
                        toFireBeforeBonus,
                        AbstractTriggerAttachment.isSatisfiedMatch(testedConditions)));

            // now list out individual types to fire, once for each of the matches above.
            final FireTriggerParams fireTriggerParams =
                new FireTriggerParams(null, null, true, true, true, true);
            TriggerAttachment.triggerNotifications(
                toFireTestedAndSatisfied, bridge, fireTriggerParams);
            TriggerAttachment.triggerPlayerPropertyChange(
                toFireTestedAndSatisfied, bridge, fireTriggerParams);
            TriggerAttachment.triggerRelationshipTypePropertyChange(
                toFireTestedAndSatisfied, bridge, fireTriggerParams);
            TriggerAttachment.triggerTerritoryPropertyChange(
                toFireTestedAndSatisfied, bridge, fireTriggerParams);
            TriggerAttachment.triggerTerritoryEffectPropertyChange(
                toFireTestedAndSatisfied, bridge, fireTriggerParams);
            TriggerAttachment.triggerChangeOwnership(
                toFireTestedAndSatisfied, bridge, fireTriggerParams);
            TriggerAttachment.triggerUnitRemoval(
                toFireTestedAndSatisfied, bridge, fireTriggerParams);
          }
        }
      }

      // repair 2-hit units at beginning of turn (some maps have combat move before purchase, so i
      // think it is better to
      // do this at beginning of combat move)
      if (GameStepPropertiesHelper.isRepairUnits(data)) {
        MoveDelegate.repairMultipleHitPointUnits(bridge, player);
      }

      // reset any bonus of units, and give movement to units which begin the turn in the same
      // territory as units with
      // giveMovement (like air and naval bases)
      if (GameStepPropertiesHelper.isGiveBonusMovement(data)) {
        resetAndGiveBonusMovement();
      }

      // take away all movement from allied fighters sitting on damaged carriers
      removeMovementFromAirOnDamagedAlliedCarriers(bridge, player);

      // placing triggered units at beginning of combat move, but after bonuses and repairing, etc,
      // have been done.
      if (GameStepPropertiesHelper.isCombatMove(data)
          && Properties.getTriggers(data.getProperties())) {
        final Set<TriggerAttachment> toFireAfterBonus =
            TriggerAttachment.collectForAllTriggersMatching(
                Set.of(player), moveCombatDelegateAfterBonusTriggerMatch);
        if (!toFireAfterBonus.isEmpty()) {

          // get all triggers that are satisfied based on the tested conditions.
          final Set<TriggerAttachment> toFireTestedAndSatisfied =
              new HashSet<>(
                  CollectionUtils.getMatches(
                      toFireAfterBonus,
                      AbstractTriggerAttachment.isSatisfiedMatch(testedConditions)));

          // now list out individual types to fire, once for each of the matches above.
          TriggerAttachment.triggerUnitPlacement(
              toFireTestedAndSatisfied,
              bridge,
              new FireTriggerParams(null, null, true, true, true, true));
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
    if (Properties.getUnitsMayGiveBonusMovement(getData().getProperties())) {
      changeBonus = giveBonusMovement(bridge, player);
    }
    if (changeBonus != null && !changeBonus.isEmpty()) {
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

    // WW2V1, fires at end of non combat move.
    // Other versions fire Rockets in BattleDelegate
    if (needToDoRockets && GameStepPropertiesHelper.isNonCombatMove(data, true)) {
      RocketsFireHelper.fireWW2V1IfNeeded(bridge);
      needToDoRockets = false;
    }

    // do at the end of the round, if we do it at the start of non combat, then we may do it in the
    // middle of the round,
    // while loading.
    if (GameStepPropertiesHelper.isResetUnitStateAtEnd(data)) {
      resetUnitStateAndDelegateState();
    } else {

      // Only air units can move during both CM and NCM in the same turn so moved units are set to
      // no moves left
      final List<Unit> alreadyMovedNonAirUnits =
          CollectionUtils.getMatches(
              data.getUnits().getUnits(), Matches.unitHasMoved().and(Matches.unitIsNotAir()));
      bridge.addChange(ChangeFactory.markNoMovementChange(alreadyMovedNonAirUnits));
    }
    needToInitialize = true;
    needToDoRockets = true;
  }

  @Override
  public Serializable saveState() {
    final MoveExtendedDelegateState state = new MoveExtendedDelegateState();
    state.superState = super.saveState();
    state.needToInitialize = needToInitialize;
    state.needToDoRockets = needToDoRockets;
    state.pusLost = pusLost;
    return state;
  }

  @Override
  public void loadState(final Serializable state) {
    final MoveExtendedDelegateState s = (MoveExtendedDelegateState) state;
    super.loadState(s.superState);
    needToInitialize = s.needToInitialize;
    needToDoRockets = s.needToDoRockets;
    pusLost = s.pusLost;
  }

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    final Predicate<Unit> moveableUnitOwnedByMe =
        PredicateBuilder.of(Matches.unitIsOwnedBy(player))
            // right now, land units on transports have movement taken away when they their
            // transport moves
            .and(
                Matches.unitHasMovementLeft()
                    .or(Matches.unitIsLand().and(Matches.unitIsBeingTransported())))
            // if not non combat, cannot move aa units
            .andIf(
                GameStepPropertiesHelper.isCombatMove(getData()),
                Matches.unitCanNotMoveDuringCombatMove().negate())
            .build();
    return !getData().getMap().getTerritories().isEmpty()
        && getData().getMap().getTerritories().stream()
            .anyMatch(t -> t.anyUnitsMatch(moveableUnitOwnedByMe));
  }

  private Change resetBonusMovement() {
    final GameState data = getData();
    final CompositeChange change = new CompositeChange();
    for (final Unit u : data.getUnits()) {
      if (u.getBonusMovement() != 0) {
        change.add(ChangeFactory.unitPropertyChange(u, 0, Unit.PropertyName.BONUS_MOVEMENT));
      }
    }
    return change;
  }

  private void resetUnitStateAndDelegateState() {
    // while not a 'unit state', this is fine here for now. since we only have one instance of this
    // delegate, as long as
    // it gets cleared once per player's turn block, we are fine.
    pusLost.clear();
    final Change change = getResetUnitStateChange(getData());
    if (!change.isEmpty()) {
      // if no non-combat occurred, we may have cleanup left from combat that we need to spawn an
      // event for
      bridge.getHistoryWriter().startEvent(CLEANING_UP_DURING_MOVEMENT_PHASE);
      bridge.addChange(change);
    }
  }

  static Change getResetUnitStateChange(final GameState data) {
    final CompositeChange change = new CompositeChange();
    for (final Unit unit : data.getUnits()) {
      if (unit.getAlreadyMoved().compareTo(BigDecimal.ZERO) != 0) {
        change.add(
            ChangeFactory.unitPropertyChange(
                unit, BigDecimal.ZERO, Unit.PropertyName.ALREADY_MOVED));
      }
      if (unit.getWasInCombat()) {
        change.add(ChangeFactory.unitPropertyChange(unit, false, Unit.PropertyName.WAS_IN_COMBAT));
      }
      if (unit.getSubmerged()) {
        change.add(ChangeFactory.unitPropertyChange(unit, false, Unit.PropertyName.SUBMERGED));
      }
      if (unit.getAirborne()) {
        change.add(ChangeFactory.unitPropertyChange(unit, false, Unit.PropertyName.AIRBORNE));
      }
      if (unit.getLaunched() != 0) {
        change.add(ChangeFactory.unitPropertyChange(unit, 0, Unit.PropertyName.LAUNCHED));
      }
      if (!unit.getUnloaded().isEmpty()) {
        change.add(
            ChangeFactory.unitPropertyChange(
                unit, Collections.EMPTY_LIST, Unit.PropertyName.UNLOADED));
      }
      if (unit.getWasLoadedThisTurn()) {
        change.add(
            ChangeFactory.unitPropertyChange(
                unit, Boolean.FALSE, Unit.PropertyName.LOADED_THIS_TURN));
      }
      if (unit.getUnloadedTo() != null) {
        change.add(ChangeFactory.unitPropertyChange(unit, null, Unit.PropertyName.UNLOADED_TO));
      }
      if (unit.getWasUnloadedInCombatPhase()) {
        change.add(
            ChangeFactory.unitPropertyChange(
                unit, Boolean.FALSE, Unit.PropertyName.UNLOADED_IN_COMBAT_PHASE));
      }
      if (unit.getWasAmphibious()) {
        change.add(
            ChangeFactory.unitPropertyChange(
                unit, Boolean.FALSE, Unit.PropertyName.UNLOADED_AMPHIBIOUS));
      }
      if (unit.getChargedFlatFuelCost()) {
        change.add(
            ChangeFactory.unitPropertyChange(
                unit, Boolean.FALSE, Unit.PropertyName.CHARGED_FLAT_FUEL_COST));
      }
    }
    return change;
  }

  private static void removeMovementFromAirOnDamagedAlliedCarriers(
      final IDelegateBridge bridge, final GamePlayer player) {
    final GameState data = bridge.getData();
    final Predicate<Unit> crippledAlliedCarriersMatch =
        Matches.isUnitAllied(player)
            .and(Matches.unitIsOwnedBy(player).negate())
            .and(Matches.unitIsCarrier())
            .and(
                Matches.unitHasWhenCombatDamagedEffect(
                    UnitAttachment.UNITS_MAY_NOT_LEAVE_ALLIED_CARRIER));
    final Predicate<Unit> ownedFightersMatch =
        Matches.unitIsOwnedBy(player)
            .and(Matches.unitIsAir())
            .and(Matches.unitCanLandOnCarrier())
            .and(Matches.unitHasMovementLeft());
    final CompositeChange change = new CompositeChange();
    for (final Territory t : data.getMap().getTerritories()) {
      final Collection<Unit> ownedFighters = t.getMatches(ownedFightersMatch);
      if (ownedFighters.isEmpty()) {
        continue;
      }
      final Collection<Unit> crippledAlliedCarriers =
          CollectionUtils.getMatches(t.getUnits(), crippledAlliedCarriersMatch);
      if (crippledAlliedCarriers.isEmpty()) {
        continue;
      }
      for (final Unit fighter : ownedFighters) {
        if (fighter.getTransportedBy() != null
            && crippledAlliedCarriers.contains(fighter.getTransportedBy())) {
          change.add(ChangeFactory.markNoMovementChange(fighter));
        }
      }
    }
    if (!change.isEmpty()) {
      bridge.addChange(change);
    }
  }

  private static Change giveBonusMovement(final IDelegateBridge bridge, final GamePlayer player) {
    final GameState data = bridge.getData();
    final CompositeChange change = new CompositeChange();
    for (final Territory t : data.getMap().getTerritories()) {
      change.add(giveBonusMovementToUnits(player, data, t));
    }
    return change;
  }

  static Change giveBonusMovementToUnits(
      final GamePlayer player, final GameState data, final Territory t) {
    final CompositeChange change = new CompositeChange();
    for (final Unit u : t.getUnits()) {
      if (Matches.unitCanBeGivenBonusMovementByFacilitiesInItsTerritory(t, player).test(u)) {
        if (!Matches.isUnitAllied(player).test(u)) {
          continue;
        }
        int bonusMovement = Integer.MIN_VALUE;
        final Predicate<Unit> givesBonusUnit =
            Matches.alliedUnit(player).and(Matches.unitCanGiveBonusMovementToThisUnit(u));
        final Collection<Unit> givesBonusUnits =
            CollectionUtils.getMatches(t.getUnits(), givesBonusUnit);
        if (Matches.unitIsSea().test(u)) {
          final Predicate<Unit> givesBonusUnitLand = givesBonusUnit.and(Matches.unitIsLand());
          final Set<Territory> neighbors =
              new HashSet<>(data.getMap().getNeighbors(t, Matches.territoryIsLand()));
          for (final Territory current : neighbors) {
            givesBonusUnits.addAll(
                CollectionUtils.getMatches(current.getUnits(), givesBonusUnitLand));
          }
        } else if (Matches.unitIsLand().test(u)) {
          final Predicate<Unit> givesBonusUnitSea = givesBonusUnit.and(Matches.unitIsSea());
          final Set<Territory> neighbors =
              new HashSet<>(data.getMap().getNeighbors(t, Matches.territoryIsWater()));
          for (final Territory current : neighbors) {
            givesBonusUnits.addAll(
                CollectionUtils.getMatches(current.getUnits(), givesBonusUnitSea));
          }
        }
        for (final Unit bonusGiver : givesBonusUnits) {
          final int tempBonus =
              bonusGiver.getUnitAttachment().getGivesMovement().getInt(u.getType());
          if (tempBonus > bonusMovement) {
            bonusMovement = tempBonus;
          }
        }
        if (bonusMovement != Integer.MIN_VALUE && bonusMovement != 0) {
          bonusMovement = Math.max(bonusMovement, (u.getUnitAttachment().getMovement(player) * -1));
          change.add(
              ChangeFactory.unitPropertyChange(u, bonusMovement, Unit.PropertyName.BONUS_MOVEMENT));
        }
      }
    }
    return change;
  }

  static void repairMultipleHitPointUnits(final IDelegateBridge bridge, final GamePlayer player) {
    final GameState data = bridge.getData();
    final boolean repairOnlyOwn =
        Properties.getBattleshipsRepairAtBeginningOfRound(bridge.getData().getProperties());
    final Predicate<Unit> damagedUnits =
        Matches.unitHasMoreThanOneHitPointTotal().and(Matches.unitHasTakenSomeDamage());
    final Predicate<Unit> damagedUnitsOwned = damagedUnits.and(Matches.unitIsOwnedBy(player));
    final Map<Territory, Set<Unit>> damagedMap = new HashMap<>();
    for (final Territory current : data.getMap().getTerritories()) {
      final Set<Unit> damaged;
      if (!Properties.getTwoHitPointUnitsRequireRepairFacilities(data.getProperties())) {
        damaged =
            new HashSet<>(current.getMatches(repairOnlyOwn ? damagedUnitsOwned : damagedUnits));
      } else {
        damaged =
            new HashSet<>(
                current.getMatches(
                    damagedUnitsOwned.and(
                        Matches.unitCanBeRepairedByFacilitiesInItsTerritory(current, player))));
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
    final var territoriesToNotify = new HashSet<Territory>();
    for (final Entry<Territory, Set<Unit>> entry : damagedMap.entrySet()) {
      for (final Unit u : entry.getValue()) {
        final int repairAmount = getLargestRepairRateForThisUnit(u, entry.getKey(), data);
        final int currentHits = u.getHits();
        final int newHits = Math.max(0, Math.min(currentHits, (currentHits - repairAmount)));
        if (newHits != currentHits) {
          newHitsMap.put(u, newHits);
          territoriesToNotify.add(entry.getKey());
        }
        if (newHits <= 0) {
          fullyRepaired.put(u, entry.getKey());
        }
      }
    }
    bridge
        .getHistoryWriter()
        .startEvent(
            newHitsMap.size()
                + " "
                + MyFormatter.pluralize("unit", newHitsMap.size())
                + " repaired.",
            new HashSet<>(newHitsMap.keySet()));
    bridge.addChange(ChangeFactory.unitsHit(newHitsMap, territoriesToNotify));

    // now if damaged includes any carriers that are repairing, and have damaged abilities set for
    // not allowing air
    // units to leave while damaged, we need to remove those air units now
    final Collection<Unit> damagedCarriers =
        CollectionUtils.getMatches(
            fullyRepaired.keySet(),
            Matches.unitHasWhenCombatDamagedEffect(
                UnitAttachment.UNITS_MAY_NOT_LEAVE_ALLIED_CARRIER));

    // now cycle through those now-repaired carriers, and remove allied air from being dependent
    final CompositeChange clearAlliedAir = new CompositeChange();
    for (final Unit carrier : damagedCarriers) {
      final CompositeChange change =
          TransportTracker.clearTransportedByForAlliedAirOnCarrier(
              Set.of(carrier), fullyRepaired.get(carrier), carrier.getOwner(), data);
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

  private static void repairedChangeInto(
      final Set<Unit> units, final Territory territory, final IDelegateBridge bridge) {
    final List<Unit> changesIntoUnits =
        CollectionUtils.getMatches(units, Matches.unitWhenHitPointsRepairedChangesInto());
    final CompositeChange changes = new CompositeChange();
    final List<Unit> unitsToRemove = new ArrayList<>();
    final List<Unit> unitsToAdd = new ArrayList<>();
    for (final Unit unit : changesIntoUnits) {
      final Map<Integer, Tuple<Boolean, UnitType>> map =
          unit.getUnitAttachment().getWhenHitPointsRepairedChangesInto();
      if (map.containsKey(unit.getHits())) {
        final boolean translateAttributes = map.get(unit.getHits()).getFirst();
        final UnitType unitType = map.get(unit.getHits()).getSecond();
        final List<Unit> toAdd = unitType.create(1, unit.getOwner());
        if (translateAttributes) {
          final Change translate =
              UnitUtils.translateAttributesToOtherUnits(unit, toAdd, territory);
          changes.add(translate);
        }
        unitsToRemove.add(unit);
        unitsToAdd.addAll(toAdd);
      }
    }
    if (!unitsToRemove.isEmpty()) {
      bridge.addChange(changes);
      final String removeText =
          MyFormatter.unitsToText(unitsToRemove) + " removed in " + territory.getName();
      bridge.getHistoryWriter().addChildToEvent(removeText, new ArrayList<>(unitsToRemove));
      bridge.addChange(ChangeFactory.removeUnits(territory, unitsToRemove));
      final String addText =
          MyFormatter.unitsToText(unitsToAdd) + " added in " + territory.getName();
      bridge.getHistoryWriter().addChildToEvent(addText, new ArrayList<>(unitsToAdd));
      bridge.addChange(ChangeFactory.addUnits(territory, unitsToAdd));
    }
  }

  /** This has to be the exact same as Matches.UnitCanBeRepairedByFacilitiesInItsTerritory() */
  private static int getLargestRepairRateForThisUnit(
      final Unit unitToBeRepaired, final Territory territoryUnitIsIn, final GameState data) {
    if (!Properties.getTwoHitPointUnitsRequireRepairFacilities(data.getProperties())) {
      return 1;
    }
    final GamePlayer owner = unitToBeRepaired.getOwner();
    final Predicate<Unit> repairUnit =
        Matches.alliedUnit(owner)
            .and(Matches.unitCanRepairOthers())
            .and(Matches.unitCanRepairThisUnit(unitToBeRepaired, territoryUnitIsIn));
    final Set<Unit> repairUnitsForThisUnit =
        new HashSet<>(territoryUnitIsIn.getMatches(repairUnit));
    if (Matches.unitIsSea().test(unitToBeRepaired)) {
      final Collection<Territory> neighbors =
          data.getMap().getNeighbors(territoryUnitIsIn, Matches.territoryIsLand());
      for (final Territory current : neighbors) {
        final Predicate<Unit> repairUnitLand =
            Matches.alliedUnit(owner)
                .and(Matches.unitCanRepairOthers())
                .and(Matches.unitCanRepairThisUnit(unitToBeRepaired, current))
                .and(Matches.unitIsLand());
        repairUnitsForThisUnit.addAll(current.getMatches(repairUnitLand));
      }
    } else if (Matches.unitIsLand().test(unitToBeRepaired)) {
      final Collection<Territory> neighbors =
          data.getMap().getNeighbors(territoryUnitIsIn, Matches.territoryIsWater());
      for (final Territory current : neighbors) {
        final Predicate<Unit> repairUnitSea =
            Matches.alliedUnit(owner)
                .and(Matches.unitCanRepairOthers())
                .and(Matches.unitCanRepairThisUnit(unitToBeRepaired, current))
                .and(Matches.unitIsSea());
        repairUnitsForThisUnit.addAll(current.getMatches(repairUnitSea));
      }
    }
    int largest = 0;
    for (final Unit u : repairUnitsForThisUnit) {
      final int repair = u.getUnitAttachment().getRepairsUnits().getInt(unitToBeRepaired.getType());
      if (largest < repair) {
        largest = repair;
      }
    }
    return largest;
  }

  @Override
  public Optional<String> performMove(final MoveDescription move) {
    final GameData data = getData();

    // the reason we use this, is if we are in edit mode, we may have a different unit owner than
    // the current player
    final GamePlayer player = getUnitsOwner(move.getUnits());
    final MoveValidationResult result =
        new MoveValidator(data, GameStepPropertiesHelper.isNonCombatMove(data, false))
            .validateMove(move, player, movesToUndo);
    final StringBuilder errorMsg = new StringBuilder(100);
    final int numProblems = result.getTotalWarningCount() - (result.hasError() ? 0 : 1);
    final String numErrorsMsg =
        numProblems > 0
            ? ("; "
                + numProblems
                + " "
                + MyFormatter.pluralize("error", numProblems)
                + " not shown")
            : "";
    if (result.hasError()) {
      return Optional.of(errorMsg.append(result.getError()).append(numErrorsMsg).toString());
    }
    if (result.hasDisallowedUnits()) {
      return Optional.of(
          errorMsg.append(result.getDisallowedUnitWarning(0)).append(numErrorsMsg).toString());
    }
    boolean isKamikaze = false;
    final boolean getKamikazeAir = Properties.getKamikazeAirplanes(data.getProperties());
    Collection<Unit> kamikazeUnits = new ArrayList<>();

    // confirm kamikaze moves, and remove them from unresolved units
    if (getKamikazeAir || move.getUnits().stream().anyMatch(Matches.unitIsKamikaze())) {
      kamikazeUnits = result.getUnresolvedUnits(AirMovementValidator.NOT_ALL_AIR_UNITS_CAN_LAND);
      if (!kamikazeUnits.isEmpty() && bridge.getRemotePlayer().confirmMoveKamikaze()) {
        for (final Unit unit : kamikazeUnits) {
          if (getKamikazeAir || Matches.unitIsKamikaze().test(unit)) {
            result.removeUnresolvedUnit(AirMovementValidator.NOT_ALL_AIR_UNITS_CAN_LAND, unit);
            isKamikaze = true;
          }
        }
      }
    }
    if (result.hasUnresolvedUnits()) {
      return Optional.of(
          errorMsg.append(result.getUnresolvedUnitWarning(0)).append(numErrorsMsg).toString());
    }

    // allow user to cancel move if aa guns will fire
    final AaInMoveUtil aaInMoveUtil = new AaInMoveUtil();
    aaInMoveUtil.initialize(bridge);
    final Collection<Territory> aaFiringTerritores =
        aaInMoveUtil.getTerritoriesWhereAaWillFire(move.getRoute(), move.getUnits());
    if (!aaFiringTerritores.isEmpty()
        && !bridge.getRemotePlayer().confirmMoveInFaceOfAa(aaFiringTerritores)) {
      return Optional.empty();
    }

    // do the move
    final Route route = move.getRoute();
    final UndoableMove currentMove = new UndoableMove(move.getUnits(), move.getRoute());
    final String transcriptText =
        MyFormatter.unitsToTextNoOwner(move.getUnits())
            + " moved from "
            + route.getStart().getName()
            + " to "
            + route.getEnd().getName();
    bridge.getHistoryWriter().startEvent(transcriptText, currentMove.getDescriptionObject());
    if (isKamikaze) {
      bridge
          .getHistoryWriter()
          .addChildToEvent(
              "This was a kamikaze move, for at least some of the units", kamikazeUnits);
    }
    tempMovePerformer = new MovePerformer();
    tempMovePerformer.initialize(this);
    tempMovePerformer.moveUnits(move, player, currentMove);
    tempMovePerformer = null;
    return Optional.empty();
  }

  public static Collection<Territory> getEmptyNeutral(final Route route) {
    final Predicate<Territory> emptyNeutral =
        Matches.territoryIsEmpty().and(Matches.territoryIsNeutralButNotWater());
    return route.getMatches(emptyNeutral);
  }

  private void removeAirThatCantLand() {
    final GameData data = getData();
    final boolean lhtrCarrierProd =
        Properties.getLhtrCarrierProductionRules(data.getProperties())
            || Properties.getLandExistingFightersOnNewCarriers(data.getProperties());
    boolean hasProducedCarriers = false;
    for (final GamePlayer p : GameStepPropertiesHelper.getCombinedTurns(data, player)) {
      if (p.anyUnitsMatch(Matches.unitIsCarrier())) {
        hasProducedCarriers = true;
        break;
      }
    }
    final AirThatCantLandUtil util = new AirThatCantLandUtil(bridge);
    util.removeAirThatCantLand(player, lhtrCarrierProd && hasProducedCarriers);

    // if edit mode has been on, we need to clean up after all players
    for (final GamePlayer player : data.getPlayerList()) {

      // Check if player still has units to place
      if (!player.equals(this.player)) {
        util.removeAirThatCantLand(
            player,
            ((player.anyUnitsMatch(Matches.unitIsCarrier()) || hasProducedCarriers)
                && lhtrCarrierProd));
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
