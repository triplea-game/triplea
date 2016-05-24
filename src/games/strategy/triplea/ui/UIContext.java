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
import javax.swing.*;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.UnitType;
import games.strategy.sound.ClipPlayer;
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
 * A place to find images and map data for a ui.
 */
public class UIContext extends AbstractUIContext implements IUIContext {
  protected MapData m_mapData;
  protected final TileImageFactory m_tileImageFactory = new TileImageFactory();
  protected final UnitImageFactory m_unitImageFactory = new UnitImageFactory();
  protected final ResourceImageFactory m_resourceImageFactory = new ResourceImageFactory();
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

  public UIContext() {
    super();
    m_mapImage = new MapImage();
    // m_frame = frame;
  }

  @Override
  public Cursor getCursor() {
    return m_cursor;
  }

  @Override
  public void setScale(final double scale) {
    super.setScale(scale);
    m_tileImageFactory.setScale(scale);
  }

  @Override
  protected void internalSetMapDir(final String dir, final GameData data) {
    final Stopwatch stopWatch = new Stopwatch(s_logger, Level.FINE, "Loading UI Context");
    m_resourceLoader = ResourceLoader.getMapResourceLoader(dir);
    if (m_mapData != null) {
      m_mapData.close();
    }
    m_mapData = new MapData(m_resourceLoader);
    // DiceImageFactory needs loader and game data
    m_diceImageFactory = new DiceImageFactory(m_resourceLoader, data.getDiceSides());
    final double unitScale = getPreferencesMapOrSkin(dir).getDouble(UNIT_SCALE_PREF, m_mapData.getDefaultUnitScale());
    m_scale = getPreferencesMapOrSkin(dir).getDouble(MAP_SCALE_PREF, 1);
    if (m_scale < 1) {
      setDrawTerritoryBordersAgainToMedium();
    }
    m_unitImageFactory.setResourceLoader(m_resourceLoader, unitScale, m_mapData.getDefaultUnitWidth(),
        m_mapData.getDefaultUnitHeight(), m_mapData.getDefaultUnitCounterOffsetWidth(),
        m_mapData.getDefaultUnitCounterOffsetHeight());
    // TODO: separate scale for resources
    m_resourceImageFactory.setResourceLoader(m_resourceLoader, 1);
    m_flagIconImageFactory.setResourceLoader(m_resourceLoader);
    m_PUImageFactory.setResourceLoader(m_resourceLoader);
    m_tileImageFactory.setMapDir(m_resourceLoader);
    m_tileImageFactory.setScale(m_scale);
    // load map data
    m_mapImage.loadMaps(m_resourceLoader);
    m_mapDir = dir;
    m_drawTerritoryEffects = m_mapData.useTerritoryEffectMarkers();
    // load the sounds in a background thread,
    // avoids the pause where sounds dont load right away
    final Runnable loadSounds = new Runnable() {
      @Override
      public void run() {
        // change the resource loader (this allows us to play sounds the map folder, rather than just default sounds)
        ClipPlayer.getInstance(m_resourceLoader, data);
      }
    };
    (new Thread(loadSounds, "Triplea sound loader")).start();
    // load a new cursor
    m_cursor = Cursor.getDefaultCursor();
    final Toolkit toolkit = Toolkit.getDefaultToolkit();
    // URL's use "/" not "\"
    final URL cursorURL = m_resourceLoader.getResource("misc" + "/" + "cursor.gif");
    if (cursorURL != null) {
      try {
        final Image image = ImageIO.read(cursorURL);
        if (image != null) {
          final Point hotSpot = new Point(m_mapData.getMapCursorHotspotX(), m_mapData.getMapCursorHotspotY());
          m_cursor = toolkit.createCustomCursor(image, hotSpot, data.getGameName() + " Cursor");
        }
      } catch (final Exception e) {
        e.printStackTrace();
      }
    }
    stopWatch.done();
  }

  @Override
  public MapData getMapData() {
    return m_mapData;
  }

  @Override
  public TileImageFactory getTileImageFactory() {
    return m_tileImageFactory;
  }

  @Override
  public UnitImageFactory getUnitImageFactory() {
    return m_unitImageFactory;
  }

  @Override public JLabel createUnitImageJLabel(UnitType type, PlayerID player, GameData data,
      UnitDamage damaged, UnitEnable disabled) {
    Optional<ImageIcon>
        image = this.getUnitImageFactory().getIcon(type,player,data,damaged == UnitDamage.DAMAGED, disabled == UnitEnable.DISABLED);
    if (image.isPresent()) {
      return new JLabel(image.get());
    } else {
      return new JLabel();
    }
  }

  @Override
  public ResourceImageFactory getResourceImageFactory() {
    return m_resourceImageFactory;
  }

  @Override
  public MapImage getMapImage() {
    return m_mapImage;
  }

  @Override
  public FlagIconImageFactory getFlagImageFactory() {
    return m_flagIconImageFactory;
  }

  @Override
  public PUImageFactory getPUImageFactory() {
    return m_PUImageFactory;
  }

  @Override
  public DiceImageFactory getDiceImageFactory() {
    return m_diceImageFactory;
  }

  @Override
  public void shutDown() {
    super.shutDown();
    m_mapData.close();
  }

  @Override
  public boolean getShowUnits() {
    return m_drawUnits;
  }

  @Override
  public void setShowUnits(final boolean aBool) {
    m_drawUnits = aBool;
  }

  @Override
  public OptionalExtraBorderLevel getDrawTerritoryBordersAgain() {
    return m_extraTerritoryBorderLevel;
  }

  @Override
  public void setDrawTerritoryBordersAgain(final OptionalExtraBorderLevel level) {
    m_extraTerritoryBorderLevel = level;
  }

  @Override
  public void resetDrawTerritoryBordersAgain() {
    m_extraTerritoryBorderLevel = OptionalExtraBorderLevel.LOW;
  }

  @Override
  public void setDrawTerritoryBordersAgainToMedium() {
    m_extraTerritoryBorderLevel = OptionalExtraBorderLevel.MEDIUM;
  }

  @Override
  public void setShowTerritoryEffects(final boolean aBool) {
    m_drawTerritoryEffects = aBool;
  }

  @Override
  public boolean getShowTerritoryEffects() {
    return m_drawTerritoryEffects;
  }

  @Override
  public boolean getShowMapOnly() {
    return m_drawMapOnly;
  }

  @Override
  public void setShowMapOnly(final boolean aBool) {
    m_drawMapOnly = aBool;
  }

  @Override
  public void setUnitScaleFactor(final double scaleFactor) {
    m_unitImageFactory.setScaleFactor(scaleFactor);
    final Preferences prefs = getPreferencesMapOrSkin(getMapDir());
    prefs.putDouble(UNIT_SCALE_PREF, scaleFactor);
    try {
      prefs.flush();
    } catch (final BackingStoreException e) {
      e.printStackTrace();
    }
  }
}
