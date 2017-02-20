package games.strategy.triplea.ui;

import java.util.List;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;

public interface MouseOverUnitListener {
  /**
   * units will be empty if the mouse is not over any unit
   */
  void mouseEnter(List<Unit> units, Territory territory, MouseDetails me);
}
