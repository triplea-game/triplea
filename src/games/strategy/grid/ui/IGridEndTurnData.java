package games.strategy.grid.ui;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;

import java.io.Serializable;
import java.util.Set;

public interface IGridEndTurnData extends Serializable
{
	public PlayerID getPlayer();
	
	public Set<Territory> getTerritoryUnitsRemovalAdjustment();
	
	public boolean getWantToContinuePlaying();
}
