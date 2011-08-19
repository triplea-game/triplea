package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.dataObjects.AbstractMoveDescription;

import java.io.Serializable;
import java.util.Collection;
/**
 * Contains all the data to describe an abstract move (move or placement) and to undo it.
 *
 * @author Erik von der Osten
 */
@SuppressWarnings("serial")
abstract public class AbstractUndoableMove implements Serializable
{

    /**
     * Stores the serialized state of the move and battle delegates (just
     * as if they were saved), and a CompositeChange that represents all the changes that
     * were made during the move.
     *
     * Some moves (such as those following an aa fire) can't be undone.
     */
    protected final CompositeChange m_change;
    protected int m_index;
    protected final Collection<Unit> m_units;

    public AbstractUndoableMove(CompositeChange change, Collection<Unit> units)
    {
        m_change = change;
        m_units = units;
    }

    final public void undo(GameData data, IDelegateBridge delegateBridge)
    {
        // undo any changes to the game data
    	delegateBridge.getHistoryWriter().startEvent(delegateBridge.getPlayerID().getName() + " undo move " + (getIndex() + 1) + ".");
    	delegateBridge.getHistoryWriter().setRenderingData(getDescriptionObject());
    	
        delegateBridge.addChange(m_change.invert());
        undoSpecific(data, delegateBridge);
    }

    abstract protected void undoSpecific(GameData data, IDelegateBridge bridge);

    public final CompositeChange getChange()
    {
        return m_change;
    }


    public final void addChange(Change aChange)
    {
        m_change.add(aChange);

    }

    public Collection<Unit> getUnits()
    {
        return m_units;
    }

    public int getIndex()
    {
        return m_index;
    }

    public void setIndex(int index)
    {
        m_index = index;
    }

    abstract public String getMoveLabel();
    abstract public Territory getEnd();
    abstract protected AbstractMoveDescription getDescriptionObject();
}
