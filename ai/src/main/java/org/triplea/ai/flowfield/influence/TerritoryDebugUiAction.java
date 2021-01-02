package org.triplea.ai.flowfield.influence;

import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.panels.map.MapPanel;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.Comparator;
import java.util.LongSummaryStatistics;
import java.util.Optional;
import java.util.function.Function;
import javax.swing.AbstractAction;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TerritoryDebugUiAction extends AbstractAction {
  private static final long serialVersionUID = -919496373521710039L;

  private final TripleAFrame frame;
  private final InfluenceMap influenceMap;
  private final GameMap gameMap;
  private final Function<Territory, String> territoryDetailMethod = this::buildTerritoryDetail;

  public void unselect() {
    frame.getTerritoryDetails().removeAdditionalTerritoryDetailsFunction(territoryDetailMethod);
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    final MapPanel mapPanel = frame.getMapPanel();
    final LongSummaryStatistics summary =
        influenceMap.getTerritories().values().stream()
            .sorted(Comparator.comparingLong(InfluenceTerritory::getInfluence))
            .mapToLong(InfluenceTerritory::getInfluence)
            .summaryStatistics();
    frame.getTerritoryDetails().addAdditionalTerritoryDetailsFunction(territoryDetailMethod);

    gameMap
        .getTerritories()
        .forEach(
            territory -> {
              mapPanel.clearTerritoryOverlay(territory);
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
                          mapPanel.setTerritoryOverlayForTile(territory, color, 100);
                        }
                      });
            });

    mapPanel.repaint();
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
