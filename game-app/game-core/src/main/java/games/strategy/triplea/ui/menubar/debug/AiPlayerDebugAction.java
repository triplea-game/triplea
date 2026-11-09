package games.strategy.triplea.ui.menubar.debug;

import games.strategy.engine.data.Territory;
import java.awt.Color;
import java.util.function.Consumer;
import java.util.function.Function;

/** Lets an AI debug option render its output on the game map and territory details panel. */
public interface AiPlayerDebugAction {

  /** Gives the debug option methods to draw on the rendered map */
  @FunctionalInterface
  interface DebugMapRenderer {
    void colorOnTerritory(Territory territory, Color color, int alpha);
  }

  /**
   * Allows debug options to draw on the rendered map
   *
   * @param mapRenderer The debug option uses the DebugMapRenderer to draw on the rendered map
   */
  void renderOnMap(Consumer<DebugMapRenderer> mapRenderer);

  /**
   * Allows the debug option to add additional text to the territory details panel
   *
   * @param territoryDetailsGetter Takes a territory and returns the additional text for the panel
   */
  void renderInTerritoryDetails(Function<Territory, String> territoryDetailsGetter);
}
