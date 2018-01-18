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
      throw new IllegalStateException(e);
    }
  }

  private BufferedImage smallMapImage;
  private static Font propertyMapFont = null;
  private static Color propertyTerritoryNameAndPuAndCommentColor = null;
  private static Color propertyUnitCountColor = null;
  private static Color propertyUnitFactoryDamageColor = null;
  private static Color propertyUnitHitDamageColor = null;
  private static final String PROPERTY_MAP_FONT_SIZE_STRING = "PROPERTY_MAP_FONT_SIZE";
  private static final String PROPERTY_TERRITORY_NAME_AND_PU_AND_COMMENT_COLOR_STRING =
      "PROPERTY_TERRITORY_NAME_AND_PU_AND_COMMENT_COLOR";
  private static final String PROPERTY_UNIT_COUNT_COLOR_STRING = "PROPERTY_UNIT_COUNT_COLOR";
  private static final String PROPERTY_UNIT_FACTORY_DAMAGE_COLOR_STRING = "PROPERTY_UNIT_FACTORY_DAMAGE_COLOR";
  private static final String PROPERTY_UNIT_HIT_DAMAGE_COLOR_STRING = "PROPERTY_UNIT_HIT_DAMAGE_COLOR";

  public static Font getPropertyMapFont() {
    if (propertyMapFont == null) {
      final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
      propertyMapFont = new Font("Ariel", Font.BOLD, pref.getInt(PROPERTY_MAP_FONT_SIZE_STRING, 12));
    }
    return propertyMapFont;
  }

  public static Color getPropertyTerritoryNameAndPuAndCommentColor() {
    if (propertyTerritoryNameAndPuAndCommentColor == null) {
      final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
      propertyTerritoryNameAndPuAndCommentColor =
          new Color(pref.getInt(PROPERTY_TERRITORY_NAME_AND_PU_AND_COMMENT_COLOR_STRING, Color.black.getRGB()));
    }
    return propertyTerritoryNameAndPuAndCommentColor;
  }

  public static Color getPropertyUnitCountColor() {
    if (propertyUnitCountColor == null) {
      final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
      propertyUnitCountColor = new Color(pref.getInt(PROPERTY_UNIT_COUNT_COLOR_STRING, Color.white.getRGB()));
    }
    return propertyUnitCountColor;
  }

  public static Color getPropertyUnitFactoryDamageColor() {
    if (propertyUnitFactoryDamageColor == null) {
      final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
      propertyUnitFactoryDamageColor =
          new Color(pref.getInt(PROPERTY_UNIT_FACTORY_DAMAGE_COLOR_STRING, Color.black.getRGB()));
    }
    return propertyUnitFactoryDamageColor;
  }

  public static Color getPropertyUnitHitDamageColor() {
    if (propertyUnitHitDamageColor == null) {
      final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
      propertyUnitHitDamageColor =
          new Color(pref.getInt(PROPERTY_UNIT_HIT_DAMAGE_COLOR_STRING, Color.black.getRGB()));
    }
    return propertyUnitHitDamageColor;
  }

  public static void setPropertyMapFont(final Font font) {
    final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
    pref.putInt(PROPERTY_MAP_FONT_SIZE_STRING, font.getSize());
    propertyMapFont = font;
  }

  public static void setPropertyTerritoryNameAndPuAndCommentColor(final Color color) {
    final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
    pref.putInt(PROPERTY_TERRITORY_NAME_AND_PU_AND_COMMENT_COLOR_STRING, color.getRGB());
    propertyTerritoryNameAndPuAndCommentColor = color;
  }

  public static void setPropertyUnitCountColor(final Color color) {
    final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
    pref.putInt(PROPERTY_UNIT_COUNT_COLOR_STRING, color.getRGB());
    propertyUnitCountColor = color;
  }

  public static void setPropertyUnitFactoryDamageColor(final Color color) {
    final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
    pref.putInt(PROPERTY_UNIT_FACTORY_DAMAGE_COLOR_STRING, color.getRGB());
    propertyUnitFactoryDamageColor = color;
  }

  public static void setPropertyUnitHitDamageColor(final Color color) {
    final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
    pref.putInt(PROPERTY_UNIT_HIT_DAMAGE_COLOR_STRING, color.getRGB());
    propertyUnitHitDamageColor = color;
  }

  public static void resetPropertyMapFont() {
    final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
    pref.remove(PROPERTY_MAP_FONT_SIZE_STRING);
    propertyMapFont = new Font("Ariel", Font.BOLD, 12);
  }

  public static void resetPropertyTerritoryNameAndPuAndCommentColor() {
    final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
    pref.remove(PROPERTY_TERRITORY_NAME_AND_PU_AND_COMMENT_COLOR_STRING);
    propertyTerritoryNameAndPuAndCommentColor = Color.black;
  }

  public static void resetPropertyUnitCountColor() {
    final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
    pref.remove(PROPERTY_UNIT_COUNT_COLOR_STRING);
    propertyUnitCountColor = Color.white;
  }

  public static void resetPropertyUnitFactoryDamageColor() {
    final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
    pref.remove(PROPERTY_UNIT_FACTORY_DAMAGE_COLOR_STRING);
    propertyUnitFactoryDamageColor = Color.black;
  }

  public static void resetPropertyUnitHitDamageColor() {
    final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
    pref.remove(PROPERTY_UNIT_HIT_DAMAGE_COLOR_STRING);
    propertyUnitHitDamageColor = Color.black;
  }

  /** Creates a new instance of MapImage. */
  public MapImage() {}

  public BufferedImage getSmallMapImage() {
    return smallMapImage;
  }

  public void loadMaps(final ResourceLoader loader) {
    final Image smallFromFile = loadImage(loader, Constants.SMALL_MAP_FILENAME);
    smallMapImage = Util.createImage(smallFromFile.getWidth(null), smallFromFile.getHeight(null), false);
    final Graphics g = smallMapImage.getGraphics();
    g.drawImage(smallFromFile, 0, 0, null);
    g.dispose();
    smallFromFile.flush();
  }
}
