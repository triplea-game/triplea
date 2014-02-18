package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.LocalPlayers;
import games.strategy.triplea.image.DiceImageFactory;
import games.strategy.triplea.image.FlagIconImageFactory;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.image.PUImageFactory;
import games.strategy.triplea.image.ResourceImageFactory;
import games.strategy.triplea.image.TileImageFactory;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.ui.screen.IDrawable.OptionalExtraBorderLevel;
import games.strategy.util.CountDownLatchHandler;

import java.awt.Cursor;
import java.awt.Window;
import java.util.concurrent.CountDownLatch;

public interface IUIContext
{
	public Cursor getCursor();
	
	public double getScale();
	
	public void setScale(double scale);
	
	public void setDefaultMapDir(GameData data);
	
	public void setMapDir(GameData data, String mapDir);
	
	public MapData getMapData();
	
	public TileImageFactory getTileImageFactory();
	
	public UnitImageFactory getUnitImageFactory();
	
	public ResourceImageFactory getResourceImageFactory();
	
	public MapImage getMapImage();
	
	public FlagIconImageFactory getFlagImageFactory();
	
	public PUImageFactory getPUImageFactory();
	
	public DiceImageFactory getDiceImageFactory();
	
	public void removeActive(Active actor);
	
	public void addActive(Active actor);
	
	public void addShutdownLatch(CountDownLatch latch);
	
	public void removeShutdownLatch(CountDownLatch latch);
	
	public CountDownLatchHandler getCountDownLatchHandler();
	
	public void addShutdownWindow(Window window);
	
	public void removeShutdownWindow(Window window);
	
	public boolean isShutDown();
	
	public void shutDown();
	
	public boolean getShowUnits();
	
	public void setShowUnits(boolean aBool);
	
	public OptionalExtraBorderLevel getDrawTerritoryBordersAgain();
	
	public void setDrawTerritoryBordersAgain(OptionalExtraBorderLevel level);
	
	public void resetDrawTerritoryBordersAgain();
	
	public void setDrawTerritoryBordersAgainToMedium();
	
	public void setShowTerritoryEffects(boolean aBool);
	
	public boolean getShowTerritoryEffects();
	
	public boolean getShowMapOnly();
	
	public void setShowMapOnly(boolean aBool);
	
	public boolean getLockMap();
	
	public void setLockMap(boolean aBool);
	
	public boolean getShowEndOfTurnReport();
	
	public void setShowEndOfTurnReport(boolean value);
	
	public boolean getShowTriggeredNotifications();
	
	public void setShowTriggeredNotifications(boolean value);
	
	public boolean getShowTriggerChanceSuccessful();
	
	public void setShowTriggerChanceSuccessful(boolean value);
	
	public boolean getShowTriggerChanceFailure();
	
	public void setShowTriggerChanceFailure(boolean value);
	
	public boolean getShowBattlesBetweenAIs();
	
	public void setShowBattlesBetweenAIs(boolean aBool);
	
	public LocalPlayers getLocalPlayers();
	
	public void setLocalPlayers(LocalPlayers players);
	
	public void setUnitScaleFactor(double scaleFactor);
}
