package games.strategy.triplea.ui;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeAttachmentChange;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.engine.data.events.TerritoryListener;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.ui.logic.MapScrollUtil;
import games.strategy.triplea.ui.screen.SmallMapImageManager;
import games.strategy.triplea.ui.screen.Tile;
import games.strategy.triplea.ui.screen.TileManager;
import games.strategy.triplea.ui.screen.UnitsDrawer;
import games.strategy.triplea.util.Stopwatch;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.ui.ImageScrollModel;
import games.strategy.ui.ImageScrollerLargeView;
import games.strategy.ui.Util;
import games.strategy.util.CollectionUtils;
import games.strategy.util.Interruptibles;
import games.strategy.util.Tuple;

/**
 * Responsible for drawing the large map and keeping it updated.
 */
public class MapPanel extends ImageScrollerLargeView {
  private static final long serialVersionUID = -3571551538356292556L;
  private final List<MapSelectionListener> mapSelectionListeners = new ArrayList<>();
  private final List<UnitSelectionListener> unitSelectionListeners = new ArrayList<>();
  private final List<MouseOverUnitListener> mouseOverUnitsListeners = new ArrayList<>();
  private GameData gameData;
  // the territory that the mouse is
  private Territory currentTerritory;
  // currently over
  // could be null
  private final MapPanelSmallView smallView;
  // units the mouse is currently over
  private Tuple<Territory, List<Unit>> currentUnits;
  private final SmallMapImageManager smallMapImageManager;
  private RouteDescription routeDescription;
  private final TileManager tileManager;
  private BufferedImage mouseShadowImage = null;
  private String movementLeftForCurrentUnits = "";
  private ResourceCollection movementFuelCost;
  private final UiContext uiContext;
  private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
  private Map<Territory, List<Unit>> highlightedUnits;
  private Cursor hiddenCursor = null;
  private final MapRouteDrawer routeDrawer;


  /** Creates new MapPanel. */
  public MapPanel(final GameData data, final MapPanelSmallView smallView, final UiContext uiContext,
      final ImageScrollModel model, final Supplier<Integer> computeScrollSpeed) {
    super(uiContext.getMapData().getMapDimensions(), model);
    this.uiContext = uiContext;
    this.smallView = smallView;
    tileManager = new TileManager(uiContext);
    scale = uiContext.getScale();
    routeDrawer = new MapRouteDrawer(this, uiContext.getMapData());
    smallMapImageManager = new SmallMapImageManager(smallView, uiContext.getMapImage().getSmallMapImage(), tileManager);
    movementFuelCost = new ResourceCollection(data);
    setGameData(data);

    ((ThreadPoolExecutor) executor).setKeepAliveTime(2L, TimeUnit.SECONDS);
    ((ThreadPoolExecutor) executor).allowCoreThreadTimeOut(true);

    setCursor(uiContext.getCursor());
    setDoubleBuffered(false);
    addMouseListener(new MouseAdapter() {

      private boolean is4Pressed = false;
      private boolean is5Pressed = false;
      private int lastActive = -1;

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
          Tuple<Territory, List<Unit>> tuple = tileManager.getUnitsAtPoint(x, y, gameData);
          if (tuple == null) {
            tuple = Tuple.of(getTerritory(x, y), new ArrayList<>(0));
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
              Interruptibles.sleep(50);
            }
          }).start();
        }
        lastActive = e.getButton();
      }
    });
    addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseMoved(final MouseEvent e) {
        updateMouseHoverState(convert(e), e.getX(), e.getY());
      }
    });
    // When map is scrolled, update information about what we're hovering over.
    model.addObserver((object, arg) -> {
      SwingUtilities.invokeLater(() -> {
        final PointerInfo pointer = MouseInfo.getPointerInfo();
        if (pointer != null) {
          final Point loc = pointer.getLocation();
          SwingUtilities.convertPointFromScreen(loc, MapPanel.this);
          updateMouseHoverState(null, loc.x, loc.y);
        }
      });
    });
    addScrollListener((x2, y2) -> SwingUtilities.invokeLater(this::repaint));
    executor.execute(() -> recreateTiles(data, uiContext));
    uiContext.addActive(() -> {
      // super.deactivate
      deactivate();
      clearPendingDrawOperations();
      executor.shutdown();
    });
  }

  private void updateMouseHoverState(final MouseDetails md, final int mouseX, final int mouseY) {
    final double scaledMouseX = mouseX / scale;
    final double scaledMouseY = mouseY / scale;
    final double x = normalizeX(scaledMouseX + getXOffset());
    final double y = normalizeY(scaledMouseY + getYOffset());
    final Territory terr = getTerritory(x, y);
    if (!Objects.equals(terr, currentTerritory)) {
      currentTerritory = terr;
      notifyMouseEntered(terr);
    }
    if (md != null) {
      notifyMouseMoved(terr, md);
    }
    final Tuple<Territory, List<Unit>> tuple = tileManager.getUnitsAtPoint(x, y, gameData);
    if (unitsChanged(tuple)) {
      currentUnits = tuple;
      if (tuple == null) {
        notifyMouseEnterUnit(Collections.emptyList(), getTerritory(x, y), md);
      } else {
        notifyMouseEnterUnit(tuple.getSecond(), tuple.getFirst(), md);
      }
    }
  }

  private void recreateTiles(final GameData data, final UiContext uiContext) {
    tileManager.createTiles(new Rectangle(this.uiContext.getMapData().getMapDimensions()));
    tileManager.resetTiles(data, uiContext.getMapData());
    for (final Tile tile : tileManager.getTiles()) {
      enqueueTile(tile, data);
    }
  }

  GameData getData() {
    return gameData;
  }

  // Beagle Code used to change map skin
  void changeImage(final Dimension newDimensions) {
    model.setMaxBounds((int) newDimensions.getWidth(), (int) newDimensions.getHeight());
    tileManager.createTiles(new Rectangle(newDimensions));
    tileManager.resetTiles(gameData, uiContext.getMapData());
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
    // when centering don't want the map to wrap around,
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
    return gameData.getMap().getTerritory(name);
  }

  private double normalizeX(final double x) {
    if (!uiContext.getMapData().scrollWrapX()) {
      return x;
    }
    final int imageWidth = (int) getImageDimensions().getWidth();
    if (x < 0) {
      return x + imageWidth;
    } else if (x > imageWidth) {
      return x - imageWidth;
    }
    return x;
  }

  private double normalizeY(final double y) {
    if (!uiContext.getMapData().scrollWrapY()) {
      return y;
    }
    final int imageHeight = (int) getImageDimensions().getHeight();
    if (y < 0) {
      return y + imageHeight;
    } else if (y > imageHeight) {
      return y - imageHeight;
    }
    return y;
  }

  public void resetMap() {
    tileManager.resetTiles(gameData, uiContext.getMapData());
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
            || !CollectionUtils.equals(newUnits.getSecond(), currentUnits.getSecond()));
  }

  public void updateCountries(final Collection<Territory> countries) {
    tileManager.updateTerritories(countries, gameData, uiContext.getMapData());
    smallMapImageManager.update(uiContext.getMapData());
    SwingUtilities.invokeLater(() -> {
      smallView.repaint();
      repaint();
    });
  }

  void setGameData(final GameData data) {
    // clean up any old listeners
    if (gameData != null) {
      gameData.removeTerritoryListener(territoryListener);
      gameData.removeDataChangeListener(dataChangeListener);
    }
    gameData = data;
    gameData.addTerritoryListener(territoryListener);
    gameData.addDataChangeListener(dataChangeListener);
    clearPendingDrawOperations();
    executor.execute(() -> tileManager.resetTiles(gameData, uiContext.getMapData()));
  }

  private final TerritoryListener territoryListener = new TerritoryListener() {
    @Override
    public void unitsChanged(final Territory territory) {
      updateCountries(Collections.singleton(territory));
      SwingUtilities.invokeLater(MapPanel.this::repaint);
    }

    @Override
    public void ownerChanged(final Territory territory) {
      smallMapImageManager.updateTerritoryOwner(territory, gameData, uiContext.getMapData());
      updateCountries(Collections.singleton(territory));
      SwingUtilities.invokeLater(MapPanel.this::repaint);
    }

    @Override
    public void attachmentChanged(final Territory territory) {
      updateCountries(Collections.singleton(territory));
      SwingUtilities.invokeLater(MapPanel.this::repaint);
    }
  };

  private final GameDataChangeListener dataChangeListener = new GameDataChangeListener() {
    @Override
    public void gameDataChanged(final Change change) {
      // find the players with tech changes
      final Set<PlayerID> playersWithTechChange = new HashSet<>();
      getPlayersWithTechChanges(change, playersWithTechChange);
      if (!playersWithTechChange.isEmpty()
          || UnitIconProperties.getInstance(gameData).testIfConditionsHaveChanged(gameData)) {
        tileManager.resetTiles(gameData, uiContext.getMapData());
        SwingUtilities.invokeLater(() -> {
          recreateTiles(getData(), uiContext);
          repaint();
        });
      }
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
    final Graphics2D graphics = (Graphics2D) checkNotNull(g);
    graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
        RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    final Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, getImageWidth(), getImageHeight());
    drawTiles(graphics, gameData, bounds);
    try {
      // This makes use of the FIFO queue the executor uses
      executor.submit(() -> drawTiles((Graphics2D) checkNotNull(g), gameData, bounds)).get();
    } catch (final ExecutionException e) {
      throw new IllegalStateException(e);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void paint(final Graphics g) {
    final Graphics2D g2d = (Graphics2D) g;
    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
        RenderingHints.VALUE_ALPHA_INTERPOLATION_DEFAULT);
    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    super.paint(g2d);
    g2d.clip(new Rectangle2D.Double(0, 0, getImageWidth() * scale, getImageHeight() * scale));
    final Stopwatch stopWatch = new Stopwatch("Paint");
    drawTiles(g2d, gameData, new Rectangle2D.Double(model.getX(), model.getY(), getScaledWidth(), getScaledHeight()));
    if (routeDescription != null && mouseShadowImage != null && routeDescription.getEnd() != null) {
      final AffineTransform t = new AffineTransform();
      t.translate(scale * normalizeX(routeDescription.getEnd().getX() - getXOffset()),
          scale * normalizeY(routeDescription.getEnd().getY() - getYOffset()));
      t.translate(mouseShadowImage.getWidth() / -2, mouseShadowImage.getHeight() / -2);
      t.scale(scale, scale);
      g2d.drawImage(mouseShadowImage, t, this);
    }
    if (routeDescription != null) {
      routeDrawer.drawRoute(g2d, routeDescription, movementLeftForCurrentUnits, movementFuelCost,
          uiContext.getResourceImageFactory());
    }
    if (highlightedUnits != null) {
      for (final List<Unit> value : highlightedUnits.values()) {
        for (final UnitCategory category : UnitSeperator.categorize(value)) {
          final List<Unit> territoryUnitsOfSameCategory = category.getUnits();
          if (territoryUnitsOfSameCategory.isEmpty()) {
            continue;
          }
          final Rectangle r = tileManager.getUnitRect(territoryUnitsOfSameCategory, gameData);
          if (r == null) {
            continue;
          }

          final Optional<Image> image = uiContext.getUnitImageFactory().getHighlightImage(category.getType(),
              category.getOwner(), category.hasDamageOrBombingUnitDamage(), category.getDisabled());
          if (image.isPresent()) {
            final AffineTransform transform = AffineTransform.getScaleInstance(scale, scale);
            transform.translate(normalizeX(r.getX() - getXOffset()), normalizeY(r.getY() - getYOffset()));
            g2d.drawImage(image.get(), transform, this);
          }
        }
      }
    }
    stopWatch.done();
  }

  private void clearPendingDrawOperations() {
    ((ThreadPoolExecutor) executor).getQueue().clear();
  }

  private void drawTiles(final Graphics2D graphics, final GameData data, final Rectangle2D bounds) {
    for (final Tile tile : tileManager.getTiles(bounds)) {
      if (tile.isDirty()) {
        enqueueTile(tile, data);
      }
      final Image image = tile.getImage();
      final List<AffineTransform> transforms = MapScrollUtil.getPossibleTranslations(
          model.getScrollX(), model.getScrollY(), model.getMaxWidth(), model.getMaxHeight());
      for (final AffineTransform transform : transforms) {
        final AffineTransform viewTransformation = new AffineTransform();
        viewTransformation.scale(scale, scale);
        viewTransformation.translate(-bounds.getX(), -bounds.getY());
        viewTransformation.translate(tile.getBounds().x, tile.getBounds().y);
        viewTransformation.concatenate(transform);
        graphics.drawImage(image, viewTransformation, this);
      }
    }
  }

  private void enqueueTile(final Tile tile, final GameData data) {
    executor.execute(() -> {
      try {
        data.acquireReadLock();
        tile.drawImage(data, uiContext.getMapData());
      } finally {
        data.releaseReadLock();
      }
      SwingUtilities.invokeLater(this::repaint);
    });
  }

  Image getTerritoryImage(final Territory territory) {
    getData().acquireReadLock();
    try {
      return tileManager.createTerritoryImage(territory, gameData, uiContext.getMapData());
    } finally {
      getData().releaseReadLock();
    }
  }

  Image getTerritoryImage(final Territory territory, final Territory focusOn) {
    getData().acquireReadLock();
    try {
      return tileManager.createTerritoryImage(territory, focusOn, gameData, uiContext.getMapData());
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
    uiContext.setScale(scale);
    repaint();
  }

  void initSmallMap() {
    for (final Territory territory : gameData.getMap().getTerritories()) {
      smallMapImageManager.updateTerritoryOwner(territory, gameData, uiContext.getMapData());
    }
    smallMapImageManager.update(uiContext.getMapData());
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

    final Tuple<Integer, Integer> movementLeft = TripleAUnit
        .getMinAndMaxMovementLeft(CollectionUtils.getMatches(units, Matches.unitIsBeingTransported().negate()));
    movementLeftForCurrentUnits =
        movementLeft.getFirst() + (movementLeft.getSecond() > movementLeft.getFirst() ? "+" : "");
    gameData.acquireReadLock();
    try {
      movementFuelCost = Route.getMovementFuelCostCharge(units, routeDescription.getRoute(),
          units.iterator().next().getOwner(), gameData);
    } finally {
      gameData.releaseReadLock();
    }

    final Set<UnitCategory> categories = UnitSeperator.categorize(units);
    final int iconWidth = uiContext.getUnitImageFactory().getUnitImageWidth();
    final int horizontalSpace = 5;
    final BufferedImage img = Util.createImage(categories.size() * (horizontalSpace + iconWidth),
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
        final Point place = new Point(i * (iconWidth + horizontalSpace), 0);
        final UnitsDrawer drawer = new UnitsDrawer(category.getUnits().size(), category.getType().getName(),
            category.getOwner().getName(), place, category.getDamaged(), category.getBombingDamage(),
            category.getDisabled(), false, "", uiContext);
        drawer.draw(bounds, gameData, g, uiContext.getMapData());
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
    tileManager.setTerritoryOverlay(territory, color, alpha, gameData, uiContext.getMapData());
  }

  void setTerritoryOverlayForBorder(final Territory territory, final Color color) {
    tileManager.setTerritoryOverlayForBorder(territory, color, gameData, uiContext.getMapData());
  }

  void clearTerritoryOverlay(final Territory territory) {
    tileManager.clearTerritoryOverlay(territory, gameData, uiContext.getMapData());
  }

  public UiContext getUiContext() {
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
