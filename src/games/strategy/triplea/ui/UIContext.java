/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.LocalPlayers;
import games.strategy.sound.ClipPlayer;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.image.DiceImageFactory;
import games.strategy.triplea.image.FlagIconImageFactory;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.image.PUImageFactory;
import games.strategy.triplea.image.TileImageFactory;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.ui.screen.IDrawable.OptionalExtraBorderLevel;
import games.strategy.triplea.util.Stopwatch;
import games.strategy.util.CountDownLatchHandler;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * A place to find images and map data for a ui.
 * 
 * @author sgb
 */
public class UIContext
{
	private static final String UNIT_SCALE_PREF = "UnitScale";
	private static final String MAP_SKIN_PREF = "MapSkin";
	private static final String MAP_SCALE_PREF = "MapScale";
	private static final Logger s_logger = Logger.getLogger(UIContext.class.getName());
	private MapData m_mapData;
	private final TileImageFactory m_tileImageFactory = new TileImageFactory();
	private static String m_mapDir;
	private final UnitImageFactory m_unitImageFactory = new UnitImageFactory();
	private final MapImage m_mapImage;
	private final FlagIconImageFactory m_flagIconImageFactory = new FlagIconImageFactory();
	private DiceImageFactory m_diceImageFactory;
	private final PUImageFactory m_PUImageFactory = new PUImageFactory();
	private boolean m_isShutDown;
	private boolean m_drawUnits = true;
	private boolean m_drawTerritoryEffects = false;
	private boolean m_drawMapOnly = false;
	private OptionalExtraBorderLevel m_extraTerritoryBorderLevel = OptionalExtraBorderLevel.LOW;
	private final List<Window> m_windowsToCloseOnShutdown = new ArrayList<Window>();
	private final List<Active> m_activeToDeactivate = new ArrayList<Active>();
	private final CountDownLatchHandler m_latchesToCloseOnShutdown = new CountDownLatchHandler(false); // List<CountDownLatch> m_latchesToCloseOnShutdown = new ArrayList<CountDownLatch>();
	private final static String LOCK_MAP = "LockMap";
	private final static String SHOW_END_OF_TURN_REPORT = "ShowEndOfTurnReport";
	private final static String SHOW_BATTLES_BETWEEN_AIS = "ShowBattlesBetweenAIs";
	private final static String AI_PAUSE_DURATION = "AIPauseDuration";
	private LocalPlayers m_localPlayers;
	private double m_scale = 1;
	private static ResourceLoader m_resourceLoader;
	// private final MainGameFrame m_frame;
	private Cursor m_cursor = Cursor.getDefaultCursor();
	
	public UIContext()
	{
		m_mapImage = new MapImage();
		// m_frame = frame;
	}
	
	public static ResourceLoader getResourceLoader()
	{
		return m_resourceLoader;
	}
	
	public static int getAIPauseDuration()
	{
		final Preferences prefs = Preferences.userNodeForPackage(UIContext.class);
		return prefs.getInt(AI_PAUSE_DURATION, 800);
	}
	
	public static void setAIPauseDuration(final int value)
	{
		final Preferences prefs = Preferences.userNodeForPackage(UIContext.class);
		prefs.putInt(AI_PAUSE_DURATION, value);
		try
		{
			prefs.flush();
		} catch (final BackingStoreException ex)
		{
			ex.printStackTrace();
		}
	}
	
	public Cursor getCursor()
	{
		return m_cursor;
	}
	
	public double getScale()
	{
		return m_scale;
	}
	
	public void setScale(final double scale)
	{
		m_scale = scale;
		m_tileImageFactory.setScale(scale);
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
	private static Preferences getPreferencesForMap(final String mapName)
	{
		return Preferences.userNodeForPackage(UIContext.class).node(mapName);
	}
	
	/**
	 * Get the preferences for the map or map skin
	 */
	private static Preferences getPreferencesMapOrSkin(final String mapDir)
	{
		return Preferences.userNodeForPackage(UIContext.class).node(mapDir);
	}
	
	private static String getDefaultMapDir(final GameData data)
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
	
	private void internalSetMapDir(final String dir, final GameData data)
	{
		final Stopwatch stopWatch = new Stopwatch(s_logger, Level.FINE, "Loading UI Context");
		m_resourceLoader = ResourceLoader.getMapResourceLoader(dir, false);
		if (m_mapData != null)
		{
			m_mapData.close();
		}
		m_mapData = new MapData(m_resourceLoader);
		m_diceImageFactory = new DiceImageFactory(m_resourceLoader, data.getDiceSides()); // DiceImageFactory needs loader and game data
		final double unitScale = getPreferencesMapOrSkin(dir).getDouble(UNIT_SCALE_PREF, m_mapData.getDefaultUnitScale());
		m_scale = getPreferencesMapOrSkin(dir).getDouble(MAP_SCALE_PREF, 1);
		if (m_scale < 1)
			setDrawTerritoryBordersAgainToMedium();
		m_unitImageFactory.setResourceLoader(m_resourceLoader, unitScale, m_mapData.getDefaultUnitWidth(), m_mapData.getDefaultUnitHeight(), m_mapData.getDefaultUnitCounterOffsetWidth(),
					m_mapData.getDefaultUnitCounterOffsetHeight());
		m_flagIconImageFactory.setResourceLoader(m_resourceLoader);
		m_PUImageFactory.setResourceLoader(m_resourceLoader);
		m_tileImageFactory.setMapDir(m_resourceLoader);
		m_tileImageFactory.setScale(m_scale);
		m_mapImage.loadMaps(m_resourceLoader); // load map data
		m_mapDir = dir;
		m_drawTerritoryEffects = m_mapData.useTerritoryEffectMarkers();
		// load the sounds in a background thread,
		// avoids the pause where sounds dont load right away
		final Runnable loadSounds = new Runnable()
		{
			public void run()
			{
				// change the resource loader (this allows us to play sounds the map folder, rather than just default sounds)
				ClipPlayer.getInstance(m_resourceLoader, data);
				SoundPath.preLoadSounds(SoundPath.SoundType.TRIPLEA);
			}
		};
		(new Thread(loadSounds, "Triplea sound loader")).start();
		// load a new cursor
		m_cursor = Cursor.getDefaultCursor();
		final Toolkit toolkit = Toolkit.getDefaultToolkit();
		final URL cursorURL = m_resourceLoader.getResource("misc" + "/" + "cursor.gif"); // URL's use "/" not "\"
		if (cursorURL != null)
		{
			try
			{
				final Image image = ImageIO.read(cursorURL);
				if (image != null)
				{
					final Point hotSpot = new Point(m_mapData.getMapCursorHotspotX(), m_mapData.getMapCursorHotspotY());
					m_cursor = toolkit.createCustomCursor(image, hotSpot, data.getGameName() + " Cursor");
				}
			} catch (final Exception e)
			{
				e.printStackTrace();
			}
		}
		stopWatch.done();
	}
	
	public MapData getMapData()
	{
		return m_mapData;
	}
	
	public static String getMapDir()
	{
		return m_mapDir;
	}
	
	public TileImageFactory getTileImageFactory()
	{
		return m_tileImageFactory;
	}
	
	public UnitImageFactory getUnitImageFactory()
	{
		return m_unitImageFactory;
	}
	
	public MapImage getMapImage()
	{
		return m_mapImage;
	}
	
	public FlagIconImageFactory getFlagImageFactory()
	{
		return m_flagIconImageFactory;
	}
	
	public PUImageFactory getPUImageFactory()
	{
		return m_PUImageFactory;
	}
	
	public DiceImageFactory getDiceImageFactory()
	{
		return m_diceImageFactory;
	}
	
	public void removeACtive(final Active actor)
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
	
	private static void closeWindow(final Window window)
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
	
	private static void clearInputMap(final JComponent c)
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
	
	/* private void releaseLatch(final CountDownLatch latch)
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
		m_mapData.close();
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
	
	private static void getSkins(final String mapName, final Map<String, String> rVal, final File root)
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
	
	private void closeActor(final Active actor)
	{
		try
		{
			actor.deactivate();
		} catch (final RuntimeException re)
		{
			re.printStackTrace();
		}
	}
	
	public boolean getShowUnits()
	{
		return m_drawUnits;
	}
	
	public void setShowUnits(final boolean aBool)
	{
		m_drawUnits = aBool;
	}
	
	public OptionalExtraBorderLevel getDrawTerritoryBordersAgain()
	{
		return m_extraTerritoryBorderLevel;
	}
	
	public void setDrawTerritoryBordersAgain(final OptionalExtraBorderLevel level)
	{
		m_extraTerritoryBorderLevel = level;
	}
	
	public void resetDrawTerritoryBordersAgain()
	{
		m_extraTerritoryBorderLevel = OptionalExtraBorderLevel.LOW;
	}
	
	public void setDrawTerritoryBordersAgainToMedium()
	{
		m_extraTerritoryBorderLevel = OptionalExtraBorderLevel.MEDIUM;
	}
	
	public void setShowTerritoryEffects(final boolean aBool)
	{
		m_drawTerritoryEffects = aBool;
	}
	
	public boolean getShowTerritoryEffects()
	{
		return m_drawTerritoryEffects;
	}
	
	public boolean getShowMapOnly()
	{
		return m_drawMapOnly;
	}
	
	public void setShowMapOnly(final boolean aBool)
	{
		m_drawMapOnly = aBool;
	}
	
	public boolean getLockMap()
	{
		final Preferences prefs = Preferences.userNodeForPackage(UIContext.class);
		return prefs.getBoolean(LOCK_MAP, false);
	}
	
	public void setLockMap(final boolean aBool)
	{
		final Preferences prefs = Preferences.userNodeForPackage(UIContext.class);
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
		final Preferences prefs = Preferences.userNodeForPackage(UIContext.class);
		return prefs.getBoolean(SHOW_END_OF_TURN_REPORT, true);
	}
	
	public void setShowEndOfTurnReport(final boolean value)
	{
		final Preferences prefs = Preferences.userNodeForPackage(UIContext.class);
		prefs.putBoolean(SHOW_END_OF_TURN_REPORT, value);
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
		final Preferences prefs = Preferences.userNodeForPackage(UIContext.class);
		return prefs.getBoolean(SHOW_BATTLES_BETWEEN_AIS, true);
	}
	
	public void setShowBattlesBetweenAIs(final boolean aBool)
	{
		final Preferences prefs = Preferences.userNodeForPackage(UIContext.class);
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
	
	public void setUnitScaleFactor(final double scaleFactor)
	{
		m_unitImageFactory.setScaleFactor(scaleFactor);
		final Preferences prefs = getPreferencesMapOrSkin(getMapDir());
		prefs.putDouble(UNIT_SCALE_PREF, scaleFactor);
		try
		{
			prefs.flush();
		} catch (final BackingStoreException e)
		{
			e.printStackTrace();
		}
	}
	
}
