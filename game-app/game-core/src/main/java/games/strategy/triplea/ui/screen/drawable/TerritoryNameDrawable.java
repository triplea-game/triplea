package games.strategy.triplea.ui.screen.drawable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.ui.mapdata.MapData;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

/** Draws the name, comments, and production value for the associated territory. */
public class TerritoryNameDrawable extends AbstractDrawable {
  private final String territoryName;
  private final UiContext uiContext;
  private Rectangle territoryBounds;

  public TerritoryNameDrawable(final String territoryName, final UiContext uiContext) {
    this.territoryName = territoryName;
    this.uiContext = uiContext;
  }

  @Override
  public void draw(
      final Rectangle bounds,
      final GameData data,
      final Graphics2D graphics,
      final MapData mapData) {
    final Territory territory = data.getMap().getTerritoryOrThrow(territoryName);
    final Optional<TerritoryAttachment> optionalTerritoryAttachment =
        TerritoryAttachment.get(territory);
    final boolean drawFromTopLeft = mapData.drawNamesFromTopLeft();
    final boolean showSeaNames = mapData.drawSeaZoneNames();
    final boolean showComments = mapData.drawComments();
    boolean drawComments = false;
    String commentText = null;
    if (territory.isWater()) {
      // this is for special comments, like convoy zones, etc.
      if (optionalTerritoryAttachment.isPresent() && showComments) {
        final TerritoryAttachment ta = optionalTerritoryAttachment.get();
        Optional<GamePlayer> optionalOriginalOwner = ta.getOriginalOwner();
        if (ta.getConvoyRoute() && ta.getProduction() > 0 && optionalOriginalOwner.isPresent()) {
          drawComments = true;
          if (ta.getConvoyAttached().isEmpty()) {
            commentText =
                MyFormatter.defaultNamedToTextList(
                        TerritoryAttachment.getWhatTerritoriesThisIsUsedInConvoysFor(
                            territory, data))
                    + " "
                    + optionalOriginalOwner.get().getName()
                    + " Blockade Route";
          } else {
            commentText =
                MyFormatter.defaultNamedToTextList(ta.getConvoyAttached())
                    + " "
                    + optionalOriginalOwner.get().getName()
                    + " Convoy Route";
          }
        } else if (ta.getConvoyRoute()) {
          drawComments = true;
          if (ta.getConvoyAttached().isEmpty()) {
            commentText =
                MyFormatter.defaultNamedToTextList(
                        TerritoryAttachment.getWhatTerritoriesThisIsUsedInConvoysFor(
                            territory, data))
                    + " Blockade Route";
          } else {
            commentText =
                MyFormatter.defaultNamedToTextList(ta.getConvoyAttached()) + " Convoy Route";
          }
        } else if (ta.getProduction() > 0 && optionalOriginalOwner.isPresent()) {
          drawComments = true;
          commentText = optionalOriginalOwner.get().getName() + " Convoy Center";
        }
      }
      if (!drawComments && !showSeaNames) {
        return;
      }
    }

    graphics.setFont(MapImage.getPropertyMapFont());
    graphics.setColor(MapImage.getPropertyTerritoryNameAndPuAndCommentColor());
    final FontMetrics fm = graphics.getFontMetrics();

    // if we specify a placement point, use it otherwise try to center it
    final int x;
    final int y;
    final Optional<Point> namePlace = mapData.getNamePlacementPoint(territory);
    if (namePlace.isPresent()) {
      x = namePlace.get().x;
      y = namePlace.get().y;
    } else {
      if (territoryBounds == null) {
        // Cache the bounds since re-computing it is expensive.
        territoryBounds = getBestTerritoryNameRect(mapData, territory, fm);
      }
      x =
          territoryBounds.x
              + (int) territoryBounds.getWidth() / 2
              - fm.stringWidth(territory.getName()) / 2;
      y = territoryBounds.y + (int) territoryBounds.getHeight() / 2 + fm.getAscent() / 2;
    }

    // draw comments above names
    if (showComments && drawComments) {
      final Optional<Point> place = mapData.getCommentMarkerLocation(territory);
      if (place.isPresent()) {
        draw(bounds, graphics, place.get().x, place.get().y, null, commentText, drawFromTopLeft);
      } else {
        draw(bounds, graphics, x, y - fm.getHeight(), null, commentText, drawFromTopLeft);
      }
    }
    // draw territory names
    if (mapData.drawTerritoryNames()
        && mapData.shouldDrawTerritoryName(territoryName)
        && (!territory.isWater() || showSeaNames)) {
      final Image nameImage = mapData.getTerritoryNameImages().get(territory.getName());
      draw(bounds, graphics, x, y, nameImage, territory.getName(), drawFromTopLeft);
    }
    // draw the PUs.
    final int production =
        optionalTerritoryAttachment.map(TerritoryAttachment::getProduction).orElse(0);
    if (production > 0 && mapData.drawResources()) {
      final Image img = uiContext.getPuImageFactory().getPuImage(production).orElse(null);
      final String prod = Integer.toString(production);
      final Optional<Point> place = mapData.getPuPlacementPoint(territory);
      // if pu_place.txt is specified draw there
      if (place.isPresent()) {
        draw(bounds, graphics, place.get().x, place.get().y, img, prod, drawFromTopLeft);
      } else {
        // otherwise, draw under the territory name
        draw(
            bounds,
            graphics,
            x + (fm.stringWidth(territoryName) >> 1) - (fm.stringWidth(prod) >> 1),
            y + fm.getLeading() + fm.getAscent(),
            img,
            prod,
            drawFromTopLeft);
      }
    }
  }

  private static void draw(
      final Rectangle bounds,
      final Graphics2D graphics,
      final int x,
      final int y,
      final @Nullable Image img,
      final String prod,
      final boolean drawFromTopLeft) {
    int normalizedY = y;
    if (img == null) {
      if (graphics.getFont().getSize() <= 0) {
        return;
      }
      if (drawFromTopLeft) {
        final FontMetrics fm = graphics.getFontMetrics();
        normalizedY += fm.getHeight();
      }
      graphics.drawString(prod, x - bounds.x, normalizedY - bounds.y);
    } else {
      // we want to be consistent
      // drawString takes y as the base line position
      // drawImage takes x as the top right corner
      if (!drawFromTopLeft) {
        normalizedY -= img.getHeight(null);
      }
      graphics.drawImage(img, x - bounds.x, normalizedY - bounds.y, null);
    }
  }

  /**
   * Find the best rectangle inside the territory to place the name in. Finds the rectangle that can
   * fit the name, that is the closest to the vertical center, and has a large width at that
   * location. If there isn't any rectangles that can fit the name then default back to the bounding
   * rectangle.
   */
  private static Rectangle getBestTerritoryNameRect(
      final MapData mapData, final Territory territory, final FontMetrics fontMetrics) {

    // Find bounding rectangle and parameters for creating a grid (20 x 20) across the territory
    final Rectangle territoryBounds = mapData.getBoundingRect(territory);
    Rectangle result = territoryBounds;
    final int maxX = territoryBounds.x + territoryBounds.width;
    final int maxY = territoryBounds.y + territoryBounds.height;
    final int centerY = territoryBounds.y + territoryBounds.height / 2;
    final int incrementX = (int) Math.ceil(territoryBounds.width / 20.0);
    final int incrementY = (int) Math.ceil(territoryBounds.height / 20.0);
    final int nameWidth = fontMetrics.stringWidth(territory.getName());
    final int nameHeight = fontMetrics.getAscent();
    int maxScore = 0;

    // Loop through the grid moving the starting point and determining max width at that point
    for (int x = territoryBounds.x; x < maxX - nameWidth; x += incrementX) {
      for (int y = territoryBounds.y; y < maxY - nameHeight; y += incrementY) {
        for (int endX = maxX; endX > x; endX -= incrementX) {
          final Rectangle rectangle = new Rectangle(x, y, endX - x, nameHeight);

          // Ranges from 0 when at very top or bottom of territory to height/2 when at vertical
          // center
          final int verticalDistanceFromEdge =
              territoryBounds.height / 2 - Math.abs(centerY - nameHeight - y);

          // Score rectangle based on how close to vertical center and territory width at location
          final int score = verticalDistanceFromEdge * rectangle.width;

          // Check to make sure rectangle is contained in the territory
          if (rectangle.width > nameWidth
              && score > maxScore
              && isRectangleContainedInTerritory(rectangle, territory, mapData)) {
            maxScore = score;
            result = rectangle;
            break;
          }
        }
      }
    }
    return result;
  }

  private static boolean isRectangleContainedInTerritory(
      final Rectangle rectangle, final Territory territory, final MapData mapData) {
    final List<Polygon> polygons = mapData.getPolygons(territory.getName());
    for (final Polygon polygon : polygons) {
      if (polygon.contains(rectangle)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public DrawLevel getLevel() {
    return DrawLevel.TERRITORY_TEXT_LEVEL;
  }
}
