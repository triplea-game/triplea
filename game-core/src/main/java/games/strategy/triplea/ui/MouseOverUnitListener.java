package games.strategy.triplea.ui;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import java.util.List;

interface MouseOverUnitListener {
  /** units will be empty if the mouse is not over any unit. */
  void mouseEnter(List<Unit> units, Territory territory);
}
