package games.strategy.triplea.ui.screen.drawable;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.Optional;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.ui.mapdata.MapData;

public interface IDrawable {
  int BASE_MAP_LEVEL = 1;
  int POLYGONS_LEVEL = 2;
  int RELIEF_LEVEL = 3;
  int OPTIONAL_EXTRA_TERRITORY_BORDERS_MEDIUM_LEVEL = 4;
  int OPTIONAL_EXTRA_TERRITORY_BORDERS_HIGH_LEVEL = 18;
  int TERRITORY_EFFECT_LEVEL = 6;
  int CAPITOL_MARKER_LEVEL = 8;
  int VC_MARKER_LEVEL = 9;
  int DECORATOR_LEVEL = 11;
  int TERRITORY_TEXT_LEVEL = 13;
  int BATTLE_HIGHLIGHT_LEVEL = 14;
  int UNITS_LEVEL = 15;
  int TERRITORY_OVERLAY_LEVEL = 16;

  /**
   * This is for the optional extra territory borders. LOW means off
   */
  enum OptionalExtraBorderLevel {
    LOW, MEDIUM, HIGH
  }

  /**
   * Draw the tile
   * If the graphics are scaled, then unscaled and scaled will be non null.
   * <p>
   * The affine transform will be set to the scaled version.
   */
  void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData, AffineTransform unscaled,
      AffineTransform scaled);

  int getLevel();

  default void drawImage(final Graphics2D graphics, final Optional<Image> image, final Point location,
      final Rectangle bounds) {
    if (image.isPresent()) {
      graphics.drawImage(image.get(), location.x - bounds.x, location.y - bounds.y, null);
    }
  }
}


