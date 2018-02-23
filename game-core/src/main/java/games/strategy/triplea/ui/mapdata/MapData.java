package games.strategy.triplea.ui.mapdata;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.FileNotFoundException;
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
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.ui.Util;
import games.strategy.util.PointFileReaderWriter;

/**
 * contains data about the territories useful for drawing.
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
  public static final String PROPERTY_COLOR_PREFIX = "color.";
  public static final String PROPERTY_UNITS_COUNTER_OFFSET_WIDTH = "units.counter.offset.width";
  public static final String PROPERTY_UNITS_COUNTER_OFFSET_HEIGHT = "units.counter.offset.height";
  public static final String PROPERTY_UNITS_STACK_SIZE = "units.stack.size";
  public static final String PROPERTY_MAP_WIDTH = "map.width";
  public static final String PROPERTY_MAP_HEIGHT = "map.height";
  public static final String PROPERTY_MAP_SCROLLWRAPX = "map.scrollWrapX";
  public static final String PROPERTY_MAP_SCROLLWRAPY = "map.scrollWrapY";
  public static final String PROPERTY_MAP_HASRELIEF = "map.hasRelief";
  public static final String PROPERTY_MAP_CURSOR_HOTSPOT_X = "map.cursor.hotspot.x";
  public static final String PROPERTY_MAP_CURSOR_HOTSPOT_Y = "map.cursor.hotspot.y";
  public static final String PROPERTY_MAP_SHOWCAPITOLMARKERS = "map.showCapitolMarkers";
  public static final String PROPERTY_MAP_USETERRITORYEFFECTMARKERS = "map.useTerritoryEffectMarkers";
  public static final String PROPERTY_MAP_SHOWTERRITORYNAMES = "map.showTerritoryNames";
  public static final String PROPERTY_MAP_SHOWRESOURCES = "map.showResources";
  public static final String PROPERTY_MAP_SHOWCOMMENTS = "map.showComments";
  public static final String PROPERTY_MAP_SHOWSEAZONENAMES = "map.showSeaZoneNames";
  public static final String PROPERTY_MAP_DRAWNAMESFROMTOPLEFT = "map.drawNamesFromTopLeft";
  public static final String PROPERTY_MAP_USENATION_CONVOYFLAGS = "map.useNation_convoyFlags";
  public static final String PROPERTY_DONT_DRAW_TERRITORY_NAMES = "dont_draw_territory_names";
  public static final String PROPERTY_MAP_MAPBLENDS = "map.mapBlends";
  public static final String PROPERTY_MAP_MAPBLENDMODE = "map.mapBlendMode";
  public static final String PROPERTY_MAP_MAPBLENDALPHA = "map.mapBlendAlpha";

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

  private final DefaultColors defaultColors = new DefaultColors();
  private final Map<String, Color> playerColors = new HashMap<>();
  private Map<String, List<Point>> place;
  private Map<String, List<Polygon>> polys;
  private Map<String, Point> centers;
  private Map<String, Point> vcPlace;
  private Map<String, Point> blockadePlace;
  private Map<String, Point> convoyPlace;
  private Map<String, Point> commentPlace;
  private Map<String, Point> puPlace;
  private Map<String, Point> namePlace;
  private Map<String, Point> kamikazePlace;
  private Map<String, Point> capitolPlace;
  private Map<String, List<String>> contains;
  private Properties mapProperties;
  private Map<String, List<Point>> territoryEffects;
  private Set<String> undrawnTerritoriesNames;
  private Map<Image, List<Point>> decorations;
  private Map<String, Image> territoryNameImages;
  private final Map<String, Image> effectImages = new HashMap<>();
  private final ResourceLoader resourceLoader;

  public boolean scrollWrapX() {
    return Boolean.valueOf(mapProperties.getProperty(PROPERTY_MAP_SCROLLWRAPX, "true"));
  }

  public boolean scrollWrapY() {
    return Boolean.valueOf(mapProperties.getProperty(PROPERTY_MAP_SCROLLWRAPY, "false"));
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
    resourceLoader = loader;
    try {
      place = readPointsOneToMany(optionalResource(PLACEMENT_FILE));
      territoryEffects = readPointsOneToMany(optionalResource(TERRITORY_EFFECT_FILE));

      if (loader.getResource(POLYGON_FILE) == null) {
        throw new IllegalStateException(
            "Error in resource loading. Unable to load expected resource: " + POLYGON_FILE + ", the error"
                + " is that either we did not find the correct path to load. Check the resource loader to make"
                + " sure the map zip or dir was added. Failing that, the path in this error message should be available"
                + " relative to the map folder, or relative to the root of the map zip");
      }

      polys = readPolygonsOneToMany(requiredResource(POLYGON_FILE));
      centers = readPointsOneToOne(requiredResource(CENTERS_FILE));
      vcPlace = readPointsOneToOne(optionalResource(VC_MARKERS));
      convoyPlace = readPointsOneToOne(optionalResource(CONVOY_MARKERS));
      commentPlace = readPointsOneToOne(optionalResource(COMMENT_MARKERS));
      blockadePlace = readPointsOneToOne(optionalResource(BLOCKADE_MARKERS));
      capitolPlace = readPointsOneToOne(optionalResource(CAPITAL_MARKERS));
      puPlace = readPointsOneToOne(optionalResource(PU_PLACE_FILE));
      namePlace = readPointsOneToOne(optionalResource(TERRITORY_NAME_PLACE_FILE));
      kamikazePlace = readPointsOneToOne(optionalResource(KAMIKAZE_FILE));
      mapProperties = new Properties();
      decorations = loadDecorations();
      territoryNameImages = territoryNameImages();

      try (InputStream inputStream = requiredResource(MAP_PROPERTIES).newInputStream()) {
        mapProperties.load(inputStream);
      } catch (final Exception e) {
        ClientLogger.logQuietly("Error reading map.properties", e);
      }

      initializeContains();
    } catch (final IOException ex) {
      ClientLogger.logQuietly("Failed to initialize map data", ex);
    }
  }

  private InputStreamFactory optionalResource(final String path) {
    return () -> Optional.ofNullable(resourceLoader.getResourceAsStream(path))
        .orElseGet(() -> new ByteArrayInputStream(new byte[0]));
  }

  private InputStreamFactory requiredResource(final String path) {
    return () -> Optional.ofNullable(resourceLoader.getResourceAsStream(path))
        .orElseThrow(() -> new FileNotFoundException(path));
  }

  private static Map<String, Point> readPointsOneToOne(final InputStreamFactory inputStreamFactory) throws IOException {
    return runWithInputStream(inputStreamFactory, PointFileReaderWriter::readOneToOne);
  }

  private static Map<String, List<Point>> readPointsOneToMany(final InputStreamFactory inputStreamFactory)
      throws IOException {
    return runWithInputStream(inputStreamFactory, PointFileReaderWriter::readOneToMany);
  }

  private static Map<String, List<Polygon>> readPolygonsOneToMany(final InputStreamFactory inputStreamFactory)
      throws IOException {
    return runWithInputStream(inputStreamFactory, PointFileReaderWriter::readOneToManyPolygons);
  }

  private static <R> R runWithInputStream(
      final InputStreamFactory inputStreamFactory,
      final InputStreamReader<R> reader) throws IOException {
    try (InputStream is = inputStreamFactory.newInputStream()) {
      return reader.read(is);
    }
  }

  @FunctionalInterface
  private interface InputStreamFactory {
    InputStream newInputStream() throws IOException;
  }

  @FunctionalInterface
  private interface InputStreamReader<R> {
    R read(InputStream is) throws IOException;
  }

  @Override
  public void close() {
    resourceLoader.close();
  }

  private Map<String, Image> territoryNameImages() {
    if (!resourceLoader.hasPath("territoryNames/")) {
      return new HashMap<>();
    }

    final Map<String, Image> territoryNameImages = new HashMap<>();
    for (final String name : centers.keySet()) {
      final Optional<Image> territoryNameImage = loadTerritoryNameImage(name);

      if (territoryNameImage.isPresent()) {
        territoryNameImages.put(name, territoryNameImage.get());
      }
    }
    return territoryNameImages;
  }

  private Optional<Image> loadTerritoryNameImage(final String imageName) {
    try {
      // try first file names that have underscores instead of spaces
      final String normalizedName = imageName.replace(' ', '_');
      Optional<Image> img = loadImage(constructTerritoryNameImagePath(normalizedName));
      if (!img.isPresent()) {
        img = loadImage(constructTerritoryNameImagePath(imageName));
      }
      return img;
    } catch (final Exception e) {
      // TODO: this is checking for IllegalStateException - we should bubble up the Optional image load and just
      // check instead if the optional is empty.
      ClientLogger.logQuietly("Image loading failed: " + imageName, e);
      return Optional.empty();
    }
  }

  private static String constructTerritoryNameImagePath(final String baseName) {
    return "territoryNames/" + baseName + ".png";
  }

  private Map<Image, List<Point>> loadDecorations() throws IOException {
    final Map<Image, List<Point>> decorations = new HashMap<>();
    final Map<String, List<Point>> points = readPointsOneToMany(optionalResource(DECORATIONS_FILE));
    for (final String name : points.keySet()) {
      loadImage("misc/" + name).ifPresent(img -> decorations.put(img, points.get(name)));
    }
    return decorations;
  }

  /**
   * returns the named property, or null.
   */
  public String getProperty(final String propertiesKey) {
    return mapProperties.getProperty(propertiesKey);
  }

  private <T> T getProperty(
      final String name,
      final Supplier<T> defaultValueSupplier,
      final Function<String, T> parser) {
    return getProperty(mapProperties, name, defaultValueSupplier, parser);
  }

  @VisibleForTesting
  static <T> T getProperty(
      final Properties properties,
      final String name,
      final Supplier<T> defaultValueSupplier,
      final Function<String, T> parser) {
    final @Nullable String encodedValue = properties.getProperty(name);
    if (encodedValue == null) {
      return defaultValueSupplier.get();
    }

    try {
      return parser.apply(encodedValue);
    } catch (final NumberFormatException e) {
      ClientLogger.logQuietly("Failed to parse map property: " + name, e);
      return defaultValueSupplier.get();
    }
  }

  private double getDoubleProperty(final String name, final Supplier<Double> defaultValueSupplier) {
    return getProperty(name, defaultValueSupplier, Double::parseDouble);
  }

  private int getIntegerProperty(final String name, final Supplier<Integer> defaultValueSupplier) {
    return getProperty(name, defaultValueSupplier, Integer::parseInt);
  }

  public double getDefaultUnitScale() {
    return getDoubleProperty(PROPERTY_UNITS_SCALE, () -> 1.0);
  }

  /**
   * Does not take account of any scaling.
   */
  public int getDefaultUnitWidth() {
    return getIntegerProperty(PROPERTY_UNITS_WIDTH, () -> UnitImageFactory.DEFAULT_UNIT_ICON_SIZE);
  }

  /**
   * Does not take account of any scaling.
   */
  public int getDefaultUnitHeight() {
    return getIntegerProperty(PROPERTY_UNITS_HEIGHT, () -> UnitImageFactory.DEFAULT_UNIT_ICON_SIZE);
  }

  /**
   * Does not take account of any scaling.
   */
  public int getDefaultUnitCounterOffsetWidth() {
    // if it is not set, divide by 4 so that it is roughly centered
    return getIntegerProperty(PROPERTY_UNITS_COUNTER_OFFSET_WIDTH, () -> getDefaultUnitWidth() / 4);
  }

  /**
   * Does not take account of any scaling.
   */
  public int getDefaultUnitCounterOffsetHeight() {
    // put at bottom of unit, if not set
    return getIntegerProperty(PROPERTY_UNITS_COUNTER_OFFSET_HEIGHT, this::getDefaultUnitHeight);
  }

  public int getDefaultUnitsStackSize() {
    // zero = normal behavior
    final String stack = mapProperties.getProperty(PROPERTY_UNITS_STACK_SIZE, "0");
    return Math.max(0, Integer.parseInt(stack));
  }

  public boolean shouldDrawTerritoryName(final String territoryName) {
    if (undrawnTerritoriesNames == null) {
      final String property = mapProperties.getProperty(PROPERTY_DONT_DRAW_TERRITORY_NAMES, "");
      undrawnTerritoriesNames = new HashSet<>(Arrays.asList(property.split(",")));
    }
    return !undrawnTerritoriesNames.contains(territoryName);
  }

  public boolean getHasRelief() {
    return Boolean.valueOf(mapProperties.getProperty(PROPERTY_MAP_HASRELIEF, "true"));
  }

  public int getMapCursorHotspotX() {
    return Math.max(0,
        Math.min(256, Integer.parseInt(mapProperties.getProperty(PROPERTY_MAP_CURSOR_HOTSPOT_X, "0"))));
  }

  public int getMapCursorHotspotY() {
    return Math.max(0,
        Math.min(256, Integer.parseInt(mapProperties.getProperty(PROPERTY_MAP_CURSOR_HOTSPOT_Y, "0"))));
  }

  public String getMapBlendMode() {
    return String.valueOf(mapProperties.getProperty(PROPERTY_MAP_MAPBLENDMODE, "normal"));
  }

  public float getMapBlendAlpha() {
    return Float.valueOf(mapProperties.getProperty(PROPERTY_MAP_MAPBLENDALPHA, "0.5f"));
  }

  public boolean drawCapitolMarkers() {
    return Boolean.valueOf(mapProperties.getProperty(PROPERTY_MAP_SHOWCAPITOLMARKERS, "true"));
  }

  public boolean drawTerritoryNames() {
    return Boolean.valueOf(mapProperties.getProperty(PROPERTY_MAP_SHOWTERRITORYNAMES, "true"));
  }

  public boolean drawResources() {
    return Boolean.valueOf(mapProperties.getProperty(PROPERTY_MAP_SHOWRESOURCES, "true"));
  }

  public boolean drawComments() {
    return Boolean.valueOf(mapProperties.getProperty(PROPERTY_MAP_SHOWCOMMENTS, "true"));
  }

  public boolean drawSeaZoneNames() {
    return Boolean.valueOf(mapProperties.getProperty(PROPERTY_MAP_SHOWSEAZONENAMES, "false"));
  }

  public boolean drawNamesFromTopLeft() {
    return Boolean.valueOf(mapProperties.getProperty(PROPERTY_MAP_DRAWNAMESFROMTOPLEFT, "false"));
  }

  public boolean useNation_convoyFlags() {
    return Boolean.valueOf(mapProperties.getProperty(PROPERTY_MAP_USENATION_CONVOYFLAGS, "false"));
  }

  public boolean useTerritoryEffectMarkers() {
    return Boolean.valueOf(mapProperties.getProperty(PROPERTY_MAP_USETERRITORYEFFECTMARKERS, "false"));
  }

  private void initializeContains() {
    contains = new HashMap<>();
    for (final String seaTerritory : getTerritories()) {
      if (!Util.isTerritoryNameIndicatingWater(seaTerritory)) {
        continue;
      }
      final List<String> contained = new ArrayList<>();
      for (final String landTerritory : getTerritories()) {
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
        contains.put(seaTerritory, contained);
      }
    }
  }

  public boolean getBooleanProperty(final String propertiesKey) {
    return Boolean.valueOf(mapProperties.getProperty(propertiesKey, "true"));
  }

  public Color getColorProperty(final String propertiesKey) throws IllegalStateException {
    if (mapProperties.getProperty(propertiesKey) != null) {
      final String colorString = mapProperties.getProperty(propertiesKey);
      if (colorString.length() != 6) {
        throw new IllegalStateException("Colors must be a 6 digit hex number, eg FF0011, not:" + colorString);
      }
      try {
        return new Color(Integer.decode("0x" + colorString));
      } catch (final NumberFormatException nfe) {
        throw new IllegalStateException("Player colors must be a 6 digit hex number, eg FF0011");
      }
    }
    return null;
  }

  public Color getPlayerColor(final String playerName) {
    // already loaded, just return
    if (playerColors.containsKey(playerName)) {
      return playerColors.get(playerName);
    }
    // look in map.properties
    Color color;
    try {
      final String propertiesKey = PROPERTY_COLOR_PREFIX + playerName;
      color = getColorProperty(propertiesKey);
    } catch (final Exception e) {
      throw new IllegalStateException("Player colors must be a 6 digit hex number, eg FF0011");
    }
    if (color == null) {
      // dont crash, use one of our default colors
      // its ugly, but usable
      color = defaultColors.nextColor();
    }
    playerColors.put(playerName, color);
    return color;
  }

  /**
   * returns the color for impassable territories.
   */
  public Color impassableColor() {
    // just use getPlayerColor, since it parses the properties
    return getPlayerColor(Constants.PLAYER_NAME_IMPASSABLE);
  }

  /**
   * @return a Set of territory names as Strings. generally this shouldnt be
   *         used, instead you should use aGameData.getMap().getTerritories()
   */
  public Set<String> getTerritories() {
    return polys.keySet();
  }

  /**
   * Does this territory have any territories contained within it.
   */
  public boolean hasContainedTerritory(final String territoryName) {
    return contains.containsKey(territoryName);
  }

  /**
   * returns the name of the territory contained in the given territory. This
   * applies to islands within sea zones.
   *
   * @return possiblly null
   */
  public List<String> getContainedTerritory(final String territoryName) {
    return contains.get(territoryName);
  }

  public void verify(final GameData data) {
    verifyKeys(data, centers, "centers");
    verifyKeys(data, polys, "polygons");
    verifyKeys(data, place, "place");
  }

  private static void verifyKeys(final GameData data, final Map<String, ?> map, final String dataTypeForErrorMessage)
      throws IllegalStateException {
    final StringBuilder errors = new StringBuilder();
    final Iterator<String> iter = map.keySet().iterator();
    while (iter.hasNext()) {
      final String name = iter.next();
      final Territory terr = data.getMap().getTerritory(name);
      // allow loading saved games with missing territories; just ignore them
      if (terr == null) {
        iter.remove();
      }
    }
    final Set<String> keySet = map.keySet();
    for (final Territory terr : data.getMap().getTerritories()) {
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
    return place.get(terr.getName());
  }

  public List<Polygon> getPolygons(final String terr) {
    return polys.get(terr);
  }

  public List<Polygon> getPolygons(final Territory terr) {
    return getPolygons(terr.getName());
  }

  public Point getCenter(final String terr) {
    if (centers.get(terr) == null) {
      throw new IllegalStateException("Missing " + CENTERS_FILE + " data for " + terr);
    }
    return new Point(centers.get(terr));
  }

  public Point getCenter(final Territory terr) {
    return getCenter(terr.getName());
  }

  public Point getCapitolMarkerLocation(final Territory terr) {
    if (capitolPlace.containsKey(terr.getName())) {
      return capitolPlace.get(terr.getName());
    }
    return getCenter(terr);
  }

  public Point getConvoyMarkerLocation(final Territory terr) {
    if (convoyPlace.containsKey(terr.getName())) {
      return convoyPlace.get(terr.getName());
    }
    return getCenter(terr);
  }

  public Optional<Point> getCommentMarkerLocation(final Territory terr) {
    return Optional.ofNullable(commentPlace.get(terr.getName()));
  }

  public Point getKamikazeMarkerLocation(final Territory terr) {
    if (kamikazePlace.containsKey(terr.getName())) {
      return kamikazePlace.get(terr.getName());
    }
    return getCenter(terr);
  }

  public Point getVcPlacementPoint(final Territory terr) {
    if (vcPlace.containsKey(terr.getName())) {
      return vcPlace.get(terr.getName());
    }
    return getCenter(terr);
  }

  public Point getBlockadePlacementPoint(final Territory terr) {
    if (blockadePlace.containsKey(terr.getName())) {
      return blockadePlace.get(terr.getName());
    }
    return getCenter(terr);
  }

  public Optional<Point> getPuPlacementPoint(final Territory terr) {
    return Optional.ofNullable(puPlace.get(terr.getName()));
  }

  public Optional<Point> getNamePlacementPoint(final Territory terr) {
    return Optional.ofNullable(namePlace.get(terr.getName()));
  }

  /**
   * Get the territory at the x,y co-ordinates could be null.
   */
  public String getTerritoryAt(final double x, final double y) {
    String seaName = null;
    // try to find a land territory.
    // sea zones often surround a land territory
    for (final String name : polys.keySet()) {
      final Collection<Polygon> polygons = polys.get(name);
      for (final Polygon poly : polygons) {
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
    final String widthProperty = mapProperties.getProperty(PROPERTY_MAP_WIDTH);
    final String heightProperty = mapProperties.getProperty(PROPERTY_MAP_HEIGHT);
    if ((widthProperty == null) || (heightProperty == null)) {
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
    final List<Polygon> polys = this.polys.get(name);
    if (polys == null) {
      throw new IllegalStateException("No polygons found for:" + name + " All territories:" + this.polys.keySet());
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
    if ((scrollWrapX() && (bounds.width > 1800) && (bounds.width > (mapDimensions.width * 0.9)))
        || (scrollWrapY() && (bounds.height > 1200) && (bounds.height > (mapDimensions.height * 0.9)))) {
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
      if (scrollWrapX && (itemRect.getMaxX() >= closeToMapWidth)) {
        itemRect.translate(-mapWidth, 0);
      }
      if (scrollWrapY && (itemRect.getMaxY() >= closeToMapHeight)) {
        itemRect.translate(0, -mapHeight);
      }
      if (boundingRect == null) {
        boundingRect = itemRect;
      } else {
        boundingRect.add(itemRect);
      }
    }
    // if the polygon is completely negative, we can make translate it back to normal
    if ((boundingRect.x < 0) && (boundingRect.getMaxX() <= 0)) {
      boundingRect.translate(mapWidth, 0);
    }
    if ((boundingRect.y < 0) && (boundingRect.getMaxY() <= 0)) {
      boundingRect.translate(0, mapHeight);
    }
    return boundingRect;
  }

  public Optional<Image> getVcImage() {
    return loadImage("misc/vc.png");
  }

  public Optional<Image> getBlockadeImage() {
    return loadImage("misc/blockade.png");
  }

  public Optional<Image> getErrorImage() {
    return loadImage("misc/error.gif");
  }

  public Optional<Image> getWarningImage() {
    return loadImage("misc/warning.gif");
  }

  private Optional<Image> loadImage(final String imageName) {
    final URL url = resourceLoader.getResource(imageName);
    if (url == null) {
      // this is actually pretty common that we try to read images that are not there. Let the caller
      // decide if this is an error or not.
      return Optional.empty();
    }
    try {
      return Optional.of(ImageIO.read(url));
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public Map<String, Image> getTerritoryNameImages() {
    return Collections.unmodifiableMap(territoryNameImages);
  }

  public Map<Image, List<Point>> getDecorations() {
    return Collections.unmodifiableMap(decorations);
  }

  public List<Point> getTerritoryEffectPoints(final Territory territory) {
    if (territoryEffects.get(territory.getName()) == null) {
      return Arrays.asList(getCenter(territory));
    }
    return territoryEffects.get(territory.getName());
  }

  public Optional<Image> getTerritoryEffectImage(final String effectName) {
    // TODO: what does this cache buy us? should we still keep it?
    if (effectImages.get(effectName) != null) {
      return Optional.of(effectImages.get(effectName));
    }
    final Optional<Image> effectImage = loadImage("territoryEffects/" + effectName + ".png");
    effectImages.put(effectName, effectImage.orElse(null));
    return effectImage;
  }
}
