package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.image.DiceImageFactory;
import games.strategy.triplea.image.FlagIconImageFactory;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.image.PUImageFactory;
import games.strategy.triplea.image.ResourceImageFactory;
import games.strategy.triplea.image.TileImageFactory;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.ui.screen.IDrawable.OptionalExtraBorderLevel;
import games.strategy.triplea.util.Stopwatch;

import java.awt.Cursor;
import java.util.logging.Level;

/**
 * Headless version, so that we don't get error in linux when the system has no graphics configuration.
 * 
 * @author veqryn
 * 
 */
public class HeadlessUIContext extends AbstractUIContext implements IUIContext
{
	public HeadlessUIContext()
	{
		super();
	}
	
	@Override
	protected void internalSetMapDir(final String dir, final GameData data)
	{
		final Stopwatch stopWatch = new Stopwatch(s_logger, Level.FINE, "Loading UI Context");
		m_resourceLoader = ResourceLoader.getMapResourceLoader(dir, false);
		m_scale = getPreferencesMapOrSkin(dir).getDouble(MAP_SCALE_PREF, 1);
		m_mapDir = dir;
		stopWatch.done();
	}
	
	public Cursor getCursor()
	{
		return null;
	}
	
	public MapData getMapData()
	{
		return null;
	}
	
	public TileImageFactory getTileImageFactory()
	{
		return null;
	}
	
	public UnitImageFactory getUnitImageFactory()
	{
		return null;
	}
	
	public ResourceImageFactory getResourceImageFactory()
	{
		return null;
	}
	
	public MapImage getMapImage()
	{
		return null;
	}
	
	public FlagIconImageFactory getFlagImageFactory()
	{
		return null;
	}
	
	public PUImageFactory getPUImageFactory()
	{
		return null;
	}
	
	public DiceImageFactory getDiceImageFactory()
	{
		return null;
	}
	
	public boolean getShowUnits()
	{
		return false;
	}
	
	public void setShowUnits(final boolean aBool)
	{
	}
	
	public OptionalExtraBorderLevel getDrawTerritoryBordersAgain()
	{
		return null;
	}
	
	public void setDrawTerritoryBordersAgain(final OptionalExtraBorderLevel level)
	{
	}
	
	public void resetDrawTerritoryBordersAgain()
	{
	}
	
	public void setDrawTerritoryBordersAgainToMedium()
	{
	}
	
	public void setShowTerritoryEffects(final boolean aBool)
	{
	}
	
	public boolean getShowTerritoryEffects()
	{
		return false;
	}
	
	public boolean getShowMapOnly()
	{
		return false;
	}
	
	public void setShowMapOnly(final boolean aBool)
	{
	}
	
	public void setUnitScaleFactor(final double scaleFactor)
	{
	}
}
