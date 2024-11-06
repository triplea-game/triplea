package games.strategy.triplea.ui.screen;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.triplea.ui.screen.TerritoryOverLayDrawable.Operation;
import games.strategy.triplea.ui.screen.drawable.BaseMapDrawable;
import games.strategy.triplea.ui.screen.drawable.BattleDrawable;
import games.strategy.triplea.ui.screen.drawable.BlockadeZoneDrawable;
import games.strategy.triplea.ui.screen.drawable.CapitolMarkerDrawable;
import games.strategy.triplea.ui.screen.drawable.ConvoyZoneDrawable;
import games.strategy.triplea.ui.screen.drawable.DecoratorDrawable;
import games.strategy.triplea.ui.screen.drawable.IDrawable;
import games.strategy.triplea.ui.screen.drawable.KamikazeZoneDrawable;
import games.strategy.triplea.ui.screen.drawable.LandTerritoryDrawable;
import games.strategy.triplea.ui.screen.drawable.ReliefMapDrawable;
import games.strategy.triplea.ui.screen.drawable.SeaZoneOutlineDrawable;
import games.strategy.triplea.ui.screen.drawable.TerritoryEffectDrawable;
import games.strategy.triplea.ui.screen.drawable.TerritoryNameDrawable;
import games.strategy.triplea.ui.screen.drawable.VcDrawable;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeparator;
import games.strategy.ui.Util;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.triplea.util.Tuple;

/** Orchestrates the rendering of all map tiles. */
public class TileManager {

  // Note: This value cannot currently change as map images are stored in tile files of this size.
  public static final int TILE_SIZE = 256;

  private List<Tile> tiles = new ArrayList<>();
  private final Object mutex = new Object();
  private final Map<String, IDrawable> territoryOverlays = new HashMap<>();
  private final Map<String, Set<IDrawable>> territoryDrawables = new HashMap<>();
  private final Map<String, Set<Tile>> territoryTiles = new HashMap<>();
  private final Collection<UnitsDrawer> allUnitDrawables = new ArrayList<>();
  private final UiContext uiContext;

  public TileManager(final UiContext uiContext) {
    this.uiContext = uiContext;
  }

  /**
   * Selects tiles which fall into rectangle bounds.
   *
   * @param bounds rectangle for selection
   * @return tiles which fall into the rectangle
   */
  public List<Tile> getTiles(final Rectangle2D bounds) {
    // if the rectangle exceeds the map dimensions we to do shift the rectangle and check for each
    // shifted rectangle as
    // well as the original rectangle
    final MapData mapData = uiContext.getMapData();
    final Dimension mapDimensions = mapData.getMapDimensions();
    final boolean testXshift =
        (mapData.scrollWrapX() && (bounds.getMaxX() > mapDimensions.width || bounds.getMinX() < 0));
    final boolean testYshift =
        (mapData.scrollWrapY()
            && (bounds.getMaxY() > mapDimensions.height || bounds.getMinY() < 0));
    Rectangle2D boundsXshift = null;
    if (testXshift) {
      if (bounds.getMinX() < 0) {
        boundsXshift =
            new Rectangle(
                (int) bounds.getMinX() + mapDimensions.width,
                (int) bounds.getMinY(),
                (int) bounds.getWidth(),
                (int) bounds.getHeight());
      } else {
        boundsXshift =
            new Rectangle(
                (int) bounds.getMinX() - mapDimensions.width,
                (int) bounds.getMinY(),
                (int) bounds.getWidth(),
                (int) bounds.getHeight());
      }
    }
    Rectangle2D boundsYshift = null;
    if (testYshift) {
      if (bounds.getMinY() < 0) {
        boundsYshift =
            new Rectangle(
                (int) bounds.getMinX(),
                (int) bounds.getMinY() + mapDimensions.height,
                (int) bounds.getWidth(),
                (int) bounds.getHeight());
      } else {
        boundsYshift =
            new Rectangle(
                (int) bounds.getMinX(),
                (int) bounds.getMinY() - mapDimensions.height,
                (int) bounds.getWidth(),
                (int) bounds.getHeight());
      }
    }
    synchronized (mutex) {
      final List<Tile> tilesInBounds = new ArrayList<>();
      for (final Tile tile : tiles) {
        final Rectangle tileBounds = tile.getBounds();
        if (tileBounds.intersects(bounds)) {
          tilesInBounds.add(tile);
        }
      }
      if (boundsXshift != null) {
        for (final Tile tile : tiles) {
          final Rectangle tileBounds = tile.getBounds();
          if (boundsXshift.contains(tileBounds) || tileBounds.intersects(boundsXshift)) {
            tilesInBounds.add(tile);
          }
        }
      }
      if (boundsYshift != null) {
        for (final Tile tile : tiles) {
          final Rectangle tileBounds = tile.getBounds();
          if (boundsYshift.contains(tileBounds) || tileBounds.intersects(boundsYshift)) {
            tilesInBounds.add(tile);
          }
        }
      }
      return tilesInBounds;
    }
  }

  Collection<UnitsDrawer> getUnitDrawables() {
    synchronized (mutex) {
      return new ArrayList<>(allUnitDrawables);
    }
  }

  /** Clears all existing tiles and creates those tiles that intersect {@code bounds}. */
  public void createTiles(final Rectangle bounds) {
    synchronized (mutex) {
      // create our tiles
      tiles = new ArrayList<>();
      for (int x = 0; x * TILE_SIZE < bounds.width; x++) {
        for (int y = 0; y * TILE_SIZE < bounds.height; y++) {
          tiles.add(new Tile(new Rectangle(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE)));
        }
      }
    }
  }

  /** Re-renders all tiles. */
  public void resetTiles(final GameData data, final MapData mapData) {
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      synchronized (mutex) {
        for (final Tile tile : tiles) {
          tile.clear();
          final int x = tile.getBounds().x / TILE_SIZE;
          final int y = tile.getBounds().y / TILE_SIZE;
          tile.addDrawable(new BaseMapDrawable(x, y, uiContext));
          tile.addDrawable(new ReliefMapDrawable(x, y, uiContext));
        }
        for (final Territory territory : data.getMap().getTerritories()) {
          clearTerritory(territory);
          drawTerritory(territory, data, mapData);
        }
        // add the decorations
        final Map<Image, List<Point>> decorations = mapData.getDecorations();
        for (final Entry<Image, List<Point>> entry : decorations.entrySet()) {
          final Image img = entry.getKey();
          for (final Point p : entry.getValue()) {
            final DecoratorDrawable drawable = new DecoratorDrawable(p, img);
            final Rectangle bounds =
                new Rectangle(p.x, p.y, img.getWidth(null), img.getHeight(null));
            for (final Tile t : getTiles(bounds)) {
              t.addDrawable(drawable);
            }
          }
        }
      }
    }
  }

  /** Re-renders all tiles that intersect any of the specified territories. */
  public void updateTerritories(
      final Collection<Territory> territories, final GameData data, final MapData mapData) {
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      synchronized (mutex) {
        for (final Territory territory : territories) {
          updateTerritory(territory, data, mapData);
        }
      }
    }
  }

  private void updateTerritory(
      final Territory territory, final GameData data, final MapData mapData) {
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      synchronized (mutex) {
        clearTerritory(territory);
        drawTerritory(territory, data, mapData);
      }
    }
  }

  private void clearTerritory(final Territory territory) {
    if (territoryTiles.get(territory.getName()) == null) {
      return;
    }
    final Collection<IDrawable> drawables = territoryDrawables.get(territory.getName());
    if (drawables == null || drawables.isEmpty()) {
      return;
    }
    for (final Tile tile : territoryTiles.get(territory.getName())) {
      tile.removeDrawables(drawables);
    }
    allUnitDrawables.removeAll(drawables);
  }

  private void drawTerritory(
      final Territory territory, final GameState data, final MapData mapData) {
    final Set<Tile> drawnOn = new HashSet<>();
    final Set<IDrawable> drawing = new HashSet<>();
    if (territoryOverlays.get(territory.getName()) != null) {
      drawing.add(territoryOverlays.get(territory.getName()));
    }
    if (uiContext.getShowTerritoryEffects()) {
      drawTerritoryEffects(territory, mapData, drawing);
    }
    if (uiContext.getShowUnits()) {
      drawUnits(territory, mapData, drawnOn, drawing);
    }
    drawing.add(new BattleDrawable(territory.getName()));
    final TerritoryAttachment ta = TerritoryAttachment.get(territory);
    if (!territory.isWater()) {
      drawing.add(new LandTerritoryDrawable(territory));
    } else {
      if (ta != null) {
        // Kamikaze Zones
        if (ta.getKamikazeZone()) {
          drawing.add(new KamikazeZoneDrawable(territory, uiContext));
        }
        // Blockades
        if (ta.getBlockadeZone()) {
          drawing.add(new BlockadeZoneDrawable(territory));
        }
        // Convoy Routes
        if (ta.getConvoyRoute()) {
          drawing.add(new ConvoyZoneDrawable(territory.getOwner(), territory, uiContext));
        }
        // Convoy Centers
        if (ta.getProduction() > 0) {
          drawing.add(new ConvoyZoneDrawable(territory.getOwner(), territory, uiContext));
        }
      }
      drawing.add(new SeaZoneOutlineDrawable(territory.getName()));
    }
    drawing.add(new TerritoryNameDrawable(territory.getName(), uiContext));
    if (ta != null && ta.isCapital() && mapData.drawCapitolMarkers()) {
      final GamePlayer capitalOf = data.getPlayerList().getPlayerId(ta.getCapital());
      drawing.add(new CapitolMarkerDrawable(capitalOf, territory, uiContext));
    }
    if (ta != null && (ta.getVictoryCity() != 0)) {
      drawing.add(new VcDrawable(territory));
    }
    // add to the relevant tiles
    for (final Tile tile : getTiles(mapData.getBoundingRect(territory.getName()))) {
      drawnOn.add(tile);
      tile.addDrawables(drawing);
    }
    territoryDrawables.put(territory.getName(), drawing);
    territoryTiles.put(territory.getName(), drawnOn);
  }

  private static void drawTerritoryEffects(
      final Territory territory, final MapData mapData, final Set<IDrawable> drawing) {
    final Iterator<Point> effectPoints = mapData.getTerritoryEffectPoints(territory).iterator();
    Point drawingPoint = effectPoints.next();
    for (final TerritoryEffect te : TerritoryEffectHelper.getEffects(territory)) {
      drawing.add(new TerritoryEffectDrawable(te, drawingPoint));
      drawingPoint = effectPoints.hasNext() ? effectPoints.next() : drawingPoint;
    }
  }

  private void drawUnits(
      final Territory territory,
      final MapData mapData,
      final Set<Tile> drawnOn,
      final Set<IDrawable> drawing) {
    final Iterator<Point> placementPoints = mapData.getPlacementPoints(territory).iterator();
    if (!placementPoints.hasNext()) {
      throw new IllegalStateException("No where to place units: " + territory.getName());
    }

    Point lastPlace = null;
    for (final UnitCategory category : UnitSeparator.getSortedUnitCategories(territory, mapData)) {
      final boolean overflow;
      if (placementPoints.hasNext()) {
        lastPlace = new Point(placementPoints.next());
        overflow = false;
      } else {
        lastPlace = new Point(lastPlace);
        overflow = true;
        if (mapData.getPlacementOverflowToLeft(territory)) {
          lastPlace.x -= uiContext.getUnitImageFactory().getUnitImageWidth();
        } else {
          lastPlace.x += uiContext.getUnitImageFactory().getUnitImageWidth();
        }
      }
      final UnitsDrawer drawable =
          new UnitsDrawer(
              category.getUnits().size(),
              category.getType().getName(),
              category.getOwner().getName(),
              lastPlace,
              category.getDamaged(),
              category.getBombingDamage(),
              category.getDisabled(),
              overflow,
              territory.getName(),
              uiContext);
      drawing.add(drawable);
      allUnitDrawables.add(drawable);
      for (final Tile tile : getTiles(drawable.getPlacementRectangle())) {
        tile.addDrawable(drawable);
        drawnOn.add(tile);
      }
    }
  }

  public Image newTerritoryImage(final Territory t, final GameData data, final MapData mapData) {
    return newTerritoryImage(t, t, data, mapData, true);
  }

  public Image newTerritoryImage(
      final Territory selected,
      final Territory focusOn,
      final GameData data,
      final MapData mapData) {
    return newTerritoryImage(selected, focusOn, data, mapData, false);
  }

  private Image newTerritoryImage(
      final Territory selected,
      final Territory focusOn,
      final GameData data,
      final MapData mapData,
      final boolean drawOutline) {
    synchronized (mutex) {
      // make a square
      final Rectangle bounds = mapData.getBoundingRect(focusOn);
      int squareLength = Math.max(bounds.width, bounds.height);
      final int grow = squareLength / 4;
      bounds.x -= grow;
      bounds.y -= grow;
      squareLength += grow * 2;
      // make sure it is not bigger than the whole map
      final int mapDataWidth = mapData.getMapDimensions().width;
      final int mapDataHeight = mapData.getMapDimensions().height;
      if (squareLength > mapDataWidth) {
        squareLength = mapDataWidth;
      }
      if (squareLength > mapDataHeight) {
        squareLength = mapDataHeight;
      }
      bounds.width = squareLength;
      bounds.height = squareLength;
      // keep it in bounds
      if (!mapData.scrollWrapX()) {
        if (bounds.x < 0) {
          bounds.x = 0;
        }
        if (bounds.width + bounds.x > mapDataWidth) {
          bounds.x = mapDataWidth - bounds.width;
        }
      }
      if (!mapData.scrollWrapY()) {
        if (bounds.y < 0) {
          bounds.y = 0;
        }
        if (bounds.height + bounds.y > mapDataHeight) {
          bounds.y = mapDataHeight - bounds.height;
        }
      }
      final Image territoryImage = Util.newImage(squareLength, squareLength, false);
      final Graphics2D graphics = (Graphics2D) territoryImage.getGraphics();
      graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      graphics.setRenderingHint(
          RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
      graphics.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      if (bounds.x < 0) {
        bounds.x += mapDataWidth;
        drawForCreate(selected, data, mapData, bounds, graphics, drawOutline);
        if (bounds.y < 0) {
          bounds.y += mapDataHeight;
          drawForCreate(selected, data, mapData, bounds, graphics, drawOutline);
          bounds.y -= mapDataHeight;
        }
        bounds.x -= mapDataWidth;
      }
      if (bounds.y < 0) {
        bounds.y += mapDataHeight;
        drawForCreate(selected, data, mapData, bounds, graphics, drawOutline);
        bounds.y -= mapDataHeight;
      }
      // start as a set to prevent duplicates
      drawForCreate(selected, data, mapData, bounds, graphics, drawOutline);
      if (bounds.x + bounds.height > mapDataWidth) {
        bounds.x -= mapDataWidth;
        drawForCreate(selected, data, mapData, bounds, graphics, drawOutline);
        if (bounds.y + bounds.width > mapDataHeight) {
          bounds.y -= mapDataHeight;
          drawForCreate(selected, data, mapData, bounds, graphics, drawOutline);
          bounds.y += mapDataHeight;
        }
        bounds.x += mapDataWidth;
      }
      if (bounds.y + bounds.width > mapDataHeight) {
        bounds.y -= mapDataHeight;
        drawForCreate(selected, data, mapData, bounds, graphics, drawOutline);
        bounds.y += mapDataHeight;
      }
      graphics.dispose();
      return territoryImage;
    }
  }

  private void drawForCreate(
      final Territory selected,
      final GameData data,
      final MapData mapData,
      final Rectangle bounds,
      final Graphics2D graphics,
      final boolean drawOutline) {
    final List<IDrawable> drawables =
        getTiles(bounds).stream()
            .map(Tile::getDrawables)
            .flatMap(Collection::stream)
            .sorted()
            .collect(Collectors.toList());
    for (final IDrawable drawer : drawables) {
      if (drawer.getLevel().ordinal() >= IDrawable.DrawLevel.UNITS_LEVEL.ordinal()) {
        break;
      }
      if (drawer.getLevel() == IDrawable.DrawLevel.TERRITORY_TEXT_LEVEL) {
        continue;
      }
      drawer.draw(bounds, data, graphics, mapData);
    }
    if (!drawOutline) {
      final Color c = selected.isWater() ? Color.RED : Color.BLACK;
      final TerritoryOverLayDrawable told =
          new TerritoryOverLayDrawable(c, selected.getName(), 100, Operation.FILL);
      told.draw(bounds, data, graphics, mapData);
    }
    graphics.setStroke(new BasicStroke(10));
    graphics.setColor(Color.RED);
    for (final Polygon polygon : mapData.getPolygons(selected)) {
      graphics.drawPolygon(Util.translatePolygon(polygon, -bounds.x, -bounds.y));
    }
  }

  /**
   * Returns the rectangle within which all the specified units will be drawn stacked or {@code
   * null} if no such rectangle exists. Because the units are assumed to be drawn stacked, the
   * returned rectangle will always have a size equal to the standard unit image size.
   */
  public @Nullable Rectangle getUnitRect(final List<Unit> units, final GameData data) {
    if (units.isEmpty()) {
      return null;
    }
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      synchronized (mutex) {
        for (final UnitsDrawer drawer : allUnitDrawables) {
          final List<Unit> drawerUnits = drawer.getUnits(data);
          if (!drawerUnits.isEmpty() && units.containsAll(drawerUnits)) {
            return drawer.getPlacementRectangle();
          }
        }
        return null;
      }
    }
  }

  /**
   * Returns the territory and units at the specified point or {@code null} if the point does not
   * lie within the bounds of any {@link UnitsDrawer}.
   */
  public @Nullable Tuple<Territory, List<Unit>> getUnitsAtPoint(
      final double x, final double y, final GameData gameData) {
    try (GameData.Unlocker ignored = gameData.acquireReadLock()) {
      synchronized (mutex) {
        for (final UnitsDrawer drawer : allUnitDrawables) {
          if (drawer.getPlacementRectangle().contains(x, y)) {
            return Tuple.of(drawer.getTerritory(gameData), drawer.getUnits(gameData));
          }
        }
        return null;
      }
    }
  }

  public void setTerritoryOverlay(
      final Territory territory,
      final Color color,
      final int alpha,
      final GameData data,
      final MapData mapData) {
    synchronized (mutex) {
      final IDrawable drawable =
          new TerritoryOverLayDrawable(color, territory.getName(), alpha, Operation.DRAW);
      territoryOverlays.put(territory.getName(), drawable);
    }
    updateTerritory(territory, data, mapData);
  }

  public void setTerritoryOverlayForTile(
      final Territory territory,
      final Color color,
      final int alpha,
      final GameData data,
      final MapData mapData) {
    synchronized (mutex) {
      final IDrawable drawable =
          new TerritoryOverLayDrawable(color, territory.getName(), alpha, Operation.FILL);
      territoryOverlays.put(territory.getName(), drawable);
    }
    updateTerritory(territory, data, mapData);
  }

  public void setTerritoryOverlayForBorder(
      final Territory territory, final Color color, final GameData data, final MapData mapData) {
    synchronized (mutex) {
      final IDrawable drawable =
          new TerritoryOverLayDrawable(color, territory.getName(), Operation.DRAW);
      territoryOverlays.put(territory.getName(), drawable);
    }
    updateTerritory(territory, data, mapData);
  }

  public void clearTerritoryOverlay(
      final Territory territory, final GameData data, final MapData mapData) {
    synchronized (mutex) {
      territoryOverlays.remove(territory.getName());
    }
    updateTerritory(territory, data, mapData);
  }
}
