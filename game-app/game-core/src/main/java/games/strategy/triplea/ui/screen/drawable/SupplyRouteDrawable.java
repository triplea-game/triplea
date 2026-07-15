package games.strategy.triplea.ui.screen.drawable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.supply.SupplyNetworkResolver;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.triplea.ui.status.OperationalStatusFormatter;
import games.strategy.triplea.ui.visibility.LocalPlayerVisibility;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.List;
import java.util.Set;

/** Draws a map-authored supply-road edge without disclosing hidden connection status. */
public final class SupplyRouteDrawable extends AbstractDrawable {
  private static final Color PUBLIC_ROAD = new Color(130, 130, 130, 190);
  private static final Color ACTIVE_ROAD = new Color(40, 140, 70, 210);
  private static final Color CUT_ROAD = new Color(190, 65, 45, 210);
  private static final Stroke SOLID_ROAD =
      new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
  private static final Stroke CUT_ROAD_STROKE =
      new BasicStroke(
          3.0f,
          BasicStroke.CAP_ROUND,
          BasicStroke.JOIN_ROUND,
          10.0f,
          new float[] {8.0f, 6.0f},
          0.0f);

  private final String fromTerritory;
  private final String toTerritory;
  private final UiContext uiContext;

  public SupplyRouteDrawable(
      final String fromTerritory, final String toTerritory, final UiContext uiContext) {
    this.fromTerritory = fromTerritory;
    this.toTerritory = toTerritory;
    this.uiContext = uiContext;
  }

  public Rectangle getDrawingBounds(final MapData mapData) {
    final Point from = mapData.getCenter(fromTerritory);
    final Point to = mapData.getCenter(toTerritory);
    final int padding = 6;
    return new Rectangle(
        Math.min(from.x, to.x) - padding,
        Math.min(from.y, to.y) - padding,
        Math.abs(from.x - to.x) + padding * 2,
        Math.abs(from.y - to.y) + padding * 2);
  }

  @Override
  public void draw(
      final Rectangle bounds,
      final GameData data,
      final Graphics2D graphics,
      final MapData mapData) {
    final Territory from = data.getMap().getTerritoryOrNull(fromTerritory);
    final Territory to = data.getMap().getTerritoryOrNull(toTerritory);
    if (from == null || to == null) {
      return;
    }

    Color color = PUBLIC_ROAD;
    Stroke stroke = SOLID_ROAD;
    final List<GamePlayer> viewers = LocalPlayerVisibility.getViewers(uiContext, data);
    final boolean masking = LocalPlayerVisibility.isMaskingEnabled(uiContext, data);
    final Set<Territory> visible =
        masking ? LocalPlayerVisibility.getVisibleTerritories(uiContext, data) : Set.of(from, to);
    if (!masking || (visible.contains(from) && visible.contains(to))) {
      final GamePlayer perspective =
          OperationalStatusFormatter.perspectivePlayer(from, viewers, data);
      if (perspective != null) {
        final boolean active =
            SupplyNetworkResolver.isSupplied(from, perspective, data)
                && SupplyNetworkResolver.isSupplied(to, perspective, data);
        color = active ? ACTIVE_ROAD : CUT_ROAD;
        stroke = active ? SOLID_ROAD : CUT_ROAD_STROKE;
      }
    }

    final Point fromPoint = mapData.getCenter(from);
    final Point toPoint = mapData.getCenter(to);
    final Color previousColor = graphics.getColor();
    final Stroke previousStroke = graphics.getStroke();
    graphics.setColor(color);
    graphics.setStroke(stroke);
    graphics.drawLine(
        fromPoint.x - bounds.x, fromPoint.y - bounds.y, toPoint.x - bounds.x, toPoint.y - bounds.y);
    graphics.setStroke(previousStroke);
    graphics.setColor(previousColor);
  }

  @Override
  public DrawLevel getLevel() {
    return DrawLevel.SUPPLY_ROUTE_LEVEL;
  }
}
