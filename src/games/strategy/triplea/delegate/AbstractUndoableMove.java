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
abstract public class AbstractUndoableMove implements Serializable
{
	private static final long serialVersionUID = -3164832285286161069L;
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
	
	public AbstractUndoableMove(final CompositeChange change, final Collection<Unit> units)
	{
		m_change = change;
		m_units = units;
	}
	
	final public void undo(final GameData data, final IDelegateBridge delegateBridge)
	{
		// undo any changes to the game data
		delegateBridge.getHistoryWriter().startEvent(delegateBridge.getPlayerID().getName() + " undo move " + (getIndex() + 1) + ".");
		delegateBridge.getHistoryWriter().setRenderingData(getDescriptionObject());
		delegateBridge.addChange(m_change.invert());
		undoSpecific(delegateBridge);
	}
	
	abstract protected void undoSpecific(IDelegateBridge bridge);
	
	public final CompositeChange getChange()
	{
		return m_change;
	}
	
	public final void addChange(final Change aChange)
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
	
	public void setIndex(final int index)
	{
		m_index = index;
	}
	
	abstract public String getMoveLabel();
	
	abstract public Territory getEnd();
	
	abstract protected AbstractMoveDescription getDescriptionObject();
}
