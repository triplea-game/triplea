package games.strategy.triplea.ui.mapdata;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.ui.Util;
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
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import lombok.extern.java.Log;
import org.triplea.java.function.ThrowingFunction;
import org.triplea.java.function.ThrowingSupplier;
import org.triplea.util.PointFileReaderWriter;
import org.triplea.util.Tuple;

/** contains data about the territories useful for drawing. */
@Log
public class MapData implements Closeable {
  public static final String PROPERTY_UNITS_SCALE = "units.scale";
  public static final String PROPERTY_UNITS_WIDTH = "units.width";
  public static final String PROPERTY_UNITS_HEIGHT = "units.height";
  public static final String PROPERTY_UNITS_COUNTER_OFFSET_WIDTH = "units.counter.offset.width";
  public static final String PROPERTY_UNITS_COUNTER_OFFSET_HEIGHT = "units.counter.offset.height";
  public static final String PROPERTY_UNITS_STACK_SIZE = "units.stack.size";
  public static final String PROPERTY_SCREENSHOT_TITLE_ENABLED = "screenshot.title.enabled";
  public static final String PROPERTY_SCREENSHOT_TITLE_X = "screenshot.title.x";
  public static final String PROPERTY_SCREENSHOT_TITLE_Y = "screenshot.title.y";
  public static final String PROPERTY_SCREENSHOT_TITLE_COLOR = "screenshot.title.color";
  public static final String PROPERTY_SCREENSHOT_TITLE_FONT_SIZE = "screenshot.title.font.size";
  public static final String PROPERTY_COLOR_PREFIX = "color.";
  public static final String PROPERTY_MAP_WIDTH = "map.width";
  public static final String PROPERTY_MAP_HEIGHT = "map.height";
  public static final String PROPERTY_MAP_SCROLLWRAPX = "map.scrollWrapX";
  public static final String PROPERTY_MAP_SCROLLWRAPY = "map.scrollWrapY";
  public static final String PROPERTY_MAP_HASRELIEF = "map.hasRelief";
  public static final String PROPERTY_MAP_CURSOR_HOTSPOT_X = "map.cursor.hotspot.x";
  public static final String PROPERTY_MAP_CURSOR_HOTSPOT_Y = "map.cursor.hotspot.y";
  public static final String PROPERTY_MAP_SHOWCAPITOLMARKERS = "map.showCapitolMarkers";
  public static final String PROPERTY_MAP_USETERRITORYEFFECTMARKERS =
      "map.useTerritoryEffectMarkers";
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

  private static final String PROPERTY_DONT_DRAW_UNITS = "dont_draw_units";
  private static final String PROPERTY_MAP_SMALLMAPTERRITORYSATURATION =
      "smallMap.territory.saturation";
  private static final String PROPERTY_MAP_SMALLMAPUNITSIZE = "smallMap.unit.size";
  private static final String PROPERTY_MAP_SMALLMAPVIEWERBORDERCOLOR =
      "smallMap.viewer.borderColor";
  private static final String PROPERTY_MAP_SMALLMAPVIEWERFILLCOLOR = "smallMap.viewer.fillColor";
  private static final String PROPERTY_MAP_SMALLMAPVIEWERFILLALPHA = "smallMap.viewer.fillAlpha";
  private static final String PROPERTY_UNITS_TRANSFORM_COLOR_PREFIX = "units.transform.color.";
  private static final String PROPERTY_UNITS_TRANSFORM_BRIGHTNESS_PREFIX =
      "units.transform.brightness.";
  private static final String PROPERTY_UNITS_TRANSFORM_FLIP_PREFIX = "units.transform.flip.";
  private static final String PROPERTY_UNITS_TRANSFORM_IGNORE = "units.transform.ignore";

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
  private Set<String> ignoreTransformingUnits;
  private final Map<String, Tuple<List<Point>, Boolean>> place = new HashMap<>();
  private final Map<String, List<Polygon>> polys = new HashMap<>();
  private final Map<String, Point> centers = new HashMap<>();
  private final Map<String, Point> vcPlace = new HashMap<>();
  private final Map<String, Point> blockadePlace = new HashMap<>();
  private final Map<String, Point> convoyPlace = new HashMap<>();
  private final Map<String, Point> commentPlace = new HashMap<>();
  private final Map<String, Point> puPlace = new HashMap<>();
  private final Map<String, Point> namePlace = new HashMap<>();
  private final Map<String, Point> kamikazePlace = new HashMap<>();
  private final Map<String, Point> capitolPlace = new HashMap<>();
  private final Map<String, List<String>> contains = new HashMap<>();
  private final Properties mapProperties = new Properties();
  private final Map<String, List<Point>> territoryEffects = new HashMap<>();
  private Set<String> undrawnUnits;
  private Set<String> undrawnTerritoriesNames;
  private final Map<Image, List<Point>> decorations = new HashMap<>();
  private final Map<String, Image> territoryNameImages = new HashMap<>();
  private final Map<String, Image> effectImages = new HashMap<>();
  private final ResourceLoader resourceLoader;

  public MapData(final String mapNameDir) {
    this(ResourceLoader.getMapResourceLoader(mapNameDir));
  }

  public MapData(final ResourceLoader loader) {
    resourceLoader = loader;
    try {
      if (loader.getResource(POLYGON_FILE) == null) {
        throw new IllegalStateException(
            "Error in resource loading. Unable to load expected resource: "
                + POLYGON_FILE
                + ", the error"
                + " is that either we did not find the correct path to load. Check the resource "
                + "loader to make sure the map zip or dir was added. Failing that, the path in "
                + "this error message should be available relative to the map folder, or relative "
                + "to the root of the map zip");
      }

      place.putAll(readPlacementsOneToMany(optionalResource(PLACEMENT_FILE)));
      territoryEffects.putAll(readPointsOneToMany(optionalResource(TERRITORY_EFFECT_FILE)));

      polys.putAll(readPolygonsOneToMany(requiredResource(POLYGON_FILE)));
      centers.putAll(readPointsOneToOne(requiredResource(CENTERS_FILE)));
      vcPlace.putAll(readPointsOneToOne(optionalResource(VC_MARKERS)));
      convoyPlace.putAll(readPointsOneToOne(optionalResource(CONVOY_MARKERS)));
      commentPlace.putAll(readPointsOneToOne(optionalResource(COMMENT_MARKERS)));
      blockadePlace.putAll(readPointsOneToOne(optionalResource(BLOCKADE_MARKERS)));
      capitolPlace.putAll(readPointsOneToOne(optionalResource(CAPITAL_MARKERS)));
      puPlace.putAll(readPointsOneToOne(optionalResource(PU_PLACE_FILE)));
      namePlace.putAll(readPointsOneToOne(optionalResource(TERRITORY_NAME_PLACE_FILE)));
      kamikazePlace.putAll(readPointsOneToOne(optionalResource(KAMIKAZE_FILE)));
      decorations.putAll(loadDecorations());
      territoryNameImages.putAll(territoryNameImages());

      try (InputStream inputStream = requiredResource(MAP_PROPERTIES).get()) {
        mapProperties.load(inputStream);
      } catch (final Exception e) {
        log.log(Level.SEVERE, "Error reading map.properties", e);
      }

      contains.putAll(IslandTerritoryFinder.findIslands(polys));
    } catch (final IOException ex) {
      log.log(Level.SEVERE, "Failed to initialize map data", ex);
    }
  }

  private ThrowingSupplier<InputStream, IOException> optionalResource(final String path) {
    return () ->
        Optional.ofNullable(resourceLoader.getResourceAsStream(path))
            .orElseGet(() -> new ByteArrayInputStream(new byte[0]));
  }

  private ThrowingSupplier<InputStream, IOException> requiredResource(final String path) {
    return () ->
        Optional.ofNullable(resourceLoader.getResourceAsStream(path))
            .orElseThrow(() -> new FileNotFoundException(path));
  }

  private static Map<String, Point> readPointsOneToOne(
      final ThrowingSupplier<InputStream, IOException> inputStreamFactory) throws IOException {
    return runWithInputStream(inputStreamFactory, PointFileReaderWriter::readOneToOne);
  }

  private static Map<String, List<Point>> readPointsOneToMany(
      final ThrowingSupplier<InputStream, IOException> inputStreamFactory) throws IOException {
    return runWithInputStream(inputStreamFactory, PointFileReaderWriter::readOneToMany);
  }

  private static Map<String, Tuple<List<Point>, Boolean>> readPlacementsOneToMany(
      final ThrowingSupplier<InputStream, IOException> inputStreamFactory) throws IOException {
    return runWithInputStream(inputStreamFactory, PointFileReaderWriter::readOneToManyPlacements);
  }

  private static Map<String, List<Polygon>> readPolygonsOneToMany(
      final ThrowingSupplier<InputStream, IOException> inputStreamFactory) throws IOException {
    return runWithInputStream(inputStreamFactory, PointFileReaderWriter::readOneToManyPolygons);
  }

  private static <R> R runWithInputStream(
      final ThrowingSupplier<InputStream, IOException> inputStreamFactory,
      final ThrowingFunction<InputStream, R, IOException> reader)
      throws IOException {
    try (InputStream is = inputStreamFactory.get()) {
      return reader.apply(is);
    }
  }

  public boolean scrollWrapX() {
    return Boolean.parseBoolean(mapProperties.getProperty(PROPERTY_MAP_SCROLLWRAPX, "true"));
  }

  public boolean scrollWrapY() {
    return Boolean.parseBoolean(mapProperties.getProperty(PROPERTY_MAP_SCROLLWRAPY, "false"));
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

      territoryNameImage.ifPresent(image -> territoryNameImages.put(name, image));
    }
    return territoryNameImages;
  }

  private Optional<Image> loadTerritoryNameImage(final String imageName) {
    try {
      // try first file names that have underscores instead of spaces
      final String normalizedName = imageName.replace(' ', '_');
      Optional<Image> img = loadImage(constructTerritoryNameImagePath(normalizedName));
      if (img.isEmpty()) {
        img = loadImage(constructTerritoryNameImagePath(imageName));
      }
      return img;
    } catch (final Exception e) {
      // TODO: this is checking for IllegalStateException - we should bubble up the Optional image
      // load and just
      // check instead if the optional is empty.
      log.log(Level.SEVERE, "Image loading failed: " + imageName, e);
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

  /** returns the named property, or null. */
  public String getProperty(final String propertiesKey) {
    return mapProperties.getProperty(propertiesKey);
  }

  private <T> T getProperty(
      final String name, final Supplier<T> defaultValueSupplier, final Function<String, T> parser) {
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
      log.log(Level.SEVERE, "Failed to parse map property: " + name, e);
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

  /** Does not take account of any scaling. */
  public int getDefaultUnitWidth() {
    return getIntegerProperty(PROPERTY_UNITS_WIDTH, () -> UnitImageFactory.DEFAULT_UNIT_ICON_SIZE);
  }

  /** Does not take account of any scaling. */
  public int getDefaultUnitHeight() {
    return getIntegerProperty(PROPERTY_UNITS_HEIGHT, () -> UnitImageFactory.DEFAULT_UNIT_ICON_SIZE);
  }

  /** Does not take account of any scaling. */
  public int getDefaultUnitCounterOffsetWidth() {
    // if it is not set, divide by 4 so that it is roughly centered
    return getIntegerProperty(PROPERTY_UNITS_COUNTER_OFFSET_WIDTH, () -> getDefaultUnitWidth() / 4);
  }

  /** Does not take account of any scaling. */
  public int getDefaultUnitCounterOffsetHeight() {
    // put at bottom of unit, if not set
    return getIntegerProperty(PROPERTY_UNITS_COUNTER_OFFSET_HEIGHT, this::getDefaultUnitHeight);
  }

  public int getDefaultUnitsStackSize() {
    // zero = normal behavior
    final String stack = mapProperties.getProperty(PROPERTY_UNITS_STACK_SIZE, "0");
    return Math.max(0, Integer.parseInt(stack));
  }

  /** Returns the unit color associated with the player named {@code playerName}. */
  public Optional<Color> getUnitColor(final String playerName) {
    final Color color = getColorProperty(PROPERTY_UNITS_TRANSFORM_COLOR_PREFIX + playerName);
    return Optional.ofNullable(color);
  }

  /** Returns the unit brightness associated with the player named {@code playerName}. */
  public int getUnitBrightness(final String playerName) {
    final int brightness =
        Integer.parseInt(
            mapProperties.getProperty(
                PROPERTY_UNITS_TRANSFORM_BRIGHTNESS_PREFIX + playerName, "0"));
    if (brightness < -100 || brightness > 100) {
      throw new IllegalStateException(
          "Valid brightness value range is -100 to 100, not: " + brightness);
    }
    return brightness;
  }

  /** Returns whether to flip unit images associated with the player named {@code playerName}. */
  public boolean shouldFlipUnit(final String playerName) {
    return Boolean.parseBoolean(
        mapProperties.getProperty(PROPERTY_UNITS_TRANSFORM_FLIP_PREFIX + playerName, "false"));
  }

  public boolean ignoreTransformingUnit(final String unitName) {
    if (ignoreTransformingUnits == null) {
      final String property = mapProperties.getProperty(PROPERTY_UNITS_TRANSFORM_IGNORE, "");
      ignoreTransformingUnits = new HashSet<>(List.of(property.split(",")));
    }
    return ignoreTransformingUnits.contains(unitName);
  }

  public boolean shouldDrawUnit(final String unitName) {
    if (undrawnUnits == null) {
      final String property = mapProperties.getProperty(PROPERTY_DONT_DRAW_UNITS, "");
      undrawnUnits = new HashSet<>(List.of(property.split(",")));
    }
    return !undrawnUnits.contains(unitName);
  }

  public boolean shouldDrawTerritoryName(final String territoryName) {
    if (undrawnTerritoriesNames == null) {
      final String property = mapProperties.getProperty(PROPERTY_DONT_DRAW_TERRITORY_NAMES, "");
      undrawnTerritoriesNames = new HashSet<>(List.of(property.split(",")));
    }
    return !undrawnTerritoriesNames.contains(territoryName);
  }

  public boolean getHasRelief() {
    return Boolean.parseBoolean(mapProperties.getProperty(PROPERTY_MAP_HASRELIEF, "true"));
  }

  public int getMapCursorHotspotX() {
    return Math.max(
        0,
        Math.min(
            256, Integer.parseInt(mapProperties.getProperty(PROPERTY_MAP_CURSOR_HOTSPOT_X, "0"))));
  }

  public int getMapCursorHotspotY() {
    return Math.max(
        0,
        Math.min(
            256, Integer.parseInt(mapProperties.getProperty(PROPERTY_MAP_CURSOR_HOTSPOT_Y, "0"))));
  }

  public String getMapBlendMode() {
    return String.valueOf(mapProperties.getProperty(PROPERTY_MAP_MAPBLENDMODE, "normal"));
  }

  public float getMapBlendAlpha() {
    return Float.parseFloat(mapProperties.getProperty(PROPERTY_MAP_MAPBLENDALPHA, "0.5f"));
  }

  public boolean drawCapitolMarkers() {
    return Boolean.parseBoolean(mapProperties.getProperty(PROPERTY_MAP_SHOWCAPITOLMARKERS, "true"));
  }

  public boolean drawTerritoryNames() {
    return Boolean.parseBoolean(mapProperties.getProperty(PROPERTY_MAP_SHOWTERRITORYNAMES, "true"));
  }

  public boolean drawResources() {
    return Boolean.parseBoolean(mapProperties.getProperty(PROPERTY_MAP_SHOWRESOURCES, "true"));
  }

  public boolean drawComments() {
    return Boolean.parseBoolean(mapProperties.getProperty(PROPERTY_MAP_SHOWCOMMENTS, "true"));
  }

  public boolean drawSeaZoneNames() {
    return Boolean.parseBoolean(mapProperties.getProperty(PROPERTY_MAP_SHOWSEAZONENAMES, "false"));
  }

  public boolean drawNamesFromTopLeft() {
    return Boolean.parseBoolean(
        mapProperties.getProperty(PROPERTY_MAP_DRAWNAMESFROMTOPLEFT, "false"));
  }

  public boolean useNation_convoyFlags() {
    return Boolean.parseBoolean(
        mapProperties.getProperty(PROPERTY_MAP_USENATION_CONVOYFLAGS, "false"));
  }

  public boolean useTerritoryEffectMarkers() {
    return Boolean.parseBoolean(
        mapProperties.getProperty(PROPERTY_MAP_USETERRITORYEFFECTMARKERS, "false"));
  }

  public float getSmallMapTerritorySaturation() {
    return Float.parseFloat(
        mapProperties.getProperty(PROPERTY_MAP_SMALLMAPTERRITORYSATURATION, "1.0f"));
  }

  public int getSmallMapUnitSize() {
    return Integer.parseInt(mapProperties.getProperty(PROPERTY_MAP_SMALLMAPUNITSIZE, "4"));
  }

  public Color getSmallMapViewerBorderColor() {
    return getColorProperty(PROPERTY_MAP_SMALLMAPVIEWERBORDERCOLOR, Color.LIGHT_GRAY);
  }

  public Color getSmallMapViewerFillColor() {
    return getColorProperty(PROPERTY_MAP_SMALLMAPVIEWERFILLCOLOR, Color.LIGHT_GRAY);
  }

  public float getSmallMapViewerFillAlpha() {
    return Float.parseFloat(
        mapProperties.getProperty(PROPERTY_MAP_SMALLMAPVIEWERFILLALPHA, "0.0f"));
  }

  public boolean getBooleanProperty(final String propertiesKey) {
    return Boolean.parseBoolean(mapProperties.getProperty(propertiesKey, "true"));
  }

  public Color getColorProperty(final String propertiesKey) {
    return getColorProperty(propertiesKey, null);
  }

  /**
   * Returns the value of the property named {@code propertiesKey} of type {@link Color}. Returns
   * {@code defaultColor} if the property doesn't exist.
   *
   * @throws IllegalStateException If the property value does not represent a valid color.
   */
  private Color getColorProperty(final String propertiesKey, final Color defaultColor) {
    if (mapProperties.getProperty(propertiesKey) != null) {
      final String colorString = mapProperties.getProperty(propertiesKey);
      if (colorString.length() != 6) {
        throw new IllegalStateException(
            "Colors must be 6 digit hex numbers, eg FF0011, not: " + colorString);
      }
      try {
        return new Color(Integer.decode("0x" + colorString));
      } catch (final NumberFormatException nfe) {
        throw new IllegalStateException(
            "Colors must be 6 digit hex numbers, eg FF0011, not: " + colorString);
      }
    }
    return defaultColor;
  }

  /** Returns the color associated with the player named {@code playerName}. */
  public Color getPlayerColor(final String playerName) {
    Color color = getColorProperty(PROPERTY_COLOR_PREFIX + playerName);
    if (color == null) {
      // use one of our default colors, its ugly, but usable
      color = defaultColors.nextColor();
    }
    return color;
  }

  /** returns the color for impassable territories. */
  public Color impassableColor() {
    // just use getPlayerColor, since it parses the properties
    return getPlayerColor(Constants.PLAYER_NAME_IMPASSABLE);
  }

  /**
   * Returns a Set of territory names as Strings. generally this shouldn't be used, instead you
   * should use aGameData.getMap().getTerritories()
   */
  public Set<String> getTerritories() {
    return polys.keySet();
  }

  /**
   * Returns the polygons for any territories contained within a given territory.
   *
   * @param territoryName Name of the territory, expected to be a sea territory, where we will be
   *     checking for any contained 'island' territories.
   */
  public Set<Polygon> getContainedTerritoryPolygons(final String territoryName) {
    return Optional.ofNullable(contains.get(territoryName)).orElse(List.of()).stream()
        .map(this::getPolygons)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  public void verify(final GameData data) {
    verifyKeys(data, centers, "centers");
    verifyKeys(data, polys, "polygons");
    verifyKeys(data, place, "place");
  }

  private static void verifyKeys(
      final GameData data, final Map<String, ?> map, final String dataTypeForErrorMessage) {
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
        errors
            .append("No data of type ")
            .append(dataTypeForErrorMessage)
            .append(" for territory: ")
            .append(terr.getName())
            .append("\n");
      }
    }
    if (errors.length() > 0) {
      throw new IllegalStateException(errors.toString());
    }
  }

  public List<Point> getPlacementPoints(final Territory terr) {
    return place.get(terr.getName()).getFirst();
  }

  public boolean getPlacementOverflowToLeft(final Territory terr) {
    return place.get(terr.getName()).getSecond();
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

  /** Get the territory at the x,y co-ordinates could be null. */
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

  /**
   * Returns the bounding rectangle for the territory named {@code name}.
   *
   * @throws IllegalStateException If a bounding rectangle cannot be calculated for the specified
   *     territory.
   */
  public Rectangle getBoundingRect(final String name) {
    final List<Polygon> polys = this.polys.get(name);
    if (polys == null) {
      throw new IllegalStateException(
          "No polygons found for:" + name + " All territories:" + this.polys.keySet());
    }
    final Iterator<Polygon> polyIter = polys.iterator();
    final Rectangle bounds = polyIter.next().getBounds();
    while (polyIter.hasNext()) {
      bounds.add(polyIter.next().getBounds());
    }
    // if we have a territory that straddles the map divide, ie: which has polygons on both the left
    // and right sides of
    // the map,
    // then the polygon's width or height could be almost equal to the map width or height
    // this can cause lots of problems, like when we want to get the tiles for the territory we
    // would end up getting all
    // the tiles for the map (and a java heap space error)
    final Dimension mapDimensions = getMapDimensions();
    if ((scrollWrapX() && bounds.width > 1800 && bounds.width > mapDimensions.width * 0.9)
        || (scrollWrapY() && bounds.height > 1200 && bounds.height > mapDimensions.height * 0.9)) {
      return getBoundingRectWithTranslate(polys, mapDimensions);
    }
    return bounds;
  }

  private Rectangle getBoundingRectWithTranslate(
      final List<Polygon> polys, final Dimension mapDimensions) {
    Rectangle boundingRect = null;
    final int mapWidth = mapDimensions.width;
    final int mapHeight = mapDimensions.height;
    final int closeToMapWidth = (int) (mapWidth * 0.9);
    final int closeToMapHeight = (int) (mapHeight * 0.9);
    final boolean scrollWrapX = this.scrollWrapX();
    final boolean scrollWrapY = this.scrollWrapY();
    for (final Polygon item : polys) {
      // if our rectangle is on the right side (mapscrollx) then we push it to be on the negative
      // left side, so that the
      // bounds.x will be negative
      // this solves the issue of maps that have a territory where polygons were on both sides of
      // the map divide
      // (so our bounds.x was 0, and our bounds.y would be the entire map width)
      // (when in fact it should actually be bounds.x = -10 or something, and bounds.width = 20 or
      // something)
      // we use map dimensions.width * 0.9 because the polygon may not actually touch the side of
      // the map (like if the
      // territory borders are thick)
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
      // this is actually pretty common that we try to read images that are not there. Let the
      // caller
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
      return List.of(getCenter(territory));
    }
    return territoryEffects.get(territory.getName());
  }

  public Optional<Image> getTerritoryEffectImage(final String effectName) {
    // TODO: what does this cache buy us? should we still keep it?
    if (effectImages.get(effectName) != null) {
      return Optional.of(effectImages.get(effectName));
    }
    Optional<Image> effectImage = loadImage("territoryEffects/" + effectName + "_large.png");
    if (effectImage.isEmpty()) {
      effectImage = loadImage("territoryEffects/" + effectName + ".png");
    }
    effectImages.put(effectName, effectImage.orElse(null));
    return effectImage;
  }
}
