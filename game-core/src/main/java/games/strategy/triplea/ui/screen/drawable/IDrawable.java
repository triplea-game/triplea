package games.strategy.triplea.ui.screen.drawable;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Optional;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.ui.mapdata.MapData;

/**
 * A service responsible for drawing a single layer of the map. The map is rendered as a sequence of layers (or
 * levels). The lowest layer is drawn first with each successive layer drawn on top of it.
 */
public interface IDrawable {
  enum DrawLevel {
    BASE_MAP_LEVEL,
    POLYGONS_LEVEL,
    RELIEF_LEVEL,
    OPTIONAL_EXTRA_TERRITORY_BORDERS_MEDIUM_LEVEL,
    TERRITORY_EFFECT_LEVEL,
    CAPITOL_MARKER_LEVEL,
    VC_MARKER_LEVEL,
    DECORATOR_LEVEL,
    TERRITORY_TEXT_LEVEL,
    BATTLE_HIGHLIGHT_LEVEL,
    UNITS_LEVEL,
    TERRITORY_OVERLAY_LEVEL,
    OPTIONAL_EXTRA_TERRITORY_BORDERS_HIGH_LEVEL,
  }

  /**
   * This is for the optional extra territory borders. LOW means off
   */
  enum OptionalExtraBorderLevel {
    LOW, MEDIUM, HIGH
  }

  /**
   * Draw the tile. If the graphics are scaled, then unscaled and scaled will be non null.
   *
   * <p>
   * The affine transform will be set to the scaled version.
   * </p>
   */
  void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData);

  DrawLevel getLevel();

  default void drawImage(final Graphics2D graphics, final Optional<Image> image, final Point location,
      final Rectangle bounds) {
    image.ifPresent(image1 -> graphics.drawImage(image1, location.x - bounds.x, location.y - bounds.y, null));
  }
}
