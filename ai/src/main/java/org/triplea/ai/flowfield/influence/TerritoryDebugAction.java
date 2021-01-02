package org.triplea.ai.flowfield.influence;

import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.menubar.debug.AiPlayerDebugAction;
import java.awt.Color;
import java.util.Comparator;
import java.util.LongSummaryStatistics;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TerritoryDebugAction implements Consumer<AiPlayerDebugAction> {

  private final InfluenceMap influenceMap;
  private final GameMap gameMap;
  private final Function<Territory, String> territoryDetailMethod = this::buildTerritoryDetail;

  @Override
  public void accept(final AiPlayerDebugAction aiPlayerDebugAction) {
    aiPlayerDebugAction.renderInTerritoryDetails(territoryDetailMethod);

    final LongSummaryStatistics summary =
        influenceMap.getTerritories().values().stream()
            .sorted(Comparator.comparingLong(InfluenceTerritory::getInfluence))
            .mapToLong(InfluenceTerritory::getInfluence)
            .summaryStatistics();

    aiPlayerDebugAction.renderOnMap(
        debugMapRenderer -> {
          gameMap
              .getTerritories()
              .forEach(
                  territory -> {
                    Optional.ofNullable(influenceMap.getTerritories().get(territory))
                        .ifPresent(
                            field -> {
                              if (field.getInfluence() > 0) {
                                final Color color =
                                    interpolate(
                                        ((float) (field.getInfluence() - summary.getMin()))
                                            / (summary.getMax() - summary.getMin()),
                                        Color.BLUE,
                                        Color.RED);
                                debugMapRenderer.colorOnTerritory(territory, color, 100);
                              }
                            });
                  });
        });
  }

  private Color interpolate(final float fraction, final Color start, final Color end) {
    return new Color(
        (int) (start.getRed() * fraction) + (int) (end.getRed() * (1 - fraction)),
        (int) (start.getGreen() * fraction) + (int) (end.getGreen() * (1 - fraction)),
        (int) (start.getBlue() * fraction) + (int) (end.getBlue() * (1 - fraction)));
  }

  private String buildTerritoryDetail(final Territory territory) {
    final InfluenceTerritory influenceTerritory =
        influenceMap.getTerritories().getOrDefault(territory, new InfluenceTerritory(territory));

    return influenceMap.getName() + " Value: " + influenceTerritory.getInfluence();
  }
}
