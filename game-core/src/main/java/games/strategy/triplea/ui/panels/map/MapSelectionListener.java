package games.strategy.triplea.ui.panels.map;

import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.MouseDetails;
import javax.annotation.Nullable;

public interface MapSelectionListener {
  void territorySelected(Territory territory, MouseDetails md);

  /** The mouse has entered the given territory, null if the mouse is in no territory. */
  void mouseEntered(@Nullable Territory territory);

  void mouseMoved(@Nullable Territory territory, MouseDetails md);
}
