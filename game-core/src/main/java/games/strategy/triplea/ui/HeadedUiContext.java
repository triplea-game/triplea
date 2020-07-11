package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.image.DiceImageFactory;
import games.strategy.triplea.image.FlagIconImageFactory;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.image.PuImageFactory;
import games.strategy.triplea.image.ResourceImageFactory;
import games.strategy.triplea.image.TerritoryEffectImageFactory;
import games.strategy.triplea.image.TileImageFactory;
import games.strategy.triplea.image.UnitIconImageFactory;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.ui.mapdata.MapData;
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
import lombok.Getter;
import lombok.extern.java.Log;
import org.triplea.sound.ClipPlayer;

/** A place to find images and map data for a ui. */
@Log
public class HeadedUiContext extends AbstractUiContext {
  protected MapData mapData;
  private final TileImageFactory tileImageFactory = new TileImageFactory();
  private UnitImageFactory unitImageFactory;
  private final ResourceImageFactory resourceImageFactory = new ResourceImageFactory();
  private final TerritoryEffectImageFactory territoryEffectImageFactory =
      new TerritoryEffectImageFactory();
  private final MapImage mapImage;
  private final UnitIconImageFactory unitIconImageFactory = new UnitIconImageFactory();
  private final FlagIconImageFactory flagIconImageFactory = new FlagIconImageFactory();
  private DiceImageFactory diceImageFactory;
  private final PuImageFactory puImageFactory = new PuImageFactory();
  private boolean drawUnits = true;
  private boolean drawTerritoryEffects = false;

  @Getter(onMethod_ = {@Override})
  private Cursor cursor = Cursor.getDefaultCursor();

  HeadedUiContext() {
    mapImage = new MapImage();
  }

  @Override
  protected void internalSetMapDir(final String dir, final GameData data) {
    if (resourceLoader != null) {
      resourceLoader.close();
    }
    resourceLoader = ResourceLoader.getMapResourceLoader(dir);
    mapData = new MapData(dir);
    // DiceImageFactory needs loader and game data
    diceImageFactory = new DiceImageFactory(resourceLoader, data.getDiceSides());
    final double unitScale =
        getPreferencesMapOrSkin(dir).getDouble(UNIT_SCALE_PREF, mapData.getDefaultUnitScale());
    scale = getPreferencesMapOrSkin(dir).getDouble(MAP_SCALE_PREF, 1);
    unitImageFactory = new UnitImageFactory(resourceLoader, unitScale, mapData);
    // TODO: separate scale for resources
    resourceImageFactory.setResourceLoader(resourceLoader);
    territoryEffectImageFactory.setResourceLoader(resourceLoader);
    unitIconImageFactory.setResourceLoader(resourceLoader);
    flagIconImageFactory.setResourceLoader(resourceLoader);
    puImageFactory.setResourceLoader(resourceLoader);
    tileImageFactory.setMapDir(resourceLoader);
    // load map data
    mapImage.loadMaps(resourceLoader);
    mapDir = dir;
    drawTerritoryEffects = mapData.useTerritoryEffectMarkers();
    // load the sounds in a background thread,
    // avoids the pause where sounds dont load right away
    // change the resource loader (this allows us to play sounds the map folder, rather than just
    // default sounds)
    new Thread(() -> ClipPlayer.getInstance(resourceLoader), "TripleA sound loader").start();
    // load a new cursor
    cursor = Cursor.getDefaultCursor();
    final Toolkit toolkit = Toolkit.getDefaultToolkit();
    // URL's use "/" not "\"
    final URL cursorUrl = resourceLoader.getResource("misc/cursor.gif");
    if (cursorUrl != null) {
      try {
        final Image image = ImageIO.read(cursorUrl);
        if (image != null) {
          final Point hotSpot =
              new Point(mapData.getMapCursorHotspotX(), mapData.getMapCursorHotspotY());
          cursor = toolkit.createCustomCursor(image, hotSpot, data.getGameName() + " Cursor");
        }
      } catch (final Exception e) {
        log.log(Level.SEVERE, "Failed to create cursor from: " + cursorUrl, e);
      }
    }
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
  public JLabel newUnitImageLabel(
      final UnitType type,
      final GamePlayer player,
      final UnitDamage damaged,
      final UnitEnable disabled) {
    final Optional<ImageIcon> image =
        getUnitImageFactory()
            .getUnscaledIcon(
                type, player, damaged == UnitDamage.DAMAGED, disabled == UnitEnable.DISABLED);
    final JLabel label = image.map(JLabel::new).orElseGet(JLabel::new);
    MapUnitTooltipManager.setUnitTooltip(label, type, player, 1);
    return label;
  }

  @Override
  public ResourceImageFactory getResourceImageFactory() {
    return resourceImageFactory;
  }

  @Override
  public TerritoryEffectImageFactory getTerritoryEffectImageFactory() {
    return territoryEffectImageFactory;
  }

  @Override
  public MapImage getMapImage() {
    return mapImage;
  }

  @Override
  public UnitIconImageFactory getUnitIconImageFactory() {
    return unitIconImageFactory;
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
    resourceLoader.close();
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
  public void setShowTerritoryEffects(final boolean showTerritoryEffects) {
    drawTerritoryEffects = showTerritoryEffects;
  }

  @Override
  public boolean getShowTerritoryEffects() {
    return drawTerritoryEffects;
  }

  @Override
  public void setUnitScaleFactor(final double scaleFactor) {
    unitImageFactory = unitImageFactory.withScaleFactor(scaleFactor);
    final Preferences prefs = getPreferencesMapOrSkin(getMapDir());
    prefs.putDouble(UNIT_SCALE_PREF, scaleFactor);
    try {
      prefs.flush();
    } catch (final BackingStoreException e) {
      log.log(Level.SEVERE, "Failed to flush preferences: " + prefs.absolutePath(), e);
    }
  }
}
