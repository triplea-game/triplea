package games.strategy.grid.ui;

import java.io.Serializable;
import java.util.List;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;


public interface IGridPlayData extends Serializable {
  public Territory getStart();

  public List<Territory> getMiddleSteps();

  public Territory getEnd();

  public List<Territory> getAllSteps();

  public List<Territory> getAllStepsExceptStart();

  public PlayerID getPlayerID();

  public boolean isBiggerThanAndContains(IGridPlayData otherPlay);

  public boolean isPass();
}
