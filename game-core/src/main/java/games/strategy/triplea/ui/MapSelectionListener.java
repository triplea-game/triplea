package games.strategy.triplea.ui;

import games.strategy.engine.data.Territory;
import javax.annotation.Nullable;

interface MapSelectionListener {
  void territorySelected(Territory territory, MouseDetails md);

  /** The mouse has entered the given territory, null if the mouse is in no territory. */
  void mouseEntered(@Nullable Territory territory);

  void mouseMoved(@Nullable Territory territory, MouseDetails md);
}
