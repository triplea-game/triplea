package games.strategy.triplea.ui.panels.map;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeAttachmentChange;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.engine.data.events.TerritoryListener;
import games.strategy.engine.data.events.ZoomMapListener;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.EditDelegate;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.MouseDetails;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.triplea.ui.screen.SmallMapImageManager;
import games.strategy.triplea.ui.screen.Tile;
import games.strategy.triplea.ui.screen.TileManager;
import games.strategy.triplea.ui.screen.UnitsDrawer;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeparator;
import games.strategy.ui.ImageScrollModel;
import games.strategy.ui.ImageScrollerLargeView;
import games.strategy.ui.ImageScrollerSmallView;
import games.strategy.ui.Util;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Range;
import org.triplea.java.Interruptibles;
import org.triplea.java.ObjectUtils;
import org.triplea.java.ThreadRunner;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.concurrency.AsyncRunner;
import org.triplea.util.Tuple;

/** Responsible for drawing the large map and keeping it updated. */
@Slf4j
public class MapPanel extends ImageScrollerLargeView {
  private static final long serialVersionUID = -3571551538356292556L;
  private final List<MapSelectionListener> mapSelectionListeners = new ArrayList<>();
  private final List<UnitSelectionListener> unitSelectionListeners = new ArrayList<>();
  private final List<MouseOverUnitListener> mouseOverUnitsListeners = new ArrayList<>();
  private final List<ZoomMapListener> zoomMapListeners = new ArrayList<>();
  private GameData gameData;
  // the territory that the mouse is currently over
  @Getter private @Nullable Territory currentTerritory;

  private @Nullable Territory highlightedTerritory;
  private final TerritoryHighlighter territoryHighlighter = new TerritoryHighlighter();
  private final ImageScrollerSmallView smallView;
  // units the mouse is currently over
  private Tuple<Territory, List<Unit>> currentUnits;
  private final SmallMapImageManager smallMapImageManager;
  private RouteDescription routeDescription;
  private final TileManager tileManager;
  private BufferedImage mouseShadowImage = null;
  private String movementLeftForCurrentUnits = "";
  private ResourceCollection movementFuelCost;
  @Getter private final UiContext uiContext;
  private final ExecutorService executor =
      Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
  @Getter private Collection<Collection<Unit>> highlightedUnits = List.of();
  private Cursor hiddenCursor = null;
  private final MapRouteDrawer routeDrawer;
  private Set<Territory> countriesToUpdate = new HashSet<>();
  private final Object countriesToUpdateLock = new Object();

  private final TerritoryListener territoryListener =
      new TerritoryListener() {
        @Override
        public void unitsChanged(final Territory territory) {
          updateCountries(Set.of(territory));
        }

        @Override
        public void ownerChanged(final Territory territory) {
          smallMapImageManager.updateTerritoryOwner(territory, gameData, uiContext.getMapData());
          updateCountries(Set.of(territory));
        }

        @Override
        public void attachmentChanged(final Territory territory) {
          updateCountries(Set.of(territory));
        }
      };

  private final GameDataChangeListener dataChangeListener =
      new GameDataChangeListener() {
        @Override
        public void gameDataChanged(final Change change) {
          // find the players with tech changes
          final Set<GamePlayer> playersWithTechChange = new HashSet<>();
          getPlayersWithTechChanges(change, playersWithTechChange);
          if (!playersWithTechChange.isEmpty()
              || uiContext
                  .getUnitIconImageFactory()
                  .getUnitIconProperties()
                  .testIfConditionsHaveChanged(gameData)) {
            tileManager.resetTiles(gameData, uiContext.getMapData());
            SwingUtilities.invokeLater(
                () -> {
                  recreateTiles(gameData, uiContext);
                  repaint();
                });
          }
        }

        private void getPlayersWithTechChanges(final Change change, final Set<GamePlayer> players) {
          if (change instanceof CompositeChange) {
            final CompositeChange composite = (CompositeChange) change;
            for (final Change item : composite.getChanges()) {
              getPlayersWithTechChanges(item, players);
            }
          } else {
            if (change instanceof ChangeAttachmentChange) {
              final ChangeAttachmentChange changeAttachment = (ChangeAttachmentChange) change;
              if (changeAttachment.getAttachmentName().equals(Constants.TECH_ATTACHMENT_NAME)) {
                players.add((GamePlayer) changeAttachment.getAttachedTo());
              }
            }
          }
        }
      };

  public MapPanel(
      final GameData data,
      final ImageScrollerSmallView smallView,
      final UiContext uiContext,
      final ImageScrollModel model,
      final Supplier<Integer> computeScrollSpeed) {
    super(uiContext.getMapData().getMapDimensions(), model);
    this.uiContext = uiContext;
    this.smallView = smallView;
    tileManager = new TileManager(uiContext);
    scale = uiContext.getScale();
    routeDrawer = new MapRouteDrawer(this, uiContext.getMapData());
    smallMapImageManager =
        new SmallMapImageManager(
            smallView, uiContext.getMapImage().getSmallMapImage(), tileManager);
    movementFuelCost = new ResourceCollection(data);
    setGameData(data);

    ((ThreadPoolExecutor) executor).setKeepAliveTime(2L, TimeUnit.SECONDS);
    ((ThreadPoolExecutor) executor).allowCoreThreadTimeOut(true);

    setCursor(uiContext.getCursor());
    setDoubleBuffered(false);
    addMouseListener(
        new MouseAdapter() {

          private boolean is4Pressed = false;
          private boolean is5Pressed = false;
          private int lastActive = -1;

          @Override
          public void mouseExited(final MouseEvent e) {
            if (unitsChanged(null)) {
              currentUnits = null;
              notifyMouseEnterUnit(List.of(), getTerritory(e.getX(), e.getY()));
            }
          }

          // this can't be mouseClicked, since a lot of people complain that clicking doesn't work
          // well
          @Override
          public void mouseReleased(final MouseEvent e) {
            final MouseDetails md = convert(e);
            final double scaledMouseX = e.getX() / scale;
            final double scaledMouseY = e.getY() / scale;
            final double x = normalizeX(scaledMouseX + getXOffset());
            final double y = normalizeY(scaledMouseY + getYOffset());
            final @Nullable Territory terr = getTerritory(x, y);
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
                tuple = Tuple.of(getTerritory(x, y), List.of());
              }
              notifyUnitSelected(tuple.getSecond(), tuple.getFirst(), md);
            }
          }

          @Override
          public void mousePressed(final MouseEvent e) {
            is4Pressed = e.getButton() == 4 || is4Pressed;
            is5Pressed = e.getButton() == 5 || is5Pressed;
            if (lastActive == -1) {
              ThreadRunner.runInNewThread(
                  () -> {
                    // Mouse Events are different from key events
                    // That's why we're "simulating" multiple clicks while the mouse button is
                    // held down so the map keeps scrolling
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
                  });
            }
            lastActive = e.getButton();
          }
        });
    addMouseMotionListener(
        new MouseMotionAdapter() {
          @Override
          public void mouseMoved(final MouseEvent e) {
            updateMouseHoverState(convert(e), e.getX(), e.getY());
          }
        });
    // When map is scrolled, update information about what we're hovering over.
    model.addListener(
        () ->
            SwingUtilities.invokeLater(
                () -> {
                  if (highlightedTerritory != null) {
                    currentTerritory = highlightedTerritory;
                    highlightedTerritory = null;
                    notifyMouseEntered(currentTerritory);
                  } else {
                    final PointerInfo pointer = MouseInfo.getPointerInfo();
                    if (pointer != null) {
                      final Point loc = pointer.getLocation();
                      SwingUtilities.convertPointFromScreen(loc, MapPanel.this);
                      updateMouseHoverState(null, loc.x, loc.y);
                    }
                  }
                }));
    addScrollListener((x2, y2) -> SwingUtilities.invokeLater(this::repaint));
    addComponentListener(
        new ComponentAdapter() {
          @Override
          public void componentResized(final ComponentEvent e) {
            // Adjust scale factor to new window bounds
            setScale(getScale());
          }
        });
    executor.execute(() -> recreateTiles(data, uiContext));
    uiContext.addShutdownHook(
        () -> {
          deactivate();
          clearPendingDrawOperations();
          executor.shutdown();
          // Desperate attempt to fix a memory leak
          KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent();
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
        notifyMouseEnterUnit(List.of(), getTerritory(x, y));
      } else {
        notifyMouseEnterUnit(tuple.getSecond(), tuple.getFirst());
      }
    }
  }

  private void recreateTiles(final GameData data, final UiContext uiContext) {
    this.tileManager.createTiles(new Rectangle(this.uiContext.getMapData().getMapDimensions()));
    this.tileManager.resetTiles(data, uiContext.getMapData());
  }

  public List<Unit> getMouseHoverUnits() {
    return Optional.ofNullable(currentUnits).map(Tuple::getSecond).orElse(List.of());
  }

  public GameData getData() {
    return gameData;
  }

  public boolean getEditMode() {
    final boolean isEditMode;
    // use GameData from mapPanel since it will follow current history node
    try (GameData.Unlocker ignored = gameData.acquireReadLock()) {
      isEditMode = EditDelegate.getEditMode(gameData.getProperties());
    }
    return isEditMode;
  }

  // Beagle Code used to change map skin
  public void changeImage(final Dimension newDimensions) {
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
        new Rectangle2D.Double(
            super.getXOffset(),
            super.getYOffset(),
            super.getScaledWidth(),
            super.getScaledHeight());
    return screenBounds.contains(territoryCenter);
  }

  /** the units must all be in the same stack on the map, and exist in the given territory. */
  public void setUnitHighlight(@Nonnull final Collection<Collection<Unit>> units) {
    highlightedUnits = checkNotNull(units);
    SwingUtilities.invokeLater(this::repaint);
  }

  public void centerOnTerritoryIgnoringMapLock(final @Nonnull Territory territory) {
    final Point p = uiContext.getMapData().getCenter(territory);
    // when centering don't want the map to wrap around, e.g. if centering on hawaii
    super.setTopLeft((int) (p.x - (getScaledWidth() / 2)), (int) (p.y - (getScaledHeight() / 2)));
  }

  public void centerOn(final @Nullable Territory territory) {
    if (territory == null || ClientSetting.lockMap.getSetting()) {
      return;
    }
    centerOnTerritoryIgnoringMapLock(territory);
  }

  /**
   * Centers on a territory and highlights it, the territory highlight animation does not turn off
   * until #clearHighlight is called.
   */
  public void highlightTerritory(final Territory territory) {
    highlightTerritory(territory, AnimationDuration.INFINITE, HighlightDelay.STANDARD_DELAY);
  }

  /**
   * Centers on and highlights a territory. The highlight is a white outline that will flash.
   *
   * @param territory The territory to highlight
   * @param animationDuration Duration for how long the flashing territory animation would be
   *     active.
   * @param highlightDelay Time duration for how long before starting the flashing territory
   *     animation.
   */
  public void highlightTerritory(
      final Territory territory,
      final AnimationDuration animationDuration,
      final HighlightDelay highlightDelay) {
    centerOnTerritoryIgnoringMapLock(territory);
    highlightedTerritory = territory;
    territoryHighlighter.highlight(territory, animationDuration.frameCount, highlightDelay.delayMs);
  }

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public enum AnimationDuration {
    STANDARD(4),
    INFINITE(Integer.MAX_VALUE);

    private final int frameCount;
  }

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public enum HighlightDelay {
    SHORT_DELAY(200),
    STANDARD_DELAY(500);

    private final int delayMs;
  }

  public void clearHighlightedTerritory() {
    highlightedTerritory = null;
    territoryHighlighter.clear();
  }

  public void setRoute(final Route route) {
    setRoute(route, null, null, null);
  }

  /** Set the route, could be null. */
  public void setRoute(
      final Route route, final Point start, final Point end, final Image cursorImage) {
    if (route == null) {
      routeDescription = null;
      SwingUtilities.invokeLater(this::repaint);
      return;
    }
    final RouteDescription newRouteDescription =
        new RouteDescription(route, start, end, cursorImage);
    if (routeDescription != null && routeDescription.equals(newRouteDescription)) {
      return;
    }
    routeDescription = newRouteDescription;
    SwingUtilities.invokeLater(this::repaint);
  }

  public void addZoomMapListener(final ZoomMapListener listener) {
    zoomMapListeners.add(listener);
  }

  public void removeZoomMapListener(final ZoomMapListener listener) {
    zoomMapListeners.remove(listener);
  }

  public void addMapSelectionListener(final MapSelectionListener listener) {
    mapSelectionListeners.add(listener);
  }

  public void removeMapSelectionListener(final MapSelectionListener listener) {
    mapSelectionListeners.remove(listener);
  }

  public void addMouseOverUnitListener(final MouseOverUnitListener listener) {
    mouseOverUnitsListeners.add(listener);
  }

  public void removeMouseOverUnitListener(final MouseOverUnitListener listener) {
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

  public void addUnitSelectionListener(final UnitSelectionListener listener) {
    unitSelectionListeners.add(listener);
  }

  public void removeUnitSelectionListener(final UnitSelectionListener listener) {
    unitSelectionListeners.remove(listener);
  }

  private void notifyUnitSelected(
      final List<Unit> units, final Territory t, final MouseDetails me) {
    for (final UnitSelectionListener listener : unitSelectionListeners) {
      listener.unitsSelected(units, t, me);
    }
  }

  private void notifyMouseEnterUnit(final List<Unit> units, final Territory t) {
    for (final MouseOverUnitListener listener : mouseOverUnitsListeners) {
      listener.mouseEnter(units, t);
    }
  }

  @Nullable
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
    return !ObjectUtils.referenceEquals(newUnits, currentUnits)
        && (newUnits == null
            || currentUnits == null
            || !newUnits.getFirst().equals(currentUnits.getFirst())
            || !CollectionUtils.haveEqualSizeAndEquivalentElements(
                newUnits.getSecond(), currentUnits.getSecond()));
  }

  public void updateCountries(Collection<Territory> countries) {
    // When there are multiple updateCountries() notifications in a row, for example from going far
    // back in history, no need to do this repeatedly. Instead, a single update is possible if the
    // async code runs after all the notifications have been received.
    final boolean scheduleUpdate;
    synchronized (countriesToUpdateLock) {
      scheduleUpdate = countriesToUpdate.isEmpty();
      countriesToUpdate.addAll(countries);
    }
    if (!scheduleUpdate) {
      return; // An update is already scheduled.
    }
    AsyncRunner.runAsync(
            () -> {
              Collection<Territory> toUpdate;
              synchronized (countriesToUpdateLock) {
                // Note: Don't run updateTerritories() inside countriesToUpdate lock, as this causes
                // a deadlock due to locking game data inside updateTerritories().
                toUpdate = countriesToUpdate;
                countriesToUpdate = new HashSet<>();
              }
              tileManager.updateTerritories(toUpdate, gameData, uiContext.getMapData());
              smallMapImageManager.update(uiContext.getMapData());
              SwingUtilities.invokeLater(
                  () -> {
                    smallView.repaint();
                    repaint();
                  });
            })
        .exceptionally(e -> log.warn("Failed to update countries", e));
  }

  public void setGameData(final GameData data) {
    // clean up any old listeners
    if (gameData != null) {
      gameData.removeTerritoryListener(territoryListener);
      gameData.removeDataChangeListener(dataChangeListener);
    }
    gameData = data;
    gameData.addTerritoryListener(territoryListener);
    gameData.addDataChangeListener(dataChangeListener);
    clearPendingDrawOperations();
    // Try to mitigate race condition where game data is set after shutting down the executor
    // technically there's no guarantee because the executor can be shut down after the if check
    // and before the call to execute, but it should be good enough!
    if (!executor.isTerminated()) {
      executor.execute(() -> tileManager.resetTiles(gameData, uiContext.getMapData()));
    }
  }

  @Override
  public void setTopLeft(final int x, final int y) {
    super.setTopLeft(x, y);
  }

  /**
   * Draws an image of the complete map to the specified graphics context.
   *
   * <p>This method is useful for capturing screenshots. This method can be called from a thread
   * other than the EDT.
   *
   * @param g The graphics context on which to draw the map; must not be {@code null}.
   */
  public void drawMapImage(final Graphics g) {
    final Graphics2D g2d = (Graphics2D) checkNotNull(g);
    // make sure we use the same data for the entire print
    final GameData gameData = this.gameData;
    try (GameData.Unlocker ignored = gameData.acquireReadLock()) {
      final Rectangle2D.Double bounds =
          new Rectangle2D.Double(0, 0, getImageWidth(), getImageHeight());
      final Collection<Tile> tileList = tileManager.getTiles(bounds);
      for (final Tile tile : tileList) {
        tile.drawImage(gameData, uiContext.getMapData());
        g2d.drawImage(tile.getImage(), tile.getBounds().x, tile.getBounds().y, this);
      }
    }
  }

  @Override
  public void paint(final Graphics g) {
    // make sure we use the same data for the entire paint
    final GameData gameData = this.gameData;
    final MapData mapData = uiContext.getMapData();

    final Graphics2D g2d = (Graphics2D) g;
    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
    g2d.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    super.paint(g2d);
    g2d.scale(scale, scale);
    g2d.clip(new Rectangle2D.Double(0, 0, getImageWidth(), getImageHeight()));
    final boolean fittingWidth = mapWidthFitsOnScreen();
    final boolean fittingHeight = mapHeightFitsOnScreen();
    int x = getXOffset();
    int y = getYOffset();
    final List<Tile> images = new ArrayList<>();
    final List<Tile> undrawnTiles = new ArrayList<>();
    final boolean drawHorizontalOverlap = !fittingWidth && mapData.scrollWrapX();
    final boolean drawVerticalOverlap = !fittingHeight && mapData.scrollWrapY();
    if (drawHorizontalOverlap || drawVerticalOverlap) {
      if (drawHorizontalOverlap && x + (int) getScaledWidth() > model.getMaxWidth()) {
        x -= model.getMaxWidth();
      }
      if (drawVerticalOverlap && y + (int) getScaledHeight() > model.getMaxHeight()) {
        y -= model.getMaxHeight();
      }
      // handle wrapping off the screen
      if (drawHorizontalOverlap && x < 0) {
        if (drawVerticalOverlap && y < 0) {
          final Rectangle2D.Double leftUpperBounds =
              new Rectangle2D.Double(
                  model.getMaxWidth() + (double) x, model.getMaxHeight() + (double) y, -x, -y);
          drawTiles(g2d, images, leftUpperBounds, undrawnTiles);
        }
        final Rectangle2D.Double leftBounds =
            new Rectangle2D.Double(model.getMaxWidth() + (double) x, y, -x, getScaledHeight());
        drawTiles(g2d, images, leftBounds, undrawnTiles);
      }
      if (drawVerticalOverlap && y < 0) {
        final Rectangle2D.Double upperBounds =
            new Rectangle2D.Double(x, model.getMaxHeight() + (double) y, getScaledWidth(), -y);
        drawTiles(g2d, images, upperBounds, undrawnTiles);
      }
    }
    // handle non overlap
    final Rectangle2D.Double mainBounds =
        new Rectangle2D.Double(x, y, getScaledWidth(), getScaledHeight());
    drawTiles(g2d, images, mainBounds, undrawnTiles);
    if (routeDescription != null) {
      if (mouseShadowImage != null && routeDescription.getEnd() != null) {
        final AffineTransform t = new AffineTransform();
        t.translate(
            normalizeX(routeDescription.getEnd().getX() - getXOffset()),
            normalizeY(routeDescription.getEnd().getY() - getYOffset()));
        t.translate(mouseShadowImage.getWidth() / -2.0, mouseShadowImage.getHeight() / -2.0);
        g2d.drawImage(mouseShadowImage, t, this);
      }
      routeDrawer.drawRoute(
          g2d,
          routeDescription,
          movementLeftForCurrentUnits,
          movementFuelCost,
          uiContext.getResourceImageFactory());
    }
    for (final Collection<Unit> value : highlightedUnits) {
      for (final UnitCategory category : UnitSeparator.categorize(value)) {
        final Rectangle r = tileManager.getUnitRect(category.getUnits(), gameData);
        if (r == null) {
          continue;
        }

        Image image =
            uiContext
                .getUnitImageFactory()
                .getHighlightImage(UnitImageFactory.ImageKey.of(category));
        final AffineTransform transform =
            AffineTransform.getTranslateInstance(
                normalizeX(r.getX() - getXOffset()), normalizeY(r.getY() - getYOffset()));
        g2d.drawImage(image, transform, this);
      }
    }
    // draw the tiles nearest us first
    // then draw farther away
    updateUndrawnTiles(undrawnTiles, 30);
    updateUndrawnTiles(undrawnTiles, 257);
    updateUndrawnTiles(undrawnTiles, 513);
    updateUndrawnTiles(undrawnTiles, 767);
    clearPendingDrawOperations();
    for (final Tile tile : undrawnTiles) {
      executor.execute(
          () -> {
            try (GameData.Unlocker ignored = gameData.acquireReadLock()) {
              tile.drawImage(gameData, mapData);
            }
            SwingUtilities.invokeLater(MapPanel.this::repaint);
          });
    }
  }

  @Override
  public int getXOffset() {
    return mapWidthFitsOnScreen() ? 0 : model.getX();
  }

  @Override
  public int getYOffset() {
    return mapHeightFitsOnScreen() ? 0 : model.getY();
  }

  private void clearPendingDrawOperations() {
    ((ThreadPoolExecutor) executor).getQueue().clear();
  }

  private boolean mapWidthFitsOnScreen() {
    return model.getMaxWidth() < getScaledWidth();
  }

  private boolean mapHeightFitsOnScreen() {
    return model.getMaxHeight() < getScaledHeight();
  }

  /** If we have nothing left undrawn, draw the tiles within preDrawMargin of us. */
  private void updateUndrawnTiles(final List<Tile> undrawnTiles, final int preDrawMargin) {
    // draw tiles near us if we have nothing left to draw
    // that way when we scroll slowly we won't notice a glitch
    if (undrawnTiles.isEmpty()) {
      final Rectangle2D extendedBounds =
          new Rectangle2D.Double(
              Math.max(model.getX() - preDrawMargin, 0),
              Math.max(model.getY() - preDrawMargin, 0),
              getScaledWidth() + (2.0 * preDrawMargin),
              getScaledHeight() + (2.0 * preDrawMargin));
      final List<Tile> tileList = tileManager.getTiles(extendedBounds);
      for (final Tile tile : tileList) {
        if (tile.needsRedraw()) {
          undrawnTiles.add(tile);
        }
      }
    }
  }

  private void drawTiles(
      final Graphics2D g,
      final List<Tile> images,
      final Rectangle2D.Double bounds,
      final List<Tile> undrawn) {
    g.translate(-bounds.getX(), -bounds.getY());
    for (final Tile tile : tileManager.getTiles(bounds)) {
      if (tile.needsRedraw()) {
        // take what we can get to avoid screen flicker
        undrawn.add(tile);
      } else {
        images.add(tile);
      }
      g.drawImage(tile.getImage(), tile.getBounds().x, tile.getBounds().y, this);
    }
    g.translate(bounds.getX(), bounds.getY());
  }

  public Image getTerritoryImage(final Territory territory) {
    try (GameData.Unlocker ignored = gameData.acquireReadLock()) {
      return tileManager.newTerritoryImage(territory, gameData, uiContext.getMapData());
    }
  }

  public Image getTerritoryImage(final Territory territory, final Territory focusOn) {
    try (GameData.Unlocker ignored = gameData.acquireReadLock()) {
      return tileManager.newTerritoryImage(territory, focusOn, gameData, uiContext.getMapData());
    }
  }

  public double getScale() {
    return scale;
  }

  @Override
  public void setScale(final double newScale) {
    super.setScale(newScale);
    zoomMapListeners.forEach(
        (zoomMapListener -> zoomMapListener.zoomMapChanged((int) (scale * 100))));
    // setScale will check bounds, and normalize the scale correctly
    uiContext.setScale(scale);
    repaint();
  }

  public void initSmallMap() {
    for (final Territory territory : gameData.getMap().getTerritories()) {
      smallMapImageManager.updateTerritoryOwner(territory, gameData, uiContext.getMapData());
    }
    smallMapImageManager.update(uiContext.getMapData());
  }

  public void changeSmallMapOffscreenMap() {
    smallMapImageManager.updateOffscreenImage(uiContext.getMapImage().getSmallMapImage());
  }

  public void setMouseShadowUnits(final Collection<Unit> units) {
    if (units == null || units.isEmpty()) {
      movementLeftForCurrentUnits = "";
      mouseShadowImage = null;
      SwingUtilities.invokeLater(this::repaint);
      return;
    }

    final Range<BigDecimal> movementLeft =
        getMinAndMaxMovementLeft(
            CollectionUtils.getMatches(units, Matches.unitIsBeingTransported().negate()));
    movementLeftForCurrentUnits =
        movementLeft.getMinimum()
            + (movementLeft.getMaximum().compareTo(movementLeft.getMinimum()) > 0 ? "+" : "");
    if (routeDescription != null) {
      try (GameData.Unlocker ignored = gameData.acquireReadLock()) {
        movementFuelCost =
            Route.getMovementFuelCostCharge(
                units,
                routeDescription.getRoute(),
                CollectionUtils.getAny(units).getOwner(),
                gameData);
      }
    }

    final Set<UnitCategory> categories = UnitSeparator.categorize(units);
    final int iconWidth = uiContext.getUnitImageFactory().getUnitImageWidth();
    final int iconHeight = uiContext.getUnitImageFactory().getUnitImageHeight();
    final int horizontalSpace = 5;
    final BufferedImage img =
        Util.newImage(categories.size() * (horizontalSpace + iconWidth), iconHeight, true);
    final Graphics2D g = img.createGraphics();
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g.setRenderingHint(
        RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    g.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    final Rectangle bounds = new Rectangle(0, 0, 0, 0);
    try (GameData.Unlocker ignored = gameData.acquireReadLock()) {
      int i = 0;
      for (final UnitCategory category : categories) {
        final Point place = new Point(i * (iconWidth + horizontalSpace), 0);
        final UnitsDrawer drawer =
            new UnitsDrawer(
                category.getUnits().size(),
                category.getType().getName(),
                category.getOwner().getName(),
                place,
                category.getDamaged(),
                category.getBombingDamage(),
                category.getDisabled(),
                false,
                "",
                uiContext);
        drawer.draw(bounds, gameData, g, uiContext.getMapData());
        i++;
      }
    }
    mouseShadowImage = img;
    SwingUtilities.invokeLater(this::repaint);
    g.dispose();
  }

  /**
   * Returns a range indicating the minimum movement and maximum movement remaining for the
   * specified collection of units.
   */
  private static Range<BigDecimal> getMinAndMaxMovementLeft(final Collection<Unit> units) {
    BigDecimal min = new BigDecimal(100000);
    BigDecimal max = BigDecimal.ZERO;
    for (final Unit unit : units) {
      final BigDecimal left = unit.getMovementLeft();
      if (left.compareTo(max) > 0) {
        max = left;
      }
      if (left.compareTo(max) < 0) {
        min = left;
      }
    }
    if (max.compareTo(min) < 0) {
      min = max;
    }
    return Range.between(min, max);
  }

  public void setTerritoryOverlay(final Territory territory, final Color color, final int alpha) {
    tileManager.setTerritoryOverlay(territory, color, alpha, gameData, uiContext.getMapData());
  }

  public void setTerritoryOverlayForTile(
      final Territory territory, final Color color, final int alpha) {
    tileManager.setTerritoryOverlayForTile(
        territory, color, alpha, gameData, uiContext.getMapData());
  }

  void setTerritoryOverlayForBorder(final Territory territory, final Color color) {
    tileManager.setTerritoryOverlayForBorder(territory, color, gameData, uiContext.getMapData());
  }

  public void clearTerritoryOverlay(final Territory territory) {
    tileManager.clearTerritoryOverlay(territory, gameData, uiContext.getMapData());
  }

  public void hideMouseCursor() {
    if (hiddenCursor == null) {
      hiddenCursor =
          getToolkit()
              .createCustomCursor(
                  new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR),
                  new Point(0, 0),
                  "Hidden");
    }
    setCursor(hiddenCursor);
  }

  public void showMouseCursor() {
    setCursor(uiContext.getCursor());
  }

  public Optional<Image> getErrorImage() {
    return uiContext.getMapData().getErrorImage();
  }

  public Optional<Image> getWarningImage() {
    return uiContext.getMapData().getWarningImage();
  }

  private final class TerritoryHighlighter {
    private int frame;
    private int totalFrames;
    private @Nullable Territory territory;
    private final Timer timer = new Timer(500, e -> animateNextFrame());

    void highlight(final Territory territory, final int totalFrames, final int delay) {
      stopAnimation();
      startAnimation(territory, totalFrames, delay);
    }

    private void stopAnimation() {
      timer.stop();

      if (territory != null) {
        clearTerritoryOverlay(territory);
        paintImmediately(getBounds());
      }
      territory = null;
    }

    private void startAnimation(final Territory territory, final int totalFrames, final int delay) {
      this.territory = territory;
      this.totalFrames = totalFrames;
      frame = 0;

      timer.setDelay(delay);
      timer.start();
    }

    void clear() {
      stopAnimation();
    }

    private void animateNextFrame() {
      if ((frame % 2) == 0) {
        setTerritoryOverlayForBorder(territory, Color.WHITE);
      } else {
        clearTerritoryOverlay(territory);
      }
      paintImmediately(getBounds());

      if (++frame >= totalFrames) {
        stopAnimation();
      }
    }
  }
}
