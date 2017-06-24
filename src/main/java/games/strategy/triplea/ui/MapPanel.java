package games.strategy.triplea.ui;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeAttachmentChange;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.engine.data.events.TerritoryListener;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.ui.screen.SmallMapImageManager;
import games.strategy.triplea.ui.screen.Tile;
import games.strategy.triplea.ui.screen.TileManager;
import games.strategy.triplea.ui.screen.UnitsDrawer;
import games.strategy.triplea.ui.screen.drawable.IDrawable.OptionalExtraBorderLevel;
import games.strategy.triplea.util.Stopwatch;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.ui.ImageScrollModel;
import games.strategy.ui.ImageScrollerLargeView;
import games.strategy.ui.Util;
import games.strategy.util.Match;
import games.strategy.util.ThreadUtil;
import games.strategy.util.Tuple;

/**
 * Responsible for drawing the large map and keeping it updated.
 */
public class MapPanel extends ImageScrollerLargeView {
  private static final long serialVersionUID = -3571551538356292556L;
  private static final Logger logger = Logger.getLogger(MapPanel.class.getName());
  private final List<MapSelectionListener> mapSelectionListeners = new CopyOnWriteArrayList<>();
  private final List<UnitSelectionListener> unitSelectionListeners = new CopyOnWriteArrayList<>();
  private final List<MouseOverUnitListener> mouseOverUnitsListeners = new CopyOnWriteArrayList<>();
  private GameData m_data;
  // the territory that the mouse is
  private Territory currentTerritory;
  // currently over
  // could be null
  private final MapPanelSmallView smallView;
  // units the mouse is currently over
  private Tuple<Territory, List<Unit>> currentUnits;
  private final SmallMapImageManager smallMapImageManager;
  // keep a reference to the images from the last paint to
  // prevent them from being gcd
  private final List<Tile> images = new ArrayList<>();
  private RouteDescription routeDescription;
  private final TileManager tileManager;
  private final BackgroundDrawer backgroundDrawer;
  private BufferedImage mouseShadowImage = null;
  private String movementLeftForCurrentUnits = "";
  private final IUIContext uiContext;
  private final LinkedBlockingQueue<Tile> undrawnTiles = new LinkedBlockingQueue<>();
  private Map<Territory, List<Unit>> highlightedUnits;
  private Cursor hiddenCursor = null;
  private final MapRouteDrawer routeDrawer;


  /** Creates new MapPanel. */
  public MapPanel(final GameData data, final MapPanelSmallView smallView, final IUIContext uiContext,
      final ImageScrollModel model, final Supplier<Integer> computeScrollSpeed) {
    super(uiContext.getMapData().getMapDimensions(), model);
    this.uiContext = uiContext;
    routeDrawer = new MapRouteDrawer(this, uiContext.getMapData());
    setCursor(this.uiContext.getCursor());
    this.scale = this.uiContext.getScale();
    this.backgroundDrawer = new BackgroundDrawer(this);
    this.tileManager = new TileManager(this.uiContext);
    final Thread t = new Thread(this.backgroundDrawer, "Map panel background drawer");
    t.setDaemon(true);
    t.start();
    setDoubleBuffered(false);
    this.smallView = smallView;
    this.smallMapImageManager =
        new SmallMapImageManager(smallView, this.uiContext.getMapImage().getSmallMapImage(), this.tileManager);
    setGameData(data);
    this.addMouseListener(new MouseAdapter() {

      private boolean is4Pressed = false;
      private boolean is5Pressed = false;
      private int lastActive = -1;

      /**
       * Invoked when the mouse exits a component.
       */
      @Override
      public void mouseExited(final MouseEvent e) {
        if (unitsChanged(null)) {
          final MouseDetails md = convert(e);
          currentUnits = null;
          notifyMouseEnterUnit(Collections.emptyList(), getTerritory(e.getX(), e.getY()), md);
        }
      }

      // this can't be mouseClicked, since a lot of people complain that clicking doesn't work well
      @Override
      public void mouseReleased(final MouseEvent e) {
        final MouseDetails md = convert(e);
        final double scaledMouseX = e.getX() / scale;
        final double scaledMouseY = e.getY() / scale;
        final double x = normalizeX(scaledMouseX + getXOffset());
        final double y = normalizeY(scaledMouseY + getYOffset());
        final Territory terr = getTerritory(x, y);
        if (terr != null) {
          notifyTerritorySelected(terr, md);
        }
        if (e.getButton() == 4 || e.getButton() == 5) {
          // the numbers 4 and 5 stand for the corresponding mouse button
          lastActive = is4Pressed && is5Pressed ? (e.getButton() == 4 ? 5 : 4) : -1;
          // we only want to change the variables if the corresponding button was released
          is4Pressed = e.getButton() != 4 && is4Pressed;
          is5Pressed = e.getButton() != 5 && is5Pressed;
          // we want to return here, because otherwise a menu might be opened
          return;
        }
        if (!unitSelectionListeners.isEmpty()) {
          Tuple<Territory, List<Unit>> tuple = tileManager.getUnitsAtPoint(x, y, m_data);
          if (tuple == null) {
            tuple = Tuple.of(getTerritory(x, y), new ArrayList<Unit>(0));
          }
          notifyUnitSelected(tuple.getSecond(), tuple.getFirst(), md);
        }
      }

      @Override
      public void mousePressed(final MouseEvent e) {
        is4Pressed = e.getButton() == 4 || is4Pressed;
        is5Pressed = e.getButton() == 5 || is5Pressed;
        if (lastActive == -1) {
          new Thread(() -> {
            // Mouse Events are different than key events
            // Thats why we're "simulating" multiple
            // clicks while the mouse button is held down
            // so the map keeps scrolling
            while (lastActive != -1) {
              final int diffPixel = computeScrollSpeed.get();
              if (lastActive == 5) {
                setTopLeft(getXOffset() + diffPixel, getYOffset());
              } else if (lastActive == 4) {
                setTopLeft(getXOffset() - diffPixel, getYOffset());
              }
              // 50ms seems to be a good interval between "clicks"
              // changing this number changes the scroll speed
              ThreadUtil.sleep(50);
            }
          }).start();
        }
        lastActive = e.getButton();
      }
    });
    this.addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseMoved(final MouseEvent e) {
        final MouseDetails md = convert(e);
        final double scaledMouseX = e.getX() / scale;
        final double scaledMouseY = e.getY() / scale;
        final double x = normalizeX(scaledMouseX + getXOffset());
        final double y = normalizeY(scaledMouseY + getYOffset());
        final Territory terr = getTerritory(x, y);
        // we can use == here since they will be the same object.
        // dont use .equals since we have nulls
        if (terr != currentTerritory) {
          currentTerritory = terr;
          notifyMouseEntered(terr);
        }
        notifyMouseMoved(terr, md);
        final Tuple<Territory, List<Unit>> tuple = tileManager.getUnitsAtPoint(x, y, m_data);
        if (unitsChanged(tuple)) {
          currentUnits = tuple;
          if (tuple == null) {
            notifyMouseEnterUnit(Collections.emptyList(), getTerritory(x, y), md);
          } else {
            notifyMouseEnterUnit(tuple.getSecond(), tuple.getFirst(), md);
          }
        }
      }
    });
    this.addScrollListener((x2, y2) -> SwingUtilities.invokeLater(this::repaint));
    recreateTiles(data, this.uiContext);
    this.uiContext.addActive(() -> {
      // super.deactivate
      MapPanel.this.deactivate();
      clearUndrawn();
      backgroundDrawer.stop();
    });
  }

  LinkedBlockingQueue<Tile> getUndrawnTiles() {
    return undrawnTiles;
  }

  private void recreateTiles(final GameData data, final IUIContext uiContext) {
    this.tileManager.createTiles(new Rectangle(this.uiContext.getMapData().getMapDimensions()), data,
        this.uiContext.getMapData());
    this.tileManager.resetTiles(data, uiContext.getMapData());
  }

  GameData getData() {
    return m_data;
  }

  // Beagle Code used to chnage map skin
  void changeImage(final Dimension newDimensions) {
    model.setMaxBounds((int) newDimensions.getWidth(), (int) newDimensions.getHeight());
    tileManager.createTiles(new Rectangle(newDimensions), m_data, uiContext.getMapData());
    tileManager.resetTiles(m_data, uiContext.getMapData());
  }

  @Override
  public Dimension getPreferredSize() {
    return getImageDimensions();
  }

  @Override
  public Dimension getMinimumSize() {
    return new Dimension(200, 200);
  }

  public boolean isShowing(final Territory territory) {
    final Point territoryCenter = uiContext.getMapData().getCenter(territory);
    final Rectangle2D screenBounds =
        new Rectangle2D.Double(super.getXOffset(), super.getYOffset(), super.getScaledWidth(), super.getScaledHeight());
    return screenBounds.contains(territoryCenter);
  }

  /**
   * the units must all be in the same stack on the map, and exist in the given territory.
   * call with an null args
   */
  void setUnitHighlight(final Map<Territory, List<Unit>> units) {
    highlightedUnits = units;
    SwingUtilities.invokeLater(this::repaint);
  }

  Map<Territory, List<Unit>> getHighlightedUnits() {
    return highlightedUnits;
  }

  public void centerOn(final Territory territory) {
    if (territory == null || uiContext.getLockMap()) {
      return;
    }
    final Point p = uiContext.getMapData().getCenter(territory);
    // when centering dont want the map to wrap around,
    // eg if centering on hawaii
    super.setTopLeft((int) (p.x - (getScaledWidth() / 2)), (int) (p.y - (getScaledHeight() / 2)));
  }

  public void setRoute(final Route route) {
    setRoute(route, null, null, null);
  }

  /**
   * Set the route, could be null.
   */
  void setRoute(final Route route, final Point start, final Point end, final Image cursorImage) {
    if (route == null) {
      routeDescription = null;
      SwingUtilities.invokeLater(this::repaint);
      return;
    }
    final RouteDescription newRouteDescription = new RouteDescription(route, start, end, cursorImage);
    if (routeDescription != null && routeDescription.equals(newRouteDescription)) {
      return;
    }
    routeDescription = newRouteDescription;
    SwingUtilities.invokeLater(this::repaint);
  }

  void addMapSelectionListener(final MapSelectionListener listener) {
    mapSelectionListeners.add(listener);
  }

  void removeMapSelectionListener(final MapSelectionListener listener) {
    mapSelectionListeners.remove(listener);
  }

  void addMouseOverUnitListener(final MouseOverUnitListener listener) {
    mouseOverUnitsListeners.add(listener);
  }

  void removeMouseOverUnitListener(final MouseOverUnitListener listener) {
    mouseOverUnitsListeners.remove(listener);
  }

  private void notifyTerritorySelected(final Territory t, final MouseDetails me) {
    for (final MapSelectionListener msl : mapSelectionListeners) {
      msl.territorySelected(t, me);
    }
  }

  private void notifyMouseMoved(final Territory t, final MouseDetails me) {
    for (final MapSelectionListener msl : mapSelectionListeners) {
      msl.mouseMoved(t, me);
    }
  }

  private void notifyMouseEntered(final Territory t) {
    for (final MapSelectionListener msl : mapSelectionListeners) {
      msl.mouseEntered(t);
    }
  }

  void addUnitSelectionListener(final UnitSelectionListener listener) {
    unitSelectionListeners.add(listener);
  }

  void removeUnitSelectionListener(final UnitSelectionListener listener) {
    unitSelectionListeners.remove(listener);
  }

  private void notifyUnitSelected(final List<Unit> units, final Territory t, final MouseDetails me) {
    for (final UnitSelectionListener listener : unitSelectionListeners) {
      listener.unitsSelected(units, t, me);
    }
  }

  private void notifyMouseEnterUnit(final List<Unit> units, final Territory t, final MouseDetails me) {
    for (final MouseOverUnitListener listener : mouseOverUnitsListeners) {
      listener.mouseEnter(units, t, me);
    }
  }

  private Territory getTerritory(final double x, final double y) {
    final String name = uiContext.getMapData().getTerritoryAt(normalizeX(x), normalizeY(y));
    if (name == null) {
      return null;
    }
    return m_data.getMap().getTerritory(name);
  }

  private double normalizeX(double x) {
    if (!uiContext.getMapData().scrollWrapX()) {
      return x;
    }
    final int imageWidth = (int) getImageDimensions().getWidth();
    if (x < 0) {
      x += imageWidth;
    } else if (x > imageWidth) {
      x -= imageWidth;
    }
    return x;
  }

  private double normalizeY(double y) {
    if (!uiContext.getMapData().scrollWrapY()) {
      return y;
    }
    final int imageHeight = (int) getImageDimensions().getHeight();
    if (y < 0) {
      y += imageHeight;
    } else if (y > imageHeight) {
      y -= imageHeight;
    }
    return y;
  }

  public void resetMap() {
    tileManager.resetTiles(m_data, uiContext.getMapData());
    SwingUtilities.invokeLater(this::repaint);
    initSmallMap();
  }

  private MouseDetails convert(final MouseEvent me) {
    final double scaledMouseX = me.getX() / scale;
    final double scaledMouseY = me.getY() / scale;
    final double x = normalizeX(scaledMouseX + getXOffset());
    final double y = normalizeY(scaledMouseY + getYOffset());
    return new MouseDetails(me, x, y);
  }

  private boolean unitsChanged(final Tuple<Territory, List<Unit>> newUnits) {
    return newUnits != currentUnits
        && (newUnits == null
            || currentUnits == null
            || !newUnits.getFirst().equals(currentUnits.getFirst())
            || !games.strategy.util.Util.equals(newUnits.getSecond(), currentUnits.getSecond()));
  }

  public void updateCountries(final Collection<Territory> countries) {
    tileManager.updateTerritories(countries, m_data, uiContext.getMapData());
    smallMapImageManager.update(m_data, uiContext.getMapData());
    SwingUtilities.invokeLater(() -> {
      smallView.repaint();
      repaint();
    });
  }

  void setGameData(final GameData data) {
    // clean up any old listeners
    if (m_data != null) {
      m_data.removeTerritoryListener(territoryListener);
      m_data.removeDataChangeListener(techUpdateListener);
    }
    m_data = data;
    m_data.addTerritoryListener(territoryListener);
    m_data.addDataChangeListener(techUpdateListener);
    clearUndrawn();
    tileManager.resetTiles(m_data, uiContext.getMapData());
  }

  private final TerritoryListener territoryListener = new TerritoryListener() {
    @Override
    public void unitsChanged(final Territory territory) {
      updateCountries(Collections.singleton(territory));
      SwingUtilities.invokeLater(() -> repaint());
    }

    @Override
    public void ownerChanged(final Territory territory) {
      smallMapImageManager.updateTerritoryOwner(territory, m_data, uiContext.getMapData());
      updateCountries(Collections.singleton(territory));
      SwingUtilities.invokeLater(() -> repaint());
    }

    @Override
    public void attachmentChanged(final Territory territory) {
      updateCountries(Collections.singleton(territory));
      SwingUtilities.invokeLater(() -> repaint());
    }
  };
  private final GameDataChangeListener techUpdateListener = new GameDataChangeListener() {
    @Override
    public void gameDataChanged(final Change change) {
      // find the players with tech changes
      final Set<PlayerID> playersWithTechChange = new HashSet<>();
      getPlayersWithTechChanges(change, playersWithTechChange);
      if (playersWithTechChange.isEmpty()) {
        return;
      }
      tileManager.resetTiles(m_data, uiContext.getMapData());
      SwingUtilities.invokeLater(() -> repaint());
    }

    private void getPlayersWithTechChanges(final Change change, final Set<PlayerID> players) {
      if (change instanceof CompositeChange) {
        final CompositeChange composite = (CompositeChange) change;
        for (final Change item : composite.getChanges()) {
          getPlayersWithTechChanges(item, players);
        }
      } else {
        if (change instanceof ChangeAttachmentChange) {
          final ChangeAttachmentChange changeAttachment = (ChangeAttachmentChange) change;
          if (changeAttachment.getAttachmentName().equals(Constants.TECH_ATTACHMENT_NAME)) {
            players.add((PlayerID) changeAttachment.getAttachedTo());
          }
        }
      }
    }
  };

  @Override
  public void setTopLeft(final int x, final int y) {
    super.setTopLeft(x, y);
  }

  /**
   * Draws an image of the complete map to the specified graphics context.
   *
   * <p>
   * This method is useful for capturing screenshots. This method can be called from a thread other than the EDT.
   * </p>
   *
   * @param g The graphics context on which to draw the map; must not be {@code null}.
   */
  public void drawMapImage(final Graphics g) {
    final Graphics2D g2d = (Graphics2D) checkNotNull(g);
    // make sure we use the same data for the entire print
    final GameData gameData = m_data;
    gameData.acquireReadLock();
    try {
      final Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, getImageWidth(), getImageHeight());
      final Collection<Tile> tileList = tileManager.getTiles(bounds);
      for (final Tile tile : tileList) {
        tile.acquireLock();
        try {
          final Image img = tile.getImage(gameData, uiContext.getMapData());
          if (img != null) {
            final AffineTransform t = new AffineTransform();
            t.translate((tile.getBounds().x - bounds.getX()) * scale, (tile.getBounds().y - bounds.getY()) * scale);
            g2d.drawImage(img, t, this);
          }
        } finally {
          tile.releaseLock();
        }
      }
    } finally {
      gameData.releaseReadLock();
    }
  }

  @Override
  public void paint(final Graphics g) {
    final Graphics2D g2d = (Graphics2D) g;
    super.paint(g2d);
    g2d.clip(new Rectangle2D.Double(0, 0, (getImageWidth() * scale), (getImageHeight() * scale)));
    int x = model.getX();
    int y = model.getY();
    final List<Tile> images = new ArrayList<>();
    final List<Tile> undrawnTiles = new ArrayList<>();
    final Stopwatch stopWatch = new Stopwatch(logger, Level.FINER, "Paint");
    // make sure we use the same data for the entire paint
    final GameData data = m_data;
    // if the map fits on screen, dont draw any overlap
    final boolean fitAxisX = !mapWidthFitsOnScreen() && uiContext.getMapData().scrollWrapX();
    final boolean fitAxisY = !mapHeightFitsOnScreen() && uiContext.getMapData().scrollWrapY();
    if (fitAxisX || fitAxisY) {
      if (fitAxisX && x + (int) getScaledWidth() > model.getMaxWidth()) {
        x -= model.getMaxWidth();
      }
      if (fitAxisY && y + (int) getScaledHeight() > model.getMaxHeight()) {
        y -= model.getMaxHeight();
      }
      // handle wrapping off the screen
      if (fitAxisX && x < 0) {
        if (fitAxisY && y < 0) {
          final Rectangle2D.Double leftUpperBounds =
              new Rectangle2D.Double(model.getMaxWidth() + x, model.getMaxHeight() + y, -x, -y);
          drawTiles(g2d, images, data, leftUpperBounds, undrawnTiles);
        }
        final Rectangle2D.Double leftBounds =
            new Rectangle2D.Double(model.getMaxWidth() + x, y, -x, getScaledHeight());
        drawTiles(g2d, images, data, leftBounds, undrawnTiles);
      }
      if (fitAxisY && y < 0) {
        final Rectangle2D.Double upperBounds =
            new Rectangle2D.Double(x, model.getMaxHeight() + y, getScaledWidth(), -y);
        drawTiles(g2d, images, data, upperBounds, undrawnTiles);
      }
    }
    // handle non overlap
    final Rectangle2D.Double mainBounds = new Rectangle2D.Double(x, y, getScaledWidth(), getScaledHeight());
    drawTiles(g2d, images, data, mainBounds, undrawnTiles);
    if (routeDescription != null && mouseShadowImage != null && routeDescription.getEnd() != null) {
      final AffineTransform t = new AffineTransform();
      t.translate(scale * normalizeX(routeDescription.getEnd().getX() - getXOffset()),
          scale * normalizeY(routeDescription.getEnd().getY() - getYOffset()));
      t.translate(mouseShadowImage.getWidth() / -2, mouseShadowImage.getHeight() / -2);
      t.scale(scale, scale);
      g2d.drawImage(mouseShadowImage, t, this);
    }
    if (routeDescription != null) {
      routeDrawer.drawRoute(g2d, routeDescription, movementLeftForCurrentUnits);
    }
    // used to keep strong references to what is on the screen so it wont be garbage collected
    // other references to the images are weak references
    this.images.clear();
    this.images.addAll(images);
    if (highlightedUnits != null) {
      for (final Entry<Territory, List<Unit>> entry : highlightedUnits.entrySet()) {
        final Set<UnitCategory> categories = UnitSeperator.categorize(entry.getValue());
        for (final UnitCategory category : categories) {
          final List<Unit> territoryUnitsOfSameCategory = category.getUnits();
          if (territoryUnitsOfSameCategory.isEmpty()) {
            continue;
          }
          final Rectangle r = tileManager.getUnitRect(territoryUnitsOfSameCategory, m_data);
          if (r == null) {
            continue;
          }

          final Optional<Image> image = uiContext.getUnitImageFactory().getHighlightImage(category.getType(),
              category.getOwner(), m_data, category.hasDamageOrBombingUnitDamage(), category.getDisabled());
          if (image.isPresent()) {
            final AffineTransform t = new AffineTransform();
            t.translate(normalizeX(r.getX() - getXOffset()) * scale, normalizeY(r.getY() - getYOffset()) * scale);
            t.scale(scale, scale);
            g2d.drawImage(image.get(), t, this);
          }
        }
      }
    }
    // draw the tiles nearest us first
    // then draw farther away
    updateUndrawnTiles(undrawnTiles, 30, true);
    updateUndrawnTiles(undrawnTiles, 257, true);
    // when we are this far away, dont force the tiles to stay in memroy
    updateUndrawnTiles(undrawnTiles, 513, false);
    updateUndrawnTiles(undrawnTiles, 767, false);
    clearUndrawn();
    this.undrawnTiles.addAll(undrawnTiles);
    stopWatch.done();
  }

  private void clearUndrawn() {
    for (int i = 0; i < 3; i++) {
      try {
        // several bug reports indicate that
        // clear can throw an exception
        // http://sourceforge.net/tracker/index.php?func=detail&aid=1832130&group_id=44492&atid=439737
        // ignore
        undrawnTiles.clear();
        return;
      } catch (final Exception e) {
        e.printStackTrace(System.out);
      }
    }
  }

  private boolean mapWidthFitsOnScreen() {
    return model.getMaxWidth() < getScaledWidth();
  }

  private boolean mapHeightFitsOnScreen() {
    return model.getMaxHeight() < getScaledHeight();
  }

  /**
   * If we have nothing left undrawn, draw the tiles within preDrawMargin of us, optionally
   * forcing the tiles to remain in memory.
   */
  private void updateUndrawnTiles(final List<Tile> undrawnTiles, final int preDrawMargin, final boolean forceInMemory) {
    // draw tiles near us if we have nothing left to draw
    // that way when we scroll slowly we wont notice a glitch
    if (undrawnTiles.isEmpty()) {
      final Rectangle2D extendedBounds = new Rectangle2D.Double(Math.max(model.getX() - preDrawMargin, 0),
          Math.max(model.getY() - preDrawMargin, 0), getScaledWidth() + (2 * preDrawMargin),
          getScaledHeight() + (2 * preDrawMargin));
      final List<Tile> tileList = tileManager.getTiles(extendedBounds);
      for (final Tile tile : tileList) {
        if (tile.isDirty()) {
          undrawnTiles.add(tile);
        } else if (forceInMemory) {
          images.add(tile);
        }
      }
    }
  }

  private void drawTiles(final Graphics2D g, final List<Tile> images, final GameData data, Rectangle2D.Double bounds,
      final List<Tile> undrawn) {
    final List<Tile> tileList = tileManager.getTiles(bounds);
    bounds = new Rectangle2D.Double(bounds.getX(), bounds.getY(), bounds.getHeight(), bounds.getWidth());
    for (final Tile tile : tileList) {
      Image img = null;
      tile.acquireLock();
      try {
        if (tile.isDirty()) {
          // take what we can get to avoid screen flicker
          undrawn.add(tile);
          img = tile.getRawImage();
        } else {
          img = tile.getImage(data, uiContext.getMapData());
          images.add(tile);
        }
        if (img != null) {
          final AffineTransform t = new AffineTransform();
          t.translate(scale * (tile.getBounds().x - bounds.getX()), scale * (tile.getBounds().y - bounds.getY()));
          g.drawImage(img, t, this);
        }
      } finally {
        tile.releaseLock();
      }
    }
  }

  Image getTerritoryImage(final Territory territory) {
    getData().acquireReadLock();
    try {
      return tileManager.createTerritoryImage(territory, m_data, uiContext.getMapData());
    } finally {
      getData().releaseReadLock();
    }
  }

  Image getTerritoryImage(final Territory territory, final Territory focusOn) {
    getData().acquireReadLock();
    try {
      return tileManager.createTerritoryImage(territory, focusOn, m_data, uiContext.getMapData());
    } finally {
      getData().releaseReadLock();
    }
  }

  public double getScale() {
    return scale;
  }

  @Override
  public void setScale(final double newScale) {
    super.setScale(newScale);
    // setScale will check bounds, and normalize the scale correctly
    final double normalizedScale = scale;
    final OptionalExtraBorderLevel drawBorderOption = uiContext.getDrawTerritoryBordersAgain();
    // so what is happening here is that when we zoom out, the territory borders get blurred or even removed
    // so we have a special setter to have them be drawn a second time, on top of the relief tiles
    if (normalizedScale >= 1) {
      if (drawBorderOption != OptionalExtraBorderLevel.LOW) {
        uiContext.resetDrawTerritoryBordersAgain();
      }
    } else {
      if (drawBorderOption == OptionalExtraBorderLevel.LOW) {
        uiContext.setDrawTerritoryBordersAgainToMedium();
      }
    }
    uiContext.setScale(normalizedScale);
    recreateTiles(getData(), uiContext);
    repaint();
  }

  void initSmallMap() {
    for (final Territory territory : m_data.getMap().getTerritories()) {
      smallMapImageManager.updateTerritoryOwner(territory, m_data, uiContext.getMapData());
    }
    smallMapImageManager.update(m_data, uiContext.getMapData());
  }

  void changeSmallMapOffscreenMap() {
    smallMapImageManager.updateOffscreenImage(uiContext.getMapImage().getSmallMapImage());
  }

  void setMouseShadowUnits(final Collection<Unit> units) {
    if (units == null || units.isEmpty()) {
      movementLeftForCurrentUnits = "";
      mouseShadowImage = null;
      SwingUtilities.invokeLater(this::repaint);
      return;
    }
    final Tuple<Integer, Integer> movementLeft =
        TripleAUnit.getMinAndMaxMovementLeft(Match.getMatches(units, Matches.unitIsBeingTransported().invert()));
    movementLeftForCurrentUnits =
        movementLeft.getFirst() + (movementLeft.getSecond() > movementLeft.getFirst() ? "+" : "");
    final Set<UnitCategory> categories = UnitSeperator.categorize(units);
    final int icon_width = uiContext.getUnitImageFactory().getUnitImageWidth();
    final int xSpace = 5;
    final BufferedImage img = Util.createImage(categories.size() * (xSpace + icon_width),
        uiContext.getUnitImageFactory().getUnitImageHeight(), true);
    final Graphics2D g = img.createGraphics();
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    final Rectangle bounds = new Rectangle(0, 0, 0, 0);
    getData().acquireReadLock();
    try {
      int i = 0;
      for (final UnitCategory category : categories) {
        final Point place = new Point(i * (icon_width + xSpace), 0);
        final UnitsDrawer drawer = new UnitsDrawer(category.getUnits().size(), category.getType().getName(),
            category.getOwner().getName(), place, category.getDamaged(), category.getBombingDamage(),
            category.getDisabled(), false, "", uiContext);
        drawer.draw(bounds, m_data, g, uiContext.getMapData(), null, null);
        i++;
      }
    } finally {
      getData().releaseReadLock();
    }
    mouseShadowImage = img;
    SwingUtilities.invokeLater(this::repaint);
    g.dispose();
  }

  void setTerritoryOverlay(final Territory territory, final Color color, final int alpha) {
    tileManager.setTerritoryOverlay(territory, color, alpha, m_data, uiContext.getMapData());
  }

  void setTerritoryOverlayForBorder(final Territory territory, final Color color) {
    tileManager.setTerritoryOverlayForBorder(territory, color, m_data, uiContext.getMapData());
  }

  void clearTerritoryOverlay(final Territory territory) {
    tileManager.clearTerritoryOverlay(territory, m_data, uiContext.getMapData());
  }

  public IUIContext getUIContext() {
    return uiContext;
  }

  void hideMouseCursor() {
    if (hiddenCursor == null) {
      hiddenCursor = getToolkit().createCustomCursor(new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR),
          new Point(0, 0), "Hidden");
    }
    setCursor(hiddenCursor);
  }

  void showMouseCursor() {
    setCursor(uiContext.getCursor());
  }

  Optional<Image> getErrorImage() {
    return uiContext.getMapData().getErrorImage();
  }

  Optional<Image> getWarningImage() {
    return uiContext.getMapData().getWarningImage();
  }
}


class BackgroundDrawer implements Runnable {
  private MapPanel mapPanel;

  BackgroundDrawer(final MapPanel panel) {
    mapPanel = panel;
  }

  public void stop() {
    // the thread will eventually wake up and notice we are done
    mapPanel = null;
  }

  @Override
  public void run() {
    while (mapPanel != null) {
      final BlockingQueue<Tile> undrawnTiles;
      final MapPanel panel = mapPanel;
      undrawnTiles = panel.getUndrawnTiles();
      final Tile tile;
      try {
        tile = undrawnTiles.poll(2000, TimeUnit.MILLISECONDS);
      } catch (final InterruptedException e) {
        continue;
      }
      if (tile == null) {
        continue;
      }
      final GameData data = mapPanel.getData();
      data.acquireReadLock();
      try {
        tile.getImage(data, mapPanel.getUIContext().getMapData());
      } finally {
        data.releaseReadLock();
      }
      SwingUtilities.invokeLater(mapPanel::repaint);
    }
  }
}
