package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.LocalPlayers;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ResourceLoader;
import games.strategy.util.CountDownLatchHandler;

import java.awt.BorderLayout;
import java.awt.Window;
import java.io.File;
import java.util.ArrayList;
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

/**
 * Abstraction by veqryn.
 * 
 * @author veqryn
 * 
 */
public abstract class AbstractUIContext implements IUIContext
{
	// static
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
	protected static final String AI_PAUSE_DURATION = "AIPauseDuration";
	protected static ResourceLoader m_resourceLoader;
	// instance
	protected boolean m_isShutDown;
	protected final List<Window> m_windowsToCloseOnShutdown = new ArrayList<Window>();
	protected final List<Active> m_activeToDeactivate = new ArrayList<Active>();
	protected final CountDownLatchHandler m_latchesToCloseOnShutdown = new CountDownLatchHandler(false); // List<CountDownLatch> m_latchesToCloseOnShutdown = new ArrayList<CountDownLatch>();
	protected LocalPlayers m_localPlayers;
	protected double m_scale = 1;
	
	public static ResourceLoader getResourceLoader()
	{
		return m_resourceLoader;
	}
	
	public static int getAIPauseDuration()
	{
		final Preferences prefs = Preferences.userNodeForPackage(AbstractUIContext.class);
		return prefs.getInt(AI_PAUSE_DURATION, 700);
	}
	
	public static void setAIPauseDuration(final int value)
	{
		final Preferences prefs = Preferences.userNodeForPackage(AbstractUIContext.class);
		prefs.putInt(AI_PAUSE_DURATION, value);
		try
		{
			prefs.flush();
		} catch (final BackingStoreException ex)
		{
			ex.printStackTrace();
		}
	}
	
	public double getScale()
	{
		return m_scale;
	}
	
	public void setScale(final double scale)
	{
		m_scale = scale;
		// m_tileImageFactory.setScale(scale);
		final Preferences prefs = getPreferencesMapOrSkin(getMapDir());
		prefs.putDouble(MAP_SCALE_PREF, scale);
		try
		{
			prefs.flush();
		} catch (final BackingStoreException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Get the preferences for the map.
	 */
	protected static Preferences getPreferencesForMap(final String mapName)
	{
		return Preferences.userNodeForPackage(AbstractUIContext.class).node(mapName);
	}
	
	/**
	 * Get the preferences for the map or map skin
	 */
	protected static Preferences getPreferencesMapOrSkin(final String mapDir)
	{
		return Preferences.userNodeForPackage(AbstractUIContext.class).node(mapDir);
	}
	
	protected static String getDefaultMapDir(final GameData data)
	{
		final String mapName = (String) data.getProperties().get(Constants.MAP_NAME);
		if (mapName == null || mapName.trim().length() == 0)
		{
			throw new IllegalStateException("Map name property not set on game");
		}
		final Preferences prefs = getPreferencesForMap(mapName);
		final String mapDir = prefs.get(MAP_SKIN_PREF, mapName);
		// check for existence
		try
		{
			ResourceLoader.getMapResourceLoader(mapDir, false).close();
		} catch (final RuntimeException re)
		{
			// an error
			// clear the skin
			prefs.remove(MAP_SKIN_PREF);
			// return the default
			return mapName;
		}
		return mapDir;
	}
	
	public void setDefaultMapDir(final GameData data)
	{
		internalSetMapDir(getDefaultMapDir(data), data);
	}
	
	public void setMapDir(final GameData data, final String mapDir)
	{
		internalSetMapDir(mapDir, data);
		this.getMapData().verify(data);
		// set the default after internal succeeds, if an error is thrown
		// we don't want to persist it
		final String mapName = (String) data.getProperties().get(Constants.MAP_NAME);
		final Preferences prefs = getPreferencesForMap(mapName);
		prefs.put(MAP_SKIN_PREF, mapDir);
		try
		{
			prefs.flush();
		} catch (final BackingStoreException e)
		{
			e.printStackTrace();
		}
	}
	
	protected abstract void internalSetMapDir(final String dir, final GameData data);
	
	public static String getMapDir()
	{
		return m_mapDir;
	}
	
	public void removeActive(final Active actor)
	{
		synchronized (this)
		{
			// closeActor(actor);
			m_activeToDeactivate.remove(actor);
		}
	}
	
	/**
	 * Add a latch that will be released when the game shuts down.
	 */
	public void addActive(final Active actor)
	{
		synchronized (this)
		{
			if (m_isShutDown)
			{
				closeActor(actor);
				return;
			}
			m_activeToDeactivate.add(actor);
		}
	}
	
	/**
	 * Add a latch that will be released when the game shuts down.
	 */
	public void addShutdownLatch(final CountDownLatch latch)
	{
		m_latchesToCloseOnShutdown.addShutdownLatch(latch);
		/* synchronized (this)
		{
			if (m_isShutDown)
			{
				releaseLatch(latch);
				return;
			}
			m_latchesToCloseOnShutdown.add(latch);
		} */
	}
	
	public void removeShutdownLatch(final CountDownLatch latch)
	{
		m_latchesToCloseOnShutdown.removeShutdownLatch(latch);
		/* synchronized (this)
		{
			releaseLatch(latch);
			m_latchesToCloseOnShutdown.remove(latch);
		} */
	}
	
	public CountDownLatchHandler getCountDownLatchHandler()
	{
		return m_latchesToCloseOnShutdown;
	}
	
	/**
	 * Add a latch that will be released when the game shuts down.
	 */
	public void addShutdownWindow(final Window window)
	{
		synchronized (this)
		{
			if (m_isShutDown)
			{
				closeWindow(window);
				return;
			}
			m_windowsToCloseOnShutdown.add(window);
		}
	}
	
	protected static void closeWindow(final Window window)
	{
		window.setVisible(false);
		window.dispose();
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				// there is a bug in java (1.50._06 for linux at least)
				// where frames are not garbage collected.
				//
				// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6364875
				// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6368950
				//
				// so remove all references to everything
				// to minimize the damage
				if (window instanceof JFrame)
				{
					final JFrame frame = ((JFrame) window);
					final JMenuBar menu = frame.getJMenuBar();
					if (menu != null)
					{
						while (menu.getMenuCount() > 0)
							menu.remove(0);
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
			}
		});
	}
	
	protected static void clearInputMap(final JComponent c)
	{
		c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).clear();
		c.getInputMap(JComponent.WHEN_FOCUSED).clear();
		c.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).clear();
		c.getActionMap().clear();
	}
	
	public void removeShutdownWindow(final Window window)
	{
		synchronized (this)
		{
			// closeWindow(window);
			m_windowsToCloseOnShutdown.remove(window);
		}
	}
	
	/* protected void releaseLatch(final CountDownLatch latch)
	{
		while (latch.getCount() > 0)
		{
			latch.countDown();
		}
	} */
	
	public boolean isShutDown()
	{
		return m_isShutDown;
	}
	
	public void shutDown()
	{
		synchronized (this)
		{
			if (m_isShutDown)
				return;
			m_isShutDown = true;
		}
		m_latchesToCloseOnShutdown.shutDown();
		/* for (final CountDownLatch latch : m_latchesToCloseOnShutdown)
		{
			releaseLatch(latch);
		}*/
		for (final Window window : m_windowsToCloseOnShutdown)
		{
			closeWindow(window);
		}
		for (final Active actor : m_activeToDeactivate)
		{
			closeActor(actor);
		}
		m_activeToDeactivate.clear();
		m_windowsToCloseOnShutdown.clear();
		// m_latchesToCloseOnShutdown.clear();
		// m_mapData.close();
	}
	
	/**
	 * returns the map skins for the game data.
	 * 
	 * returns is a map of display-name -> map directory
	 */
	public static Map<String, String> getSkins(final GameData data)
	{
		final String mapName = data.getProperties().get(Constants.MAP_NAME).toString();
		final Map<String, String> rVal = new LinkedHashMap<String, String>();
		rVal.put("Original", mapName);
		getSkins(mapName, rVal, new File(GameRunner2.getRootFolder(), "maps"));
		getSkins(mapName, rVal, GameRunner2.getUserMapsFolder());
		return rVal;
	}
	
	protected static void getSkins(final String mapName, final Map<String, String> rVal, final File root)
	{
		final File[] files = root.listFiles();
		if (files == null)
			return;
		for (final File f : files)
		{
			if (!f.isDirectory())
			{
				// jar files
				if (f.getName().endsWith(".zip") && f.getName().startsWith(mapName + "-"))
				{
					final String nameWithExtension = f.getName().substring(f.getName().indexOf('-') + 1);
					rVal.put(nameWithExtension.substring(0, nameWithExtension.length() - 4), f.getName().substring(0, f.getName().length() - 4));
				}
			}
			// directories
			else if (f.getName().startsWith(mapName + "-"))
			{
				rVal.put(f.getName().substring(f.getName().indexOf('-') + 1), f.getName());
			}
		}
	}
	
	protected void closeActor(final Active actor)
	{
		try
		{
			actor.deactivate();
		} catch (final RuntimeException re)
		{
			re.printStackTrace();
		}
	}
	
	public boolean getLockMap()
	{
		final Preferences prefs = Preferences.userNodeForPackage(AbstractUIContext.class);
		return prefs.getBoolean(LOCK_MAP, false);
	}
	
	public void setLockMap(final boolean aBool)
	{
		final Preferences prefs = Preferences.userNodeForPackage(AbstractUIContext.class);
		prefs.putBoolean(LOCK_MAP, aBool);
		try
		{
			prefs.flush();
		} catch (final BackingStoreException ex)
		{
			ex.printStackTrace();
		}
	}
	
	public boolean getShowEndOfTurnReport()
	{
		final Preferences prefs = Preferences.userNodeForPackage(AbstractUIContext.class);
		return prefs.getBoolean(SHOW_END_OF_TURN_REPORT, true);
	}
	
	public void setShowEndOfTurnReport(final boolean value)
	{
		final Preferences prefs = Preferences.userNodeForPackage(AbstractUIContext.class);
		prefs.putBoolean(SHOW_END_OF_TURN_REPORT, value);
		try
		{
			prefs.flush();
		} catch (final BackingStoreException ex)
		{
			ex.printStackTrace();
		}
	}
	
	public boolean getShowTriggeredNotifications()
	{
		final Preferences prefs = Preferences.userNodeForPackage(AbstractUIContext.class);
		return prefs.getBoolean(SHOW_TRIGGERED_NOTIFICATIONS, true);
	}
	
	public void setShowTriggeredNotifications(final boolean value)
	{
		final Preferences prefs = Preferences.userNodeForPackage(AbstractUIContext.class);
		prefs.putBoolean(SHOW_TRIGGERED_NOTIFICATIONS, value);
		try
		{
			prefs.flush();
		} catch (final BackingStoreException ex)
		{
			ex.printStackTrace();
		}
	}
	
	public boolean getShowTriggerChanceSuccessful()
	{
		final Preferences prefs = Preferences.userNodeForPackage(AbstractUIContext.class);
		return prefs.getBoolean(SHOW_TRIGGERED_CHANCE_SUCCESSFUL, true);
	}
	
	public void setShowTriggerChanceSuccessful(final boolean value)
	{
		final Preferences prefs = Preferences.userNodeForPackage(AbstractUIContext.class);
		prefs.putBoolean(SHOW_TRIGGERED_CHANCE_SUCCESSFUL, value);
		try
		{
			prefs.flush();
		} catch (final BackingStoreException ex)
		{
			ex.printStackTrace();
		}
	}
	
	public boolean getShowTriggerChanceFailure()
	{
		final Preferences prefs = Preferences.userNodeForPackage(AbstractUIContext.class);
		return prefs.getBoolean(SHOW_TRIGGERED_CHANCE_FAILURE, true);
	}
	
	public void setShowTriggerChanceFailure(final boolean value)
	{
		final Preferences prefs = Preferences.userNodeForPackage(AbstractUIContext.class);
		prefs.putBoolean(SHOW_TRIGGERED_CHANCE_FAILURE, value);
		try
		{
			prefs.flush();
		} catch (final BackingStoreException ex)
		{
			ex.printStackTrace();
		}
	}
	
	public boolean getShowBattlesBetweenAIs()
	{
		final Preferences prefs = Preferences.userNodeForPackage(AbstractUIContext.class);
		return prefs.getBoolean(SHOW_BATTLES_BETWEEN_AIS, true);
	}
	
	public void setShowBattlesBetweenAIs(final boolean aBool)
	{
		final Preferences prefs = Preferences.userNodeForPackage(AbstractUIContext.class);
		prefs.putBoolean(SHOW_BATTLES_BETWEEN_AIS, aBool);
		try
		{
			prefs.flush();
		} catch (final BackingStoreException ex)
		{
			ex.printStackTrace();
		}
	}
	
	public LocalPlayers getLocalPlayers()
	{
		return m_localPlayers;
	}
	
	public void setLocalPlayers(final LocalPlayers players)
	{
		m_localPlayers = players;
	}
}
