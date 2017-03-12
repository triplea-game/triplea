package games.strategy.triplea.ui;

import java.util.List;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;

public interface UnitSelectionListener {
  /**
   * Note, if the mouse is clicked where there are no units, units will be empty but territory will still be correct.
   */
  void unitsSelected(List<Unit> units, Territory territory, MouseDetails md);
}
