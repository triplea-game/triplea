package games.strategy.triplea.ui.screen.drawable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.image.TileImageFactory;
import games.strategy.triplea.ui.IUIContext;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.triplea.util.Stopwatch;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface IDrawable {
  public Logger s_logger = Logger.getLogger(IDrawable.class.getName());
  int BASE_MAP_LEVEL = 1;
  int POLYGONS_LEVEL = 2;
  int RELIEF_LEVEL = 3;
  int OPTIONAL_EXTRA_TERRITORY_BORDERS_MEDIUM_LEVEL = 4;
  int OPTIONAL_EXTRA_TERRITORY_BORDERS_HIGH_LEVEL = 18;
  int CONVOY_LEVEL = 5;
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
  public static enum OptionalExtraBorderLevel {
    LOW, MEDIUM, HIGH
  }

  /**
   * Draw the tile
   * If the graphics are scaled, then unscaled and scaled will be non null.
   * <p>
   * The affine transform will be set to the scaled version.
   */
  public void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData, AffineTransform unscaled,
      AffineTransform scaled);

  public int getLevel();
}


