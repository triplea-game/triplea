package games.strategy.triplea.delegate;

import games.strategy.engine.data.AbstractMoveDescription;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import java.io.Serializable;
import java.util.Collection;
import lombok.Getter;

/** Contains all the data to describe an abstract move (move or placement) and to undo it. */
public abstract class AbstractUndoableMove implements Serializable {
  private static final long serialVersionUID = -3164832285286161069L;

  /**
   * Stores the serialized state of the move and battle delegates (just as if they were saved), and
   * a CompositeChange that represents all the changes that were made during the move. Some moves
   * (such as those following an aa fire) can't be undone.
   */
  protected final CompositeChange change;

  @Getter protected int index;
  @Getter protected final Collection<Unit> units;

  public AbstractUndoableMove(final CompositeChange change, final Collection<Unit> units) {
    this.change = change;
    this.units = units;
  }

  public boolean containsUnit(final Unit unit) {
    return units.contains(unit);
  }

  final void undo(final IDelegateBridge delegateBridge) {
    // undo any changes to the game data
    delegateBridge
        .getHistoryWriter()
        .startEvent(
            delegateBridge.getGamePlayer().getName() + " undo move " + (getIndex() + 1) + ".",
            getDescriptionObject());
    delegateBridge.addChange(change.invert());
    undoSpecific(delegateBridge);
  }

  protected abstract void undoSpecific(IDelegateBridge bridge);

  public final void addChange(final Change change) {
    this.change.add(change);
  }

  public void setIndex(final int index) {
    this.index = index;
  }

  public abstract String getMoveLabel();

  public abstract Territory getEnd();

  protected abstract AbstractMoveDescription getDescriptionObject();
}
