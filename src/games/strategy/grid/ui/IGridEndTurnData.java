package games.strategy.grid.ui;

import java.io.Serializable;
import java.util.Set;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;

public interface IGridEndTurnData extends Serializable {
  public PlayerID getPlayer();

  public Set<Territory> getTerritoryUnitsRemovalAdjustment();

  public boolean getWantToContinuePlaying();
}
