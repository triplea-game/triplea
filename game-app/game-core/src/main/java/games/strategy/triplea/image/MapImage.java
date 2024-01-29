package games.strategy.triplea.image;

import games.strategy.triplea.Constants;
import games.strategy.triplea.ResourceLoader;
import games.strategy.ui.Util;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import lombok.Getter;

/**
 * Responsible for drawing countries on the map. Is not responsible for drawing things on top of the
 * map, such as units, routes etc.
 */
@Getter
public class MapImage {
  private static Font propertyMapFont = null;
  private static Color propertyTerritoryNameAndPuAndCommentColor = null;
  private static Color propertyUnitCountColor = null;
  private static Color propertyUnitCountOutline = null;
  private static Color propertyUnitFactoryDamageColor = null;
  private static Color propertyUnitFactoryDamageOutline = null;
  private static Color propertyUnitHitDamageColor = null;
  private static Color propertyUnitHitDamageOutline = null;

  private static final String PROPERTY_MAP_FONT_SIZE_STRING = "PROPERTY_MAP_FONT_SIZE";
  private static final String PROPERTY_TERRITORY_NAME_AND_PU_AND_COMMENT_COLOR_STRING =
      "PROPERTY_TERRITORY_NAME_AND_PU_AND_COMMENT_COLOR";
  private static final String PROPERTY_UNIT_COUNT_COLOR_STRING = "PROPERTY_UNIT_COUNT_COLOR";
  private static final String PROPERTY_UNIT_COUNT_OUTLINE_STRING = "PROPERTY_UNIT_COUNT_OUTLINE";
  private static final String PROPERTY_UNIT_FACTORY_DAMAGE_COLOR_STRING =
      "PROPERTY_UNIT_FACTORY_DAMAGE_COLOR";
  private static final String PROPERTY_UNIT_FACTORY_DAMAGE_OUTLINE_STRING =
      "PROPERTY_UNIT_FACTORY_DAMAGE_OUTLINE";
  private static final String PROPERTY_UNIT_HIT_DAMAGE_COLOR_STRING =
      "PROPERTY_UNIT_HIT_DAMAGE_COLOR";
  private static final String PROPERTY_UNIT_HIT_DAMAGE_OUTLINE_STRING =
      "PROPERTY_UNIT_HIT_DAMAGE_OUTLINE";
  private static final int MAP_FONT_SIZE_DEFAULT = 12;
  private static final Color TERRITORY_NAME_AND_PU_AND_COMMENT_COLOR_DEFAULT = Color.BLACK;
  private static final Color UNIT_COUNT_COLOR_DEFAULT = Color.WHITE;
  private static final Color UNIT_COUNT_OUTLINE_DEFAULT = Color.BLACK;
  private static final Color UNIT_FACTORY_DAMAGE_COLOR_DEFAULT = Color.BLACK;
  private static final Color UNIT_FACTORY_DAMAGE_OUTLINE_DEFAULT = Color.LIGHT_GRAY;
  private static final Color UNIT_HIT_DAMAGE_COLOR_DEFAULT = Color.BLACK;
  private static final Color UNIT_HIT_DAMAGE_OUTLINE_DEFAULT = Color.LIGHT_GRAY;

  private final BufferedImage smallMapImage;

  public static Font getPropertyMapFont() {
    if (propertyMapFont == null) {
      final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
      propertyMapFont =
          new Font(
              "Arial",
              Font.BOLD,
              pref.getInt(PROPERTY_MAP_FONT_SIZE_STRING, MAP_FONT_SIZE_DEFAULT));
    }
    return propertyMapFont;
  }

  public static Color getPropertyTerritoryNameAndPuAndCommentColor() {
    if (propertyTerritoryNameAndPuAndCommentColor == null) {
      final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
      propertyTerritoryNameAndPuAndCommentColor =
          new Color(
              pref.getInt(
                  PROPERTY_TERRITORY_NAME_AND_PU_AND_COMMENT_COLOR_STRING,
                  TERRITORY_NAME_AND_PU_AND_COMMENT_COLOR_DEFAULT.getRGB()));
    }
    return propertyTerritoryNameAndPuAndCommentColor;
  }

  public static Color getPropertyUnitCountColor() {
    if (propertyUnitCountColor == null) {
      final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
      propertyUnitCountColor =
          new Color(
              pref.getInt(PROPERTY_UNIT_COUNT_COLOR_STRING, UNIT_COUNT_COLOR_DEFAULT.getRGB()));
    }
    return propertyUnitCountColor;
  }

  public static Color getPropertyUnitCountOutline() {
    if (propertyUnitCountOutline == null) {
      final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
      propertyUnitCountOutline =
          new Color(
              pref.getInt(PROPERTY_UNIT_COUNT_OUTLINE_STRING, UNIT_COUNT_OUTLINE_DEFAULT.getRGB()));
    }
    return propertyUnitCountOutline;
  }

  public static Color getPropertyUnitFactoryDamageColor() {
    if (propertyUnitFactoryDamageColor == null) {
      final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
      propertyUnitFactoryDamageColor =
          new Color(
              pref.getInt(
                  PROPERTY_UNIT_FACTORY_DAMAGE_COLOR_STRING,
                  UNIT_FACTORY_DAMAGE_COLOR_DEFAULT.getRGB()));
    }
    return propertyUnitFactoryDamageColor;
  }

  public static Color getPropertyUnitFactoryDamageOutline() {
    if (propertyUnitFactoryDamageOutline == null) {
      final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
      propertyUnitFactoryDamageOutline =
          new Color(
              pref.getInt(
                  PROPERTY_UNIT_FACTORY_DAMAGE_OUTLINE_STRING,
                  UNIT_FACTORY_DAMAGE_OUTLINE_DEFAULT.getRGB()));
    }
    return propertyUnitFactoryDamageOutline;
  }

  public static Color getPropertyUnitHitDamageColor() {
    if (propertyUnitHitDamageColor == null) {
      final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
      propertyUnitHitDamageColor =
          new Color(
              pref.getInt(
                  PROPERTY_UNIT_HIT_DAMAGE_COLOR_STRING, UNIT_HIT_DAMAGE_COLOR_DEFAULT.getRGB()));
    }
    return propertyUnitHitDamageColor;
  }

  public static Color getPropertyUnitHitDamageOutline() {
    if (propertyUnitHitDamageOutline == null) {
      final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
      propertyUnitHitDamageOutline =
          new Color(
              pref.getInt(
                  PROPERTY_UNIT_HIT_DAMAGE_OUTLINE_STRING,
                  UNIT_HIT_DAMAGE_OUTLINE_DEFAULT.getRGB()));
    }
    return propertyUnitHitDamageOutline;
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

  public static void setPropertyUnitCountOutline(final Color color) {
    final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
    pref.putInt(PROPERTY_UNIT_COUNT_OUTLINE_STRING, color.getRGB());
    propertyUnitCountOutline = color;
  }

  public static void setPropertyUnitFactoryDamageColor(final Color color) {
    final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
    pref.putInt(PROPERTY_UNIT_FACTORY_DAMAGE_COLOR_STRING, color.getRGB());
    propertyUnitFactoryDamageColor = color;
  }

  public static void setPropertyUnitFactoryDamageOutline(final Color color) {
    final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
    pref.putInt(PROPERTY_UNIT_FACTORY_DAMAGE_OUTLINE_STRING, color.getRGB());
    propertyUnitFactoryDamageOutline = color;
  }

  public static void setPropertyUnitHitDamageColor(final Color color) {
    final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
    pref.putInt(PROPERTY_UNIT_HIT_DAMAGE_COLOR_STRING, color.getRGB());
    propertyUnitHitDamageColor = color;
  }

  public static void setPropertyUnitHitDamageOutline(final Color color) {
    final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
    pref.putInt(PROPERTY_UNIT_HIT_DAMAGE_OUTLINE_STRING, color.getRGB());
    propertyUnitHitDamageOutline = color;
  }

  public static void resetPropertyMapFont() {
    removeProperty(PROPERTY_MAP_FONT_SIZE_STRING);
    propertyMapFont = new Font("Arial", Font.BOLD, MAP_FONT_SIZE_DEFAULT);
  }

  public static void resetPropertyTerritoryNameAndPuAndCommentColor() {
    removeProperty(PROPERTY_TERRITORY_NAME_AND_PU_AND_COMMENT_COLOR_STRING);
    propertyTerritoryNameAndPuAndCommentColor = TERRITORY_NAME_AND_PU_AND_COMMENT_COLOR_DEFAULT;
  }

  public static void resetPropertyUnitCountColor() {
    removeProperty(PROPERTY_UNIT_COUNT_COLOR_STRING);
    propertyUnitCountColor = UNIT_COUNT_COLOR_DEFAULT;
  }

  public static void resetPropertyUnitCountOutline() {
    removeProperty(PROPERTY_UNIT_COUNT_OUTLINE_STRING);
    propertyUnitCountOutline = UNIT_COUNT_OUTLINE_DEFAULT;
  }

  public static void resetPropertyUnitFactoryDamageColor() {
    removeProperty(PROPERTY_UNIT_FACTORY_DAMAGE_COLOR_STRING);
    propertyUnitFactoryDamageColor = UNIT_FACTORY_DAMAGE_COLOR_DEFAULT;
  }

  public static void resetPropertyUnitFactoryDamageOutline() {
    removeProperty(PROPERTY_UNIT_FACTORY_DAMAGE_OUTLINE_STRING);
    propertyUnitFactoryDamageOutline = UNIT_FACTORY_DAMAGE_OUTLINE_DEFAULT;
  }

  public static void resetPropertyUnitHitDamageColor() {
    removeProperty(PROPERTY_UNIT_HIT_DAMAGE_COLOR_STRING);
    propertyUnitHitDamageColor = UNIT_HIT_DAMAGE_COLOR_DEFAULT;
  }

  public static void resetPropertyUnitHitDamageOutline() {
    removeProperty(PROPERTY_UNIT_HIT_DAMAGE_OUTLINE_STRING);
    propertyUnitHitDamageOutline = UNIT_HIT_DAMAGE_OUTLINE_DEFAULT;
  }

  private static void removeProperty(final String property) {
    final Preferences pref = Preferences.userNodeForPackage(MapImage.class);
    pref.remove(property);
  }

  public MapImage(final ResourceLoader loader) {
    final Image smallFromFile =
        loadImage(loader, Constants.SMALL_MAP_FILENAME, Constants.SMALL_MAP_EXTENSIONS);
    smallMapImage =
        Util.newImage(smallFromFile.getWidth(null), smallFromFile.getHeight(null), false);
    final Graphics g = smallMapImage.getGraphics();
    g.drawImage(smallFromFile, 0, 0, null);
    g.dispose();
    smallFromFile.flush();
  }

  private static Image loadImage(
      final ResourceLoader loader, final String name, final String[] extensions) {
    URL mapFileUrl = null;
    for (final String extension : extensions) {
      mapFileUrl = loader.getResource(name + "." + extension);
      if (mapFileUrl != null) {
        break;
      }
    }
    if (mapFileUrl == null) {
      throw new IllegalStateException(
          "File not found: " + name + " with extensions: " + Arrays.toString(extensions));
    }
    try {
      return ImageIO.read(mapFileUrl);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
