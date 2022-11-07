package games.strategy.triplea.ui;

import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.panels.map.MapSelectionListener;
import javax.annotation.Nullable;

public class DefaultMapSelectionListener implements MapSelectionListener {
  @Override
  public void territorySelected(final Territory territory, final MouseDetails me) {}

  @Override
  public void mouseEntered(final @Nullable Territory territory) {}

  @Override
  public void mouseMoved(final @Nullable Territory territory, final MouseDetails me) {}
}
