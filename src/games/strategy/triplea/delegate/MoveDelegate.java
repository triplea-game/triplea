package games.strategy.triplea.delegate;

import games.strategy.common.delegate.GameDelegateBridge;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.AbstractTriggerAttachment;
import games.strategy.triplea.attachments.ICondition;
import games.strategy.triplea.attachments.TriggerAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.dataObjects.MoveValidationResult;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Responsible for moving units on the board.
 * <p>
 * Responsible for checking the validity of a move, and for moving the units. <br>
 */
public class MoveDelegate extends AbstractMoveDelegate implements IMoveDelegate {

  public static String CLEANING_UP_DURING_MOVEMENT_PHASE = "Cleaning up during movement phase";

  // needToInitialize means we only do certain things once, so that if a game is saved then
  // loaded, they aren't done again
  private boolean m_needToInitialize = true;
  private boolean m_needToDoRockets = true;
  private IntegerMap<Territory> m_PUsLost = new IntegerMap<>();

  public MoveDelegate() {}

  /**
   * Called before the delegate will run, AND before "start" is called.
   */
  @Override
  public void setDelegateBridgeAndPlayer(final IDelegateBridge iDelegateBridge) {
    super.setDelegateBridgeAndPlayer(new GameDelegateBridge(iDelegateBridge));
  }

  /**
   * Called before the delegate will run.
   */
  @Override
  public void start() {
    super.start();
    final GameData data = getData();
    if (m_needToInitialize) {

      // territory property changes triggered at beginning of combat move
      // TODO create new delegate called "start of turn" and move them there.
      // First set up a match for what we want to have fire as a default in this delegate. List out as a composite match
      // OR. use 'null, null' because this is the Default firing location for any trigger that does NOT have 'when' set.
      HashMap<ICondition, Boolean> testedConditions = null;
      final Match<TriggerAttachment> moveCombatDelegateBeforeBonusTriggerMatch =
          new CompositeMatchAnd<>(AbstractTriggerAttachment.availableUses,
              AbstractTriggerAttachment.whenOrDefaultMatch(null, null),
              new CompositeMatchOr<TriggerAttachment>(AbstractTriggerAttachment.notificationMatch(),
                  TriggerAttachment.playerPropertyMatch(), TriggerAttachment.relationshipTypePropertyMatch(),
                  TriggerAttachment.territoryPropertyMatch(), TriggerAttachment.territoryEffectPropertyMatch(),
                  TriggerAttachment.removeUnitsMatch(), TriggerAttachment.changeOwnershipMatch()));
      final Match<TriggerAttachment> moveCombatDelegateAfterBonusTriggerMatch =
          new CompositeMatchAnd<>(AbstractTriggerAttachment.availableUses,
              AbstractTriggerAttachment.whenOrDefaultMatch(null, null),
              new CompositeMatchOr<TriggerAttachment>(TriggerAttachment.placeMatch()));
      final Match<TriggerAttachment> moveCombatDelegateAllTriggerMatch = new CompositeMatchOr<>(
          moveCombatDelegateBeforeBonusTriggerMatch, moveCombatDelegateAfterBonusTriggerMatch);
      if (GameStepPropertiesHelper.isCombatMove(data, false) && games.strategy.triplea.Properties.getTriggers(data)) {
        final HashSet<TriggerAttachment> toFirePossible = TriggerAttachment.collectForAllTriggersMatching(
            new HashSet<>(Collections.singleton(m_player)), moveCombatDelegateAllTriggerMatch, m_bridge);
        if (!toFirePossible.isEmpty()) {

          // collect conditions and test them for ALL triggers, both those that we will fire before and those we will
          // fire after.
          testedConditions = TriggerAttachment.collectTestsForAllTriggers(toFirePossible, m_bridge);
          final HashSet<TriggerAttachment> toFireBeforeBonus =
              TriggerAttachment.collectForAllTriggersMatching(new HashSet<>(Collections.singleton(m_player)),
                  moveCombatDelegateBeforeBonusTriggerMatch, m_bridge);
          if (!toFireBeforeBonus.isEmpty()) {

            // get all triggers that are satisfied based on the tested conditions.
            final Set<TriggerAttachment> toFireTestedAndSatisfied = new HashSet<>(
                Match.getMatches(toFireBeforeBonus, AbstractTriggerAttachment.isSatisfiedMatch(testedConditions)));

            // now list out individual types to fire, once for each of the matches above.
            TriggerAttachment.triggerNotifications(toFireTestedAndSatisfied, m_bridge, null, null, true, true, true,
                true);
            TriggerAttachment.triggerPlayerPropertyChange(toFireTestedAndSatisfied, m_bridge, null, null, true, true,
                true, true);
            TriggerAttachment.triggerRelationshipTypePropertyChange(toFireTestedAndSatisfied, m_bridge, null, null,
                true, true, true, true);
            TriggerAttachment.triggerTerritoryPropertyChange(toFireTestedAndSatisfied, m_bridge, null, null, true,
                true, true, true);
            TriggerAttachment.triggerTerritoryEffectPropertyChange(toFireTestedAndSatisfied, m_bridge, null, null,
                true, true, true, true);
            TriggerAttachment.triggerChangeOwnership(toFireTestedAndSatisfied, m_bridge, null, null, true, true, true,
                true);
            TriggerAttachment.triggerUnitRemoval(toFireTestedAndSatisfied, m_bridge, null, null, true, true, true,
                true);
          }
        }
      }

      // repair 2-hit units at beginning of turn (some maps have combat move before purchase, so i think it is better to
      // do this at beginning of combat move)
      if (GameStepPropertiesHelper.isRepairUnits(data)) {
        MoveDelegate.repairMultipleHitPointUnits(m_bridge, m_player);
      }

      // reset any bonus of units, and give movement to units which begin the turn in the same territory as units with
      // giveMovement (like air and naval bases)
      if (GameStepPropertiesHelper.isGiveBonusMovement(data)) {
        resetAndGiveBonusMovement();
      }

      // take away all movement from allied fighters sitting on damaged carriers
      removeMovementFromAirOnDamagedAlliedCarriers(m_bridge, m_player);

      // placing triggered units at beginning of combat move, but after bonuses and repairing, etc, have been done.
      if (GameStepPropertiesHelper.isCombatMove(data, false) && games.strategy.triplea.Properties.getTriggers(data)) {
        final HashSet<TriggerAttachment> toFireAfterBonus = TriggerAttachment.collectForAllTriggersMatching(
            new HashSet<>(Collections.singleton(m_player)), moveCombatDelegateAfterBonusTriggerMatch, m_bridge);
        if (!toFireAfterBonus.isEmpty()) {

          // get all triggers that are satisfied based on the tested conditions.
          final Set<TriggerAttachment> toFireTestedAndSatisfied = new HashSet<>(
              Match.getMatches(toFireAfterBonus, AbstractTriggerAttachment.isSatisfiedMatch(testedConditions)));

          // now list out individual types to fire, once for each of the matches above.
          TriggerAttachment.triggerUnitPlacement(toFireTestedAndSatisfied, m_bridge, null, null, true, true, true,
              true);
        }
      }
      if (GameStepPropertiesHelper.isResetUnitStateAtStart(data)) {
        resetUnitStateAndDelegateState();
      }
      m_needToInitialize = false;
    }
  }

  private void resetAndGiveBonusMovement() {
    boolean addedHistoryEvent = false;
    final Change changeReset = resetBonusMovement();
    if (!changeReset.isEmpty()) {
      m_bridge.getHistoryWriter().startEvent("Resetting and Giving Bonus Movement to Units");
      m_bridge.addChange(changeReset);
      addedHistoryEvent = true;
    }
    Change changeBonus = null;
    if (games.strategy.triplea.Properties.getUnitsMayGiveBonusMovement(getData())) {
      changeBonus = giveBonusMovement(m_bridge, m_player);
    }
    if (changeBonus != null && !changeBonus.isEmpty()) {
      if (!addedHistoryEvent) {
        m_bridge.getHistoryWriter().startEvent("Resetting and Giving Bonus Movement to Units");
      }
      m_bridge.addChange(changeBonus);
    }
  }

  /**
   * Called before the delegate will stop running.
   */
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
      if (m_needToDoRockets && TechTracker.hasRocket(m_bridge.getPlayerID())) {
        final RocketsFireHelper helper = new RocketsFireHelper();
        helper.fireRockets(m_bridge, m_bridge.getPlayerID());
        m_needToDoRockets = false;
      }
    }

    // do at the end of the round, if we do it at the start of non combat, then we may do it in the middle of the round,
    // while loading.
    if (GameStepPropertiesHelper.isResetUnitStateAtEnd(data)) {
      resetUnitStateAndDelegateState();
    }
    m_needToInitialize = true;
    m_needToDoRockets = true;
  }

  @Override
  public Serializable saveState() {
    return saveState(true);
  }

  /**
   * Returns the state of the Delegate. We don't want to save the undoState if
   * we are saving the state for an undo move (we don't need it, it will just
   * take up extra space).
   */
  private Serializable saveState(final boolean saveUndo) {
    final MoveExtendedDelegateState state = new MoveExtendedDelegateState();
    state.superState = super.saveState();
    state.m_needToInitialize = m_needToInitialize;
    state.m_needToDoRockets = m_needToDoRockets;
    state.m_PUsLost = m_PUsLost;
    return state;
  }

  @Override
  public void loadState(final Serializable state) {
    final MoveExtendedDelegateState s = (MoveExtendedDelegateState) state;
    super.loadState(s.superState);
    m_needToInitialize = s.m_needToInitialize;
    m_needToDoRockets = s.m_needToDoRockets;
    m_PUsLost = s.m_PUsLost;
  }

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    final CompositeMatchAnd<Unit> moveableUnitOwnedByMe = new CompositeMatchAnd<>();
    moveableUnitOwnedByMe.add(Matches.unitIsOwnedBy(m_player));

    // right now, land units on transports have movement taken away when they their transport moves
    moveableUnitOwnedByMe.add(new CompositeMatchOr<>(Matches.unitHasMovementLeft,
        new CompositeMatchAnd<Unit>(Matches.UnitIsLand, Matches.unitIsBeingTransported())));

    // if not non combat, cannot move aa units
    if (GameStepPropertiesHelper.isCombatMove(getData(), false)) {
      moveableUnitOwnedByMe.add(Matches.UnitCanNotMoveDuringCombatMove.invert());
    }
    for (final Territory item : getData().getMap().getTerritories()) {
      if (item.getUnits().someMatch(moveableUnitOwnedByMe)) {
        return true;
      }
    }
    return false;
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
    m_PUsLost.clear();
    final Change change = getResetUnitStateChange(getData());
    if (!change.isEmpty()) {
      // if no non-combat occurred, we may have cleanup left from combat
      // that we need to spawn an event for
      m_bridge.getHistoryWriter().startEvent(CLEANING_UP_DURING_MOVEMENT_PHASE);
      m_bridge.addChange(change);
    }
  }

  public static Change getResetUnitStateChange(final GameData data) {
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

  private void removeMovementFromAirOnDamagedAlliedCarriers(final IDelegateBridge aBridge, final PlayerID player) {
    final GameData data = aBridge.getData();
    final Match<Unit> crippledAlliedCarriersMatch = new CompositeMatchAnd<>(Matches.isUnitAllied(player, data),
        Matches.unitIsOwnedBy(player).invert(), Matches.UnitIsCarrier,
        Matches.UnitHasWhenCombatDamagedEffect(UnitAttachment.UNITSMAYNOTLEAVEALLIEDCARRIER));
    final Match<Unit> ownedFightersMatch =
        new CompositeMatchAnd<>(Matches.unitIsOwnedBy(player), Matches.UnitIsAir,
            Matches.UnitCanLandOnCarrier, Matches.unitHasMovementLeft);
    final CompositeChange change = new CompositeChange();
    for (final Territory t : data.getMap().getTerritories()) {
      final Collection<Unit> ownedFighters = t.getUnits().getMatches(ownedFightersMatch);
      if (ownedFighters.isEmpty()) {
        continue;
      }
      final Collection<Unit> crippledAlliedCarriers =
          Match.getMatches(t.getUnits().getUnits(), crippledAlliedCarriersMatch);
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
      aBridge.addChange(change);
    }
  }

  private Change giveBonusMovement(final IDelegateBridge aBridge, final PlayerID player) {
    final GameData data = aBridge.getData();
    final CompositeChange change = new CompositeChange();
    for (final Territory t : data.getMap().getTerritories()) {
      for (final Unit u : t.getUnits().getUnits()) {
        if (Matches.UnitCanBeGivenBonusMovementByFacilitiesInItsTerritory(t, player, data).match(u)) {
          if (!Matches.isUnitAllied(player, data).match(u)) {
            continue;
          }
          int bonusMovement = Integer.MIN_VALUE;
          final Collection<Unit> givesBonusUnits = new ArrayList<>();
          final Match<Unit> givesBonusUnit = new CompositeMatchAnd<>(Matches.alliedUnit(player, data),
              Matches.UnitCanGiveBonusMovementToThisUnit(u));
          givesBonusUnits.addAll(Match.getMatches(t.getUnits().getUnits(), givesBonusUnit));
          if (Matches.UnitIsSea.match(u)) {
            final Match<Unit> givesBonusUnitLand = new CompositeMatchAnd<>(givesBonusUnit, Matches.UnitIsLand);
            final List<Territory> neighbors =
                new ArrayList<>(data.getMap().getNeighbors(t, Matches.TerritoryIsLand));
            for (final Territory current : neighbors) {
              givesBonusUnits.addAll(Match.getMatches(current.getUnits().getUnits(), givesBonusUnitLand));
            }
          } else if (Matches.UnitIsLand.match(u)) {
            final Match<Unit> givesBonusUnitSea = new CompositeMatchAnd<>(givesBonusUnit, Matches.UnitIsSea);
            final List<Territory> neighbors =
                new ArrayList<>(data.getMap().getNeighbors(t, Matches.TerritoryIsWater));
            for (final Territory current : neighbors) {
              givesBonusUnits.addAll(Match.getMatches(current.getUnits().getUnits(), givesBonusUnitSea));
            }
          }
          for (final Unit bonusGiver : givesBonusUnits) {
            final int tempBonus = UnitAttachment.get(bonusGiver.getType()).getGivesMovement().getInt(u.getType());
            if (tempBonus > bonusMovement) {
              bonusMovement = tempBonus;
            }
          }
          if (bonusMovement != Integer.MIN_VALUE && bonusMovement != 0) {
            bonusMovement = Math.max(bonusMovement, (UnitAttachment.get(u.getType()).getMovement(player) * -1));
            change.add(ChangeFactory.unitPropertyChange(u, bonusMovement, TripleAUnit.BONUS_MOVEMENT));
          }
        }
      }
    }
    return change;
  }

  public static void repairMultipleHitPointUnits(final IDelegateBridge aBridge, final PlayerID player) {
    final GameData data = aBridge.getData();
    final boolean repairOnlyOwn =
        games.strategy.triplea.Properties.getBattleshipsRepairAtBeginningOfRound(aBridge.getData());
    final Match<Unit> damagedUnits =
        new CompositeMatchAnd<>(Matches.UnitHasMoreThanOneHitPointTotal, Matches.UnitHasTakenSomeDamage);
    final Match<Unit> damagedUnitsOwned = new CompositeMatchAnd<>(damagedUnits, Matches.unitIsOwnedBy(player));
    final Map<Territory, Set<Unit>> damagedMap = new HashMap<>();
    final Iterator<Territory> iterTerritories = data.getMap().getTerritories().iterator();
    while (iterTerritories.hasNext()) {
      final Territory current = iterTerritories.next();
      final Set<Unit> damaged;
      if (!games.strategy.triplea.Properties.getTwoHitPointUnitsRequireRepairFacilities(data)) {
        if (repairOnlyOwn) {
          // we only repair ours
          damaged = new HashSet<>(current.getUnits().getMatches(damagedUnitsOwned));
        } else {
          // we repair everyone's
          damaged = new HashSet<>(current.getUnits().getMatches(damagedUnits));
        }
      } else {
        damaged = new HashSet<>(current.getUnits().getMatches(new CompositeMatchAnd<>(damagedUnitsOwned,
            Matches.UnitCanBeRepairedByFacilitiesInItsTerritory(current, player, data))));
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
    aBridge.getHistoryWriter().startEvent(
        newHitsMap.size() + " " + MyFormatter.pluralize("unit", newHitsMap.size()) + " repaired.",
        new HashSet<>(newHitsMap.keySet()));
    aBridge.addChange(ChangeFactory.unitsHit(newHitsMap));

    // now if damaged includes any carriers that are repairing, and have damaged abilities set for not allowing air
    // units to leave while damaged, we need to remove those air units now
    final Collection<Unit> damagedCarriers = Match.getMatches(fullyRepaired.keySet(),
        Matches.UnitHasWhenCombatDamagedEffect(UnitAttachment.UNITSMAYNOTLEAVEALLIEDCARRIER));

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
      aBridge.addChange(clearAlliedAir);
    }
  }

  /**
   * This has to be the exact same as Matches.UnitCanBeRepairedByFacilitiesInItsTerritory()
   */
  private static int getLargestRepairRateForThisUnit(final Unit unitToBeRepaired, final Territory territoryUnitIsIn,
      final GameData data) {
    if (!games.strategy.triplea.Properties.getTwoHitPointUnitsRequireRepairFacilities(data)) {
      return 1;
    }
    final Set<Unit> repairUnitsForThisUnit = new HashSet<>();
    final PlayerID owner = unitToBeRepaired.getOwner();
    final Match<Unit> repairUnit = new CompositeMatchAnd<>(Matches.alliedUnit(owner, data),
        Matches.UnitCanRepairOthers, Matches.UnitCanRepairThisUnit(unitToBeRepaired));
    repairUnitsForThisUnit.addAll(territoryUnitIsIn.getUnits().getMatches(repairUnit));
    if (Matches.UnitIsSea.match(unitToBeRepaired)) {
      final Match<Unit> repairUnitLand = new CompositeMatchAnd<>(repairUnit, Matches.UnitIsLand);
      final List<Territory> neighbors =
          new ArrayList<>(data.getMap().getNeighbors(territoryUnitIsIn, Matches.TerritoryIsLand));
      for (final Territory current : neighbors) {
        repairUnitsForThisUnit.addAll(current.getUnits().getMatches(repairUnitLand));
      }
    } else if (Matches.UnitIsLand.match(unitToBeRepaired)) {
      final Match<Unit> repairUnitSea = new CompositeMatchAnd<>(repairUnit, Matches.UnitIsSea);
      final List<Territory> neighbors =
          new ArrayList<>(data.getMap().getNeighbors(territoryUnitIsIn, Matches.TerritoryIsWater));
      for (final Territory current : neighbors) {
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
        newDependents, GameStepPropertiesHelper.isNonCombatMove(data, false), m_movesToUndo, data);
    final StringBuilder errorMsg = new StringBuilder(100);
    final int numProblems = result.getTotalWarningCount() - (result.hasError() ? 0 : 1);
    final String numErrorsMsg =
        numProblems > 0 ? ("; " + numProblems + " " + MyFormatter.pluralize("error", numProblems) + " not shown") : "";
    if (result.hasError()) {
      return errorMsg.append(result.getError()).append(numErrorsMsg).toString();
    }
    if (result.hasDisallowedUnits()) {
      return errorMsg.append(result.getDisallowedUnitWarning(0)).append(numErrorsMsg).toString();
    }
    boolean isKamikaze = false;
    final boolean getKamikazeAir = games.strategy.triplea.Properties.getKamikaze_Airplanes(data);
    Collection<Unit> kamikazeUnits = new ArrayList<>();

    // confirm kamikaze moves, and remove them from unresolved units
    if (getKamikazeAir || Match.someMatch(units, Matches.UnitIsKamikaze)) {
      kamikazeUnits = result.getUnresolvedUnits(MoveValidator.NOT_ALL_AIR_UNITS_CAN_LAND);
      if (kamikazeUnits.size() > 0 && getRemotePlayer().confirmMoveKamikaze()) {
        for (final Unit unit : kamikazeUnits) {
          if (getKamikazeAir || Matches.UnitIsKamikaze.match(unit)) {
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
    aaInMoveUtil.initialize(m_bridge);
    final Collection<Territory> aaFiringTerritores = aaInMoveUtil.getTerritoriesWhereAAWillFire(route, units);
    if (!aaFiringTerritores.isEmpty()) {
      if (!getRemotePlayer().confirmMoveInFaceOfAA(aaFiringTerritores)) {
        return null;
      }
    }

    // do the move
    final UndoableMove currentMove = new UndoableMove(units, route);
    final String transcriptText = MyFormatter.unitsToTextNoOwner(units) + " moved from " + route.getStart().getName()
        + " to " + route.getEnd().getName();
    m_bridge.getHistoryWriter().startEvent(transcriptText, currentMove.getDescriptionObject());
    if (isKamikaze) {
      m_bridge.getHistoryWriter().addChildToEvent("This was a kamikaze move, for at least some of the units",
          kamikazeUnits);
    }
    m_tempMovePerformer = new MovePerformer();
    m_tempMovePerformer.initialize(this);
    m_tempMovePerformer.moveUnits(units, route, player, transportsThatCanBeLoaded, newDependents, currentMove);
    m_tempMovePerformer = null;
    return null;
  }

  public static Collection<Territory> getEmptyNeutral(final Route route) {
    final Match<Territory> emptyNeutral =
        new CompositeMatchAnd<>(Matches.TerritoryIsEmpty, Matches.TerritoryIsNeutralButNotWater);
    final Collection<Territory> neutral = route.getMatches(emptyNeutral);
    return neutral;
  }

  private void removeAirThatCantLand() {
    final GameData data = getData();
    final boolean lhtrCarrierProd = AirThatCantLandUtil.isLHTRCarrierProduction(data)
        || AirThatCantLandUtil.isLandExistingFightersOnNewCarriers(data);
    boolean hasProducedCarriers = false;
    for (final PlayerID p : GameStepPropertiesHelper.getCombinedTurns(data, m_player)) {
      if (p.getUnits().someMatch(Matches.UnitIsCarrier)) {
        hasProducedCarriers = true;
        break;
      }
    }
    final AirThatCantLandUtil util = new AirThatCantLandUtil(m_bridge);
    util.removeAirThatCantLand(m_player, lhtrCarrierProd && hasProducedCarriers);

    // if edit mode has been on, we need to clean up after all players
    for (final PlayerID player : data.getPlayerList()) {

      // Check if player still has units to place
      if (!player.equals(m_player)) {
        util.removeAirThatCantLand(player,
            ((player.getUnits().someMatch(Matches.UnitIsCarrier) || hasProducedCarriers) && lhtrCarrierProd));
      }
    }
  }

  /**
   * @return the number of PUs that have been lost by bombing, rockets, etc.
   */
  @Override
  public int PUsAlreadyLost(final Territory t) {
    return m_PUsLost.getInt(t);
  }

  /**
   * Add more PUs lost to a territory due to bombing, rockets, etc.
   */
  @Override
  public void PUsLost(final Territory t, final int amt) {
    m_PUsLost.add(t, amt);
  }
}


class MoveExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = 5352248885420819215L;
  Serializable superState;
  // add other variables here:
  public boolean m_firstRun = true;
  public boolean m_needToInitialize;
  public boolean m_needToDoRockets;
  public IntegerMap<Territory> m_PUsLost;
}
