package games.strategy.triplea.ui;

import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.net.URL;
import java.util.Optional;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.UnitType;
import games.strategy.sound.ClipPlayer;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.image.DiceImageFactory;
import games.strategy.triplea.image.FlagIconImageFactory;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.image.PuImageFactory;
import games.strategy.triplea.image.ResourceImageFactory;
import games.strategy.triplea.image.TileImageFactory;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.triplea.ui.screen.drawable.IDrawable.OptionalExtraBorderLevel;
import games.strategy.triplea.util.Stopwatch;

/**
 * A place to find images and map data for a ui.
 */
public class HeadedUiContext extends AbstractUiContext {
  protected MapData mapData;
  protected final TileImageFactory tileImageFactory = new TileImageFactory();
  protected final UnitImageFactory unitImageFactory = new UnitImageFactory();
  protected final ResourceImageFactory resourceImageFactory = new ResourceImageFactory();
  protected final MapImage mapImage;
  protected final FlagIconImageFactory flagIconImageFactory = new FlagIconImageFactory();
  protected DiceImageFactory diceImageFactory;
  protected final PuImageFactory puImageFactory = new PuImageFactory();
  protected boolean drawUnits = true;
  protected boolean drawTerritoryEffects = false;
  protected boolean drawMapOnly = false;
  protected OptionalExtraBorderLevel extraTerritoryBorderLevel = OptionalExtraBorderLevel.LOW;
  protected Cursor cursor = Cursor.getDefaultCursor();

  HeadedUiContext() {
    super();
    mapImage = new MapImage();
  }

  @Override
  public Cursor getCursor() {
    return cursor;
  }

  @Override
  public void setScale(final double scale) {
    super.setScale(scale);
    tileImageFactory.setScale(scale);
  }

  @Override
  protected void internalSetMapDir(final String dir, final GameData data) {
    final Stopwatch stopWatch = new Stopwatch(logger, Level.FINE, "Loading UI Context");
    resourceLoader = ResourceLoader.getMapResourceLoader(dir);
    if (mapData != null) {
      mapData.close();
    }
    mapData = new MapData(resourceLoader);
    // DiceImageFactory needs loader and game data
    diceImageFactory = new DiceImageFactory(resourceLoader, data.getDiceSides());
    final double unitScale = getPreferencesMapOrSkin(dir).getDouble(UNIT_SCALE_PREF, mapData.getDefaultUnitScale());
    scale = getPreferencesMapOrSkin(dir).getDouble(MAP_SCALE_PREF, 1);
    if (scale < 1) {
      setDrawTerritoryBordersAgainToMedium();
    }
    unitImageFactory.setResourceLoader(resourceLoader, unitScale, mapData.getDefaultUnitWidth(),
        mapData.getDefaultUnitHeight(), mapData.getDefaultUnitCounterOffsetWidth(),
        mapData.getDefaultUnitCounterOffsetHeight());
    // TODO: separate scale for resources
    resourceImageFactory.setResourceLoader(resourceLoader, 1);
    flagIconImageFactory.setResourceLoader(resourceLoader);
    puImageFactory.setResourceLoader(resourceLoader);
    tileImageFactory.setMapDir(resourceLoader);
    tileImageFactory.setScale(scale);
    // load map data
    mapImage.loadMaps(resourceLoader);
    mapDir = dir;
    drawTerritoryEffects = mapData.useTerritoryEffectMarkers();
    // load the sounds in a background thread,
    // avoids the pause where sounds dont load right away
    // change the resource loader (this allows us to play sounds the map folder, rather than just default sounds)
    new Thread(() -> ClipPlayer.getInstance(resourceLoader), "Triplea sound loader").start();
    // load a new cursor
    cursor = Cursor.getDefaultCursor();
    final Toolkit toolkit = Toolkit.getDefaultToolkit();
    // URL's use "/" not "\"
    final URL cursorUrl = resourceLoader.getResource("misc" + "/" + "cursor.gif");
    if (cursorUrl != null) {
      try {
        final Image image = ImageIO.read(cursorUrl);
        if (image != null) {
          final Point hotSpot = new Point(mapData.getMapCursorHotspotX(), mapData.getMapCursorHotspotY());
          cursor = toolkit.createCustomCursor(image, hotSpot, data.getGameName() + " Cursor");
        }
      } catch (final Exception e) {
        ClientLogger.logQuietly("Failed to create cursor from: " + cursorUrl, e);
      }
    }
    stopWatch.done();
  }

  @Override
  public MapData getMapData() {
    return mapData;
  }

  @Override
  public TileImageFactory getTileImageFactory() {
    return tileImageFactory;
  }

  @Override
  public UnitImageFactory getUnitImageFactory() {
    return unitImageFactory;
  }

  @Override
  public JLabel createUnitImageJLabel(final UnitType type, final PlayerID player, final GameData data,
      final UnitDamage damaged, final UnitEnable disabled) {
    final Optional<ImageIcon> image = getUnitImageFactory().getIcon(type, player, damaged == UnitDamage.DAMAGED,
        disabled == UnitEnable.DISABLED);
    return image.map(JLabel::new).orElseGet(JLabel::new);
  }

  @Override
  public ResourceImageFactory getResourceImageFactory() {
    return resourceImageFactory;
  }

  @Override
  public MapImage getMapImage() {
    return mapImage;
  }

  @Override
  public FlagIconImageFactory getFlagImageFactory() {
    return flagIconImageFactory;
  }

  @Override
  public PuImageFactory getPuImageFactory() {
    return puImageFactory;
  }

  @Override
  public DiceImageFactory getDiceImageFactory() {
    return diceImageFactory;
  }

  @Override
  public void shutDown() {
    super.shutDown();
    mapData.close();
  }

  @Override
  public boolean getShowUnits() {
    return drawUnits;
  }

  @Override
  public void setShowUnits(final boolean showUnits) {
    drawUnits = showUnits;
  }

  @Override
  public OptionalExtraBorderLevel getDrawTerritoryBordersAgain() {
    return extraTerritoryBorderLevel;
  }

  @Override
  public void setDrawTerritoryBordersAgain(final OptionalExtraBorderLevel level) {
    extraTerritoryBorderLevel = level;
  }

  @Override
  public void resetDrawTerritoryBordersAgain() {
    extraTerritoryBorderLevel = OptionalExtraBorderLevel.LOW;
  }

  @Override
  public void setDrawTerritoryBordersAgainToMedium() {
    extraTerritoryBorderLevel = OptionalExtraBorderLevel.MEDIUM;
  }

  @Override
  public void setShowTerritoryEffects(final boolean showTerritoryEffects) {
    drawTerritoryEffects = showTerritoryEffects;
  }

  @Override
  public boolean getShowTerritoryEffects() {
    return drawTerritoryEffects;
  }

  @Override
  public boolean getShowMapOnly() {
    return drawMapOnly;
  }

  @Override
  public void setShowMapOnly(final boolean showMapOnly) {
    drawMapOnly = showMapOnly;
  }

  @Override
  public void setUnitScaleFactor(final double scaleFactor) {
    unitImageFactory.setScaleFactor(scaleFactor);
    final Preferences prefs = getPreferencesMapOrSkin(getMapDir());
    prefs.putDouble(UNIT_SCALE_PREF, scaleFactor);
    try {
      prefs.flush();
    } catch (final BackingStoreException e) {
      ClientLogger.logQuietly("Failed to flush preferences: " + prefs.absolutePath(), e);
    }
  }
}
