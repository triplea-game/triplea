package games.strategy.triplea.ui.menubar.debug;

import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.AdditionalTerritoryDetails;
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
  AdditionalTerritoryDetails additionalTerritoryDetails;
  Collection<Territory> territoriesRendered = new ArrayList<>();
  Collection<Function<Territory, String>> territoryDetailsRendered = new ArrayList<>();

  /** Gives the debug option methods to draw on the rendered map */
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

  /**
   * Allows debug options to draw on the rendered map
   *
   * @param mapRenderer The debug option uses the DebugMapRenderer to draw on the rendered map
   */
  public void renderOnMap(final Consumer<DebugMapRenderer> mapRenderer) {
    mapRenderer.accept(new DebugMapRenderer());
    mapPanel.repaint();
  }

  /**
   * Allows the debug option to add additional text to the territory details panel
   *
   * @param territoryDetailsGetter Takes a territory and returns the additional text for the panel
   */
  public void renderInTerritoryDetails(final Function<Territory, String> territoryDetailsGetter) {
    additionalTerritoryDetails.addAdditionalTerritoryDetailsFunction(territoryDetailsGetter);
    territoryDetailsRendered.add(territoryDetailsGetter);
  }

  /**
   * Undoes the changes that the debug option caused.
   *
   * <p>This should not be called by the debug option.
   */
  public void deselect() {
    territoriesRendered.forEach(mapPanel::clearTerritoryOverlay);
    territoryDetailsRendered.forEach(
        additionalTerritoryDetails::removeAdditionalTerritoryDetailsFunction);
    mapPanel.repaint();
  }
}
