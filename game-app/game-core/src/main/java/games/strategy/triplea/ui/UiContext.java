package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.framework.LocalPlayers;
import games.strategy.engine.framework.map.file.system.loader.InstalledMapsListing;
import games.strategy.engine.framework.startup.launcher.MapNotFoundException;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.image.DiceImageFactory;
import games.strategy.triplea.image.FlagIconImageFactory;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.image.PuImageFactory;
import games.strategy.triplea.image.ResourceImageFactory;
import games.strategy.triplea.image.TerritoryEffectImageFactory;
import games.strategy.triplea.image.TileImageFactory;
import games.strategy.triplea.image.UnitIconImageFactory;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.image.UnitImageFactory.ImageKey;
import games.strategy.triplea.ui.mapdata.MapData;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.triplea.debug.error.reporting.StackTraceReportModel;
import org.triplea.java.concurrency.CountDownLatchHandler;
import org.triplea.sound.ClipPlayer;

/** A place to find images and map data for the ui. */
@Slf4j
public class UiContext {
  private static final String UNIT_SCALE_PREF = "UnitScale";
  private static final String MAP_SCALE_PREF = "MapScale";

  public static final double MAP_SCALE_MAX_VALUE = 2.0;

  private static final String ORIGINAL_SKIN_NAME = "Original";

  private static final String MAP_SKIN_PREF = "MapSkin";
  private static final String SHOW_END_OF_TURN_REPORT = "ShowEndOfTurnReport";
  private static final String SHOW_TRIGGERED_NOTIFICATIONS = "ShowTriggeredNotifications";
  private static final String SHOW_TRIGGERED_CHANCE_SUCCESSFUL = "ShowTriggeredChanceSuccessful";
  private static final String SHOW_TRIGGERED_CHANCE_FAILURE = "ShowTriggeredChanceFailure";
  @Getter private final ClipPlayer clipPlayer;

  @Getter protected MapData mapData;
  @Getter @Setter protected LocalPlayers localPlayers;

  @Getter protected double scale;
  @Getter private final String mapName;
  @Getter private final String skinName;
  @Getter private final Path mapLocation;
  @Getter private final ResourceLoader resourceLoader;
  @Getter private final TileImageFactory tileImageFactory = new TileImageFactory();
  @Getter private UnitImageFactory unitImageFactory;
  @Getter private final UnitIconImageFactory unitIconImageFactory;
  @Getter private final ResourceImageFactory resourceImageFactory = new ResourceImageFactory();
  @Getter private final TooltipProperties tooltipProperties;

  @Getter
  private final TerritoryEffectImageFactory territoryEffectImageFactory =
      new TerritoryEffectImageFactory();

  @Getter private final MapImage mapImage;
  @Getter private final FlagIconImageFactory flagImageFactory = new FlagIconImageFactory();
  @Getter private final DiceImageFactory diceImageFactory;
  @Getter private final PuImageFactory puImageFactory = new PuImageFactory();
  private boolean drawUnits = true;
  @Getter @Setter private boolean showUnitsInStatusBar = true;
  private boolean drawTerritoryEffects;

  @Getter private Cursor cursor;

  @Getter private boolean isShutDown = false;

  private final List<Window> windowsToCloseOnShutdown = new ArrayList<>();
  private final List<Runnable> activeToDeactivate = new ArrayList<>();
  private final CountDownLatchHandler latchesToCloseOnShutdown = new CountDownLatchHandler(false);

  UiContext(final GameData data) {
    if (data.getMapName() == null || data.getMapName().isBlank()) {
      throw new IllegalStateException("Map name property not set on game");
    }
    mapName = data.getMapName();
    StackTraceReportModel.setCurrentMapName(mapName + " / " + data.getGameName());

    List<Path> resourceLoadingPaths = new ArrayList<>();

    String preferredSkinPath =
        getPreferencesForMap(data.getMapName()) //
            .get(MAP_SKIN_PREF, null);
    skinName = preferredSkinPath;

    InstalledMapsListing.parseMapFiles()
        .findMapSkin(data.getMapName(), preferredSkinPath)
        .ifPresentOrElse(
            resourceLoadingPaths::add,
            () -> getPreferencesForMap(data.getMapName()).remove(MAP_SKIN_PREF));

    Path mapPath =
        InstalledMapsListing.searchAllMapsForMapName(data.getMapName())
            .orElseThrow(() -> new MapNotFoundException(data.getMapName()));
    mapLocation = mapPath;
    resourceLoadingPaths.add(mapPath);

    resourceLoader = new ResourceLoader(resourceLoadingPaths);
    mapData = new MapData(resourceLoader);
    diceImageFactory = new DiceImageFactory(resourceLoader, data.getDiceSides());
    final double unitScale =
        getPreferencesMapOrSkin(data.getMapName())
            .getDouble(UNIT_SCALE_PREF, mapData.getDefaultUnitScale());
    scale = getPreferencesMapOrSkin(data.getMapName()).getDouble(MAP_SCALE_PREF, 1.0);
    unitImageFactory = new UnitImageFactory(resourceLoader, unitScale, mapData);
    resourceImageFactory.setResourceLoader(resourceLoader);
    territoryEffectImageFactory.setResourceLoader(resourceLoader);
    unitIconImageFactory = new UnitIconImageFactory(data, resourceLoader);
    flagImageFactory.setResourceLoader(resourceLoader);
    puImageFactory.setResourceLoader(resourceLoader);
    tileImageFactory.setResourceLoader(resourceLoader);
    mapImage = new MapImage(resourceLoader);
    drawTerritoryEffects = mapData.useTerritoryEffectMarkers();
    // set the resource loader
    // this allows us to play sounds the map folder, rather than just default sounds
    clipPlayer = new ClipPlayer(resourceLoader);
    // load a new cursor
    cursor = Cursor.getDefaultCursor();
    final Toolkit toolkit = Toolkit.getDefaultToolkit();
    // URLs use "/" not "\"
    final URL cursorUrl = resourceLoader.getResource("misc/cursor.gif");
    if (cursorUrl != null) {
      try {
        final Image image = ImageIO.read(cursorUrl);
        if (image != null) {
          final Point hotSpot =
              new Point(mapData.getMapCursorHotspotX(), mapData.getMapCursorHotspotY());
          cursor = toolkit.createCustomCursor(image, hotSpot, data.getGameName() + " Cursor");
        }
      } catch (final Exception e) {
        log.error("Failed to create cursor from: " + cursorUrl, e);
      }
    }
    tooltipProperties = new TooltipProperties(this);
  }

  public JLabel newUnitImageLabel(final ImageKey imageKey) {
    final JLabel label = new JLabel(getUnitImageFactory().getIcon(imageKey));
    MapUnitTooltipManager.setUnitTooltip(label, imageKey.getType(), imageKey.getPlayer(), 1, this);
    return label;
  }

  JLabel newUnitImageLabel(final UnitType type, final GamePlayer player) {
    return newUnitImageLabel(ImageKey.builder().type(type).player(player).build());
  }

  public void shutDown() {
    synchronized (this) {
      if (isShutDown) {
        return;
      }
      isShutDown = true;
      latchesToCloseOnShutdown.shutDown();
      for (final Window window : windowsToCloseOnShutdown) {
        closeWindow(window);
      }
      for (final Runnable actor : activeToDeactivate) {
        runHook(actor);
      }
      activeToDeactivate.clear();
      windowsToCloseOnShutdown.clear();
      unitImageFactory.deleteTempFiles();
    }
    StackTraceReportModel.setCurrentMapName(null);
    resourceLoader.close();
  }

  public boolean getShowUnits() {
    return drawUnits;
  }

  public void setShowUnits(final boolean showUnits) {
    drawUnits = showUnits;
  }

  public void setShowTerritoryEffects(final boolean showTerritoryEffects) {
    drawTerritoryEffects = showTerritoryEffects;
  }

  public boolean getShowTerritoryEffects() {
    return drawTerritoryEffects;
  }

  public void setUnitScaleFactor(final double scaleFactor) {
    unitImageFactory = unitImageFactory.withScaleFactor(scaleFactor);
    final Preferences prefs =
        getPreferencesMapOrSkin(Optional.ofNullable(skinName).orElse(mapName));
    prefs.putDouble(UNIT_SCALE_PREF, scaleFactor);
    try {
      prefs.flush();
    } catch (final BackingStoreException e) {
      log.error("Failed to flush preferences: " + prefs.absolutePath(), e);
    }
  }

  public void setScale(final double scale) {
    this.scale = scale;
    final Preferences prefs =
        getPreferencesMapOrSkin(Optional.ofNullable(skinName).orElse(mapName));
    prefs.putDouble(MAP_SCALE_PREF, scale);
    try {
      prefs.flush();
    } catch (final BackingStoreException e) {
      log.error("Failed to flush preferences: " + prefs.absolutePath(), e);
    }
  }

  /** Get the preferences for the map. */
  private static Preferences getPreferencesForMap(final String mapName) {
    return Preferences.userNodeForPackage(UiContext.class).node(mapName);
  }

  /** Get the preferences for the map or map skin. */
  private static Preferences getPreferencesMapOrSkin(final String mapDir) {
    return Preferences.userNodeForPackage(UiContext.class).node(mapDir);
  }

  public static UiContext changeMapSkin(GameData gameData, String skinName) {
    final Preferences prefs = getPreferencesForMap(gameData.getMapName());

    if (skinName.equals(ORIGINAL_SKIN_NAME)) {
      prefs.put(MAP_SKIN_PREF, skinName);
    } else {
      prefs.remove(MAP_SKIN_PREF);
    }
    try {
      prefs.flush();
    } catch (final BackingStoreException e) {
      log.error("Failed to flush preferences: " + prefs.absolutePath(), e);
    }
    UiContext uiContext = new UiContext(gameData);
    uiContext.getMapData().verify(gameData);
    return uiContext;
  }

  public void removeShutdownHook(final Runnable hook) {
    if (isShutDown) {
      return;
    }
    synchronized (this) {
      activeToDeactivate.remove(hook);
    }
  }

  /** Add a latch that will be released when the game shuts down. */
  public void addShutdownHook(final Runnable hook) {
    if (isShutDown) {
      runHook(hook);
      return;
    }
    synchronized (this) {
      if (isShutDown) {
        runHook(hook);
        return;
      }
      activeToDeactivate.add(hook);
    }
  }

  /**
   * Utility method to wait for user input, that can optionally be aborted when the UiContext shuts
   * down.
   *
   * @param future The future to wait for.
   * @param <T> The return type of the future.
   * @return An optional result in case the future completes. In case the future completes with
   *     null, is completed exceptionally or aborted in any other way, the optional will be empty.
   */
  public <T> Optional<T> awaitUserInput(final CompletableFuture<T> future) {
    final Runnable rejectionCallback =
        () -> future.completeExceptionally(new RuntimeException("Shutting down"));
    try {
      addShutdownHook(rejectionCallback);
      return Optional.ofNullable(future.get());
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (final ExecutionException e) {
      log.info("UiContext shut down before supplying result", e);
    } finally {
      removeShutdownHook(rejectionCallback);
    }
    return Optional.empty();
  }

  /** Add a latch that will be released when the game shuts down. */
  public void addShutdownLatch(final CountDownLatch latch) {
    latchesToCloseOnShutdown.addShutdownLatch(latch);
  }

  public void removeShutdownLatch(final CountDownLatch latch) {
    latchesToCloseOnShutdown.removeShutdownLatch(latch);
  }

  public CountDownLatchHandler getCountDownLatchHandler() {
    return latchesToCloseOnShutdown;
  }

  /** Add a latch that will be released when the game shuts down. */
  public void addShutdownWindow(final Window window) {
    if (isShutDown) {
      closeWindow(window);
      return;
    }
    synchronized (this) {
      windowsToCloseOnShutdown.add(window);
    }
  }

  private static void closeWindow(final Window window) {
    window.setVisible(false);
    // Having dispose run on anything but the Swing Event Dispatch Thread is very dangerous.
    // This is because dispose will call invokeAndWait if it is not on this thread already.
    // If you are calling this method while holding a lock on an object, while the EDT is separately
    // waiting for that lock, then you have a deadlock.
    // A real life example: player disconnects while you have the battle calc open.
    // Non-EDT thread does shutdown on IGame and UiContext, causing btl calc to shut down,
    // which calls the window closed event on the EDT, and waits for the lock on UiContext to
    // removeShutdownWindow, meanwhile our non-EDT tries to dispose the battle panel, which requires
    // the EDT with a invokeAndWait, resulting in a deadlock.
    SwingUtilities.invokeLater(window::dispose);
  }

  public void removeShutdownWindow(final Window window) {
    if (isShutDown) {
      return;
    }
    synchronized (this) {
      windowsToCloseOnShutdown.remove(window);
    }
  }

  @Getter
  public class MapSkin {
    private final boolean currentSkin;
    private final String skinName;

    public MapSkin(String skinName) {
      this.skinName = skinName;
      currentSkin = skinName.equals(UiContext.this.skinName);
    }
  }

  public List<MapSkin> getSkins(final String mapName) {
    List<MapSkin> skins = new ArrayList<>();
    skins.add(new MapSkin(ORIGINAL_SKIN_NAME));

    InstalledMapsListing.parseMapFiles().findInstalledMapByName(mapName).stream()
        .flatMap(installedMap -> installedMap.getSkinNames().stream())
        .sorted()
        .map(MapSkin::new)
        .forEach(skins::add);
    return skins;
  }

  private static void runHook(final Runnable hook) {
    try {
      hook.run();
    } catch (final RuntimeException e) {
      log.error("Failed to deactivate actor", e);
    }
  }

  public boolean getShowEndOfTurnReport() {
    final Preferences prefs = Preferences.userNodeForPackage(UiContext.class);
    return prefs.getBoolean(SHOW_END_OF_TURN_REPORT, true);
  }

  public void setShowEndOfTurnReport(final boolean value) {
    final Preferences prefs = Preferences.userNodeForPackage(UiContext.class);
    prefs.putBoolean(SHOW_END_OF_TURN_REPORT, value);
    try {
      prefs.flush();
    } catch (final BackingStoreException ex) {
      log.error("Failed to flush preferences: " + prefs.absolutePath(), ex);
    }
  }

  public boolean getShowTriggeredNotifications() {
    final Preferences prefs = Preferences.userNodeForPackage(UiContext.class);
    return prefs.getBoolean(SHOW_TRIGGERED_NOTIFICATIONS, true);
  }

  public void setShowTriggeredNotifications(final boolean value) {
    final Preferences prefs = Preferences.userNodeForPackage(UiContext.class);
    prefs.putBoolean(SHOW_TRIGGERED_NOTIFICATIONS, value);
    try {
      prefs.flush();
    } catch (final BackingStoreException ex) {
      log.error("Failed to flush preferences: " + prefs.absolutePath(), ex);
    }
  }

  public boolean getShowTriggerChanceSuccessful() {
    final Preferences prefs = Preferences.userNodeForPackage(UiContext.class);
    return prefs.getBoolean(SHOW_TRIGGERED_CHANCE_SUCCESSFUL, true);
  }

  public void setShowTriggerChanceSuccessful(final boolean value) {
    final Preferences prefs = Preferences.userNodeForPackage(UiContext.class);
    prefs.putBoolean(SHOW_TRIGGERED_CHANCE_SUCCESSFUL, value);
    try {
      prefs.flush();
    } catch (final BackingStoreException ex) {
      log.error("Failed to flush preferences: " + prefs.absolutePath(), ex);
    }
  }

  public boolean getShowTriggerChanceFailure() {
    final Preferences prefs = Preferences.userNodeForPackage(UiContext.class);
    return prefs.getBoolean(SHOW_TRIGGERED_CHANCE_FAILURE, true);
  }

  public void setShowTriggerChanceFailure(final boolean value) {
    final Preferences prefs = Preferences.userNodeForPackage(UiContext.class);
    prefs.putBoolean(SHOW_TRIGGERED_CHANCE_FAILURE, value);
    try {
      prefs.flush();
    } catch (final BackingStoreException ex) {
      log.error("Failed to flush preferences: " + prefs.absolutePath(), ex);
    }
  }
}
