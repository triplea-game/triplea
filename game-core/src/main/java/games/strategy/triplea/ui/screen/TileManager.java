package games.strategy.triplea.ui.screen;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
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
import games.strategy.triplea.ui.screen.drawable.DrawableComparator;
import games.strategy.triplea.ui.screen.drawable.IDrawable;
import games.strategy.triplea.ui.screen.drawable.IDrawable.OptionalExtraBorderLevel;
import games.strategy.triplea.ui.screen.drawable.KamikazeZoneDrawable;
import games.strategy.triplea.ui.screen.drawable.LandTerritoryDrawable;
import games.strategy.triplea.ui.screen.drawable.MapTileDrawable;
import games.strategy.triplea.ui.screen.drawable.OptionalExtraTerritoryBordersDrawable;
import games.strategy.triplea.ui.screen.drawable.ReliefMapDrawable;
import games.strategy.triplea.ui.screen.drawable.SeaZoneOutlineDrawable;
import games.strategy.triplea.ui.screen.drawable.TerritoryEffectDrawable;
import games.strategy.triplea.ui.screen.drawable.TerritoryNameDrawable;
import games.strategy.triplea.ui.screen.drawable.VcDrawable;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.ui.Util;
import games.strategy.util.Tuple;

public class TileManager {
  private static final Logger logger = Logger.getLogger(TileManager.class.getName());
  public static final int TILE_SIZE = 256;

  private List<Tile> tiles = new ArrayList<>();
  private final Lock lock = new ReentrantLock();
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
   * @param bounds
   *        rectangle for selection
   * @return tiles which fall into the rectangle
   */
  public List<Tile> getTiles(final Rectangle2D bounds) {
    // if the rectangle exceeds the map dimensions we to do shift the rectangle and check for each shifted rectangle as
    // well as the original
    // rectangle
    final MapData mapData = uiContext.getMapData();
    final Dimension mapDimensions = mapData.getMapDimensions();
    final boolean testXshift =
        (mapData.scrollWrapX() && ((bounds.getMaxX() > mapDimensions.width) || (bounds.getMinX() < 0)));
    final boolean testYshift =
        (mapData.scrollWrapY() && ((bounds.getMaxY() > mapDimensions.height) || (bounds.getMinY() < 0)));
    Rectangle2D boundsXshift = null;
    if (testXshift) {
      if (bounds.getMinX() < 0) {
        boundsXshift = new Rectangle((int) bounds.getMinX() + mapDimensions.width, (int) bounds.getMinY(),
            (int) bounds.getWidth(), (int) bounds.getHeight());
      } else {
        boundsXshift = new Rectangle((int) bounds.getMinX() - mapDimensions.width, (int) bounds.getMinY(),
            (int) bounds.getWidth(), (int) bounds.getHeight());
      }
    }
    Rectangle2D boundsYshift = null;
    if (testYshift) {
      if (bounds.getMinY() < 0) {
        boundsYshift = new Rectangle((int) bounds.getMinX(), (int) bounds.getMinY() + mapDimensions.height,
            (int) bounds.getWidth(), (int) bounds.getHeight());
      } else {
        boundsYshift = new Rectangle((int) bounds.getMinX(), (int) bounds.getMinY() - mapDimensions.height,
            (int) bounds.getWidth(), (int) bounds.getHeight());
      }
    }
    acquireLock();
    try {
      final List<Tile> tilesInBounds = new ArrayList<>();
      for (final Tile tile : tiles) {
        final Rectangle tileBounds = tile.getBounds();
        if (bounds.contains(tileBounds) || tileBounds.intersects(bounds)) {
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
    } finally {
      releaseLock();
    }
  }

  private void acquireLock() {
    Tile.LOCK_UTIL.acquireLock(lock);
  }

  private void releaseLock() {
    Tile.LOCK_UTIL.releaseLock(lock);
  }

  Collection<UnitsDrawer> getUnitDrawables() {
    acquireLock();
    try {
      return new ArrayList<>(allUnitDrawables);
    } finally {
      releaseLock();
    }
  }

  public void createTiles(final Rectangle bounds) {
    acquireLock();
    try {
      // create our tiles
      tiles = new ArrayList<>();
      for (int x = 0; ((x) * TILE_SIZE) < bounds.width; x++) {
        for (int y = 0; ((y) * TILE_SIZE) < bounds.height; y++) {
          tiles.add(new Tile(new Rectangle(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE), x, y,
              uiContext.getScale()));
        }
      }
    } finally {
      releaseLock();
    }
  }

  public void resetTiles(final GameData data, final MapData mapData) {
    data.acquireReadLock();
    try {
      acquireLock();
      try {
        final Iterator<Tile> allTiles = tiles.iterator();
        while (allTiles.hasNext()) {
          final Tile tile = allTiles.next();
          tile.clear();
          final int x = tile.getBounds().x / TILE_SIZE;
          final int y = tile.getBounds().y / TILE_SIZE;
          tile.addDrawable(new BaseMapDrawable(x, y, uiContext));
          tile.addDrawable(new ReliefMapDrawable(x, y, uiContext));
        }
        final Iterator<Territory> territories = data.getMap().getTerritories().iterator();
        while (territories.hasNext()) {
          final Territory territory = territories.next();
          clearTerritory(territory);
          drawTerritory(territory, data, mapData);
        }
        // add the decorations
        final Map<Image, List<Point>> decorations = mapData.getDecorations();
        for (final Image img : decorations.keySet()) {
          for (final Point p : decorations.get(img)) {
            final DecoratorDrawable drawable = new DecoratorDrawable(p, img);
            final Rectangle bounds = new Rectangle(p.x, p.y, img.getWidth(null), img.getHeight(null));
            for (final Tile t : getTiles(bounds)) {
              t.addDrawable(drawable);
            }
          }
        }
      } finally {
        releaseLock();
      }
    } finally {
      data.releaseReadLock();
    }
  }

  public void updateTerritories(final Collection<Territory> territories, final GameData data, final MapData mapData) {
    data.acquireReadLock();
    try {
      acquireLock();
      try {
        if (territories == null) {
          return;
        }
        final Iterator<Territory> iter = territories.iterator();
        while (iter.hasNext()) {
          final Territory territory = iter.next();
          updateTerritory(territory, data, mapData);
        }
      } finally {
        releaseLock();
      }
    } finally {
      data.releaseReadLock();
    }
  }

  private void updateTerritory(final Territory territory, final GameData data, final MapData mapData) {
    data.acquireReadLock();
    try {
      acquireLock();
      try {
        logger.log(Level.FINER, "Updating " + territory.getName());
        clearTerritory(territory);
        drawTerritory(territory, data, mapData);
      } finally {
        releaseLock();
      }
    } finally {
      data.releaseReadLock();
    }
  }

  private void clearTerritory(final Territory territory) {
    if (territoryTiles.get(territory.getName()) == null) {
      return;
    }
    final Collection<IDrawable> drawables = territoryDrawables.get(territory.getName());
    if ((drawables == null) || drawables.isEmpty()) {
      return;
    }
    final Iterator<Tile> tiles = territoryTiles.get(territory.getName()).iterator();
    while (tiles.hasNext()) {
      final Tile tile = tiles.next();
      tile.removeDrawables(drawables);
    }
    allUnitDrawables.removeAll(drawables);
  }

  private void drawTerritory(final Territory territory, final GameData data, final MapData mapData) {
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
      drawing.add(new LandTerritoryDrawable(territory.getName()));
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
    final OptionalExtraBorderLevel optionalBorderLevel = uiContext.getDrawTerritoryBordersAgain();
    if (optionalBorderLevel != OptionalExtraBorderLevel.LOW) {
      drawing.add(new OptionalExtraTerritoryBordersDrawable(territory.getName(), optionalBorderLevel));
    }
    drawing.add(new TerritoryNameDrawable(territory.getName(), uiContext));
    if ((ta != null) && ta.isCapital() && mapData.drawCapitolMarkers()) {
      final PlayerID capitalOf = data.getPlayerList().getPlayerId(ta.getCapital());
      drawing.add(new CapitolMarkerDrawable(capitalOf, territory, uiContext));
    }
    if ((ta != null) && (ta.getVictoryCity() != 0)) {
      drawing.add(new VcDrawable(territory));
    }
    // add to the relevant tiles
    final Iterator<Tile> tiles = getTiles(mapData.getBoundingRect(territory.getName())).iterator();
    while (tiles.hasNext()) {
      final Tile tile = tiles.next();
      drawnOn.add(tile);
      tile.addDrawables(drawing);
    }
    territoryDrawables.put(territory.getName(), drawing);
    territoryTiles.put(territory.getName(), drawnOn);
  }

  private static void drawTerritoryEffects(final Territory territory, final MapData mapData,
      final Set<IDrawable> drawing) {
    final Iterator<Point> effectPoints = mapData.getTerritoryEffectPoints(territory).iterator();
    Point drawingPoint = effectPoints.next();
    for (final TerritoryEffect te : TerritoryEffectHelper.getEffects(territory)) {
      drawing.add(new TerritoryEffectDrawable(te, drawingPoint));
      drawingPoint = effectPoints.hasNext() ? effectPoints.next() : drawingPoint;
    }
  }

  private void drawUnits(final Territory territory, final MapData mapData, final Set<Tile> drawnOn,
      final Set<IDrawable> drawing) {
    final Iterator<Point> placementPoints = mapData.getPlacementPoints(territory).iterator();
    if ((placementPoints == null) || !placementPoints.hasNext()) {
      throw new IllegalStateException("No where to place units:" + territory.getName());
    }
    Point lastPlace = null;
    final Iterator<UnitCategory> unitCategoryIter =
        UnitSeperator.categorize(territory.getUnits().getUnits()).iterator();
    while (unitCategoryIter.hasNext()) {
      final UnitCategory category = unitCategoryIter.next();
      final boolean overflow;
      if (placementPoints.hasNext()) {
        lastPlace = new Point(placementPoints.next());
        overflow = false;
      } else {
        lastPlace = new Point(lastPlace);
        lastPlace.x += uiContext.getUnitImageFactory().getUnitImageWidth();
        overflow = true;
      }
      final UnitsDrawer drawable = new UnitsDrawer(category.getUnits().size(), category.getType().getName(),
          category.getOwner().getName(), lastPlace, category.getDamaged(), category.getBombingDamage(),
          category.getDisabled(), overflow, territory.getName(), uiContext);
      drawing.add(drawable);
      allUnitDrawables.add(drawable);
      final Iterator<Tile> tiles =
          getTiles(new Rectangle(lastPlace.x, lastPlace.y, uiContext.getUnitImageFactory().getUnitImageWidth(),
              uiContext.getUnitImageFactory().getUnitImageHeight())).iterator();
      while (tiles.hasNext()) {
        final Tile tile = tiles.next();
        tile.addDrawable(drawable);
        drawnOn.add(tile);
      }
    }
  }

  public Image createTerritoryImage(final Territory t, final GameData data, final MapData mapData) {
    return createTerritoryImage(t, t, data, mapData, true);
  }

  public Image createTerritoryImage(final Territory selected, final Territory focusOn, final GameData data,
      final MapData mapData) {
    return createTerritoryImage(selected, focusOn, data, mapData, false);
  }

  private Image createTerritoryImage(final Territory selected, final Territory focusOn, final GameData data,
      final MapData mapData, final boolean drawOutline) {
    acquireLock();
    try {
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
        if ((bounds.width + bounds.x) > mapDataWidth) {
          bounds.x = mapDataWidth - bounds.width;
        }
      }
      if (!mapData.scrollWrapY()) {
        if (bounds.y < 0) {
          bounds.y = 0;
        }
        if ((bounds.height + bounds.y) > mapDataHeight) {
          bounds.y = mapDataHeight - bounds.height;
        }
      }
      final Image territoryImage = Util.createImage(squareLength, squareLength, false);
      final Graphics2D graphics = (Graphics2D) territoryImage.getGraphics();
      graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
          RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
      graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
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
      if ((bounds.x + bounds.height) > mapDataWidth) {
        bounds.x -= mapDataWidth;
        drawForCreate(selected, data, mapData, bounds, graphics, drawOutline);
        if ((bounds.y + bounds.width) > mapDataHeight) {
          bounds.y -= mapDataHeight;
          drawForCreate(selected, data, mapData, bounds, graphics, drawOutline);
          bounds.y += mapDataHeight;
        }
        bounds.x += mapDataWidth;
      }
      if ((bounds.y + bounds.width) > mapDataHeight) {
        bounds.y -= mapDataHeight;
        drawForCreate(selected, data, mapData, bounds, graphics, drawOutline);
        bounds.y += mapDataHeight;
      }
      graphics.dispose();
      return territoryImage;
    } finally {
      releaseLock();
    }
  }

  private void drawForCreate(final Territory selected, final GameData data, final MapData mapData,
      final Rectangle bounds, final Graphics2D graphics, final boolean drawOutline) {
    final Set<IDrawable> drawablesSet = new HashSet<>();
    final List<Tile> intersectingTiles = getTiles(bounds);
    for (final Tile tile : intersectingTiles) {
      drawablesSet.addAll(tile.getDrawables());
    }
    // the base tiles are scaled to save memory
    // but we want to draw them unscaled here
    // so unscale them
    if (uiContext.getScale() != 1) {
      final List<IDrawable> toAdd = new ArrayList<>();
      final Iterator<IDrawable> iter = drawablesSet.iterator();
      while (iter.hasNext()) {
        final IDrawable drawable = iter.next();
        if (drawable instanceof MapTileDrawable) {
          iter.remove();
          toAdd.add(((MapTileDrawable) drawable).getUnscaledCopy());
        }
      }
      drawablesSet.addAll(toAdd);
    }
    final List<IDrawable> orderedDrawables = new ArrayList<>(drawablesSet);
    Collections.sort(orderedDrawables, new DrawableComparator());
    for (final IDrawable drawer : orderedDrawables) {
      if (drawer.getLevel() >= IDrawable.UNITS_LEVEL) {
        break;
      }
      if (drawer.getLevel() == IDrawable.TERRITORY_TEXT_LEVEL) {
        continue;
      }
      drawer.draw(bounds, data, graphics, mapData, null, null);
    }
    if (!drawOutline) {
      final Color c;
      if (selected.isWater()) {
        c = Color.RED;
      } else {
        c = new Color(0, 0, 0);
      }
      final TerritoryOverLayDrawable told = new TerritoryOverLayDrawable(c, selected.getName(), 100, Operation.FILL);
      told.draw(bounds, data, graphics, mapData, null, null);
    }
    graphics.setStroke(new BasicStroke(10));
    graphics.setColor(Color.RED);
    for (Polygon poly : mapData.getPolygons(selected)) {
      poly = new Polygon(poly.xpoints, poly.ypoints, poly.npoints);
      poly.translate(-bounds.x, -bounds.y);
      graphics.drawPolygon(poly);
    }
  }

  public Rectangle getUnitRect(final List<Unit> units, final GameData data) {
    if (units == null) {
      return null;
    }
    data.acquireReadLock();
    try {
      acquireLock();
      try {
        for (final UnitsDrawer drawer : allUnitDrawables) {
          final List<Unit> drawerUnits = drawer.getUnits(data).getSecond();
          if (!drawerUnits.isEmpty() && units.containsAll(drawerUnits)) {
            final Point placementPoint = drawer.getPlacementPoint();
            return new Rectangle(placementPoint.x, placementPoint.y,
                uiContext.getUnitImageFactory().getUnitImageWidth(),
                uiContext.getUnitImageFactory().getUnitImageHeight());
          }
        }
        return null;
      } finally {
        releaseLock();
      }
    } finally {
      data.releaseReadLock();
    }
  }

  public Tuple<Territory, List<Unit>> getUnitsAtPoint(final double x, final double y, final GameData gameData) {
    gameData.acquireReadLock();
    try {
      acquireLock();
      try {
        for (final UnitsDrawer drawer : allUnitDrawables) {
          final Point placementPoint = drawer.getPlacementPoint();
          if ((x > placementPoint.x)
              && (x < (placementPoint.x + uiContext.getUnitImageFactory().getUnitImageWidth()))) {
            if ((y > placementPoint.y) && (y < (placementPoint.y + uiContext.getUnitImageFactory()
                .getUnitImageHeight()))) {
              return drawer.getUnits(gameData);
            }
          }
        }
        return null;
      } finally {
        releaseLock();
      }
    } finally {
      gameData.releaseReadLock();
    }
  }

  public void setTerritoryOverlay(final Territory territory, final Color color, final int alpha, final GameData data,
      final MapData mapData) {
    acquireLock();
    try {
      final IDrawable drawable = new TerritoryOverLayDrawable(color, territory.getName(), alpha, Operation.DRAW);
      territoryOverlays.put(territory.getName(), drawable);
    } finally {
      releaseLock();
    }
    updateTerritory(territory, data, mapData);
  }

  public void setTerritoryOverlayForBorder(final Territory territory, final Color color, final GameData data,
      final MapData mapData) {
    acquireLock();
    try {
      final IDrawable drawable = new TerritoryOverLayDrawable(color, territory.getName(), Operation.DRAW);
      territoryOverlays.put(territory.getName(), drawable);
    } finally {
      releaseLock();
    }
    updateTerritory(territory, data, mapData);
  }

  public void clearTerritoryOverlay(final Territory territory, final GameData data, final MapData mapData) {
    acquireLock();
    try {
      territoryOverlays.remove(territory.getName());
    } finally {
      releaseLock();
    }
    updateTerritory(territory, data, mapData);
  }
}
