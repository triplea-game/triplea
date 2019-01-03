package games.strategy.triplea.ui;

import javax.annotation.Nullable;

import games.strategy.engine.data.Territory;

interface MapSelectionListener {
  void territorySelected(Territory territory, MouseDetails md);

  /** The mouse has entered the given territory, null if the mouse is in no territory. */
  void mouseEntered(Territory territory);

  void mouseMoved(@Nullable Territory territory, MouseDetails md);
}
