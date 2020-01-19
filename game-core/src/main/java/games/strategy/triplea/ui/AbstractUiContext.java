package games.strategy.triplea.ui;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.LocalPlayers;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ResourceLoader;
import java.awt.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.triplea.io.FileUtils;
import org.triplea.java.concurrency.CountDownLatchHandler;

/**
 * Superclass for implementations of {@link UiContext} that provides operations common to both
 * headed and headless environments.
 */
@Log
public abstract class AbstractUiContext implements UiContext {

  @Getter protected static String mapDir;
  @Getter protected static ResourceLoader resourceLoader;

  static final String UNIT_SCALE_PREF = "UnitScale";
  static final String MAP_SCALE_PREF = "MapScale";

  private static final String MAP_SKIN_PREF = "MapSkin";
  private static final String SHOW_END_OF_TURN_REPORT = "ShowEndOfTurnReport";
  private static final String SHOW_TRIGGERED_NOTIFICATIONS = "ShowTriggeredNotifications";
  private static final String SHOW_TRIGGERED_CHANCE_SUCCESSFUL = "ShowTriggeredChanceSuccessful";
  private static final String SHOW_TRIGGERED_CHANCE_FAILURE = "ShowTriggeredChanceFailure";

  @Getter(onMethod_ = {@Override})
  @Setter(onMethod_ = {@Override})
  protected LocalPlayers localPlayers;

  @Getter(onMethod_ = {@Override})
  protected double scale = 1;

  @Getter(onMethod_ = {@Override})
  private boolean isShutDown = false;

  private final List<Window> windowsToCloseOnShutdown = new ArrayList<>();
  private final List<Runnable> activeToDeactivate = new ArrayList<>();
  private final CountDownLatchHandler latchesToCloseOnShutdown = new CountDownLatchHandler(false);

  @Override
  public void setScale(final double scale) {
    this.scale = scale;
    final Preferences prefs = getPreferencesMapOrSkin(getMapDir());
    prefs.putDouble(MAP_SCALE_PREF, scale);
    try {
      prefs.flush();
    } catch (final BackingStoreException e) {
      log.log(Level.SEVERE, "Failed to flush preferences: " + prefs.absolutePath(), e);
    }
  }

  /** Get the preferences for the map. */
  private static Preferences getPreferencesForMap(final String mapName) {
    return Preferences.userNodeForPackage(AbstractUiContext.class).node(mapName);
  }

  /** Get the preferences for the map or map skin. */
  static Preferences getPreferencesMapOrSkin(final String mapDir) {
    return Preferences.userNodeForPackage(AbstractUiContext.class).node(mapDir);
  }

  private static String getDefaultMapDir(final GameData data) {
    final String mapName = (String) data.getProperties().get(Constants.MAP_NAME);
    if (mapName == null || mapName.trim().length() == 0) {
      throw new IllegalStateException("Map name property not set on game");
    }
    final Preferences prefs = getPreferencesForMap(mapName);
    final String mapDir = prefs.get(MAP_SKIN_PREF, mapName);
    // check for existence
    try {
      ResourceLoader.getMapResourceLoader(mapDir).close();
    } catch (final RuntimeException re) {
      // an error, clear the skin
      prefs.remove(MAP_SKIN_PREF);
      // return the default
      return mapName;
    }
    return mapDir;
  }

  @Override
  public void setDefaultMapDir(final GameData data) {
    internalSetMapDir(getDefaultMapDir(data), data);
  }

  @Override
  public void setMapDir(final GameData data, final String mapDir) {
    internalSetMapDir(mapDir, data);
    this.getMapData().verify(data);
    // set the default after internal succeeds, if an error is thrown we don't want to persist it
    final String mapName = (String) data.getProperties().get(Constants.MAP_NAME);
    final Preferences prefs = getPreferencesForMap(mapName);
    prefs.put(MAP_SKIN_PREF, mapDir);
    try {
      prefs.flush();
    } catch (final BackingStoreException e) {
      log.log(Level.SEVERE, "Failed to flush preferences: " + prefs.absolutePath(), e);
    }
  }

  protected abstract void internalSetMapDir(String dir, GameData data);

  @Override
  public void removeShutdownHook(final Runnable hook) {
    if (isShutDown) {
      return;
    }
    synchronized (this) {
      activeToDeactivate.remove(hook);
    }
  }

  /** Add a latch that will be released when the game shuts down. */
  @Override
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

  @Override
  public <T> Optional<T> awaitUserInput(final CompletableFuture<T> future) {
    final Runnable rejectionCallback =
        () -> future.completeExceptionally(new RuntimeException("Shutting down"));
    try {
      addShutdownHook(rejectionCallback);
      return Optional.ofNullable(future.get());
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (final ExecutionException e) {
      log.log(Level.INFO, "UiContext shut down before supplying result", e);
    } finally {
      removeShutdownHook(rejectionCallback);
    }
    return Optional.empty();
  }

  /** Add a latch that will be released when the game shuts down. */
  @Override
  public void addShutdownLatch(final CountDownLatch latch) {
    latchesToCloseOnShutdown.addShutdownLatch(latch);
  }

  @Override
  public void removeShutdownLatch(final CountDownLatch latch) {
    latchesToCloseOnShutdown.removeShutdownLatch(latch);
  }

  @Override
  public CountDownLatchHandler getCountDownLatchHandler() {
    return latchesToCloseOnShutdown;
  }

  /** Add a latch that will be released when the game shuts down. */
  @Override
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
    // Non-EDT thread does shutdown on IGame and HeadedUiContext, causing btl calc to shutdown,
    // which calls the
    // window closed event on the EDT, and waits for the lock on HeadedUiContext to
    // removeShutdownWindow, meanwhile
    // our non-EDT tries to dispose the battle panel, which requires the EDT with a invokeAndWait,
    // resulting in a
    // deadlock.
    SwingUtilities.invokeLater(window::dispose);
  }

  @Override
  public void removeShutdownWindow(final Window window) {
    if (isShutDown) {
      return;
    }
    synchronized (this) {
      windowsToCloseOnShutdown.remove(window);
    }
  }

  @Override
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
    }
  }

  /** returns the map skins for the game data. returns is a map of display-name -> map directory */
  public static Map<String, String> getSkins(final GameData data) {
    final String mapName = data.getProperties().get(Constants.MAP_NAME).toString();
    final Map<String, String> skinsByDisplayName = new LinkedHashMap<>();
    skinsByDisplayName.put("Original", mapName);
    skinsByDisplayName.putAll(getSkins(mapName));
    return skinsByDisplayName;
  }

  private static Map<String, String> getSkins(final String mapName) {
    final Map<String, String> skinsByDisplayName = new HashMap<>();
    for (final File f : FileUtils.listFiles(ClientFileSystemHelper.getUserMapsFolder())) {
      if (mapSkinNameMatchesMapName(f.getName(), mapName)) {
        final String displayName =
            f.getName().replace(mapName + "-", "").replace("-master", "").replace(".zip", "");
        skinsByDisplayName.put(displayName, f.getName());
      }
    }
    return skinsByDisplayName;
  }

  private static boolean mapSkinNameMatchesMapName(final String mapSkin, final String mapName) {
    return mapSkin.startsWith(mapName)
        && mapSkin.toLowerCase().contains("skin")
        && mapSkin.contains("-")
        && !mapSkin.endsWith("properties");
  }

  private static void runHook(final Runnable hook) {
    try {
      hook.run();
    } catch (final RuntimeException e) {
      log.log(Level.SEVERE, "Failed to deactivate actor", e);
    }
  }

  @Override
  public boolean getShowEndOfTurnReport() {
    final Preferences prefs = Preferences.userNodeForPackage(AbstractUiContext.class);
    return prefs.getBoolean(SHOW_END_OF_TURN_REPORT, true);
  }

  @Override
  public void setShowEndOfTurnReport(final boolean value) {
    final Preferences prefs = Preferences.userNodeForPackage(AbstractUiContext.class);
    prefs.putBoolean(SHOW_END_OF_TURN_REPORT, value);
    try {
      prefs.flush();
    } catch (final BackingStoreException ex) {
      log.log(Level.SEVERE, "Failed to flush preferences: " + prefs.absolutePath(), ex);
    }
  }

  @Override
  public boolean getShowTriggeredNotifications() {
    final Preferences prefs = Preferences.userNodeForPackage(AbstractUiContext.class);
    return prefs.getBoolean(SHOW_TRIGGERED_NOTIFICATIONS, true);
  }

  @Override
  public void setShowTriggeredNotifications(final boolean value) {
    final Preferences prefs = Preferences.userNodeForPackage(AbstractUiContext.class);
    prefs.putBoolean(SHOW_TRIGGERED_NOTIFICATIONS, value);
    try {
      prefs.flush();
    } catch (final BackingStoreException ex) {
      log.log(Level.SEVERE, "Failed to flush preferences: " + prefs.absolutePath(), ex);
    }
  }

  @Override
  public boolean getShowTriggerChanceSuccessful() {
    final Preferences prefs = Preferences.userNodeForPackage(AbstractUiContext.class);
    return prefs.getBoolean(SHOW_TRIGGERED_CHANCE_SUCCESSFUL, true);
  }

  @Override
  public void setShowTriggerChanceSuccessful(final boolean value) {
    final Preferences prefs = Preferences.userNodeForPackage(AbstractUiContext.class);
    prefs.putBoolean(SHOW_TRIGGERED_CHANCE_SUCCESSFUL, value);
    try {
      prefs.flush();
    } catch (final BackingStoreException ex) {
      log.log(Level.SEVERE, "Failed to flush preferences: " + prefs.absolutePath(), ex);
    }
  }

  @Override
  public boolean getShowTriggerChanceFailure() {
    final Preferences prefs = Preferences.userNodeForPackage(AbstractUiContext.class);
    return prefs.getBoolean(SHOW_TRIGGERED_CHANCE_FAILURE, true);
  }

  @Override
  public void setShowTriggerChanceFailure(final boolean value) {
    final Preferences prefs = Preferences.userNodeForPackage(AbstractUiContext.class);
    prefs.putBoolean(SHOW_TRIGGERED_CHANCE_FAILURE, value);
    try {
      prefs.flush();
    } catch (final BackingStoreException ex) {
      log.log(Level.SEVERE, "Failed to flush preferences: " + prefs.absolutePath(), ex);
    }
  }
}
