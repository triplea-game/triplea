package games.strategy.triplea.ui.mapdata;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import javax.imageio.ImageIO;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.ui.Util;
import games.strategy.util.PointFileReaderWriter;
import games.strategy.util.UrlStreams;

/**
 * contains data about the territories useful for drawing
 */
public class MapData implements Closeable {
  public static final String PROPERTY_UNITS_SCALE = "units.scale";
  public static final String PROPERTY_UNITS_WIDTH = "units.width";
  public static final String PROPERTY_UNITS_HEIGHT = "units.height";
  public static final String PROPERTY_SCREENSHOT_TITLE_ENABLED = "screenshot.title.enabled";
  public static final String PROPERTY_SCREENSHOT_TITLE_X = "screenshot.title.x";
  public static final String PROPERTY_SCREENSHOT_TITLE_Y = "screenshot.title.y";
  public static final String PROPERTY_SCREENSHOT_TITLE_COLOR = "screenshot.title.color";
  public static final String PROPERTY_SCREENSHOT_TITLE_FONT_SIZE = "screenshot.title.font.size";
  public static final String PROPERTY_SCREENSHOT_STATS_ENABLED = "screenshot.stats.enabled";
  public static final String PROPERTY_SCREENSHOT_STATS_X = "screenshot.stats.x";
  public static final String PROPERTY_SCREENSHOT_STATS_Y = "screenshot.stats.y";
  public static final String PROPERTY_SCREENSHOT_STATS_TEXT_COLOR = "screenshot.stats.text.color";
  public static final String PROPERTY_SCREENSHOT_STATS_BORDER_COLOR = "screenshot.stats.border.color";

  private static final String PROPERTY_COLOR_PREFIX = "color.";
  private static final String PROPERTY_UNITS_COUNTER_OFFSET_WIDTH = "units.counter.offset.width";
  private static final String PROPERTY_UNITS_COUNTER_OFFSET_HEIGHT = "units.counter.offset.height";
  private static final String PROPERTY_UNITS_STACK_SIZE = "units.stack.size";
  private static final String PROPERTY_MAP_WIDTH = "map.width";
  private static final String PROPERTY_MAP_HEIGHT = "map.height";
  private static final String PROPERTY_MAP_SCROLLWRAPX = "map.scrollWrapX";
  private static final String PROPERTY_MAP_SCROLLWRAPY = "map.scrollWrapY";
  private static final String PROPERTY_MAP_HASRELIEF = "map.hasRelief";
  private static final String PROPERTY_MAP_CURSOR_HOTSPOT_X = "map.cursor.hotspot.x";
  private static final String PROPERTY_MAP_CURSOR_HOTSPOT_Y = "map.cursor.hotspot.y";
  private static final String PROPERTY_MAP_SHOWCAPITOLMARKERS = "map.showCapitolMarkers";
  private static final String PROPERTY_MAP_USETERRITORYEFFECTMARKERS = "map.useTerritoryEffectMarkers";
  private static final String PROPERTY_MAP_SHOWTERRITORYNAMES = "map.showTerritoryNames";
  private static final String PROPERTY_MAP_SHOWRESOURCES = "map.showResources";
  private static final String PROPERTY_MAP_SHOWCOMMENTS = "map.showComments";
  private static final String PROPERTY_MAP_SHOWSEAZONENAMES = "map.showSeaZoneNames";
  private static final String PROPERTY_MAP_DRAWNAMESFROMTOPLEFT = "map.drawNamesFromTopLeft";
  private static final String PROPERTY_MAP_USENATION_CONVOYFLAGS = "map.useNation_convoyFlags";
  private static final String PROPERTY_DONT_DRAW_TERRITORY_NAMES = "dont_draw_territory_names";
  private static final String PROPERTY_MAP_MAPBLENDS = "map.mapBlends";
  private static final String PROPERTY_MAP_MAPBLENDMODE = "map.mapBlendMode";
  private static final String PROPERTY_MAP_MAPBLENDALPHA = "map.mapBlendAlpha";

  private static final String CENTERS_FILE = "centers.txt";
  private static final String POLYGON_FILE = "polygons.txt";
  private static final String PLACEMENT_FILE = "place.txt";
  private static final String TERRITORY_EFFECT_FILE = "territory_effects.txt";
  private static final String MAP_PROPERTIES = "map.properties";
  private static final String CAPITAL_MARKERS = "capitols.txt";
  private static final String CONVOY_MARKERS = "convoy.txt";
  private static final String COMMENT_MARKERS = "comments.txt";
  private static final String VC_MARKERS = "vc.txt";
  private static final String BLOCKADE_MARKERS = "blockade.txt";
  private static final String PU_PLACE_FILE = "pu_place.txt";
  private static final String TERRITORY_NAME_PLACE_FILE = "name_place.txt";
  private static final String KAMIKAZE_FILE = "kamikaze_place.txt";
  private static final String DECORATIONS_FILE = "decorations.txt";
  // default colour if none is defined.
  private final List<Color> m_defaultColours = Arrays.asList(Color.RED, Color.MAGENTA, Color.YELLOW, Color.ORANGE,
      Color.CYAN, Color.GREEN, Color.PINK, Color.GRAY);
  // maps PlayerName as String to Color
  private final Map<String, Color> m_playerColors = new HashMap<>();
  // maps String -> List of points
  private Map<String, List<Point>> m_place;
  // maps String -> Collection of Polygons
  private Map<String, List<Polygon>> m_polys;
  // maps String -> Point
  private Map<String, Point> m_centers;
  // maps String -> Point
  private Map<String, Point> m_vcPlace;
  // maps String -> Point
  private Map<String, Point> m_blockadePlace;
  // maps String -> Point
  private Map<String, Point> m_convoyPlace;
  // maps String -> Point
  private Map<String, Point> m_commentPlace;
  // maps String -> Point
  private Map<String, Point> m_PUPlace;
  // maps String -> Point
  private Map<String, Point> m_namePlace;
  // maps String -> Point
  private Map<String, Point> m_kamikazePlace;
  // maps String -> Point
  private Map<String, Point> m_capitolPlace;
  // maps String -> List of String
  private Map<String, List<String>> m_contains;
  private Properties m_mapProperties;
  private Map<String, List<Point>> m_territoryEffects;
  // we shouldnt draw the names to these territories
  private Set<String> m_undrawnTerritoriesNames;
  private Map<Image, List<Point>> m_decorations;
  private Map<String, Image> m_territoryNameImages;
  private final Map<String, Image> m_effectImages = new HashMap<>();
  private final ResourceLoader m_resourceLoader;

  public boolean scrollWrapX() {
    return Boolean.valueOf(m_mapProperties.getProperty(PROPERTY_MAP_SCROLLWRAPX, "true"));
  }

  public boolean scrollWrapY() {
    return Boolean.valueOf(m_mapProperties.getProperty(PROPERTY_MAP_SCROLLWRAPY, "false"));
  }

  public MapData(final String mapNameDir) {
    this(ResourceLoader.getMapResourceLoader(mapNameDir));
  }

  /**
   * Constructor TerritoryData(java.lang.String)
   * Sets the map directory for this instance of TerritoryData
   *
   * @param loader
   *        .lang.String
   *        mapNameDir the given map directory
   */
  public MapData(final ResourceLoader loader) {
    m_resourceLoader = loader;
    try {
      final String prefix = "";
      m_place = PointFileReaderWriter.readOneToMany(loader.getResourceAsStream(prefix + PLACEMENT_FILE));
      m_territoryEffects =
          PointFileReaderWriter.readOneToMany(loader.getResourceAsStream(prefix + TERRITORY_EFFECT_FILE));
      m_polys = PointFileReaderWriter.readOneToManyPolygons(loader.getResourceAsStream(prefix + POLYGON_FILE));
      m_centers = PointFileReaderWriter.readOneToOneCenters(loader.getResourceAsStream(prefix + CENTERS_FILE));
      m_vcPlace = PointFileReaderWriter.readOneToOne(loader.getResourceAsStream(prefix + VC_MARKERS));
      m_convoyPlace = PointFileReaderWriter.readOneToOne(loader.getResourceAsStream(prefix + CONVOY_MARKERS));
      m_commentPlace = PointFileReaderWriter.readOneToOne(loader.getResourceAsStream(prefix + COMMENT_MARKERS));
      m_blockadePlace = PointFileReaderWriter.readOneToOne(loader.getResourceAsStream(prefix + BLOCKADE_MARKERS));
      m_capitolPlace = PointFileReaderWriter.readOneToOne(loader.getResourceAsStream(prefix + CAPITAL_MARKERS));
      m_PUPlace = PointFileReaderWriter.readOneToOne(loader.getResourceAsStream(prefix + PU_PLACE_FILE));
      m_namePlace = PointFileReaderWriter.readOneToOne(loader.getResourceAsStream(prefix + TERRITORY_NAME_PLACE_FILE));
      m_kamikazePlace = PointFileReaderWriter.readOneToOne(loader.getResourceAsStream(prefix + KAMIKAZE_FILE));
      m_mapProperties = new Properties();
      m_decorations = loadDecorations();
      m_territoryNameImages = territoryNameImages();
      try {
        final URL url = loader.getResource(prefix + MAP_PROPERTIES);
        if (url == null) {
          throw new IllegalStateException("No map.properties file defined");
        }
        Optional<InputStream> inputStream = UrlStreams.openStream(url);
        if (inputStream.isPresent()) {
          m_mapProperties.load(inputStream.get());
        }
      } catch (final Exception e) {
        System.out.println("Error reading map.properties:" + e);
      }
      initializeContains();
    } catch (final IOException ex) {
      ClientLogger.logQuietly(ex);
    }
  }

  @Override
  public void close() {
    m_resourceLoader.close();
  }

  private Map<String, Image> territoryNameImages() {
    if (!m_resourceLoader.hasPath("territoryNames/")) {
      return new HashMap<>();
    }

    Map<String, Image> territoryNameImages = new HashMap<>();
    for (final String name : m_centers.keySet()) {
      Optional<Image> territoryNameImage = loadTerritoryNameImage(name);

      if (territoryNameImage.isPresent()) {
        territoryNameImages.put(name, territoryNameImage.get());
      }
    }
    return territoryNameImages;
  }

  private Optional<Image> loadTerritoryNameImage(String imageName) {
    Image img = null;
    try {
      // try first file names that have underscores instead of spaces
      final String normalizedName = imageName.replace(' ', '_');
      img = loadImage(constructTerritoryNameImagePath(normalizedName));
      if (img == null) {
        img = loadImage(constructTerritoryNameImagePath(imageName));
      }
    } catch (Exception e) {
      // TODO: this is checking for IllegalStateException - we should bubble up the Optional image load and just
      // check instead if the optional is empty.
      ClientLogger.logQuietly("Image loading failed: " + imageName, e);
    }
    return Optional.ofNullable(img);
  }


  private String constructTerritoryNameImagePath(String baseName) {
    return "territoryNames/" + baseName + ".png";
  }


  private Map<Image, List<Point>> loadDecorations() {
    final URL decorationsFileUrl = m_resourceLoader.getResource(DECORATIONS_FILE);
    if (decorationsFileUrl == null) {
      return Collections.emptyMap();
    }
    Map<Image, List<Point>> decorations = new HashMap<>();
    Optional<InputStream> inputStream = UrlStreams.openStream(decorationsFileUrl);
    if (inputStream.isPresent()) {
      final Map<String, List<Point>> points = PointFileReaderWriter.readOneToMany(inputStream.get());
      for (final String name : points.keySet()) {
        final Image img = loadImage("misc/" + name);
        decorations.put(img, points.get(name));
      }
    }
    return decorations;
  }

  public double getDefaultUnitScale() {
    if (m_mapProperties.getProperty(PROPERTY_UNITS_SCALE) == null) {
      return 1.0;
    }
    try {
      return Double.parseDouble(m_mapProperties.getProperty(PROPERTY_UNITS_SCALE));
    } catch (final NumberFormatException e) {
      ClientLogger.logQuietly(e);
      return 1.0;
    }
  }

  /**
   * Does not take account of any scaling.
   */
  public int getDefaultUnitWidth() {
    if (m_mapProperties.getProperty(PROPERTY_UNITS_WIDTH) == null) {
      return UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
    }
    try {
      return Integer.parseInt(m_mapProperties.getProperty(PROPERTY_UNITS_WIDTH));
    } catch (final NumberFormatException e) {
      ClientLogger.logQuietly(e);
      return UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
    }
  }

  /**
   * Does not take account of any scaling.
   */
  public int getDefaultUnitHeight() {
    if (m_mapProperties.getProperty(PROPERTY_UNITS_HEIGHT) == null) {
      return UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
    }
    try {
      return Integer.parseInt(m_mapProperties.getProperty(PROPERTY_UNITS_HEIGHT));
    } catch (final NumberFormatException e) {
      ClientLogger.logQuietly(e);
      return UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
    }
  }

  /**
   * Does not take account of any scaling.
   */
  public int getDefaultUnitCounterOffsetWidth() {
    // if it is not set, divide by 4 so that it is roughly centered
    if (m_mapProperties.getProperty(PROPERTY_UNITS_COUNTER_OFFSET_WIDTH) == null) {
      return getDefaultUnitWidth() / 4;
    }
    try {
      return Integer.parseInt(m_mapProperties.getProperty(PROPERTY_UNITS_COUNTER_OFFSET_WIDTH));
    } catch (final NumberFormatException e) {
      ClientLogger.logQuietly(e);
      return getDefaultUnitWidth() / 4;
    }
  }

  /**
   * Does not take account of any scaling.
   */
  public int getDefaultUnitCounterOffsetHeight() {
    // put at bottom of unit, if not set
    if (m_mapProperties.getProperty(PROPERTY_UNITS_COUNTER_OFFSET_HEIGHT) == null) {
      return getDefaultUnitHeight();
    }
    try {
      return Integer.parseInt(m_mapProperties.getProperty(PROPERTY_UNITS_COUNTER_OFFSET_HEIGHT));
    } catch (final NumberFormatException e) {
      ClientLogger.logQuietly(e);
      return getDefaultUnitHeight();
    }
  }

  public int getDefaultUnitsStackSize() {
    // zero = normal behavior
    final String stack = m_mapProperties.getProperty(PROPERTY_UNITS_STACK_SIZE, "0");
    return Math.max(0, Integer.parseInt(stack));
  }

  public boolean shouldDrawTerritoryName(final String territoryName) {
    if (m_undrawnTerritoriesNames == null) {
      final String property = m_mapProperties.getProperty(PROPERTY_DONT_DRAW_TERRITORY_NAMES, "");
      m_undrawnTerritoriesNames = new HashSet<>(Arrays.asList(property.split(",")));
    }
    return !m_undrawnTerritoriesNames.contains(territoryName);
  }

  public boolean getHasRelief() {
    return Boolean.valueOf(m_mapProperties.getProperty(PROPERTY_MAP_HASRELIEF, "true"));
  }

  public int getMapCursorHotspotX() {
    return Math.max(0,
        Math.min(256, Integer.parseInt(m_mapProperties.getProperty(PROPERTY_MAP_CURSOR_HOTSPOT_X, "0"))));
  }

  public int getMapCursorHotspotY() {
    return Math.max(0,
        Math.min(256, Integer.parseInt(m_mapProperties.getProperty(PROPERTY_MAP_CURSOR_HOTSPOT_Y, "0"))));
  }

  public boolean getHasMapBlends() {
    return Boolean.valueOf(m_mapProperties.getProperty(PROPERTY_MAP_MAPBLENDS, "false"));
  }

  public String getMapBlendMode() {
    return String.valueOf(m_mapProperties.getProperty(PROPERTY_MAP_MAPBLENDMODE, "normal"));
  }

  public float getMapBlendAlpha() {
    return Float.valueOf(m_mapProperties.getProperty(PROPERTY_MAP_MAPBLENDALPHA, "0.5f"));
  }

  public boolean drawCapitolMarkers() {
    return Boolean.valueOf(m_mapProperties.getProperty(PROPERTY_MAP_SHOWCAPITOLMARKERS, "true"));
  }

  public boolean drawTerritoryNames() {
    return Boolean.valueOf(m_mapProperties.getProperty(PROPERTY_MAP_SHOWTERRITORYNAMES, "true"));
  }

  public boolean drawResources() {
    return Boolean.valueOf(m_mapProperties.getProperty(PROPERTY_MAP_SHOWRESOURCES, "true"));
  }

  public boolean drawComments() {
    return Boolean.valueOf(m_mapProperties.getProperty(PROPERTY_MAP_SHOWCOMMENTS, "true"));
  }

  public boolean drawSeaZoneNames() {
    return Boolean.valueOf(m_mapProperties.getProperty(PROPERTY_MAP_SHOWSEAZONENAMES, "false"));
  }

  public boolean drawNamesFromTopLeft() {
    return Boolean.valueOf(m_mapProperties.getProperty(PROPERTY_MAP_DRAWNAMESFROMTOPLEFT, "false"));
  }

  public boolean useNation_convoyFlags() {
    return Boolean.valueOf(m_mapProperties.getProperty(PROPERTY_MAP_USENATION_CONVOYFLAGS, "false"));
  }

  public boolean useTerritoryEffectMarkers() {
    return Boolean.valueOf(m_mapProperties.getProperty(PROPERTY_MAP_USETERRITORYEFFECTMARKERS, "false"));
  }

  private void initializeContains() {
    m_contains = new HashMap<>();
    final Iterator<String> seaIter = getTerritories().iterator();
    while (seaIter.hasNext()) {
      final List<String> contained = new ArrayList<>();
      final String seaTerritory = seaIter.next();
      if (!Util.isTerritoryNameIndicatingWater(seaTerritory)) {
        continue;
      }
      final Iterator<String> landIter = getTerritories().iterator();
      while (landIter.hasNext()) {
        final String landTerritory = landIter.next();
        if (Util.isTerritoryNameIndicatingWater(landTerritory)) {
          continue;
        }
        final Polygon landPoly = getPolygons(landTerritory).iterator().next();
        final Polygon seaPoly = getPolygons(seaTerritory).iterator().next();
        if (seaPoly.contains(landPoly.getBounds())) {
          contained.add(landTerritory);
        }
      }
      if (!contained.isEmpty()) {
        m_contains.put(seaTerritory, contained);
      }
    }
  }

  public boolean getBooleanProperty(final String propertiesKey) {
    return Boolean.valueOf(m_mapProperties.getProperty(propertiesKey, "true"));
  }

  public Color getColorProperty(final String propertiesKey) throws IllegalStateException {
    String colorString;
    if (m_mapProperties.getProperty(propertiesKey) != null) {
      colorString = m_mapProperties.getProperty(propertiesKey);
      if (colorString.length() != 6) {
        throw new IllegalStateException("Colors must be a 6 digit hex number, eg FF0011, not:" + colorString);
      }
      try {
        final Integer colorInt = Integer.decode("0x" + colorString);
        final Color color = new Color(colorInt);
        return color;
      } catch (final NumberFormatException nfe) {
        throw new IllegalStateException("Player colors must be a 6 digit hex number, eg FF0011");
      }
    }
    return null;
  }

  public Color getPlayerColor(final String playerName) {
    // already loaded, just return
    if (m_playerColors.containsKey(playerName)) {
      return m_playerColors.get(playerName);
    }
    // look in map.properties
    final String propertiesKey = PROPERTY_COLOR_PREFIX + playerName;
    Color color = null;
    try {
      color = getColorProperty(propertiesKey);
    } catch (final Exception e) {
      throw new IllegalStateException("Player colors must be a 6 digit hex number, eg FF0011");
    }
    if (color == null) {
      System.out.println("No color defined for " + playerName + ".  Edit map.properties in the map folder to set it");
      color = m_defaultColours.remove(0);
    }
    // dont crash, use one of our default colors
    // its ugly, but usable
    m_playerColors.put(playerName, color);
    return color;
  }

  /**
   * returns the named property, or null
   */
  public String getProperty(final String propertiesKey) {
    return m_mapProperties.getProperty(propertiesKey);
  }

  /**
   * returns the color for impassible territories
   */
  public Color impassibleColor() {
    // just use getPlayerColor, since it parses the properties
    return getPlayerColor(Constants.PLAYER_NAME_IMPASSIBLE);
  }

  /**
   * @return a Set of territory names as Strings. generally this shouldnt be
   *         used, instead you should use aGameData.getMap().getTerritories()
   */
  public Set<String> getTerritories() {
    return m_polys.keySet();
  }

  /**
   * Does this territory have any territories contained within it
   */
  public boolean hasContainedTerritory(final String territoryName) {
    return m_contains.containsKey(territoryName);
  }

  /**
   * returns the name of the territory contained in the given territory. This
   * applies to islands within sea zones.
   *
   * @return possiblly null
   */
  public List<String> getContainedTerritory(final String territoryName) {
    return m_contains.get(territoryName);
  }

  public void verify(final GameData data) {
    verifyKeys(data, m_centers, "centers");
    verifyKeys(data, m_polys, "polygons");
    verifyKeys(data, m_place, "place");
  }

  private void verifyKeys(final GameData data, final Map<String, ?> aMap, final String dataTypeForErrorMessage)
      throws IllegalStateException {
    final StringBuilder errors = new StringBuilder();
    final Iterator<String> iter = aMap.keySet().iterator();
    while (iter.hasNext()) {
      final String name = iter.next();
      final Territory terr = data.getMap().getTerritory(name);
      // allow loading saved games with missing territories; just ignore them
      if (terr == null) {
        iter.remove();
      }
    }
    final Iterator<Territory> territories = data.getMap().getTerritories().iterator();
    final Set<String> keySet = aMap.keySet();
    while (territories.hasNext()) {
      final Territory terr = territories.next();
      if (!keySet.contains(terr.getName())) {
        errors.append("No data of type ").append(dataTypeForErrorMessage).append(" for territory:")
            .append(terr.getName()).append("\n");
      }
    }
    if (errors.length() > 0) {
      throw new IllegalStateException(errors.toString());
    }
  }

  public List<Point> getPlacementPoints(final Territory terr) {
    return m_place.get(terr.getName());
  }

  public List<Polygon> getPolygons(final String terr) {
    return m_polys.get(terr);
  }

  public List<Polygon> getPolygons(final Territory terr) {
    return getPolygons(terr.getName());
  }

  public Point getCenter(final String terr) {
    if (m_centers.get(terr) == null) {
      throw new IllegalStateException("Missing " + CENTERS_FILE + " data for " + terr);
    }
    return new Point(m_centers.get(terr));
  }

  public Point getCenter(final Territory terr) {
    return getCenter(terr.getName());
  }

  public Point getCapitolMarkerLocation(final Territory terr) {
    if (m_capitolPlace.containsKey(terr.getName())) {
      return m_capitolPlace.get(terr.getName());
    }
    return getCenter(terr);
  }

  public Point getConvoyMarkerLocation(final Territory terr) {
    if (m_convoyPlace.containsKey(terr.getName())) {
      return m_convoyPlace.get(terr.getName());
    }
    return getCenter(terr);
  }

  public Optional<Point> getCommentMarkerLocation(final Territory terr) {
    return Optional.ofNullable(m_commentPlace.get(terr.getName()));
  }

  public Point getKamikazeMarkerLocation(final Territory terr) {
    if (m_kamikazePlace.containsKey(terr.getName())) {
      return m_kamikazePlace.get(terr.getName());
    }
    return getCenter(terr);
  }

  public Point getVCPlacementPoint(final Territory terr) {
    if (m_vcPlace.containsKey(terr.getName())) {
      return m_vcPlace.get(terr.getName());
    }
    return getCenter(terr);
  }

  public Point getBlockadePlacementPoint(final Territory terr) {
    if (m_blockadePlace.containsKey(terr.getName())) {
      return m_blockadePlace.get(terr.getName());
    }
    return getCenter(terr);
  }

  public Optional<Point> getPUPlacementPoint(final Territory terr) {
    return Optional.ofNullable(m_PUPlace.get(terr.getName()));
  }

  public Optional<Point> getNamePlacementPoint(final Territory terr) {
    return Optional.ofNullable(m_namePlace.get(terr.getName()));
  }

  /**
   * Get the territory at the x,y co-ordinates could be null.
   */
  public String getTerritoryAt(final double x, final double y) {
    String seaName = null;
    // try to find a land territory.
    // sea zones often surround a land territory
    final Iterator<String> keyIter = m_polys.keySet().iterator();
    while (keyIter.hasNext()) {
      final String name = keyIter.next();
      final Collection<Polygon> polygons = m_polys.get(name);
      final Iterator<Polygon> polyIter = polygons.iterator();
      while (polyIter.hasNext()) {
        final Polygon poly = polyIter.next();
        if (poly.contains(x, y)) {
          if (Util.isTerritoryNameIndicatingWater(name)) {
            seaName = name;
          } else {
            return name;
          }
        }
      }
    }
    return seaName;
  }

  public Dimension getMapDimensions() {
    final String widthProperty = m_mapProperties.getProperty(PROPERTY_MAP_WIDTH);
    final String heightProperty = m_mapProperties.getProperty(PROPERTY_MAP_HEIGHT);
    if (widthProperty == null || heightProperty == null) {
      throw new IllegalStateException(
          "Missing " + PROPERTY_MAP_WIDTH + " or " + PROPERTY_MAP_HEIGHT + " in " + MAP_PROPERTIES);
    }
    final int width = Integer.parseInt(widthProperty.trim());
    final int height = Integer.parseInt(heightProperty.trim());
    return new Dimension(width, height);
  }

  public Rectangle getBoundingRect(final Territory terr) {
    final String name = terr.getName();
    return getBoundingRect(name);
  }

  public Rectangle getBoundingRect(final String name) {
    final List<Polygon> polys = m_polys.get(name);
    if (polys == null) {
      throw new IllegalStateException("No polygons found for:" + name + " All territories:" + m_polys.keySet());
    }
    final Iterator<Polygon> polyIter = polys.iterator();
    final Rectangle bounds = polyIter.next().getBounds();
    while (polyIter.hasNext()) {
      bounds.add(polyIter.next().getBounds());
    }
    // if we have a territory that straddles the map divide, ie: which has polygons on both the left and right sides of
    // the map,
    // then the polygon's width or height could be almost equal to the map width or height
    // this can cause lots of problems, like when we want to get the tiles for the territory we would end up getting all
    // the tiles for the
    // map (and a java heap space error)
    final Dimension mapDimensions = getMapDimensions();
    if ((scrollWrapX() && bounds.width > 1800 && bounds.width > mapDimensions.width * 0.9)
        || (scrollWrapY() && bounds.height > 1200 && bounds.height > mapDimensions.height * 0.9)) {
      return getBoundingRectWithTranslate(polys, mapDimensions);
    }
    return bounds;
  }

  private Rectangle getBoundingRectWithTranslate(final List<Polygon> polys, final Dimension mapDimensions) {
    Rectangle boundingRect = null;
    final int mapWidth = mapDimensions.width;
    final int mapHeight = mapDimensions.height;
    final int closeToMapWidth = (int) (mapWidth * 0.9);
    final int closeToMapHeight = (int) (mapHeight * 0.9);
    final boolean scrollWrapX = this.scrollWrapX();
    final boolean scrollWrapY = this.scrollWrapY();
    for (final Polygon item : polys) {
      // if our rectangle is on the right side (mapscrollx) then we push it to be on the negative left side, so that the
      // bounds.x will be
      // negative
      // this solves the issue of maps that have a territory where polygons were on both sides of the map divide
      // (so our bounds.x was 0, and our bounds.y would be the entire map width)
      // (when in fact it should actually be bounds.x = -10 or something, and bounds.width = 20 or something)
      // we use map dimensions.width * 0.9 because the polygon may not actually touch the side of the map (like if the
      // territory borders are
      // thick)
      final Rectangle itemRect = item.getBounds();
      if (scrollWrapX && itemRect.getMaxX() >= closeToMapWidth) {
        itemRect.translate(-mapWidth, 0);
      }
      if (scrollWrapY && itemRect.getMaxY() >= closeToMapHeight) {
        itemRect.translate(0, -mapHeight);
      }
      if (boundingRect == null) {
        boundingRect = itemRect;
      } else {
        boundingRect.add(itemRect);
      }
    }
    // if the polygon is completely negative, we can make translate it back to normal
    if (boundingRect.x < 0 && boundingRect.getMaxX() <= 0) {
      boundingRect.translate(mapWidth, 0);
    }
    if (boundingRect.y < 0 && boundingRect.getMaxY() <= 0) {
      boundingRect.translate(0, mapHeight);
    }
    return boundingRect;
  }

  public Image getVCImage() {
    return loadImage("misc/vc.png");
  }

  public Image getBlockadeImage() {
    return loadImage("misc/blockade.png");
  }

  public Image getErrorImage() {
    return loadImage("misc/error.gif");
  }

  public Image getWarningImage() {
    return loadImage("misc/warning.gif");
  }

  public Image getInfoImage() {
    return loadImage("misc/information.gif");
  }

  public Image getHelpImage() {
    return loadImage("misc/help.gif");
  }

  private BufferedImage loadImage(final String imageName) {
    final URL url = m_resourceLoader.getResource(imageName);
    if (url == null) {
      throw new IllegalStateException("Could not load " + imageName);
    }
    try {
      return ImageIO.read(url);
    } catch (final IOException e) {
      ClientLogger.logQuietly(e);
      throw new IllegalStateException(e.getMessage());
    }
  }

  public Map<String, Image> getTerritoryNameImages() {
    return Collections.unmodifiableMap(m_territoryNameImages);
  }

  public Map<Image, List<Point>> getDecorations() {
    return Collections.unmodifiableMap(m_decorations);
  }

  public List<Point> getTerritoryEffectPoints(final Territory territory) {
    if (m_territoryEffects.get(territory.getName()) == null) {
      return Arrays.asList(getCenter(territory));
    }
    return m_territoryEffects.get(territory.getName());
  }

  public Image getTerritoryEffectImage(final String m_effectName) {
    if (m_effectImages.get(m_effectName) != null) {
      return m_effectImages.get(m_effectName);
    }
    final Image effectImage = loadImage("territoryEffects/" + m_effectName + ".png");
    m_effectImages.put(m_effectName, effectImage);
    return effectImage;
  }
}
