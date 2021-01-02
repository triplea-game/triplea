package games.strategy.triplea.ui.menubar.debug;

import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.TerritoryDetailPanel;
import games.strategy.triplea.ui.panels.map.MapPanel;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;

@Value
@Getter(value = AccessLevel.PRIVATE)
public class AiPlayerDebugAction {

  MapPanel mapPanel;
  TerritoryDetailPanel territoryDetails;
  Collection<Territory> territoriesRendered = new ArrayList<>();
  Collection<Function<Territory, String>> territoryDetailsRendered = new ArrayList<>();

  public class DebugMapRenderer {
    public void colorOnTerritory(
        final Territory territory, final Color color, final double transparency) {
      mapPanel.setTerritoryOverlayForTile(territory, color, 100);
      territoriesRendered.add(territory);
    }

    public void clearTerritory(final Territory territory) {
      mapPanel.clearTerritoryOverlay(territory);
    }
  }

  public void renderOnMap(final Consumer<DebugMapRenderer> mapRenderer) {
    mapRenderer.accept(new DebugMapRenderer());
    mapPanel.repaint();
  }

  public void renderInTerritoryDetails(final Function<Territory, String> territoryDetailsGetter) {
    territoryDetails.addAdditionalTerritoryDetailsFunction(territoryDetailsGetter);
    territoryDetailsRendered.add(territoryDetailsGetter);
  }

  public void deselect() {
    territoriesRendered.forEach(mapPanel::clearTerritoryOverlay);
    territoryDetailsRendered.forEach(territoryDetails::removeAdditionalTerritoryDetailsFunction);
    mapPanel.repaint();
  }
}
