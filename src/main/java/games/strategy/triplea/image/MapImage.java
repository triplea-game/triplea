package games.strategy.triplea.image;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;

import games.strategy.debug.ClientLogger;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ResourceLoader;
import games.strategy.ui.Util;

/**
 * Responsible for drawing countries on the map.
 * Is not responsible for drawing things on top of the map, such as units, routes etc.
 */
public class MapImage {
  private static Image loadImage(final ResourceLoader loader, final String name) {
    final URL mapFileUrl = loader.getResource(name);
    if (mapFileUrl == null) {
      throw new IllegalStateException("resource not found:" + name);
    }
    try {
      return ImageIO.read(mapFileUrl);
    } catch (final IOException e) {
      ClientLogger.logQuietly(e);
      throw new IllegalStateException(e.getMessage());
    }
  }

  private BufferedImage m_smallMapImage;
  private static Font PROPERTY_MAP_FONT = null;
  private static Color PROPERTY_TERRITORY_NAME_AND_PU_AND_COMMENT_COLOR = null;
  private static Color PROPERTY_UNIT_COUNT_COLOR = null;
  private static Color PROPERTY_UNIT_FACTORY_DAMAGE_COLOR = null;
  private static Color PROPERTY_UNIT_HIT_DAMAGE_COLOR = null;
  private static final String PROPERTY_MAP_FONT_SIZE_STRING = "PROPERTY_MAP_FONT_SIZE";
  private static final String PROPERTY_TERRITORY_NAME_AND_PU_AND_COMMENT_COLOR_STRING =
      "PROPERTY_TERRITORY_NAME_AND_PU_AND_COMMENT_COLOR";
  private static final String PROPERTY_UNIT_COUNT_COLOR_STRING = "PROPERTY_UNIT_COUNT_COLOR";
  private static final String PROPERTY_UNIT_FACTORY_DAMAGE_COLOR_STRING = "PROPERTY_UNIT_FACTORY_DAMAGE_COLOR";
  private static final String PROPERTY_UNIT_HIT_DAMAGE_COLOR_STRING = "PROPERTY_UNIT_HIT_DAMAGE_COLOR";

  public static Font getPropertyMapFont() {
    if (PROPERTY_MAP_FONT == null) {
      final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
      PROPERTY_MAP_FONT = new Font("Ariel", Font.BOLD, pref.getInt(PROPERTY_MAP_FONT_SIZE_STRING, 12));
    }
    return PROPERTY_MAP_FONT;
  }

  public static Color getPropertyTerritoryNameAndPUAndCommentcolor() {
    if (PROPERTY_TERRITORY_NAME_AND_PU_AND_COMMENT_COLOR == null) {
      final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
      PROPERTY_TERRITORY_NAME_AND_PU_AND_COMMENT_COLOR =
          new Color(pref.getInt(PROPERTY_TERRITORY_NAME_AND_PU_AND_COMMENT_COLOR_STRING, Color.black.getRGB()));
    }
    return PROPERTY_TERRITORY_NAME_AND_PU_AND_COMMENT_COLOR;
  }

  public static Color getPropertyUnitCountColor() {
    if (PROPERTY_UNIT_COUNT_COLOR == null) {
      final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
      PROPERTY_UNIT_COUNT_COLOR = new Color(pref.getInt(PROPERTY_UNIT_COUNT_COLOR_STRING, Color.white.getRGB()));
    }
    return PROPERTY_UNIT_COUNT_COLOR;
  }

  public static Color getPropertyUnitFactoryDamageColor() {
    if (PROPERTY_UNIT_FACTORY_DAMAGE_COLOR == null) {
      final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
      PROPERTY_UNIT_FACTORY_DAMAGE_COLOR =
          new Color(pref.getInt(PROPERTY_UNIT_FACTORY_DAMAGE_COLOR_STRING, Color.black.getRGB()));
    }
    return PROPERTY_UNIT_FACTORY_DAMAGE_COLOR;
  }

  public static Color getPropertyUnitHitDamageColor() {
    if (PROPERTY_UNIT_HIT_DAMAGE_COLOR == null) {
      final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
      PROPERTY_UNIT_HIT_DAMAGE_COLOR =
          new Color(pref.getInt(PROPERTY_UNIT_HIT_DAMAGE_COLOR_STRING, Color.black.getRGB()));
    }
    return PROPERTY_UNIT_HIT_DAMAGE_COLOR;
  }

  public static void setPropertyMapFont(final Font font) {
    final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
    pref.putInt(PROPERTY_MAP_FONT_SIZE_STRING, font.getSize());
    PROPERTY_MAP_FONT = font;
  }

  public static void setPropertyTerritoryNameAndPUAndCommentcolor(final Color color) {
    final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
    pref.putInt(PROPERTY_TERRITORY_NAME_AND_PU_AND_COMMENT_COLOR_STRING, color.getRGB());
    PROPERTY_TERRITORY_NAME_AND_PU_AND_COMMENT_COLOR = color;
  }

  public static void setPropertyUnitCountColor(final Color color) {
    final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
    pref.putInt(PROPERTY_UNIT_COUNT_COLOR_STRING, color.getRGB());
    PROPERTY_UNIT_COUNT_COLOR = color;
  }

  public static void setPropertyUnitFactoryDamageColor(final Color color) {
    final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
    pref.putInt(PROPERTY_UNIT_FACTORY_DAMAGE_COLOR_STRING, color.getRGB());
    PROPERTY_UNIT_FACTORY_DAMAGE_COLOR = color;
  }

  public static void setPropertyUnitHitDamageColor(final Color color) {
    final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
    pref.putInt(PROPERTY_UNIT_HIT_DAMAGE_COLOR_STRING, color.getRGB());
    PROPERTY_UNIT_HIT_DAMAGE_COLOR = color;
  }

  public static void resetPropertyMapFont() {
    final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
    pref.remove(PROPERTY_MAP_FONT_SIZE_STRING);
    PROPERTY_MAP_FONT = new Font("Ariel", Font.BOLD, 12);
  }

  public static void resetPropertyTerritoryNameAndPUAndCommentcolor() {
    final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
    pref.remove(PROPERTY_TERRITORY_NAME_AND_PU_AND_COMMENT_COLOR_STRING);
    PROPERTY_TERRITORY_NAME_AND_PU_AND_COMMENT_COLOR = Color.black;
  }

  public static void resetPropertyUnitCountColor() {
    final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
    pref.remove(PROPERTY_UNIT_COUNT_COLOR_STRING);
    PROPERTY_UNIT_COUNT_COLOR = Color.white;
  }

  public static void resetPropertyUnitFactoryDamageColor() {
    final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
    pref.remove(PROPERTY_UNIT_FACTORY_DAMAGE_COLOR_STRING);
    PROPERTY_UNIT_FACTORY_DAMAGE_COLOR = Color.black;
  }

  public static void resetPropertyUnitHitDamageColor() {
    final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
    pref.remove(PROPERTY_UNIT_HIT_DAMAGE_COLOR_STRING);
    PROPERTY_UNIT_HIT_DAMAGE_COLOR = Color.black;
  }

  /** Creates a new instance of MapImage. */
  public MapImage() {}

  public BufferedImage getSmallMapImage() {
    return m_smallMapImage;
  }

  public void loadMaps(final ResourceLoader loader) {
    final Image smallFromFile = loadImage(loader, Constants.SMALL_MAP_FILENAME);
    m_smallMapImage = Util.createImage(smallFromFile.getWidth(null), smallFromFile.getHeight(null), false);
    final Graphics g = m_smallMapImage.getGraphics();
    g.drawImage(smallFromFile, 0, 0, null);
    g.dispose();
    smallFromFile.flush();
  }
}
