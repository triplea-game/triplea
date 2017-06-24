package games.strategy.triplea.ui;

import java.awt.BorderLayout;
import java.awt.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientContext;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.LocalPlayers;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ResourceLoader;
import games.strategy.util.CountDownLatchHandler;


public abstract class AbstractUIContext implements IUIContext {

  protected static final String UNIT_SCALE_PREF = "UnitScale";
  protected static final String MAP_SKIN_PREF = "MapSkin";
  protected static final String MAP_SCALE_PREF = "MapScale";
  protected static final Logger s_logger = Logger.getLogger(AbstractUIContext.class.getName());
  protected static String m_mapDir;
  protected static final String LOCK_MAP = "LockMap";
  protected static final String SHOW_END_OF_TURN_REPORT = "ShowEndOfTurnReport";
  protected static final String SHOW_TRIGGERED_NOTIFICATIONS = "ShowTriggeredNotifications";
  protected static final String SHOW_TRIGGERED_CHANCE_SUCCESSFUL = "ShowTriggeredChanceSuccessful";
  protected static final String SHOW_TRIGGERED_CHANCE_FAILURE = "ShowTriggeredChanceFailure";
  protected static final String SHOW_BATTLES_BETWEEN_AIS = "ShowBattlesBetweenAIs";
  protected static ResourceLoader m_resourceLoader;
  // instance
  protected boolean m_isShutDown = false;
  protected final List<Window> m_windowsToCloseOnShutdown = new ArrayList<>();
  protected final List<Active> m_activeToDeactivate = new ArrayList<>();
  protected final CountDownLatchHandler m_latchesToCloseOnShutdown = new CountDownLatchHandler(false);
  protected LocalPlayers m_localPlayers;
  protected double m_scale = 1;

  public static ResourceLoader getResourceLoader() {
    return m_resourceLoader;
  }

  public static int getAIPauseDuration() {
    return ClientContext.aiSettings().getAiPauseDuration();
  }

  public static void setAIPauseDuration(final int value) {
    ClientContext.aiSettings().setAiPauseDuration(String.valueOf(value));
  }

  @Override
  public double getScale() {
    return m_scale;
  }

  @Override
  public void setScale(final double scale) {
    m_scale = scale;
    final Preferences prefs = getPreferencesMapOrSkin(getMapDir());
    prefs.putDouble(MAP_SCALE_PREF, scale);
    try {
      prefs.flush();
    } catch (final BackingStoreException e) {
      ClientLogger.logQuietly(e);
    }
  }

  /**
   * Get the preferences for the map.
   */
  protected static Preferences getPreferencesForMap(final String mapName) {
    return Preferences.userNodeForPackage(AbstractUIContext.class).node(mapName);
  }

  /**
   * Get the preferences for the map or map skin.
   */
  protected static Preferences getPreferencesMapOrSkin(final String mapDir) {
    return Preferences.userNodeForPackage(AbstractUIContext.class).node(mapDir);
  }

  protected static String getDefaultMapDir(final GameData data) {
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
    // set the default after internal succeeds, if an error is thrown
    // we don't want to persist it
    final String mapName = (String) data.getProperties().get(Constants.MAP_NAME);
    final Preferences prefs = getPreferencesForMap(mapName);
    prefs.put(MAP_SKIN_PREF, mapDir);
    try {
      prefs.flush();
    } catch (final BackingStoreException e) {
      ClientLogger.logQuietly(e);
    }
  }

  protected abstract void internalSetMapDir(final String dir, final GameData data);

  public static String getMapDir() {
    return m_mapDir;
  }

  @Override
  public void removeActive(final Active actor) {
    if (m_isShutDown) {
      return;
    }
    synchronized (this) {
      m_activeToDeactivate.remove(actor);
    }
  }

  /**
   * Add a latch that will be released when the game shuts down.
   */
  @Override
  public void addActive(final Active actor) {
    if (m_isShutDown) {
      closeActor(actor);
      return;
    }
    synchronized (this) {
      if (m_isShutDown) {
        closeActor(actor);
        return;
      }
      m_activeToDeactivate.add(actor);
    }
  }

  /**
   * Add a latch that will be released when the game shuts down.
   */
  @Override
  public void addShutdownLatch(final CountDownLatch latch) {
    m_latchesToCloseOnShutdown.addShutdownLatch(latch);
  }

  @Override
  public void removeShutdownLatch(final CountDownLatch latch) {
    m_latchesToCloseOnShutdown.removeShutdownLatch(latch);
  }

  @Override
  public CountDownLatchHandler getCountDownLatchHandler() {
    return m_latchesToCloseOnShutdown;
  }

  /**
   * Add a latch that will be released when the game shuts down.
   */
  @Override
  public void addShutdownWindow(final Window window) {
    if (m_isShutDown) {
      closeWindow(window);
      return;
    }
    synchronized (this) {
      if (m_isShutDown) {
        closeWindow(window);
        return;
      }
      m_windowsToCloseOnShutdown.add(window);
    }
  }

  protected static void closeWindow(final Window window) {
    window.setVisible(false);
    SwingUtilities.invokeLater(() -> {
      // Having dispose run on anything but the Swing Event Dispatch Thread is very dangerous.
      // This is because dispose will call invokeAndWait if it is not on this thread already.
      // If you are calling this method while holding a lock on an object, while the EDT is separately
      // waiting for that lock, then you have a deadlock.
      // A real life example: player disconnects while you have the battle calc open.
      // Non-EDT thread does shutdown on IGame and UIContext, causing btl calc to shutdown, which calls the
      // window closed event on the EDT, and waits for the lock on UIContext to removeShutdownWindow, meanwhile
      // our non-EDT tries to dispose the battle panel, which requires the EDT with a invokeAndWait, resulting in a
      // deadlock.
      window.dispose();
      // there is a bug in java (1.50._06 for linux at least)
      // where frames are not garbage collected.
      // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6364875
      // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6368950
      // so remove all references to everything
      // to minimize the damage
      if (window instanceof JFrame) {
        final JFrame frame = ((JFrame) window);
        final JMenuBar menu = frame.getJMenuBar();
        if (menu != null) {
          while (menu.getMenuCount() > 0) {
            menu.remove(0);
          }
        }
        frame.setMenuBar(null);
        frame.setJMenuBar(null);
        frame.getRootPane().removeAll();
        frame.getRootPane().setJMenuBar(null);
        frame.getContentPane().removeAll();
        frame.getContentPane().setLayout(new BorderLayout());
        frame.setContentPane(new JPanel());
        frame.setIconImage(null);
        clearInputMap(frame.getRootPane());
      }
    });
  }

  protected static void clearInputMap(final JComponent c) {
    c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).clear();
    c.getInputMap(JComponent.WHEN_FOCUSED).clear();
    c.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).clear();
    c.getActionMap().clear();
  }

  @Override
  public void removeShutdownWindow(final Window window) {
    if (m_isShutDown) {
      return;
    }
    synchronized (this) {
      m_windowsToCloseOnShutdown.remove(window);
    }
  }

  @Override
  public boolean isShutDown() {
    return m_isShutDown;
  }

  @Override
  public void shutDown() {
    synchronized (this) {
      if (m_isShutDown) {
        return;
      }
      m_isShutDown = true;
      m_latchesToCloseOnShutdown.shutDown();
      for (final Window window : m_windowsToCloseOnShutdown) {
        closeWindow(window);
      }
      for (final Active actor : m_activeToDeactivate) {
        closeActor(actor);
      }
      m_activeToDeactivate.clear();
      m_windowsToCloseOnShutdown.clear();
    }
  }

  /**
   * returns the map skins for the game data.
   * returns is a map of display-name -> map directory
   */
  public static Map<String, String> getSkins(final GameData data) {
    final String mapName = data.getProperties().get(Constants.MAP_NAME).toString();
    final Map<String, String> rVal = new LinkedHashMap<>();
    rVal.put("Original", mapName);
    rVal.putAll(getSkins(mapName));
    return rVal;
  }

  private static Map<String, String> getSkins(final String mapName) {
    final Map<String, String> rVal = new HashMap<>();
    final File[] files = ClientFileSystemHelper.getUserMapsFolder().listFiles();
    if (files == null) {
      return rVal;
    }
    for (final File f : files) {
      if (mapSkinNameMatchesMapName(f.getName(), mapName)) {
        final String displayName = f.getName().replace(mapName + "-", "").replace("-master", "").replace(".zip", "");
        rVal.put(displayName, f.getName());
      }
    }
    return rVal;
  }

  private static boolean mapSkinNameMatchesMapName(final String mapSkin, final String mapName) {
    return mapSkin.startsWith(mapName) && mapSkin.toLowerCase().contains("skin") && mapSkin.contains("-")
        && !mapSkin.endsWith("properties");
  }

  private static void closeActor(final Active actor) {
    try {
      actor.deactivate();
    } catch (final RuntimeException e) {
      ClientLogger.logQuietly(e);
    }
  }

  @Override
  public boolean getLockMap() {
    final Preferences prefs = Preferences.userNodeForPackage(AbstractUIContext.class);
    return prefs.getBoolean(LOCK_MAP, false);
  }

  @Override
  public void setLockMap(final boolean lockMap) {
    final Preferences prefs = Preferences.userNodeForPackage(AbstractUIContext.class);
    prefs.putBoolean(LOCK_MAP, lockMap);
    try {
      prefs.flush();
    } catch (final BackingStoreException ex) {
      ex.printStackTrace();
    }
  }

  @Override
  public boolean getShowEndOfTurnReport() {
    final Preferences prefs = Preferences.userNodeForPackage(AbstractUIContext.class);
    return prefs.getBoolean(SHOW_END_OF_TURN_REPORT, true);
  }

  @Override
  public void setShowEndOfTurnReport(final boolean value) {
    final Preferences prefs = Preferences.userNodeForPackage(AbstractUIContext.class);
    prefs.putBoolean(SHOW_END_OF_TURN_REPORT, value);
    try {
      prefs.flush();
    } catch (final BackingStoreException ex) {
      ClientLogger.logQuietly(ex);
    }
  }

  @Override
  public boolean getShowTriggeredNotifications() {
    final Preferences prefs = Preferences.userNodeForPackage(AbstractUIContext.class);
    return prefs.getBoolean(SHOW_TRIGGERED_NOTIFICATIONS, true);
  }

  @Override
  public void setShowTriggeredNotifications(final boolean value) {
    final Preferences prefs = Preferences.userNodeForPackage(AbstractUIContext.class);
    prefs.putBoolean(SHOW_TRIGGERED_NOTIFICATIONS, value);
    try {
      prefs.flush();
    } catch (final BackingStoreException ex) {
      ex.printStackTrace();
    }
  }

  @Override
  public boolean getShowTriggerChanceSuccessful() {
    final Preferences prefs = Preferences.userNodeForPackage(AbstractUIContext.class);
    return prefs.getBoolean(SHOW_TRIGGERED_CHANCE_SUCCESSFUL, true);
  }

  @Override
  public void setShowTriggerChanceSuccessful(final boolean value) {
    final Preferences prefs = Preferences.userNodeForPackage(AbstractUIContext.class);
    prefs.putBoolean(SHOW_TRIGGERED_CHANCE_SUCCESSFUL, value);
    try {
      prefs.flush();
    } catch (final BackingStoreException ex) {
      ClientLogger.logQuietly(ex);
    }
  }

  @Override
  public boolean getShowTriggerChanceFailure() {
    final Preferences prefs = Preferences.userNodeForPackage(AbstractUIContext.class);
    return prefs.getBoolean(SHOW_TRIGGERED_CHANCE_FAILURE, true);
  }

  @Override
  public void setShowTriggerChanceFailure(final boolean value) {
    final Preferences prefs = Preferences.userNodeForPackage(AbstractUIContext.class);
    prefs.putBoolean(SHOW_TRIGGERED_CHANCE_FAILURE, value);
    try {
      prefs.flush();
    } catch (final BackingStoreException ex) {
      ClientLogger.logQuietly(ex);
    }
  }

  @Override
  public boolean getShowBattlesBetweenAIs() {
    return ClientContext.aiSettings().showBattlesBetweenAi();
  }

  @Override
  public void setShowBattlesBetweenAIs(final boolean showBattlesBetweenAi) {
    ClientContext.aiSettings().setShowBattlesBetweenAi(showBattlesBetweenAi);
  }

  @Override
  public LocalPlayers getLocalPlayers() {
    return m_localPlayers;
  }

  @Override
  public void setLocalPlayers(final LocalPlayers players) {
    m_localPlayers = players;
  }
}
