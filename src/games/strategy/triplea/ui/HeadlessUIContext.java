package games.strategy.triplea.ui;

import java.awt.Cursor;
import java.util.logging.Level;

import javax.swing.JLabel;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.UnitType;
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

/**
 * Headless version, so that we don't get error in linux when the system has no graphics configuration.
 */
public class HeadlessUIContext extends AbstractUIContext {
  public HeadlessUIContext() {
    super();
  }

  @Override
  protected void internalSetMapDir(final String dir, final GameData data) {
    final Stopwatch stopWatch = new Stopwatch(s_logger, Level.FINE, "Loading UI Context");
    m_resourceLoader = ResourceLoader.getMapResourceLoader(dir);
    m_scale = getPreferencesMapOrSkin(dir).getDouble(MAP_SCALE_PREF, 1);
    m_mapDir = dir;
    stopWatch.done();
  }

  @Override
  public Cursor getCursor() {
    return null;
  }

  @Override
  public MapData getMapData() {
    return null;
  }

  @Override
  public TileImageFactory getTileImageFactory() {
    return null;
  }

  @Override
  public UnitImageFactory getUnitImageFactory() {
    return null;
  }

  @Override public JLabel createUnitImageJLabel(UnitType type, PlayerID player, GameData data,
      UnitDamage damaged, UnitEnable disabled) {
    return null;
  }

  @Override
  public ResourceImageFactory getResourceImageFactory() {
    return null;
  }

  @Override
  public MapImage getMapImage() {
    return null;
  }

  @Override
  public FlagIconImageFactory getFlagImageFactory() {
    return null;
  }

  @Override
  public PUImageFactory getPUImageFactory() {
    return null;
  }

  @Override
  public DiceImageFactory getDiceImageFactory() {
    return null;
  }

  @Override
  public boolean getShowUnits() {
    return false;
  }

  @Override
  public void setShowUnits(final boolean aBool) {}

  @Override
  public OptionalExtraBorderLevel getDrawTerritoryBordersAgain() {
    return null;
  }

  @Override
  public void setDrawTerritoryBordersAgain(final OptionalExtraBorderLevel level) {}

  @Override
  public void resetDrawTerritoryBordersAgain() {}

  @Override
  public void setDrawTerritoryBordersAgainToMedium() {}

  @Override
  public void setShowTerritoryEffects(final boolean aBool) {}

  @Override
  public boolean getShowTerritoryEffects() {
    return false;
  }

  @Override
  public boolean getShowMapOnly() {
    return false;
  }

  @Override
  public void setShowMapOnly(final boolean aBool) {}

  @Override
  public void setUnitScaleFactor(final double scaleFactor) {}
}
