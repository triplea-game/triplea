package games.strategy.triplea.ui.panels.map;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import java.util.List;

public interface MouseOverUnitListener {
  /** units will be empty if the mouse is not over any unit. */
  void mouseEnter(List<Unit> units, Territory territory);
}
