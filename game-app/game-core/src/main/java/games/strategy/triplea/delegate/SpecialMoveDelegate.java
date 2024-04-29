package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.delegate.battle.BattleDelegate;
import games.strategy.triplea.delegate.battle.BattleTracker;
import games.strategy.triplea.delegate.data.MoveValidationResult;
import games.strategy.triplea.delegate.move.validation.MoveValidator;
import games.strategy.triplea.formatter.MyFormatter;
import java.io.Serializable;
import java.util.Collection;
import java.util.Set;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

/**
 * SpecialMoveDelegate is a move delegate made for special movements like the new
 * paratrooper/airborne movement. Airborne Attacks is actually Paratroopers tech for Global 1940,
 * except that I really do not want to confuse myself by naming yet another thing Paratroopers, so
 * this is now getting a new name. This is very different than "paratroopers" for AA50. We are
 * actually launching the units from a static unit (an airbase) to another territory, instead of
 * carrying them.
 */
public class SpecialMoveDelegate extends AbstractMoveDelegate {
  private boolean needToInitialize = true;

  @Override
  public void start() {
    super.start();
    final GameData data = getData();
    if (!allowAirborne(player, data)) {
      return;
    }
    final boolean onlyWhereUnderAttackAlready =
        Properties.getAirborneAttacksOnlyInExistingBattles(data.getProperties());
    final BattleTracker battleTracker = AbstractMoveDelegate.getBattleTracker(data);
    if (needToInitialize && onlyWhereUnderAttackAlready) {
      // we do this to clear any 'finishedBattles' and also to create battles for units that didn't
      // move
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
    state.needToInitialize = needToInitialize;
    return state;
  }

  @Override
  public void loadState(final Serializable state) {
    final SpecialMoveExtendedDelegateState s = (SpecialMoveExtendedDelegateState) state;
    super.loadState(s.superState);
    needToInitialize = s.needToInitialize;
  }

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    return allowAirborne(player, getData());
  }

  @Override
  public String performMove(final MoveDescription move) {
    if (!allowAirborne(player, getData())) {
      return "No Airborne Movement Allowed Yet";
    }
    final GameData data = getData();
    final Collection<Unit> units = move.getUnits();
    final Route route = move.getRoute();
    // there reason we use this, is because if we are in edit mode, we may have a different unit
    // owner than the current
    // player.
    final GamePlayer player = getUnitsOwner(units);
    // here we have our own new validation method....
    final MoveValidationResult result =
        new MoveValidator(data, false).validateSpecialMove(move, player);
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
      return errorMsg.append(result.getError()).append(numErrorsMsg).toString();
    }
    if (result.hasDisallowedUnits()) {
      return errorMsg.append(result.getDisallowedUnitWarning(0)).append(numErrorsMsg).toString();
    }
    if (result.hasUnresolvedUnits()) {
      return errorMsg.append(result.getUnresolvedUnitWarning(0)).append(numErrorsMsg).toString();
    }
    // allow user to cancel move if aa guns will fire
    final AaInMoveUtil aaInMoveUtil = new AaInMoveUtil();
    aaInMoveUtil.initialize(bridge);
    final Collection<Territory> aaFiringTerritories =
        aaInMoveUtil.getTerritoriesWhereAaWillFire(route, units);
    if (!aaFiringTerritories.isEmpty()
        && !bridge.getRemotePlayer().confirmMoveInFaceOfAa(aaFiringTerritories)) {
      return null;
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
      airborneChange.add(ChangeFactory.unitPropertyChange(u, true, Unit.AIRBORNE));
    }
    currentMove.addChange(airborneChange);
    // make the bases start filling up their capacity
    final Collection<Unit> basesAtStart =
        route.getStart().getMatches(MoveValidator.getAirborneBaseMatch(player, data));
    final Change fillLaunchCapacity =
        getNewAssignmentOfNumberLaunchedChange(units.size(), basesAtStart, player, data);
    currentMove.addChange(fillLaunchCapacity);
    // start event
    final String transcriptText =
        MyFormatter.unitsToTextNoOwner(units)
            + " moved from "
            + route.getStart().getName()
            + " to "
            + route.getEnd().getName();
    bridge.getHistoryWriter().startEvent(transcriptText, currentMove.getDescriptionObject());
    // actually do our special changes
    bridge.addChange(airborneChange);
    bridge.addChange(fillLaunchCapacity);
    tempMovePerformer = new MovePerformer();
    tempMovePerformer.initialize(this);
    tempMovePerformer.moveUnits(move, player, currentMove);
    tempMovePerformer = null;
    return null;
  }

  private static Change getNewAssignmentOfNumberLaunchedChange(
      final int initialNewNumberLaunched,
      final Collection<Unit> bases,
      final GamePlayer player,
      final GameState data) {
    final CompositeChange launchedChange = new CompositeChange();
    int newNumberLaunched = initialNewNumberLaunched;
    if (newNumberLaunched <= 0) {
      return launchedChange;
    }
    final IntegerMap<UnitType> capacityMap =
        TechAbilityAttachment.getAirborneCapacity(
            TechTracker.getCurrentTechAdvances(player, data.getTechnologyFrontier()));
    for (final Unit u : bases) {
      if (newNumberLaunched <= 0) {
        break;
      }
      final int numberLaunchedAlready = u.getLaunched();
      final int capacity = capacityMap.getInt(u.getType());
      final int toAdd = Math.min(newNumberLaunched, capacity - numberLaunchedAlready);
      if (toAdd <= 0) {
        continue;
      }
      newNumberLaunched -= toAdd;
      launchedChange.add(
          ChangeFactory.unitPropertyChange(u, (toAdd + numberLaunchedAlready), Unit.LAUNCHED));
    }
    return launchedChange;
  }

  private static boolean allowAirborne(final GamePlayer player, final GameState data) {
    if (!TechAbilityAttachment.getAllowAirborneForces(
        TechTracker.getCurrentTechAdvances(player, data.getTechnologyFrontier()))) {
      return false;
    }
    final int airborneDistance =
        TechAbilityAttachment.getAirborneDistance(
            TechTracker.getCurrentTechAdvances(player, data.getTechnologyFrontier()));
    final Set<UnitType> airborneBases =
        TechAbilityAttachment.getAirborneBases(
            TechTracker.getCurrentTechAdvances(player, data.getTechnologyFrontier()));
    final Set<UnitType> airborneTypes =
        TechAbilityAttachment.getAirborneTypes(
            TechTracker.getCurrentTechAdvances(player, data.getTechnologyFrontier()));
    if (airborneDistance <= 0 || airborneBases.isEmpty() || airborneTypes.isEmpty()) {
      return false;
    }
    final GameMap map = data.getMap();
    final Collection<GamePlayer> alliesForBases =
        data.getRelationshipTracker().getAllies(player, true);
    final Collection<Territory> territoriesWeCanLaunchFrom =
        CollectionUtils.getMatches(
            map.getTerritories(),
            Matches.territoryHasUnitsThatMatch(
                MoveValidator.getAirborneMatch(airborneBases, alliesForBases)));

    return !territoriesWeCanLaunchFrom.isEmpty();
  }

  @Override
  public int pusAlreadyLost(final Territory t) {
    return 0;
  }

  @Override
  public void pusLost(final Territory t, final int amt) {}
}
