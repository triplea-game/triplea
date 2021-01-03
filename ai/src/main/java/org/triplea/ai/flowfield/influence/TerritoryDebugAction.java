package org.triplea.ai.flowfield.influence;

import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.menubar.debug.AiPlayerDebugAction;
import java.awt.Color;
import java.util.Comparator;
import java.util.LongSummaryStatistics;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TerritoryDebugAction implements Consumer<AiPlayerDebugAction> {

  private final InfluenceMap influenceMap;
  private final GameMap gameMap;

  @Override
  public void accept(final AiPlayerDebugAction aiPlayerDebugAction) {
    aiPlayerDebugAction.renderInTerritoryDetails(this::buildTerritoryDetail);

    final LongSummaryStatistics summary =
        influenceMap.getTerritories().values().stream()
            .sorted(Comparator.comparingLong(InfluenceTerritory::getInfluence))
            .mapToLong(InfluenceTerritory::getInfluence)
            .summaryStatistics();

    aiPlayerDebugAction.renderOnMap(
        debugMapRenderer -> {
          gameMap.getTerritories().stream()
              .map(territory -> influenceMap.getTerritories().get(territory))
              .filter(Objects::nonNull)
              .forEach(
                  influenceTerritory ->
                      colorOnTerritory(
                          this.normalize(summary), debugMapRenderer, influenceTerritory));
        });
  }

  private String buildTerritoryDetail(final Territory territory) {
    final InfluenceTerritory influenceTerritory =
        influenceMap.getTerritories().getOrDefault(territory, new InfluenceTerritory(territory));

    return influenceMap.getName() + " Value: " + influenceTerritory.getInfluence();
  }

  /** Normalize the influence value to between 0.0 and 1.0 */
  private Function<Long, Float> normalize(final LongSummaryStatistics summary) {
    return influence ->
        ((float) (influence - summary.getMin())) / (summary.getMax() - summary.getMin());
  }

  /**
   * Places a colorized overlay on top of the territory in the game map
   *
   * <p>The color of the overlay is between BLUE and RED depending on how close it is to the maximum
   * value or the minimum value.
   */
  private void colorOnTerritory(
      final Function<Long, Float> normalize,
      final AiPlayerDebugAction.DebugMapRenderer debugMapRenderer,
      final InfluenceTerritory influenceTerritory) {
    if (influenceTerritory.getInfluence() <= 0) {
      return;
    }
    final Color color =
        linearInterpolate(
            normalize.apply(influenceTerritory.getInfluence()), Color.BLUE, Color.RED);
    debugMapRenderer.colorOnTerritory(influenceTerritory.getTerritory(), color, 100);
  }

  private Color linearInterpolate(final float fraction, final Color start, final Color end) {
    return new Color(
        (int) (start.getRed() * fraction) + (int) (end.getRed() * (1 - fraction)),
        (int) (start.getGreen() * fraction) + (int) (end.getGreen() * (1 - fraction)),
        (int) (start.getBlue() * fraction) + (int) (end.getBlue() * (1 - fraction)));
  }
}
