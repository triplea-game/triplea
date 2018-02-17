package games.strategy.triplea.delegate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.pbem.PBEMMessagePoster;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.dataObjects.MoveValidationResult;
import games.strategy.triplea.delegate.remote.IMoveDelegate;

/**
 * An abstraction of MoveDelegate in order to allow other delegates to extend this.
 */
public abstract class AbstractMoveDelegate extends BaseTripleADelegate implements IMoveDelegate {
  // A collection of UndoableMoves
  protected List<UndoableMove> movesToUndo = new ArrayList<>();
  // protected final TransportTracker m_transportTracker = new TransportTracker();
  // if we are in the process of doing a move. this instance will allow us to resume the move
  protected MovePerformer tempMovePerformer;

  public enum MoveType {
    DEFAULT, SPECIAL
  }

  public AbstractMoveDelegate() {}

  @Override
  public void start() {
    super.start();
    if (tempMovePerformer != null) {
      tempMovePerformer.initialize(this);
      tempMovePerformer.resume();
      tempMovePerformer = null;
    }
  }

  @Override
  public void end() {
    super.end();
    movesToUndo.clear();
  }

  @Override
  public Serializable saveState() {
    final AbstractMoveExtendedDelegateState state = new AbstractMoveExtendedDelegateState();
    state.superState = super.saveState();
    // add other variables to state here:
    state.m_movesToUndo = movesToUndo;
    state.m_tempMovePerformer = tempMovePerformer;
    return state;
  }

  @Override
  public void loadState(final Serializable state) {
    final AbstractMoveExtendedDelegateState s = (AbstractMoveExtendedDelegateState) state;
    super.loadState(s.superState);
    // if the undo state wasnt saved, then dont load it. prevents overwriting undo state when we restore from an undo
    // move
    if (s.m_movesToUndo != null) {
      movesToUndo = s.m_movesToUndo;
    }
    tempMovePerformer = s.m_tempMovePerformer;
  }

  @Override
  public List<UndoableMove> getMovesMade() {
    return new ArrayList<>(movesToUndo);
  }

  @Override
  public String undoMove(final int moveIndex) {
    if (movesToUndo.isEmpty()) {
      return "No moves to undo";
    }
    if (moveIndex >= movesToUndo.size()) {
      return "Undo move index out of range";
    }
    final UndoableMove moveToUndo = movesToUndo.get(moveIndex);
    if (!moveToUndo.getcanUndo()) {
      return moveToUndo.getReasonCantUndo();
    }
    moveToUndo.undo(bridge);
    movesToUndo.remove(moveIndex);
    updateUndoableMoveIndexes();
    return null;
  }

  private void updateUndoableMoveIndexes() {
    for (int i = 0; i < movesToUndo.size(); i++) {
      movesToUndo.get(i).setIndex(i);
    }
  }

  protected void updateUndoableMoves(final UndoableMove currentMove) {
    currentMove.initializeDependencies(movesToUndo);
    movesToUndo.add(currentMove);
    updateUndoableMoveIndexes();
  }

  protected PlayerID getUnitsOwner(final Collection<Unit> units) {
    // if we are not in edit mode, return player. if we are in edit mode, we use whoever's units these are.
    return (units.isEmpty() || !BaseEditDelegate.getEditMode(getData())) ? player : units.iterator().next().getOwner();
  }

  @Override
  public String move(final Collection<Unit> units, final Route route) {
    return move(units, route, Collections.emptyList());
  }

  @Override
  public String move(final Collection<Unit> units, final Route route,
      final Collection<Unit> transportsThatCanBeLoaded) {
    return move(units, route, transportsThatCanBeLoaded, new HashMap<>());
  }

  @Override
  public abstract String move(final Collection<Unit> units, final Route route,
      final Collection<Unit> transportsThatCanBeLoaded, final Map<Unit, Collection<Unit>> newDependents);

  public static MoveValidationResult validateMove(final MoveType moveType, final Collection<Unit> units,
      final Route route, final PlayerID player, final Collection<Unit> transportsToLoad,
      final Map<Unit, Collection<Unit>> newDependents, final boolean isNonCombat,
      final List<UndoableMove> undoableMoves, final GameData data) {
    if (moveType == MoveType.SPECIAL) {
      return SpecialMoveDelegate.validateMove(units, route, player, data);
    }
    return MoveValidator.validateMove(units, route, player, transportsToLoad, newDependents, isNonCombat, undoableMoves,
        data);
  }

  @Override
  public Collection<Territory> getTerritoriesWhereAirCantLand(final PlayerID player) {
    return new AirThatCantLandUtil(bridge).getTerritoriesWhereAirCantLand(player);
  }

  @Override
  public Collection<Territory> getTerritoriesWhereAirCantLand() {
    return new AirThatCantLandUtil(bridge).getTerritoriesWhereAirCantLand(player);
  }

  @Override
  public Collection<Territory> getTerritoriesWhereUnitsCantFight() {
    return new UnitsThatCantFightUtil(getData()).getTerritoriesWhereUnitsCantFight(player);
  }

  /**
   * @param unit
   *        referring unit.
   * @param end
   *        target territory
   * @return the route that a unit used to move into the given territory
   */
  public Route getRouteUsedToMoveInto(final Unit unit, final Territory end) {
    return AbstractMoveDelegate.getRouteUsedToMoveInto(movesToUndo, unit, end);
  }

  /**
   * This method is static so it can be called from the client side.
   *
   * @param undoableMoves
   *        list of moves that have been done
   * @param unit
   *        referring unit
   * @param end
   *        target territory
   * @return the route that a unit used to move into the given territory.
   */
  public static Route getRouteUsedToMoveInto(final List<UndoableMove> undoableMoves, final Unit unit,
      final Territory end) {
    final ListIterator<UndoableMove> iter = undoableMoves.listIterator(undoableMoves.size());
    while (iter.hasPrevious()) {
      final UndoableMove move = iter.previous();
      if (!move.getUnits().contains(unit)) {
        continue;
      }
      if (move.getRoute().getEnd().equals(end)) {
        return move.getRoute();
      }
    }
    return null;
  }

  public static BattleTracker getBattleTracker(final GameData data) {
    return DelegateFinder.battleDelegate(data).getBattleTracker();
  }

  protected boolean isWW2V2() {
    return Properties.getWW2V2(getData());
  }

  @Override
  public void setHasPostedTurnSummary(final boolean hasPostedTurnSummary) {
    // nothing for now
  }

  @Override
  public boolean getHasPostedTurnSummary() {
    return false;
  }

  @Override
  public boolean postTurnSummary(final PBEMMessagePoster poster, final String title, final boolean includeSaveGame) {
    return poster.post(bridge.getHistoryWriter(), title, includeSaveGame);
  }

  /**
   * @return The number of PUs that have been lost by bombing, rockets, etc.
   */
  public abstract int pusAlreadyLost(final Territory t);

  /**
   * Add more PUs lost to a territory due to bombing, rockets, etc.
   */
  public abstract void pusLost(final Territory t, final int amt);

  @Override
  public Class<IMoveDelegate> getRemoteType() {
    return IMoveDelegate.class;
  }
}
