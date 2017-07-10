package games.strategy.triplea.delegate;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.dataObjects.AbstractMoveDescription;

/**
 * Contains all the data to describe an abstract move (move or placement) and to undo it.
 */
public abstract class AbstractUndoableMove implements Serializable {
  private static final long serialVersionUID = -3164832285286161069L;
  /**
   * Stores the serialized state of the move and battle delegates (just
   * as if they were saved), and a CompositeChange that represents all the changes that
   * were made during the move.
   * Some moves (such as those following an aa fire) can't be undone.
   */
  protected final CompositeChange m_change;
  protected int m_index;
  protected final Collection<Unit> m_units;

  public AbstractUndoableMove(final Collection<Unit> units) {
    this(new CompositeChange(), units);
  }


  public AbstractUndoableMove(final CompositeChange change, final Collection<Unit> units) {
    m_change = change;
    m_units = units;
  }

  public boolean containsAnyOf(final Set<Unit> units) {
    if (units == null) {
      return false;
    }
    for (final Unit unit : units) {
      if (containsUnit(unit)) {
        return true;
      }
    }
    return false;
  }

  private boolean containsUnit(final Unit unit) {
    return m_units.contains(unit);
  }

  final void undo(final IDelegateBridge delegateBridge) {
    // undo any changes to the game data
    delegateBridge.getHistoryWriter().startEvent(
        delegateBridge.getPlayerID().getName() + " undo move " + (getIndex() + 1) + ".", getDescriptionObject());
    delegateBridge.addChange(m_change.invert());
    undoSpecific(delegateBridge);
  }

  protected abstract void undoSpecific(IDelegateBridge bridge);

  public final CompositeChange getChange() {
    return m_change;
  }

  public final void addChange(final Change change) {
    m_change.add(change);
  }

  public Collection<Unit> getUnits() {
    return m_units;
  }

  public int getIndex() {
    return m_index;
  }

  public void setIndex(final int index) {
    m_index = index;
  }

  public abstract String getMoveLabel();

  public abstract Territory getEnd();

  protected abstract AbstractMoveDescription getDescriptionObject();
}
