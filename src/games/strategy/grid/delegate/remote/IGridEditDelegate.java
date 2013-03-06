package games.strategy.grid.delegate.remote;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IPersistentDelegate;
import games.strategy.engine.message.IRemote;

import java.util.Collection;

/**
 * 
 * @author veqryn
 * 
 */
public interface IGridEditDelegate extends IRemote, IPersistentDelegate
{
	public boolean getEditMode();
	
	public String setEditMode(boolean editMode);
	
	public String removeUnits(Territory t, Collection<Unit> units);
	
	public String addUnits(Territory t, Collection<Unit> units);
	
	public String changeTerritoryOwner(Territory t, PlayerID player);
	
	public String addComment(String message);
}
