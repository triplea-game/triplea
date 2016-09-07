package games.strategy.triplea.ui;

import java.awt.Window;
import java.util.concurrent.CountDownLatch;

import javax.swing.JLabel;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.framework.LocalPlayers;
import games.strategy.triplea.image.DiceImageFactory;
import games.strategy.triplea.image.FlagIconImageFactory;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.image.PUImageFactory;
import games.strategy.triplea.image.ResourceImageFactory;
import games.strategy.triplea.image.TileImageFactory;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.triplea.ui.screen.drawable.IDrawable.OptionalExtraBorderLevel;
import games.strategy.util.CountDownLatchHandler;
import javafx.scene.Cursor;

public interface IUIContext {
  Cursor getCursor();

  double getScale();

  void setScale(double scale);

  void setDefaultMapDir(GameData data);

  void setMapDir(GameData data, String mapDir);

  MapData getMapData();

  TileImageFactory getTileImageFactory();

  UnitImageFactory getUnitImageFactory();

  enum UnitDamage {
    DAMAGED, NOT_DAMAGED
  }
  enum UnitEnable {
    DISABLED, ENABLED
  }

  default JLabel createUnitImageJLabel(final UnitType type, final PlayerID player, final GameData data) {
    return createUnitImageJLabel(type, player, data, UnitDamage.NOT_DAMAGED, UnitEnable.ENABLED);
  }

  JLabel createUnitImageJLabel(final UnitType type, final PlayerID player, final GameData data,
      final UnitDamage damaged,
      final UnitEnable disabled);

  ResourceImageFactory getResourceImageFactory();

  MapImage getMapImage();

  FlagIconImageFactory getFlagImageFactory();

  PUImageFactory getPUImageFactory();

  DiceImageFactory getDiceImageFactory();

  void removeActive(Active actor);

  void addActive(Active actor);

  void addShutdownLatch(CountDownLatch latch);

  void removeShutdownLatch(CountDownLatch latch);

  CountDownLatchHandler getCountDownLatchHandler();

  void addShutdownWindow(Window window);

  void removeShutdownWindow(Window window);

  boolean isShutDown();

  void shutDown();

  boolean getShowUnits();

  void setShowUnits(boolean aBool);

  OptionalExtraBorderLevel getDrawTerritoryBordersAgain();

  void setDrawTerritoryBordersAgain(OptionalExtraBorderLevel level);

  void resetDrawTerritoryBordersAgain();

  void setDrawTerritoryBordersAgainToMedium();

  void setShowTerritoryEffects(boolean aBool);

  boolean getShowTerritoryEffects();

  boolean getShowMapOnly();

  void setShowMapOnly(boolean aBool);

  boolean getLockMap();

  void setLockMap(boolean aBool);

  boolean getShowEndOfTurnReport();

  void setShowEndOfTurnReport(boolean value);

  boolean getShowTriggeredNotifications();

  void setShowTriggeredNotifications(boolean value);

  boolean getShowTriggerChanceSuccessful();

  void setShowTriggerChanceSuccessful(boolean value);

  boolean getShowTriggerChanceFailure();

  void setShowTriggerChanceFailure(boolean value);

  boolean getShowBattlesBetweenAIs();

  void setShowBattlesBetweenAIs(boolean aBool);

  LocalPlayers getLocalPlayers();

  void setLocalPlayers(LocalPlayers players);

  void setUnitScaleFactor(double scaleFactor);
}
