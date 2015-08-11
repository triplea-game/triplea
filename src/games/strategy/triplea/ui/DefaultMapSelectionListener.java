package games.strategy.triplea.ui;

import games.strategy.engine.data.Territory;

public class DefaultMapSelectionListener implements MapSelectionListener {
  @Override
  public void territorySelected(final Territory territory, final MouseDetails me) {}

  @Override
  public void mouseEntered(final Territory territory) {}

  @Override
  public void mouseMoved(final Territory territory, final MouseDetails me) {}
}
