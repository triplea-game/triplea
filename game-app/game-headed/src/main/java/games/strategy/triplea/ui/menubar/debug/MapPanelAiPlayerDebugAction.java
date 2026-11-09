package games.strategy.triplea.ui.menubar.debug;

import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.AdditionalTerritoryDetails;
import games.strategy.triplea.ui.panels.map.MapPanel;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

/** Renders an AI debug option's output on the {@link MapPanel} and territory details panel. */
@RequiredArgsConstructor
public class MapPanelAiPlayerDebugAction implements AiPlayerDebugAction {

  private final MapPanel mapPanel;
  private final AdditionalTerritoryDetails additionalTerritoryDetails;
  private final Collection<Territory> territoriesRendered = new ArrayList<>();
  private final Collection<Function<Territory, String>> territoryDetailsRendered =
      new ArrayList<>();

  @Override
  public void renderOnMap(final Consumer<DebugMapRenderer> mapRenderer) {
    mapRenderer.accept(this::colorOnTerritory);
    mapPanel.repaint();
  }

  private void colorOnTerritory(final Territory territory, final Color color, final int alpha) {
    mapPanel.setTerritoryOverlayForTile(territory, color, alpha);
    territoriesRendered.add(territory);
  }

  @Override
  public void renderInTerritoryDetails(final Function<Territory, String> territoryDetailsGetter) {
    additionalTerritoryDetails.addAdditionalTerritoryDetailsFunction(territoryDetailsGetter);
    territoryDetailsRendered.add(territoryDetailsGetter);
  }

  /** Undoes the changes that the debug option caused. */
  public void deselect() {
    territoriesRendered.forEach(mapPanel::clearTerritoryOverlay);
    territoryDetailsRendered.forEach(
        additionalTerritoryDetails::removeAdditionalTerritoryDetailsFunction);
    mapPanel.repaint();
  }
}
