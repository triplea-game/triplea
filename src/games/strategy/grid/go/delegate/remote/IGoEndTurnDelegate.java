package games.strategy.grid.go.delegate.remote;

import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.message.IRemote;
import games.strategy.grid.ui.IGridEndTurnData;
import games.strategy.triplea.delegate.remote.IAbstractForumPosterDelegate;

public interface IGoEndTurnDelegate extends IRemote, IDelegate, IAbstractForumPosterDelegate
{
	public String territoryAdjustment(IGridEndTurnData endTurnData);
	
	public IGridEndTurnData getTerritoryAdjustment();
	
	public boolean haveTwoPassedInARow();
	
	public void signalStatus(String status);
}
