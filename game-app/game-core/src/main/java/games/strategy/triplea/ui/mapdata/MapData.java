package games.strategy.triplea.ui.mapdata;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.image.UnitImageFactory;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.triplea.java.ColorUtils;
import org.triplea.java.function.ThrowingFunction;
import org.triplea.util.PointFileReaderWriter;
import org.triplea.util.Tuple;

/** contains data about the territories useful for drawing. */
@Slf4j
public class MapData {
  @NonNls public static final String PROPERTY_UNITS_SCALE = "units.scale";
  @NonNls public static final String PROPERTY_UNITS_WIDTH = "units.width";
  @NonNls public static final String PROPERTY_UNITS_HEIGHT = "units.height";

  @NonNls
  public static final String PROPERTY_UNITS_COUNTER_OFFSET_WIDTH = "units.counter.offset.width";

  @NonNls
  public static final String PROPERTY_UNITS_COUNTER_OFFSET_HEIGHT = "units.counter.offset.height";

  @NonNls public static final String PROPERTY_UNITS_STACK_SIZE = "units.stack.size";
  @NonNls public static final String PROPERTY_SCREENSHOT_TITLE_ENABLED = "screenshot.title.enabled";
  @NonNls public static final String PROPERTY_SCREENSHOT_TITLE_X = "screenshot.title.x";
  @NonNls public static final String PROPERTY_SCREENSHOT_TITLE_Y = "screenshot.title.y";
  @NonNls public static final String PROPERTY_SCREENSHOT_TITLE_COLOR = "screenshot.title.color";

  @NonNls
  public static final String PROPERTY_SCREENSHOT_TITLE_FONT_SIZE = "screenshot.title.font.size";

  @NonNls public static final String PROPERTY_MAP_WIDTH = "map.width";
  @NonNls public static final String PROPERTY_MAP_HEIGHT = "map.height";
  @NonNls public static final String PROPERTY_MAP_SCROLLWRAPX = "map.scrollWrapX";
  @NonNls public static final String PROPERTY_MAP_SCROLLWRAPY = "map.scrollWrapY";
  @NonNls public static final String PROPERTY_MAP_HASRELIEF = "map.hasRelief";
  @NonNls public static final String PROPERTY_MAP_CURSOR_HOTSPOT_X = "map.cursor.hotspot.x";
  @NonNls public static final String PROPERTY_MAP_CURSOR_HOTSPOT_Y = "map.cursor.hotspot.y";
  @NonNls public static final String PROPERTY_MAP_SHOWCAPITOLMARKERS = "map.showCapitolMarkers";
  public static final String PROPERTY_MAP_USETERRITORYEFFECTMARKERS =
      "map.useTerritoryEffectMarkers";
  @NonNls public static final String PROPERTY_MAP_SHOWTERRITORYNAMES = "map.showTerritoryNames";
  @NonNls public static final String PROPERTY_MAP_SHOWRESOURCES = "map.showResources";
  @NonNls public static final String PROPERTY_MAP_SHOWCOMMENTS = "map.showComments";
  @NonNls public static final String PROPERTY_MAP_SHOWSEAZONENAMES = "map.showSeaZoneNames";
  @NonNls public static final String PROPERTY_MAP_DRAWNAMESFROMTOPLEFT = "map.drawNamesFromTopLeft";

  @NonNls
  public static final String PROPERTY_MAP_USENATION_CONVOYFLAGS = "map.useNation_convoyFlags";

  public static final String PROPERTY_DONT_DRAW_TERRITORY_NAMES = "dont_draw_territory_names";
  @NonNls public static final String PROPERTY_MAP_MAPBLENDS = "map.mapBlends";
  @NonNls public static final String PROPERTY_MAP_MAPBLENDMODE = "map.mapBlendMode";
  @NonNls public static final String PROPERTY_MAP_MAPBLENDALPHA = "map.mapBlendAlpha";

  @NonNls public static final String POLYGON_FILE = "polygons.txt";

  @NonNls private static final String PROPERTY_DONT_DRAW_UNITS = "dont_draw_units";

  @NonNls
  private static final String PROPERTY_MAP_SMALLMAPTERRITORYSATURATION =
      "smallMap.territory.saturation";

  @NonNls private static final String PROPERTY_MAP_SMALLMAPUNITSIZE = "smallMap.unit.size";

  @NonNls
  private static final String PROPERTY_MAP_SMALLMAPVIEWERBORDERCOLOR =
      "smallMap.viewer.borderColor";

  @NonNls
  private static final String PROPERTY_MAP_SMALLMAPVIEWERFILLCOLOR = "smallMap.viewer.fillColor";

  @NonNls
  private static final String PROPERTY_MAP_SMALLMAPVIEWERFILLALPHA = "smallMap.viewer.fillAlpha";

  @NonNls
  private static final String PROPERTY_UNITS_TRANSFORM_COLOR_PREFIX = "units.transform.color.";

  private static final String PROPERTY_UNITS_TRANSFORM_BRIGHTNESS_PREFIX =
      "units.transform.brightness.";

  @NonNls
  private static final String PROPERTY_UNITS_TRANSFORM_FLIP_PREFIX = "units.transform.flip.";

  @NonNls private static final String PROPERTY_UNITS_TRANSFORM_IGNORE = "units.transform.ignore";

  @NonNls private static final String CENTERS_FILE = "centers.txt";
  @NonNls private static final String PLACEMENT_FILE = "place.txt";
  @NonNls private static final String TERRITORY_EFFECT_FILE = "territory_effects.txt";
  @NonNls private static final String MAP_PROPERTIES = "map.properties";
  @NonNls private static final String CAPITAL_MARKERS = "capitols.txt";
  @NonNls private static final String CONVOY_MARKERS = "convoy.txt";
  @NonNls private static final String COMMENT_MARKERS = "comments.txt";
  @NonNls private static final String VC_MARKERS = "vc.txt";
  @NonNls private static final String BLOCKADE_MARKERS = "blockade.txt";
  @NonNls private static final String PU_PLACE_FILE = "pu_place.txt";
  @NonNls private static final String TERRITORY_NAME_PLACE_FILE = "name_place.txt";
  @NonNls private static final String KAMIKAZE_FILE = "kamikaze_place.txt";
  @NonNls private static final String DECORATIONS_FILE = "decorations.txt";

  private final PlayerColors playerColors;
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
  private final Map<String, Set<String>> contains = new HashMap<>();
  private final Properties mapProperties = new Properties();
  private final Map<String, List<Point>> territoryEffects = new HashMap<>();
  private Set<String> undrawnUnits;
  private Set<String> undrawnTerritoriesNames;
  private final Map<Image, List<Point>> decorations = new HashMap<>();
  private final Map<String, Image> territoryNameImages = new HashMap<>();
  // Use a synchronized map since getTerritoryEffectImage() is called from multiple threads.
  private final Map<String, Image> effectImages = new ConcurrentHashMap<>();

  @Nullable private final Image vcImage;
  @Nullable private final Image blockadeImage;
  @Nullable private final Image errorImage;
  @Nullable private final Image warningImage;

  private final ResourceLoader loader;

  public MapData(final ResourceLoader loader) {
    this.loader = loader;
    try {
      place.putAll(readOptionalPlacementsOneToMany(PLACEMENT_FILE));
      territoryEffects.putAll(readOptionalPointsOneToMany(TERRITORY_EFFECT_FILE));

      polys.putAll(
          PointFileReaderWriter.readOneToManyPolygons(loader.requiredResource(POLYGON_FILE)));
      centers.putAll(PointFileReaderWriter.readOneToOne(loader.requiredResource(CENTERS_FILE)));
      vcPlace.putAll(readOptionalPointsOneToOne(VC_MARKERS));
      convoyPlace.putAll(readOptionalPointsOneToOne(CONVOY_MARKERS));
      commentPlace.putAll(readOptionalPointsOneToOne(COMMENT_MARKERS));
      blockadePlace.putAll(readOptionalPointsOneToOne(BLOCKADE_MARKERS));
      capitolPlace.putAll(readOptionalPointsOneToOne(CAPITAL_MARKERS));
      puPlace.putAll(readOptionalPointsOneToOne(PU_PLACE_FILE));
      namePlace.putAll(readOptionalPointsOneToOne(TERRITORY_NAME_PLACE_FILE));
      kamikazePlace.putAll(readOptionalPointsOneToOne(KAMIKAZE_FILE));
      decorations.putAll(loadDecorations());
      territoryNameImages.putAll(territoryNameImages());

      try (InputStream inputStream =
          Files.newInputStream(loader.requiredResource(MAP_PROPERTIES))) {
        mapProperties.load(inputStream);
      } catch (final Exception e) {
        log.warn("Error reading map.properties, {}", e.getMessage(), e);
      }

      contains.putAll(IslandTerritoryFinder.findIslands(polys));
    } catch (final IOException ex) {
      log.warn("Failed to initialize map data: {}", ex.getMessage(), ex);
    }

    playerColors = new PlayerColors(mapProperties);
    vcImage = loader.loadImage("misc/vc.png").orElse(null);
    blockadeImage = loader.loadImage("misc/blockade.png").orElse(null);
    errorImage = loader.loadImage("misc/error.gif").orElse(null);
    warningImage = loader.loadImage("misc/warning.gif").orElse(null);
  }

  private Map<String, Point> readOptionalPointsOneToOne(final String path) throws IOException {
    return readOptionalMap(path, PointFileReaderWriter::readOneToOne);
  }

  private Map<String, List<Point>> readOptionalPointsOneToMany(final String path)
      throws IOException {
    return readOptionalMap(path, PointFileReaderWriter::readOneToMany);
  }

  private Map<String, Tuple<List<Point>, Boolean>> readOptionalPlacementsOneToMany(
      final String path) throws IOException {
    return readOptionalMap(path, PointFileReaderWriter::readOneToManyPlacements);
  }

  private <K, V> Map<K, V> readOptionalMap(
      final String path, final ThrowingFunction<Path, Map<K, V>, IOException> mapper)
      throws IOException {
    @Nullable final Path resourcePath = loader.optionalResource(path).orElse(null);
    if (resourcePath != null) {
      return mapper.apply(resourcePath);
    }
    return Map.of();
  }

  public boolean scrollWrapX() {
    return Boolean.parseBoolean(mapProperties.getProperty(PROPERTY_MAP_SCROLLWRAPX, "true"));
  }

  public boolean scrollWrapY() {
    return Boolean.parseBoolean(mapProperties.getProperty(PROPERTY_MAP_SCROLLWRAPY, "false"));
  }

  private Map<String, Image> territoryNameImages() {
    if (!loader.hasPath("territoryNames/")) {
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
    // try first file names that have underscores instead of spaces
    final String normalizedName = imageName.replace(' ', '_');
    return loader
        .loadImage(constructTerritoryNameImagePath(normalizedName))
        .or(() -> loader.loadImage(constructTerritoryNameImagePath(imageName)));
  }

  private static String constructTerritoryNameImagePath(final String baseName) {
    return "territoryNames/" + baseName + ".png";
  }

  private Map<Image, List<Point>> loadDecorations() throws IOException {
    return readOptionalPointsOneToMany(DECORATIONS_FILE).entrySet().stream()
        .map(entry -> Map.entry(loader.loadImage("misc/" + entry.getKey()), entry.getValue()))
        .filter(entry -> entry.getKey().isPresent())
        .collect(Collectors.toMap(entry -> entry.getKey().orElseThrow(), Entry::getValue));
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
      log.error("Failed to parse map property: " + name, e);
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
    return Optional.ofNullable(
        getColorProperty(PROPERTY_UNITS_TRANSFORM_COLOR_PREFIX + playerName));
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

  public boolean useNationConvoyFlags() {
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
   * @throws IllegalArgumentException If the property value does not represent a valid color.
   */
  private Color getColorProperty(final String propertiesKey, final Color defaultColor) {
    return Optional.ofNullable(mapProperties.getProperty(propertiesKey))
        .map(ColorUtils::fromHexString)
        .orElse(defaultColor);
  }

  /** Returns the color associated with the player named {@code playerName}. */
  public Color getPlayerColor(final String playerName) {
    return playerColors.getPlayerColor(playerName);
  }

  /** returns the color for impassable territories. */
  public Color impassableColor() {
    return playerColors.getImpassableColor();
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
   * @param territoryName Name of the territory, where we will be checking for any contained
   *     'island' territories.
   * @return Empty if the parameter is not a sea territory or contains no 'islands', otherwise
   *     returns any 'islands' contained within the given territory.
   */
  public Set<Polygon> getContainedTerritoryPolygons(final String territoryName) {
    return contains.getOrDefault(territoryName, Set.of()).stream()
        .map(this::getPolygons)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  public void verify(final GameState data) {
    verifyKeys(data, centers.keySet(), "centers");
    verifyKeys(data, polys.keySet(), "polygons");
    verifyKeys(data, place.keySet(), "place");
  }

  private static void verifyKeys(
      final GameState data, final Set<String> keys, final String dataTypeForErrorMessage) {
    final StringBuilder errors = new StringBuilder();

    // This block ignores mismatched territory data and the result of removing
    // from the iterator is we will wind up using the correct territory name.
    // Without this the engine becomes extremely strict about territory naming.
    // See: https://github.com/triplea-game/triplea/issues/7386
    final Iterator<String> iter = keys.iterator();
    while (iter.hasNext()) {
      final String name = iter.next();
      final Territory terr = data.getMap().getTerritory(name).orElse(null);
      // allow loading saved games with missing territories; just ignore them
      if (terr == null) {
        iter.remove();
      }
    }
    data.getMap().getTerritories().stream()
        .map(Territory::getName)
        .filter(territoryName -> !keys.contains(territoryName))
        .forEach(
            territoryName -> {
              errors
                  .append("No data of type ")
                  .append(dataTypeForErrorMessage)
                  .append(" for territory: ")
                  .append(territoryName)
                  .append("\n");
            });
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
  public @Nullable String getTerritoryAt(final double x, final double y) {
    // try to find a land territory.
    // sea zones often surround a land territory
    int smallestArea = Integer.MAX_VALUE;
    @Nullable String closestMatch = null;
    for (final String name : polys.keySet()) {
      final Collection<Polygon> polygons = polys.get(name);
      for (final Polygon poly : polygons) {
        if (poly.contains(x, y)) {
          Dimension size = poly.getBounds().getSize();
          int area = size.width * size.height;
          if (area < smallestArea) {
            closestMatch = name;
            smallestArea = area;
          }
        }
      }
    }
    return closestMatch;
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
          "No polygons found for: " + name + " All territories: " + this.polys.keySet());
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
      // left side, so that the bounds.x will be negative
      // this solves the issue of maps that have a territory where polygons were on both sides of
      // the map divide (so our bounds.x was 0, and our bounds.y would be the entire map width)
      // (when in fact it should actually be bounds.x = -10 or something, and bounds.width = 20 or
      // something)
      // we use map dimensions.width * 0.9 because the polygon may not actually touch the side of
      // the map (like if the territory borders are thick)
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

    return Optional.ofNullable(vcImage);
  }

  public Optional<Image> getBlockadeImage() {
    return Optional.ofNullable(blockadeImage);
  }

  public Optional<Image> getErrorImage() {
    return Optional.ofNullable(errorImage);
  }

  public Optional<Image> getWarningImage() {
    return Optional.ofNullable(warningImage);
  }

  public Map<String, Image> getTerritoryNameImages() {
    return Collections.unmodifiableMap(territoryNameImages);
  }

  public Map<Image, List<Point>> getDecorations() {
    return Collections.unmodifiableMap(decorations);
  }

  public List<Point> getTerritoryEffectPoints(final Territory territory) {
    List<Point> points = territoryEffects.get(territory.getName());
    if (points == null) {
      return List.of(getCenter(territory));
    }
    return points;
  }

  public Optional<Image> getTerritoryEffectImage(final String effectName) {
    return Optional.ofNullable(
        effectImages.computeIfAbsent(
            effectName,
            key -> {
              @NonNls String largeImageName = "territoryEffects/" + effectName + "_large.png";
              @NonNls String standardImageName = "territoryEffects/" + effectName + ".png";

              return loader
                  .loadImage(largeImageName)
                  .or(() -> loader.loadImage(standardImageName))
                  .orElse(null);
            }));
  }
}
