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
import games.strategy.sound.ClipPlayer;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.image.DiceImageFactory;
import games.strategy.triplea.image.FlagIconImageFactory;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.image.PUImageFactory;
import games.strategy.triplea.image.TileImageFactory;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.ui.screen.IDrawable.OptionalExtraBorderLevel;
import games.strategy.triplea.util.Stopwatch;

import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.net.URL;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;

/**
 * A place to find images and map data for a ui.
 * 
 * @author sgb
 */
public class UIContext extends AbstractUIContext implements IUIContext
{
	protected MapData m_mapData;
	protected final TileImageFactory m_tileImageFactory = new TileImageFactory();
	protected final UnitImageFactory m_unitImageFactory = new UnitImageFactory();
	protected final MapImage m_mapImage;
	protected final FlagIconImageFactory m_flagIconImageFactory = new FlagIconImageFactory();
	protected DiceImageFactory m_diceImageFactory;
	protected final PUImageFactory m_PUImageFactory = new PUImageFactory();
	protected boolean m_drawUnits = true;
	protected boolean m_drawTerritoryEffects = false;
	protected boolean m_drawMapOnly = false;
	protected OptionalExtraBorderLevel m_extraTerritoryBorderLevel = OptionalExtraBorderLevel.LOW;
	// protected final MainGameFrame m_frame;
	protected Cursor m_cursor = Cursor.getDefaultCursor();
	
	public UIContext()
	{
		super();
		m_mapImage = new MapImage();
		// m_frame = frame;
	}
	
	public Cursor getCursor()
	{
		return m_cursor;
	}
	
	@Override
	public void setScale(final double scale)
	{
		super.setScale(scale);
		m_tileImageFactory.setScale(scale);
	}
	
	@Override
	protected void internalSetMapDir(final String dir, final GameData data)
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
	
	@Override
	public void shutDown()
	{
		super.shutDown();
		m_mapData.close();
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
