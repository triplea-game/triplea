package games.strategy.triplea.ui.screen.drawable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.attachments.SupplyTerritoryAttachment;
import games.strategy.triplea.delegate.battle.AirControlTracker;
import games.strategy.triplea.delegate.supply.SupplyNetworkResolver;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.triplea.ui.status.OperationalStatusFormatter;
import games.strategy.triplea.ui.visibility.LocalPlayerVisibility;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

/** Draws compact supply and air-control status badges on a visible territory. */
public final class OperationalStatusDrawable extends AbstractDrawable {
  private static final Color SUPPLIED = new Color(40, 135, 70, 225);
  private static final Color SUPPLY_SOURCE = new Color(45, 95, 175, 225);
  private static final Color PENDING_ISOLATION = new Color(210, 135, 25, 230);
  private static final Color ISOLATED = new Color(185, 45, 45, 235);
  private static final Color CONTESTED = new Color(95, 95, 105, 230);

  private final String territoryName;
  private final UiContext uiContext;

  public OperationalStatusDrawable(final String territoryName, final UiContext uiContext) {
    this.territoryName = territoryName;
    this.uiContext = uiContext;
  }

  @Override
  public void draw(
      final Rectangle bounds,
      final GameData data,
      final Graphics2D graphics,
      final MapData mapData) {
    final Territory territory = data.getMap().getTerritoryOrNull(territoryName);
    if (territory == null) {
      return;
    }
    final Point center = mapData.getCenter(territory);
    final GamePlayer perspective =
        OperationalStatusFormatter.perspectivePlayer(
            territory, LocalPlayerVisibility.getViewers(uiContext, data), data);

    int offset = -18;
    if (SupplyNetworkResolver.isEnabled(data) && !territory.isWater() && perspective != null) {
      final int isolationTurns =
          OperationalStatusFormatter.maxIsolationTurns(territory, perspective, data);
      final boolean source =
          SupplyTerritoryAttachment.get(territory)
              .map(SupplyTerritoryAttachment::getSupplySource)
              .orElse(false);
      final boolean supplied = SupplyNetworkResolver.isSupplied(territory, perspective, data);
      final String label;
      final Color color;
      if (isolationTurns > 0) {
        label = "O" + isolationTurns;
        color = ISOLATED;
      } else if (source) {
        label = "D";
        color = SUPPLY_SOURCE;
      } else if (supplied) {
        label = "S";
        color = SUPPLIED;
      } else {
        label = "P";
        color = PENDING_ISOLATION;
      }
      drawBadge(graphics, bounds, center.x + offset, center.y - 24, label, color);
      offset += 20;
    }

    if (AirControlTracker.isEnabled(data)) {
      final AirControlTracker tracker = AirControlTracker.get(data);
      switch (tracker.getStatus(territory, data)) {
        case UNCONTROLLED -> {
          return;
        }
        case CONTESTED ->
            drawBadge(graphics, bounds, center.x + offset, center.y - 24, "AX", CONTESTED);
        case CONTROLLED -> {
          final Color controllerColor =
              tracker
                  .getController(territory, data)
                  .map(GamePlayer::getName)
                  .map(mapData::getPlayerColor)
                  .orElse(CONTESTED);
          drawBadge(graphics, bounds, center.x + offset, center.y - 24, "A", controllerColor);
        }
      }
    }
  }

  private static void drawBadge(
      final Graphics2D graphics,
      final Rectangle bounds,
      final int x,
      final int y,
      final String label,
      final Color fill) {
    final int width = label.length() > 1 ? 20 : 16;
    final int height = 16;
    final int drawX = x - bounds.x;
    final int drawY = y - bounds.y;
    final Color previousColor = graphics.getColor();
    final Font previousFont = graphics.getFont();
    graphics.setColor(new Color(0, 0, 0, 175));
    graphics.fillRoundRect(drawX - 1, drawY - 1, width + 2, height + 2, 7, 7);
    graphics.setColor(fill);
    graphics.fillRoundRect(drawX, drawY, width, height, 6, 6);
    graphics.setFont(previousFont.deriveFont(Font.BOLD, 10.0f));
    graphics.setColor(Color.WHITE);
    graphics.drawString(label, drawX + 4, drawY + 12);
    graphics.setFont(previousFont);
    graphics.setColor(previousColor);
  }

  @Override
  public DrawLevel getLevel() {
    return DrawLevel.OPERATIONAL_STATUS_LEVEL;
  }
}
