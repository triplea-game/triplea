package games.strategy.grid.go.delegate.remote;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.grid.delegate.remote.IGridPlayDelegate;

import java.util.List;
import java.util.Map;

public interface IGoPlayDelegate extends IGridPlayDelegate
{
	public boolean haveTwoPassedInARow();
	
	public List<Map<Territory, PlayerID>> getPreviousMapStates();
}
