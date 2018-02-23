package games.strategy.triplea.delegate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.MapSupport;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.delegate.dataObjects.MoveValidationResult;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;

/**
 * SpecialMoveDelegate is a move delegate made for special movements like the new paratrooper/airborne movement.
 * Airborne Attacks is actually Paratroopers tech for Global 1940, except that I really do not want to confuse myself by
 * naming yet another
 * thing Paratroopers, so this is now getting a new name.
 * This is very different than "paratroopers" for AA50. We are actually launching the units from a static unit (an
 * airbase) to another
 * territory, instead of carrying them.
 */
@MapSupport
public class SpecialMoveDelegate extends AbstractMoveDelegate {
  private boolean needToInitialize = true;

  public SpecialMoveDelegate() {}

  @Override
  public void setDelegateBridgeAndPlayer(final IDelegateBridge delegateBridge) {
    super.setDelegateBridgeAndPlayer(new GameDelegateBridge(delegateBridge));
  }

  @Override
  public void start() {
    super.start();
    final GameData data = getData();
    if (!allowAirborne(player, data)) {
      return;
    }
    final boolean onlyWhereUnderAttackAlready =
        Properties.getAirborneAttacksOnlyInExistingBattles(data);
    final BattleTracker battleTracker = AbstractMoveDelegate.getBattleTracker(data);
    if (needToInitialize && onlyWhereUnderAttackAlready) {
      // we do this to clear any 'finishedBattles' and also to create battles for units that didn't move
      BattleDelegate.doInitialize(battleTracker, bridge);
      needToInitialize = false;
    }
  }

  @Override
  public void end() {
    super.end();
    needToInitialize = true;
  }

  @Override
  public Serializable saveState() {
    final SpecialMoveExtendedDelegateState state = new SpecialMoveExtendedDelegateState();
    state.superState = super.saveState();
    state.m_needToInitialize = needToInitialize;
    return state;
  }

  @Override
  public void loadState(final Serializable state) {
    final SpecialMoveExtendedDelegateState s = (SpecialMoveExtendedDelegateState) state;
    super.loadState(s.superState);
    needToInitialize = s.m_needToInitialize;
  }

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    return allowAirborne(player, getData());
  }

  @Override
  public String move(final Collection<Unit> units, final Route route, final Collection<Unit> transportsThatCanBeLoaded,
      final Map<Unit, Collection<Unit>> newDependents) {
    if (!allowAirborne(player, getData())) {
      return "No Airborne Movement Allowed Yet";
    }
    final GameData data = getData();
    // there reason we use this, is because if we are in edit mode, we may have a different unit owner than the current
    // player.
    final PlayerID player = getUnitsOwner(units);
    // here we have our own new validation method....
    final MoveValidationResult result = validateMove(units, route, player, data);
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
    // add dependencies (any move that came before this, from this start territory, is a dependency)
    for (final UndoableMove otherMove : movesToUndo) {
      if (otherMove.getStart().equals(route.getStart())) {
        currentMove.addDependency(otherMove);
      }
    }
    // make the units airborne
    final CompositeChange airborneChange = new CompositeChange();
    for (final Unit u : units) {
      airborneChange.add(ChangeFactory.unitPropertyChange(u, true, TripleAUnit.AIRBORNE));
    }
    currentMove.addChange(airborneChange);
    // make the bases start filling up their capacity
    final Collection<Unit> basesAtStart = route.getStart().getUnits().getMatches(getAirborneBaseMatch(player, data));
    final Change fillLaunchCapacity = getNewAssignmentOfNumberLaunchedChange(units.size(), basesAtStart, player, data);
    currentMove.addChange(fillLaunchCapacity);
    // start event
    final String transcriptText = MyFormatter.unitsToTextNoOwner(units) + " moved from " + route.getStart().getName()
        + " to " + route.getEnd().getName();
    bridge.getHistoryWriter().startEvent(transcriptText, currentMove.getDescriptionObject());
    // actually do our special changes
    bridge.addChange(airborneChange);
    bridge.addChange(fillLaunchCapacity);
    tempMovePerformer = new MovePerformer();
    tempMovePerformer.initialize(this);
    tempMovePerformer.moveUnits(units, route, player, transportsThatCanBeLoaded, newDependents, currentMove);
    tempMovePerformer = null;
    return null;
  }

  static MoveValidationResult validateMove(final Collection<Unit> units, final Route route,
      final PlayerID player, final GameData data) {
    final MoveValidationResult result = new MoveValidationResult();
    if (route.hasNoSteps()) {
      return result;
    }
    if (MoveValidator.validateFirst(data, units, route, player, result).getError() != null) {
      return result;
    }
    if (MoveValidator.validateFuel(data, units, route, player, result).getError() != null) {
      return result;
    }
    final boolean isEditMode = getEditMode(data);
    if (!isEditMode) {
      // make sure all units are at least friendly
      for (final Unit unit : CollectionUtils.getMatches(units, Matches.unitIsOwnedBy(player).negate())) {
        result.addDisallowedUnit("Can only move owned units", unit);
      }
    }
    if (validateAirborneMovements(data, units, route, player, result).getError() != null) {
      return result;
    }
    return result;
  }

  private static MoveValidationResult validateAirborneMovements(final GameData data, final Collection<Unit> units,
      final Route route, final PlayerID player, final MoveValidationResult result) {
    if (!TechAbilityAttachment.getAllowAirborneForces(player, data)) {
      return result.setErrorReturnResult("Do Not Have Airborne Tech");
    }
    final int airborneDistance = TechAbilityAttachment.getAirborneDistance(player, data);
    final Set<UnitType> airborneBases = TechAbilityAttachment.getAirborneBases(player, data);
    final Set<UnitType> airborneTypes = TechAbilityAttachment.getAirborneTypes(player, data);
    if ((airborneDistance <= 0) || airborneBases.isEmpty() || airborneTypes.isEmpty()) {
      return result.setErrorReturnResult("Require Airborne Forces And Launch Capacity Tech");
    }
    if (route.numberOfSteps() > airborneDistance) {
      return result.setErrorReturnResult("Destination Is Out Of Range");
    }
    final Collection<PlayerID> alliesForBases = data.getRelationshipTracker().getAllies(player, true);
    final Predicate<Unit> airborneBaseMatch = getAirborneMatch(airborneBases, alliesForBases);
    final Territory start = route.getStart();
    final Territory end = route.getEnd();
    final Collection<Unit> basesAtStart = start.getUnits().getMatches(airborneBaseMatch);
    if (basesAtStart.isEmpty()) {
      return result.setErrorReturnResult("Require Airborne Base At Originating Territory");
    }
    final int airborneCapacity = TechAbilityAttachment.getAirborneCapacity(basesAtStart, player, data);
    if (airborneCapacity <= 0) {
      return result.setErrorReturnResult("Airborne Bases Must Have Launch Capacity");
    } else if (airborneCapacity < units.size()) {
      final Collection<Unit> overMax = new ArrayList<>(units);
      overMax.removeAll(CollectionUtils.getNMatches(units, airborneCapacity, Matches.always()));
      for (final Unit u : overMax) {
        result.addDisallowedUnit("Airborne Base Capacity Has Been Reached", u);
      }
    }
    final Collection<Unit> airborne = new ArrayList<>();
    for (final Unit u : units) {
      if (!Matches.unitIsOwnedBy(player).test(u)) {
        result.addDisallowedUnit("Must Own All Airborne Forces", u);
      } else if (!Matches.unitIsOfTypes(airborneTypes).test(u)) {
        result.addDisallowedUnit("Can Only Launch Airborne Forces", u);
      } else if (Matches.unitIsDisabled().test(u)) {
        result.addDisallowedUnit("Must Not Be Disabled", u);
      } else if (!Matches.unitHasNotMoved().test(u)) {
        result.addDisallowedUnit("Must Not Have Previously Moved Airborne Forces", u);
      } else if (Matches.unitIsAirborne().test(u)) {
        result.addDisallowedUnit("Cannot Move Units Already Airborne", u);
      } else {
        airborne.add(u);
      }
    }
    if (airborne.isEmpty()) {
      return result;
    }
    final BattleTracker battleTracker = AbstractMoveDelegate.getBattleTracker(data);
    final boolean onlyWhereUnderAttackAlready = Properties.getAirborneAttacksOnlyInExistingBattles(data);
    final boolean onlyEnemyTerritories = Properties.getAirborneAttacksOnlyInEnemyTerritories(data);
    final List<Territory> steps = route.getSteps();
    if (steps.isEmpty() || !steps.stream().allMatch(Matches.territoryIsPassableAndNotRestricted(player, data))) {
      return result.setErrorReturnResult("May Not Fly Over Impassable or Restricted Territories");
    }
    if (steps.isEmpty()
        || !steps.stream().allMatch(Matches.territoryAllowsCanMoveAirUnitsOverOwnedLand(player, data))) {
      return result.setErrorReturnResult("May Only Fly Over Territories Where Air May Move");
    }
    final boolean someLand = airborne.stream().anyMatch(Matches.unitIsLand());
    final boolean someSea = airborne.stream().anyMatch(Matches.unitIsSea());
    final boolean land = Matches.territoryIsLand().test(end);
    final boolean sea = Matches.territoryIsWater().test(end);
    if (someLand && someSea) {
      return result.setErrorReturnResult("Cannot Mix Land and Sea Units");
    } else if (someLand) {
      if (!land) {
        return result.setErrorReturnResult("Cannot Move Land Units To Sea");
      }
    } else if (someSea) {
      if (!sea) {
        return result.setErrorReturnResult("Cannot Move Sea Units To Land");
      }
    }
    if (onlyWhereUnderAttackAlready) {
      if (!battleTracker.getConquered().contains(end)) {
        final IBattle battle = battleTracker.getPendingBattle(end, false, BattleType.NORMAL);
        if (battle == null) {
          return result.setErrorReturnResult("Airborne May Only Attack Territories Already Under Assault");
        } else if (land && someLand && !battle.getAttackingUnits().stream().anyMatch(Matches.unitIsLand())) {
          return result.setErrorReturnResult("Battle Must Have Some Land Units Participating Already");
        } else if (sea && someSea && !battle.getAttackingUnits().stream().anyMatch(Matches.unitIsSea())) {
          return result.setErrorReturnResult("Battle Must Have Some Sea Units Participating Already");
        }
      }
    } else if (onlyEnemyTerritories) {
      if (!(Matches.isTerritoryEnemyAndNotUnownedWater(player, data).test(end)
          || Matches.territoryHasEnemyUnits(player, data).test(end))) {
        return result.setErrorReturnResult("Destination Must Be Enemy Or Contain Enemy Units");
      }
    }
    return result;
  }

  private static Predicate<Unit> getAirborneBaseMatch(final PlayerID player, final GameData data) {
    return getAirborneMatch(TechAbilityAttachment.getAirborneBases(player, data),
        data.getRelationshipTracker().getAllies(player, true));
  }

  private static Predicate<Unit> getAirborneMatch(final Set<UnitType> types, final Collection<PlayerID> unitOwners) {
    return Matches.unitIsOwnedByOfAnyOfThesePlayers(unitOwners)
        .and(Matches.unitIsOfTypes(types))
        .and(Matches.unitIsNotDisabled())
        .and(Matches.unitHasNotMoved())
        .and(Matches.unitIsAirborne().negate());
  }

  private static Change getNewAssignmentOfNumberLaunchedChange(int newNumberLaunched, final Collection<Unit> bases,
      final PlayerID player, final GameData data) {
    final CompositeChange launchedChange = new CompositeChange();
    if (newNumberLaunched <= 0) {
      return launchedChange;
    }
    final IntegerMap<UnitType> capacityMap = TechAbilityAttachment.getAirborneCapacity(player, data);
    for (final Unit u : bases) {
      if (newNumberLaunched <= 0) {
        break;
      }
      final int numberLaunchedAlready = ((TripleAUnit) u).getLaunched();
      final int capacity = capacityMap.getInt(u.getType());
      final int toAdd = Math.min(newNumberLaunched, capacity - numberLaunchedAlready);
      if (toAdd <= 0) {
        continue;
      }
      newNumberLaunched -= toAdd;
      launchedChange.add(ChangeFactory.unitPropertyChange(u, (toAdd + numberLaunchedAlready), TripleAUnit.LAUNCHED));
    }
    return launchedChange;
  }

  private static boolean allowAirborne(final PlayerID player, final GameData data) {
    if (!TechAbilityAttachment.getAllowAirborneForces(player, data)) {
      return false;
    }
    final int airborneDistance = TechAbilityAttachment.getAirborneDistance(player, data);
    final Set<UnitType> airborneBases = TechAbilityAttachment.getAirborneBases(player, data);
    final Set<UnitType> airborneTypes = TechAbilityAttachment.getAirborneTypes(player, data);
    if ((airborneDistance <= 0) || airborneBases.isEmpty() || airborneTypes.isEmpty()) {
      return false;
    }
    final GameMap map = data.getMap();
    final Collection<PlayerID> alliesForBases = data.getRelationshipTracker().getAllies(player, true);
    final Collection<Territory> territoriesWeCanLaunchFrom = CollectionUtils.getMatches(map.getTerritories(),
        Matches.territoryHasUnitsThatMatch(getAirborneMatch(airborneBases, alliesForBases)));

    return !territoriesWeCanLaunchFrom.isEmpty();
  }

  private static boolean getEditMode(final GameData data) {
    return BaseEditDelegate.getEditMode(data);
  }

  @Override
  public int pusAlreadyLost(final Territory t) {
    return 0;
  }

  @Override
  public void pusLost(final Territory t, final int amt) {}
}
