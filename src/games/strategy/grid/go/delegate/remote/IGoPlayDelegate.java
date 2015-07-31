package games.strategy.grid.go.delegate.remote;

import java.util.List;
import java.util.Map;
import java.util.Set;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.grid.delegate.remote.IGridPlayDelegate;

public interface IGoPlayDelegate extends IGridPlayDelegate {
  public boolean haveTwoPassedInARow();

  public List<Map<Territory, PlayerID>> getPreviousMapStates();

  public Set<Unit> getCapturedUnits();
}
