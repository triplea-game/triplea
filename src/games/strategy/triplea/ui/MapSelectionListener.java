package games.strategy.triplea.ui;

import games.strategy.engine.data.Territory;


public interface MapSelectionListener {
  public void territorySelected(Territory territory, MouseDetails md);

  /**
   * The mouse has entered the given territory,
   * null if the mouse is in no territory.
   */
  public void mouseEntered(Territory territory);

  public void mouseMoved(Territory territory, MouseDetails md);
}
