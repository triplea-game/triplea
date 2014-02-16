/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy.triplea.ui.screen;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.thread.LockUtil;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.ui.IUIContext;
import games.strategy.triplea.ui.MapData;
import games.strategy.triplea.ui.screen.IDrawable.OptionalExtraBorderLevel;
import games.strategy.triplea.ui.screen.TerritoryOverLayDrawable.OP;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.ui.Util;
import games.strategy.util.Tuple;

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

/**
 * 
 * 
 * 
 * @author Sean Bridges
 */
public class TileManager
{
	private static final Logger s_logger = Logger.getLogger(TileManager.class.getName());
	public final static int TILE_SIZE = 256;
	private List<Tile> m_tiles = new ArrayList<Tile>();
	private final Lock m_lock = new ReentrantLock();
	private final Map<String, IDrawable> m_territoryOverlays = new HashMap<String, IDrawable>();
	// maps territoryname - collection of drawables
	private final Map<String, Set<IDrawable>> m_territoryDrawables = new HashMap<String, Set<IDrawable>>();
	// maps territoryname - collection of tiles where the territory is drawn
	private final Map<String, Set<Tile>> m_territoryTiles = new HashMap<String, Set<Tile>>();
	private final Collection<UnitsDrawer> m_allUnitDrawables = new ArrayList<UnitsDrawer>();
	private final IUIContext m_uiContext;
	
	public TileManager(final IUIContext uiContext)
	{
		m_uiContext = uiContext;
	}
	
	/**
	 * Selects tiles which fall into rectangle bounds.
	 * 
	 * @param bounds
	 *            rectangle for selection
	 * @return tiles which fall into the rectangle
	 */
	public List<Tile> getTiles(final Rectangle2D bounds)
	{
		// if the rectangle exceeds the map dimensions we to do shift the rectangle and check for each shifted rectangle as well as the original rectangle
		final MapData mapData = m_uiContext.getMapData();
		final Dimension mapDimensions = mapData.getMapDimensions();
		final boolean testXshift = (mapData.scrollWrapX() && (bounds.getMaxX() > mapDimensions.width || bounds.getMinX() < 0));
		final boolean testYshift = (mapData.scrollWrapY() && (bounds.getMaxY() > mapDimensions.height || bounds.getMinY() < 0));
		
		Rectangle2D boundsXshift = null;
		if (testXshift)
		{
			if (bounds.getMinX() < 0)
				boundsXshift = new Rectangle((int) bounds.getMinX() + mapDimensions.width, (int) bounds.getMinY(), (int) bounds.getWidth(), (int) bounds.getHeight());
			else
				boundsXshift = new Rectangle((int) bounds.getMinX() - mapDimensions.width, (int) bounds.getMinY(), (int) bounds.getWidth(), (int) bounds.getHeight());
		}
		Rectangle2D boundsYshift = null;
		if (testYshift)
		{
			if (bounds.getMinY() < 0)
				boundsYshift = new Rectangle((int) bounds.getMinX(), (int) bounds.getMinY() + mapDimensions.height, (int) bounds.getWidth(), (int) bounds.getHeight());
			else
				boundsYshift = new Rectangle((int) bounds.getMinX(), (int) bounds.getMinY() - mapDimensions.height, (int) bounds.getWidth(), (int) bounds.getHeight());
		}
		
		LockUtil.acquireLock(m_lock);
		try
		{
			final List<Tile> rVal = new ArrayList<Tile>();
			for (final Tile tile : m_tiles)
			{
				final Rectangle tileBounds = tile.getBounds();
				if (bounds.contains(tileBounds) || tileBounds.intersects(bounds))
					rVal.add(tile);
			}
			if (boundsXshift != null)
			{
				for (final Tile tile : m_tiles)
				{
					final Rectangle tileBounds = tile.getBounds();
					if (boundsXshift.contains(tileBounds) || tileBounds.intersects(boundsXshift))
						rVal.add(tile);
				}
			}
			if (boundsYshift != null)
			{
				for (final Tile tile : m_tiles)
				{
					final Rectangle tileBounds = tile.getBounds();
					if (boundsYshift.contains(tileBounds) || tileBounds.intersects(boundsYshift))
						rVal.add(tile);
				}
			}
			return rVal;
		} finally
		{
			LockUtil.releaseLock(m_lock);
		}
	}
	
	/*
	public List<Tile> getTilesOldWay(final Rectangle2D bounds)
	{
		LockUtil.acquireLock(m_lock);
		try
		{
			final List<Tile> rVal = new ArrayList<Tile>();
			final Iterator<Tile> iter = m_tiles.iterator();
			while (iter.hasNext())
			{
				final Tile tile = iter.next();
				if (bounds.contains(tile.getBounds()) || tile.getBounds().intersects(bounds))
					rVal.add(tile);
			}
			return rVal;
		} finally
		{
			LockUtil.releaseLock(m_lock);
		}
	}*/
	
	public Collection<UnitsDrawer> getUnitDrawables()
	{
		LockUtil.acquireLock(m_lock);
		try
		{
			return new ArrayList<UnitsDrawer>(m_allUnitDrawables);
		} finally
		{
			LockUtil.releaseLock(m_lock);
		}
	}
	
	public void createTiles(final Rectangle bounds, final GameData data, final MapData mapData)
	{
		LockUtil.acquireLock(m_lock);
		try
		{
			// create our tiles
			m_tiles = new ArrayList<Tile>();
			for (int x = 0; (x) * TILE_SIZE < bounds.width; x++)
			{
				for (int y = 0; (y) * TILE_SIZE < bounds.height; y++)
				{
					m_tiles.add(new Tile(new Rectangle(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE), x, y, m_uiContext.getScale()));
				}
			}
		} finally
		{
			LockUtil.releaseLock(m_lock);
		}
	}
	
	public void resetTiles(final GameData data, final MapData mapData)
	{
		data.acquireReadLock();
		try
		{
			LockUtil.acquireLock(m_lock);
			try
			{
				final Iterator<Tile> allTiles = m_tiles.iterator();
				while (allTiles.hasNext())
				{
					final Tile tile = allTiles.next();
					tile.clear();
					final int x = tile.getBounds().x / TILE_SIZE;
					final int y = tile.getBounds().y / TILE_SIZE;
					tile.addDrawable(new BaseMapDrawable(x, y, m_uiContext));
					tile.addDrawable(new ReliefMapDrawable(x, y, m_uiContext));
				}
				final Iterator<Territory> territories = data.getMap().getTerritories().iterator();
				while (territories.hasNext())
				{
					final Territory territory = territories.next();
					clearTerritory(territory);
					drawTerritory(territory, data, mapData);
				}
				// add the decorations
				final Map<Image, List<Point>> decorations = mapData.getDecorations();
				for (final Image img : decorations.keySet())
				{
					for (final Point p : decorations.get(img))
					{
						final DecoratorDrawable drawable = new DecoratorDrawable(p, img);
						final Rectangle bounds = new Rectangle(p.x, p.y, img.getWidth(null), img.getHeight(null));
						for (final Tile t : getTiles(bounds))
						{
							t.addDrawable(drawable);
						}
					}
				}
			} finally
			{
				LockUtil.releaseLock(m_lock);
			}
		} finally
		{
			data.releaseReadLock();
		}
	}
	
	public void updateTerritories(final Collection<Territory> territories, final GameData data, final MapData mapData)
	{
		data.acquireReadLock();
		try
		{
			LockUtil.acquireLock(m_lock);
			try
			{
				if (territories == null)
					return;
				final Iterator<Territory> iter = territories.iterator();
				while (iter.hasNext())
				{
					final Territory territory = iter.next();
					updateTerritory(territory, data, mapData);
				}
			} finally
			{
				LockUtil.releaseLock(m_lock);
			}
		} finally
		{
			data.releaseReadLock();
		}
	}
	
	public void updateTerritory(final Territory territory, final GameData data, final MapData mapData)
	{
		data.acquireReadLock();
		try
		{
			LockUtil.acquireLock(m_lock);
			try
			{
				s_logger.log(Level.FINER, "Updating " + territory.getName());
				clearTerritory(territory);
				drawTerritory(territory, data, mapData);
			} finally
			{
				LockUtil.releaseLock(m_lock);
			}
		} finally
		{
			data.releaseReadLock();
		}
	}
	
	private void clearTerritory(final Territory territory)
	{
		if (m_territoryTiles.get(territory.getName()) == null)
			return;
		final Collection<IDrawable> drawables = m_territoryDrawables.get(territory.getName());
		if (drawables == null || drawables.isEmpty())
			return;
		final Iterator<Tile> tiles = m_territoryTiles.get(territory.getName()).iterator();
		while (tiles.hasNext())
		{
			final Tile tile = tiles.next();
			tile.removeDrawables(drawables);
		}
		m_allUnitDrawables.removeAll(drawables);
	}
	
	/**
	 * 
	 * 
	 * @param data
	 * @param mapData
	 * @param territory
	 */
	private void drawTerritory(final Territory territory, final GameData data, final MapData mapData)
	{
		final Set<Tile> drawnOn = new HashSet<Tile>();
		final Set<IDrawable> drawing = new HashSet<IDrawable>();
		if (m_territoryOverlays.get(territory.getName()) != null)
			drawing.add(m_territoryOverlays.get(territory.getName()));
		if (m_uiContext.getShowTerritoryEffects())
		{
			drawTerritoryEffects(territory, data, mapData, drawnOn, drawing);
		}
		if (m_uiContext.getShowUnits())
		{
			drawUnits(territory, data, mapData, drawnOn, drawing);
		}
		drawing.add(new BattleDrawable(territory.getName()));
		if (!territory.isWater())
			drawing.add(new LandTerritoryDrawable(territory.getName()));
		else
		{
			if (TerritoryAttachment.get(territory) != null)
			{
				// Kamikaze Zones
				if (TerritoryAttachment.get(territory).getKamikazeZone())
				{
					drawing.add(new KamikazeZoneDrawable(territory.getOwner(), territory, m_uiContext));
				}
				// Blockades
				if (TerritoryAttachment.get(territory).getBlockadeZone())
				{
					drawing.add(new BlockadeZoneDrawable(territory, m_uiContext));
				}
				// Convoy Routes
				if (TerritoryAttachment.get(territory).getConvoyRoute())
				{
					drawing.add(new ConvoyZoneDrawable(territory.getOwner(), territory, m_uiContext));
				}
				// Convoy Centers
				if (TerritoryAttachment.get(territory).getProduction() > 0)
				{
					drawing.add(new ConvoyZoneDrawable(territory.getOwner(), territory, m_uiContext));
				}
			}
			drawing.add(new SeaZoneOutlineDrawable(territory.getName()));
		}
		final OptionalExtraBorderLevel optionalBorderLevel = m_uiContext.getDrawTerritoryBordersAgain();
		if (optionalBorderLevel != OptionalExtraBorderLevel.LOW)
		{
			drawing.add(new OptionalExtraTerritoryBordersDrawable(territory.getName(), optionalBorderLevel));
		}
		drawing.add(new TerritoryNameDrawable(territory.getName(), m_uiContext));
		final TerritoryAttachment ta = TerritoryAttachment.get(territory);
		if (ta != null && ta.isCapital() && mapData.drawCapitolMarkers())
		{
			final PlayerID capitalOf = data.getPlayerList().getPlayerID(ta.getCapital());
			drawing.add(new CapitolMarkerDrawable(capitalOf, territory, m_uiContext));
		}
		if (ta != null && (ta.getVictoryCity() != 0))
		{
			drawing.add(new VCDrawable(territory));
		}
		// add to the relevant tiles
		final Iterator<Tile> tiles = getTiles(mapData.getBoundingRect(territory.getName())).iterator();
		while (tiles.hasNext())
		{
			final Tile tile = tiles.next();
			drawnOn.add(tile);
			tile.addDrawables(drawing);
		}
		m_territoryDrawables.put(territory.getName(), drawing);
		m_territoryTiles.put(territory.getName(), drawnOn);
	}
	
	private void drawTerritoryEffects(final Territory territory, final GameData data, final MapData mapData, final Set<Tile> drawnOn, final Set<IDrawable> drawing)
	{
		final Iterator<Point> effectPoints = mapData.getTerritoryEffectPoints(territory).iterator();
		Point drawingPoint = effectPoints.next();
		
		for (final TerritoryEffect te : TerritoryEffectHelper.getEffects(territory))
		{
			drawing.add(new TerritoryEffectDrawable(te, drawingPoint));
			drawingPoint = effectPoints.hasNext() ? effectPoints.next() : drawingPoint;
		}
	}
	
	private void drawUnits(final Territory territory, final GameData data, final MapData mapData, final Set<Tile> drawnOn, final Set<IDrawable> drawing)
	{
		final Iterator<Point> placementPoints = mapData.getPlacementPoints(territory).iterator();
		if (placementPoints == null || !placementPoints.hasNext())
		{
			throw new IllegalStateException("No where to place units:" + territory.getName());
		}
		Point lastPlace = null;
		final Iterator<UnitCategory> unitCategoryIter = UnitSeperator.categorize(territory.getUnits().getUnits()).iterator();
		while (unitCategoryIter.hasNext())
		{
			final UnitCategory category = unitCategoryIter.next();
			boolean overflow;
			if (placementPoints.hasNext())
			{
				lastPlace = new Point(placementPoints.next());
				overflow = false;
			}
			else
			{
				lastPlace = new Point(lastPlace);
				lastPlace.x += m_uiContext.getUnitImageFactory().getUnitImageWidth();
				overflow = true;
			}
			final UnitsDrawer drawable = new UnitsDrawer(category.getUnits().size(), category.getType().getName(), category.getOwner().getName(), lastPlace, category.getDamaged() > 0,
						category.getDisabled(), overflow, territory.getName(), m_uiContext);
			drawing.add(drawable);
			m_allUnitDrawables.add(drawable);
			final Iterator<Tile> tiles = getTiles(
						new Rectangle(lastPlace.x, lastPlace.y, m_uiContext.getUnitImageFactory().getUnitImageWidth(), m_uiContext.getUnitImageFactory().getUnitImageHeight())).iterator();
			while (tiles.hasNext())
			{
				final Tile tile = tiles.next();
				tile.addDrawable(drawable);
				drawnOn.add(tile);
			}
		}
	}
	
	public Image createTerritoryImage(final Territory t, final GameData data, final MapData mapData)
	{
		return createTerritoryImage(t, t, data, mapData, true);
		// synchronized(m_mutex)
		// {
		// Rectangle bounds = mapData.getBoundingRect(t);
		//
		// Image rVal = Util.createImage( bounds.width, bounds.height, false);
		// Graphics2D graphics = (Graphics2D) rVal.getGraphics();
		//
		// //start as a set to prevent duplicates
		// Set<IDrawable> drawablesSet = new HashSet<IDrawable>();
		// Iterator<Tile> tiles = getTiles(bounds).iterator();
		//
		// while(tiles.hasNext())
		// {
		// Tile tile = tiles.next();
		// drawablesSet.addAll(tile.getDrawables());
		// }
		//
		// List<IDrawable> orderedDrawables = new ArrayList<IDrawable>(drawablesSet);
		// Collections.sort(orderedDrawables, new DrawableComparator());
		// Iterator<IDrawable> drawers = orderedDrawables.iterator();
		// while (drawers.hasNext())
		// {
		// IDrawable drawer = drawers.next();
		// if(drawer.getLevel() >= IDrawable.UNITS_LEVEL)
		// break;
		// if(drawer.getLevel() == IDrawable.TERRITORY_TEXT_LEVEL)
		// continue;
		// drawer.draw(bounds, data, graphics, mapData);
		// }
		//
		// Iterator iter = mapData.getPolygons(t).iterator();
		//
		// graphics.setStroke(new BasicStroke(5));
		// graphics.setColor(Color.RED);
		//
		// while (iter.hasNext())
		// {
		// Polygon poly = (Polygon) iter.next();
		// poly = new Polygon(poly.xpoints, poly.ypoints, poly.npoints);
		// poly.translate(-bounds.x, -bounds.y);
		// graphics.drawPolygon(poly);
		// }
		//
		// graphics.dispose();
		// return rVal;
		// }
	}
	
	public Image createTerritoryImage(final Territory selected, final Territory focusOn, final GameData data, final MapData mapData)
	{
		return createTerritoryImage(selected, focusOn, data, mapData, false);
	}
	
	private Image createTerritoryImage(final Territory selected, final Territory focusOn, final GameData data, final MapData mapData, final boolean drawOutline)
	{
		LockUtil.acquireLock(m_lock);
		try
		{
			// make a square
			final Rectangle bounds = mapData.getBoundingRect(focusOn);
			int square_length = Math.max(bounds.width, bounds.height);
			final int grow = square_length / 4;
			bounds.x -= grow;
			bounds.y -= grow;
			square_length += grow * 2;
			
			// make sure it is not bigger than the whole map
			final int mapDataWidth = mapData.getMapDimensions().width;
			final int mapDataHeight = mapData.getMapDimensions().height;
			if (square_length > mapDataWidth)
				square_length = mapDataWidth;
			if (square_length > mapDataHeight)
				square_length = mapDataHeight;
			bounds.width = square_length;
			bounds.height = square_length;
			
			// keep it in bounds
			if (!mapData.scrollWrapX())
			{
				if (bounds.x < 0)
					bounds.x = 0;
				if (bounds.width + bounds.x > mapDataWidth)
					bounds.x = mapDataWidth - bounds.width;
			}
			if (!mapData.scrollWrapY())
			{
				if (bounds.y < 0)
					bounds.y = 0;
				if (bounds.height + bounds.y > mapDataHeight)
					bounds.y = mapDataHeight - bounds.height;
			}
			final Image rVal = Util.createImage(square_length, square_length, false);
			final Graphics2D graphics = (Graphics2D) rVal.getGraphics();
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			if (bounds.x < 0)
			{
				bounds.x += mapDataWidth;
				drawForCreate(selected, data, mapData, bounds, graphics, drawOutline);
				if (bounds.y < 0)
				{
					bounds.y += mapDataHeight;
					drawForCreate(selected, data, mapData, bounds, graphics, drawOutline);
					bounds.y -= mapDataHeight;
				}
				bounds.x -= mapDataWidth;
			}
			if (bounds.y < 0)
			{
				bounds.y += mapDataHeight;
				drawForCreate(selected, data, mapData, bounds, graphics, drawOutline);
				bounds.y -= mapDataHeight;
			}
			// start as a set to prevent duplicates
			drawForCreate(selected, data, mapData, bounds, graphics, drawOutline);
			if (bounds.x + bounds.height > mapDataWidth)
			{
				bounds.x -= mapDataWidth;
				drawForCreate(selected, data, mapData, bounds, graphics, drawOutline);
				if (bounds.y + bounds.width > mapDataHeight)
				{
					bounds.y -= mapDataHeight;
					drawForCreate(selected, data, mapData, bounds, graphics, drawOutline);
					bounds.y += mapDataHeight;
				}
				bounds.x += mapDataWidth;
			}
			if (bounds.y + bounds.width > mapDataHeight)
			{
				bounds.y -= mapDataHeight;
				drawForCreate(selected, data, mapData, bounds, graphics, drawOutline);
				bounds.y += mapDataHeight;
			}
			graphics.dispose();
			return rVal;
		} finally
		{
			LockUtil.releaseLock(m_lock);
		}
	}
	
	/*
	private Image createTerritoryImageOldWay(final Territory selected, final Territory focusOn, final GameData data, final MapData mapData, final boolean drawOutline)
	{
		LockUtil.acquireLock(m_lock);
		try
		{
			final Rectangle bounds = mapData.getBoundingRect(focusOn);
			// make it square
			if (bounds.width > bounds.height)
				bounds.height = bounds.width;
			else
				bounds.width = bounds.height;
			final int grow = bounds.width / 4;
			bounds.x -= grow;
			bounds.y -= grow;
			bounds.width += grow * 2;
			bounds.height += grow * 2;
			// make sure it is not bigger than the whole map
			if (bounds.width > mapData.getMapDimensions().width)
				bounds.width = mapData.getMapDimensions().width;
			if (bounds.height > mapData.getMapDimensions().height)
				bounds.height = mapData.getMapDimensions().height;
			// make sure it is still square
			if (bounds.width > bounds.height)
				bounds.width = bounds.height;
			else
				bounds.height = bounds.width;
			
			// keep it in bounds
			if (bounds.x < 0 && !mapData.scrollWrapX())
			{
				bounds.x = 0;
			}
			if (bounds.y < 0 && !mapData.scrollWrapY())
			{
				bounds.y = 0;
			}
			if (bounds.width + bounds.x > mapData.getMapDimensions().width && !mapData.scrollWrapX())
			{
				final int move = bounds.width + bounds.x - mapData.getMapDimensions().width;
				bounds.x -= move;
			}
			if (bounds.height + bounds.y > mapData.getMapDimensions().height && !mapData.scrollWrapY())
			{
				final int move = bounds.height + bounds.y - mapData.getMapDimensions().height;
				bounds.y -= move;
			}
			if (bounds.width != bounds.height)
				throw new IllegalStateException("NOT equal");
			final Image rVal = Util.createImage(bounds.width, bounds.height, false);
			final Graphics2D graphics = (Graphics2D) rVal.getGraphics();
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			if (bounds.x < 0)
			{
				bounds.x += mapData.getMapDimensions().width;
				drawForCreate(selected, data, mapData, bounds, graphics, drawOutline);
				bounds.x -= mapData.getMapDimensions().width;
			}
			if (bounds.y < 0)
			{
				bounds.y += mapData.getMapDimensions().height;
				drawForCreate(selected, data, mapData, bounds, graphics, drawOutline);
				bounds.y -= mapData.getMapDimensions().height;
			}
			// start as a set to prevent duplicates
			drawForCreate(selected, data, mapData, bounds, graphics, drawOutline);
			if (bounds.x + bounds.height > mapData.getMapDimensions().width)
			{
				bounds.x -= mapData.getMapDimensions().width;
				drawForCreate(selected, data, mapData, bounds, graphics, drawOutline);
				bounds.x += mapData.getMapDimensions().width;
			}
			if (bounds.y + bounds.width > mapData.getMapDimensions().height)
			{
				bounds.y -= mapData.getMapDimensions().height;
				drawForCreate(selected, data, mapData, bounds, graphics, drawOutline);
				bounds.y += mapData.getMapDimensions().height;
			}
			graphics.dispose();
			return rVal;
		} finally
		{
			LockUtil.releaseLock(m_lock);
		}
	}*/
	
	private void drawForCreate(final Territory selected, final GameData data, final MapData mapData, final Rectangle bounds, final Graphics2D graphics, final boolean drawOutline)
	{
		final Set<IDrawable> drawablesSet = new HashSet<IDrawable>();
		final List<Tile> intersectingTiles = getTiles(bounds);
		for (final Tile tile : intersectingTiles)
		{
			drawablesSet.addAll(tile.getDrawables());
		}
		// the base tiles are scaled to save memory
		// but we want to draw them unscaled here
		// so unscale them
		if (m_uiContext.getScale() != 1)
		{
			final List<IDrawable> toAdd = new ArrayList<IDrawable>();
			final Iterator<IDrawable> iter = drawablesSet.iterator();
			while (iter.hasNext())
			{
				final IDrawable drawable = iter.next();
				if (drawable instanceof MapTileDrawable)
				{
					iter.remove();
					toAdd.add(((MapTileDrawable) drawable).getUnscaledCopy());
				}
			}
			drawablesSet.addAll(toAdd);
		}
		final List<IDrawable> orderedDrawables = new ArrayList<IDrawable>(drawablesSet);
		Collections.sort(orderedDrawables, new DrawableComparator());
		for (final IDrawable drawer : orderedDrawables)
		{
			if (drawer.getLevel() >= IDrawable.UNITS_LEVEL)
				break;
			if (drawer.getLevel() == IDrawable.TERRITORY_TEXT_LEVEL)
				continue;
			drawer.draw(bounds, data, graphics, mapData, null, null);
		}
		if (!drawOutline)
		{
			Color c;
			if (selected.isWater())
			{
				c = Color.RED;
			}
			else
			{
				c = mapData.getPlayerColor(selected.getOwner().getName());
				c = new Color(c.getRed() ^ c.getRed(), c.getGreen() ^ c.getGreen(), c.getRed() ^ c.getRed());
			}
			final TerritoryOverLayDrawable told = new TerritoryOverLayDrawable(c, selected.getName(), 100, OP.FILL);
			told.draw(bounds, data, graphics, mapData, null, null);
		}
		graphics.setStroke(new BasicStroke(10));
		graphics.setColor(Color.RED);
		for (Polygon poly : mapData.getPolygons(selected))
		{
			poly = new Polygon(poly.xpoints, poly.ypoints, poly.npoints);
			poly.translate(-bounds.x, -bounds.y);
			graphics.drawPolygon(poly);
		}
	}
	
	public Rectangle getUnitRect(final List<Unit> units, final GameData data)
	{
		if (units == null)
			return null;
		data.acquireReadLock();
		try
		{
			LockUtil.acquireLock(m_lock);
			try
			{
				for (final UnitsDrawer drawer : m_allUnitDrawables)
				{
					final List<Unit> drawerUnits = drawer.getUnits(data).getSecond();
					if (!drawerUnits.isEmpty() && units.containsAll(drawerUnits))
					{
						final Point placementPoint = drawer.getPlacementPoint();
						return new Rectangle(placementPoint.x, placementPoint.y, m_uiContext.getUnitImageFactory().getUnitImageWidth(), m_uiContext.getUnitImageFactory().getUnitImageHeight());
					}
				}
				return null;
			} finally
			{
				LockUtil.releaseLock(m_lock);
			}
		} finally
		{
			data.releaseReadLock();
		}
	}
	
	public Tuple<Territory, List<Unit>> getUnitsAtPoint(final double x, final double y, final GameData gameData)
	{
		gameData.acquireReadLock();
		try
		{
			LockUtil.acquireLock(m_lock);
			try
			{
				for (final UnitsDrawer drawer : m_allUnitDrawables)
				{
					final Point placementPoint = drawer.getPlacementPoint();
					if (x > placementPoint.x && x < placementPoint.x + m_uiContext.getUnitImageFactory().getUnitImageWidth())
					{
						if (y > placementPoint.y && y < placementPoint.y + m_uiContext.getUnitImageFactory().getUnitImageHeight())
						{
							return drawer.getUnits(gameData);
						}
					}
				}
				return null;
			} finally
			{
				LockUtil.releaseLock(m_lock);
			}
		} finally
		{
			gameData.releaseReadLock();
		}
	}
	
	public void setTerritoryOverlay(final Territory territory, final Color color, final int alpha, final GameData data, final MapData mapData)
	{
		LockUtil.acquireLock(m_lock);
		try
		{
			final IDrawable drawable = new TerritoryOverLayDrawable(color, territory.getName(), alpha, OP.DRAW);
			m_territoryOverlays.put(territory.getName(), drawable);
		} finally
		{
			LockUtil.releaseLock(m_lock);
		}
		updateTerritory(territory, data, mapData);
	}
	
	public void setTerritoryOverlayForBorder(final Territory territory, final Color color, final GameData data, final MapData mapData)
	{
		LockUtil.acquireLock(m_lock);
		try
		{
			final IDrawable drawable = new TerritoryOverLayDrawable(color, territory.getName(), OP.DRAW);
			m_territoryOverlays.put(territory.getName(), drawable);
		} finally
		{
			LockUtil.releaseLock(m_lock);
		}
		updateTerritory(territory, data, mapData);
	}
	
	public void clearTerritoryOverlay(final Territory territory, final GameData data, final MapData mapData)
	{
		LockUtil.acquireLock(m_lock);
		try
		{
			m_territoryOverlays.remove(territory.getName());
		} finally
		{
			LockUtil.releaseLock(m_lock);
		}
		updateTerritory(territory, data, mapData);
	}
}
