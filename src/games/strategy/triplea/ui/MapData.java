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
package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.util.PointFileReaderWriter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.imageio.ImageIO;

/**
 * contains data about the territories useful for drawing
 */
public class MapData
{
	private static final String UNIT_SCALE_PROPERTY = "units.scale";
	private static final String UNIT_WIDTH_PROPERTY = "units.width";
	private static final String UNIT_HEIGHT_PROPERTY = "units.height";
	private static final String UNIT_COUNTER_OFFSET_WIDTH_PROPERTY = "units.counter.offset.width";
	private static final String UNIT_COUNTER_OFFSET_HEIGHT_PROPERTY = "units.counter.offset.height";
	private static final String HAS_RELIEF_IMAGES = "map.hasRelief";
	private static final String HAS_MAP_BLENDS = "map.mapBlends";
	private static final String MAP_BLEND_MODE = "map.mapBlendMode";
	private static final String MAP_BLEND_ALPHA = "map.mapBlendAlpha";
	private static final String SHOW_CAPITOL_MARKERS = "map.showCapitolMarkers";
	private static final String SHOW_TERRITORY_NAMES = "map.showTerritoryNames";
	private static final String SHOW_CONVOY_NAMES = "map.showConvoyNames";
	private static final String USE_NATION_CONVOY_FLAGS = "map.useNation_convoyFlags";
	private static final String USE_TERRITORY_EFFECTS_MARKERS = "map.useTerritoryEffectMarkers";
	private static final String CENTERS_FILE = "centers.txt";
	private static final String POLYGON_FILE = "polygons.txt";
	private static final String PLACEMENT_FILE = "place.txt";
	private static final String TERRITORY_EFFECT_FILE = "territory_effects.txt";
	private static final String MAP_PROPERTIES = "map.properties";
	private static final String CAPITAL_MARKERS = "capitols.txt";
	private static final String CONVOY_MARKERS = "convoy.txt";
	private static final String VC_MARKERS = "vc.txt";
	private static final String BLOCKADE_MARKERS = "blockade.txt";
	private static final String IMPASSIBLE = "Impassible";
	private static final String PU_PLACE_FILE = "pu_place.txt";
	private static final String TERRITORY_NAME_PLACE_FILE = "name_place.txt";
	private static final String KAMIKAZE_FILE = "kamikaze_place.txt";
	private static final String DONT_DRAW_TERRITORY_NAME = "dont_draw_territory_names";
	private static final String DECORATIONS_FILE = "decorations.txt";
	// default colour if none is defined.
	private final List<Color> m_defaultColours = new ArrayList<Color>(
				Arrays.asList(new Color[] { Color.RED, Color.MAGENTA, Color.YELLOW, Color.ORANGE, Color.CYAN, Color.GREEN, Color.PINK, Color.GRAY }));
	// maps PlayerName as String to Color
	private final Map<String, Color> m_playerColors = new HashMap<String, Color>();
	// maps String -> List of points
	private Map<String, List<Point>> m_place;
	// maps String -> Collection of Polygons
	private Map<String, List<Polygon>> m_polys;
	// maps String -> Point
	private Map<String, Point> m_centers;
	// maps String -> Point
	private Map<String, Point> m_vcPlace;
	// maps String -> Point
	private Map<String, Point> m_blockadePlace;
	// maps String -> Point
	private Map<String, Point> m_convoyPlace;
	// maps String -> Point
	private Map<String, Point> m_PUPlace;
	// maps String -> Point
	private Map<String, Point> m_namePlace;
	// maps String -> Point
	private Map<String, Point> m_kamikazePlace;
	// maps String -> Point
	private Map<String, Point> m_capitolPlace;
	// maps String -> List of String
	private Map<String, List<String>> m_contains;
	private Properties m_mapProperties;
	
	private Map<String, List<Point>> m_territoryEffects;
	
	// we shouldnt draw the names to these territories
	private Set<String> m_undrawnTerritoriesNames;
	private Map<Image, List<Point>> m_decorations;
	private final Map<String, Image> m_effectImages = new HashMap<String, Image>();
	private final ResourceLoader m_resourceLoader;
	private BufferedImage m_vcImage;
	private BufferedImage m_blockadeImage;
	private BufferedImage m_errorImage = null;
	private BufferedImage m_warningImage = null;
	private BufferedImage m_infoImage = null;
	private BufferedImage m_helpImage = null;
	
	public boolean scrollWrapX()
	{
		return Boolean.valueOf(m_mapProperties.getProperty("map.scrollWrapX", "true")).booleanValue();
	}
	
	public MapData(final String mapNameDir)
	{
		this(ResourceLoader.getMapResourceLoader(mapNameDir));
	}
	
	/**
	 * Constructor TerritoryData(java.lang.String)
	 * 
	 * Sets the map directory for this instance of TerritoryData
	 * 
	 * @param java
	 *            .lang.String
	 *            mapNameDir the given map directory
	 * 
	 */
	public MapData(final ResourceLoader loader)
	{
		m_resourceLoader = loader;
		try
		{
			final String prefix = "";
			m_place = PointFileReaderWriter.readOneToMany(loader.getResourceAsStream(prefix + PLACEMENT_FILE));
			m_territoryEffects = PointFileReaderWriter.readOneToMany(loader.getResourceAsStream(prefix + TERRITORY_EFFECT_FILE));
			m_polys = PointFileReaderWriter.readOneToManyPolygons(loader.getResourceAsStream(prefix + POLYGON_FILE));
			m_centers = PointFileReaderWriter.readOneToOneCenters(loader.getResourceAsStream(prefix + CENTERS_FILE));
			m_vcPlace = PointFileReaderWriter.readOneToOne(loader.getResourceAsStream(prefix + VC_MARKERS));
			m_convoyPlace = PointFileReaderWriter.readOneToOne(loader.getResourceAsStream(prefix + CONVOY_MARKERS));
			m_blockadePlace = PointFileReaderWriter.readOneToOne(loader.getResourceAsStream(prefix + BLOCKADE_MARKERS));
			m_capitolPlace = PointFileReaderWriter.readOneToOne(loader.getResourceAsStream(prefix + CAPITAL_MARKERS));
			m_PUPlace = PointFileReaderWriter.readOneToOne(loader.getResourceAsStream(prefix + PU_PLACE_FILE));
			m_namePlace = PointFileReaderWriter.readOneToOne(loader.getResourceAsStream(prefix + TERRITORY_NAME_PLACE_FILE));
			m_kamikazePlace = PointFileReaderWriter.readOneToOne(loader.getResourceAsStream(prefix + KAMIKAZE_FILE));
			m_mapProperties = new Properties();
			loadDecorations();
			try
			{
				final URL url = loader.getResource(prefix + MAP_PROPERTIES);
				if (url == null)
					throw new IllegalStateException("No map.properties file defined");
				m_mapProperties.load(url.openStream());
			} catch (final Exception e)
			{
				System.out.println("Error reading map.properties:" + e);
			}
			initializeContains();
		} catch (final IOException ex)
		{
			ex.printStackTrace();
		}
	}
	
	public void close()
	{
		m_resourceLoader.close();
	}
	
	private void loadDecorations() throws IOException
	{
		final URL decorations = m_resourceLoader.getResource(DECORATIONS_FILE);
		if (decorations == null)
		{
			m_decorations = Collections.emptyMap();
			return;
		}
		m_decorations = new HashMap<Image, List<Point>>();
		InputStream stream = null;
		try
		{
			stream = decorations.openStream();
			final Map<String, List<Point>> points = PointFileReaderWriter.readOneToMany(stream);
			for (final String name : points.keySet())
			{
				final Image img = loadImage("misc/" + name);
				m_decorations.put(img, points.get(name));
			}
		} finally
		{
			if (stream != null)
			{
				try
				{
					stream.close();
				} catch (final IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	public double getDefaultUnitScale()
	{
		if (m_mapProperties.getProperty(UNIT_SCALE_PROPERTY) == null)
			return 1.0;
		try
		{
			return Double.parseDouble(m_mapProperties.getProperty(UNIT_SCALE_PROPERTY));
		} catch (final NumberFormatException e)
		{
			e.printStackTrace();
			return 1.0;
		}
	}
	
	/**
	 * Does not take account of any scaling.
	 * 
	 * @return
	 */
	public int getDefaultUnitWidth()
	{
		if (m_mapProperties.getProperty(UNIT_WIDTH_PROPERTY) == null)
			return UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
		try
		{
			return Integer.parseInt(m_mapProperties.getProperty(UNIT_WIDTH_PROPERTY));
		} catch (final NumberFormatException e)
		{
			e.printStackTrace();
			return UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
		}
	}
	
	/**
	 * Does not take account of any scaling.
	 * 
	 * @return
	 */
	public int getDefaultUnitHeight()
	{
		if (m_mapProperties.getProperty(UNIT_HEIGHT_PROPERTY) == null)
			return UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
		try
		{
			return Integer.parseInt(m_mapProperties.getProperty(UNIT_HEIGHT_PROPERTY));
		} catch (final NumberFormatException e)
		{
			e.printStackTrace();
			return UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
		}
	}
	
	/**
	 * Does not take account of any scaling.
	 * 
	 * @return
	 */
	public int getDefaultUnitCounterOffsetWidth()
	{
		// if it is not set, divide by 4 so that it is roughly centered
		if (m_mapProperties.getProperty(UNIT_COUNTER_OFFSET_WIDTH_PROPERTY) == null)
			return getDefaultUnitWidth() / 4;
		try
		{
			return Integer.parseInt(m_mapProperties.getProperty(UNIT_COUNTER_OFFSET_WIDTH_PROPERTY));
		} catch (final NumberFormatException e)
		{
			e.printStackTrace();
			return getDefaultUnitWidth() / 4;
		}
	}
	
	/**
	 * Does not take account of any scaling.
	 * 
	 * @return
	 */
	public int getDefaultUnitCounterOffsetHeight()
	{
		// put at bottom of unit, if not set
		if (m_mapProperties.getProperty(UNIT_COUNTER_OFFSET_HEIGHT_PROPERTY) == null)
			return getDefaultUnitHeight();
		try
		{
			return Integer.parseInt(m_mapProperties.getProperty(UNIT_COUNTER_OFFSET_HEIGHT_PROPERTY));
		} catch (final NumberFormatException e)
		{
			e.printStackTrace();
			return getDefaultUnitHeight();
		}
	}
	
	public boolean shouldDrawTerritoryName(final String territoryName)
	{
		if (m_undrawnTerritoriesNames == null)
		{
			final String property = m_mapProperties.getProperty(DONT_DRAW_TERRITORY_NAME, "");
			m_undrawnTerritoriesNames = new HashSet<String>(Arrays.asList(property.split(",")));
		}
		return !m_undrawnTerritoriesNames.contains(territoryName);
	}
	
	public boolean getHasRelief()
	{
		return Boolean.valueOf(m_mapProperties.getProperty(HAS_RELIEF_IMAGES, "true")).booleanValue();
	}
	
	public boolean getHasMapBlends()
	{
		return Boolean.valueOf(m_mapProperties.getProperty(HAS_MAP_BLENDS, "false")).booleanValue();
	}
	
	public String getMapBlendMode()
	{
		return String.valueOf(m_mapProperties.getProperty(MAP_BLEND_MODE, "normal")).toString();
	}
	
	public float getMapBlendAlpha()
	{
		return Float.valueOf(m_mapProperties.getProperty(MAP_BLEND_ALPHA, "0.5f"));
	}
	
	public boolean drawCapitolMarkers()
	{
		return Boolean.valueOf(m_mapProperties.getProperty(SHOW_CAPITOL_MARKERS, "true")).booleanValue();
	}
	
	public boolean drawTerritoryNames()
	{
		return Boolean.valueOf(m_mapProperties.getProperty(SHOW_TERRITORY_NAMES, "true")).booleanValue();
	}
	
	public boolean drawConvoyNames()
	{
		return Boolean.valueOf(m_mapProperties.getProperty(SHOW_CONVOY_NAMES, "true")).booleanValue();
	}
	
	public boolean useNation_convoyFlags()
	{
		return Boolean.valueOf(m_mapProperties.getProperty(USE_NATION_CONVOY_FLAGS, "false")).booleanValue();
	}
	
	public boolean useTerritoryEffectMarkers()
	{
		return Boolean.valueOf(m_mapProperties.getProperty(USE_TERRITORY_EFFECTS_MARKERS, "false")).booleanValue();
	}
	
	private void initializeContains()
	{
		m_contains = new HashMap<String, List<String>>();
		final Iterator<String> seaIter = getTerritories().iterator();
		while (seaIter.hasNext())
		{
			final List<String> contained = new ArrayList<String>();
			final String seaTerritory = seaIter.next();
			if (!(seaTerritory.endsWith("Sea Zone") || seaTerritory.startsWith("Sea Zone")))
				continue;
			final Iterator<String> landIter = getTerritories().iterator();
			while (landIter.hasNext())
			{
				final String landTerritory = landIter.next();
				if (landTerritory.endsWith("Sea Zone") || landTerritory.startsWith("Sea Zone"))
					continue;
				final Polygon landPoly = getPolygons(landTerritory).iterator().next();
				final Polygon seaPoly = getPolygons(seaTerritory).iterator().next();
				if (seaPoly.contains(landPoly.getBounds()))
				{
					contained.add(landTerritory);
				}
			}
			if (!contained.isEmpty())
				m_contains.put(seaTerritory, contained);
		}
	}
	
	public boolean getBooleanProperty(final String propertiesKey)
	{
		return Boolean.valueOf(m_mapProperties.getProperty(propertiesKey, "true")).booleanValue();
	}
	
	public Color getColorProperty(final String propertiesKey) throws IllegalStateException
	{
		String colorString;
		if (m_mapProperties.getProperty(propertiesKey) != null)
		{
			colorString = m_mapProperties.getProperty(propertiesKey);
			if (colorString.length() != 6)
				throw new IllegalStateException("Colors must be a 6 digit hex number, eg FF0011, not:" + colorString);
			try
			{
				final Integer colorInt = Integer.decode("0x" + colorString);
				final Color color = new Color(colorInt.intValue());
				return color;
			} catch (final NumberFormatException nfe)
			{
				throw new IllegalStateException("Player colors must be a 6 digit hex number, eg FF0011");
			}
		}
		return null;
	}
	
	public Color getPlayerColor(final String playerName)
	{
		// already loaded, just return
		if (m_playerColors.containsKey(playerName))
			return m_playerColors.get(playerName);
		// look in map.properties
		final String propertiesKey = "color." + playerName;
		Color color = null;
		try
		{
			color = getColorProperty(propertiesKey);
		} catch (final Exception e)
		{
			throw new IllegalStateException("Player colors must be a 6 digit hex number, eg FF0011");
		}
		if (color == null)
		{
			System.out.println("No color defined for " + playerName + ".  Edit map.properties in the map folder to set it");
			color = m_defaultColours.remove(0);
		}
		// dont crash, use one of our default colors
		// its ugly, but usable
		m_playerColors.put(playerName, color);
		return color;
	}
	
	/**
	 * returns the named property, or null
	 */
	public String getProperty(final String propertiesKey)
	{
		return m_mapProperties.getProperty(propertiesKey);
	}
	
	/**
	 * returns the color for impassible territories
	 */
	public Color impassibleColor()
	{
		// just use getPlayerColor, since it parses the properties
		return getPlayerColor(IMPASSIBLE);
	}
	
	/**
	 * 
	 * @return a Set of territory names as Strings. generally this shouldnt be
	 *         used, instead you should use aGameData.getMap().getTerritories()
	 */
	public Set<String> getTerritories()
	{
		return m_polys.keySet();
	}
	
	/**
	 * Does this territory have any territories contained within it
	 */
	public boolean hasContainedTerritory(final String territoryName)
	{
		return m_contains.containsKey(territoryName);
	}
	
	/**
	 * returns the name of the territory contained in the given territory. This
	 * applies to islands within sea zones.
	 * 
	 * @return possiblly null
	 */
	public List<String> getContainedTerritory(final String territoryName)
	{
		return m_contains.get(territoryName);
	}
	
	public void verify(final GameData data)
	{
		verifyKeys(data, m_centers, "centers");
		verifyKeys(data, m_polys, "polygons");
		verifyKeys(data, m_place, "place");
	}
	
	private void verifyKeys(final GameData data, final Map<String, ?> aMap, final String dataTypeForErrorMessage) throws IllegalStateException
	{
		final StringBuilder errors = new StringBuilder();
		final Iterator<String> iter = aMap.keySet().iterator();
		while (iter.hasNext())
		{
			final String name = iter.next();
			final Territory terr = data.getMap().getTerritory(name);
			// allow loading saved games with missing territories; just ignore them
			if (terr == null)
				iter.remove();
		}
		final Iterator<Territory> territories = data.getMap().getTerritories().iterator();
		final Set<String> keySet = aMap.keySet();
		while (territories.hasNext())
		{
			final Territory terr = territories.next();
			if (!keySet.contains(terr.getName()))
			{
				errors.append("No data of type " + dataTypeForErrorMessage + " for territory:" + terr.getName() + "\n");
			}
		}
		if (errors.length() > 0)
			throw new IllegalStateException(errors.toString());
	}
	
	public List<Point> getPlacementPoints(final Territory terr)
	{
		return m_place.get(terr.getName());
	}
	
	public List<Polygon> getPolygons(final String terr)
	{
		return m_polys.get(terr);
	}
	
	public List<Polygon> getPolygons(final Territory terr)
	{
		return getPolygons(terr.getName());
	}
	
	public Point getCenter(final String terr)
	{
		if (m_centers.get(terr) == null)
		{
			throw new IllegalStateException("Missing " + CENTERS_FILE + " data for " + terr);
		}
		return new Point(m_centers.get(terr));
	}
	
	public Point getCenter(final Territory terr)
	{
		return getCenter(terr.getName());
	}
	
	public Point getCapitolMarkerLocation(final Territory terr)
	{
		if (m_capitolPlace.containsKey(terr.getName()))
			return m_capitolPlace.get(terr.getName());
		return getCenter(terr);
	}
	
	public Point getConvoyMarkerLocation(final Territory terr)
	{
		if (m_convoyPlace.containsKey(terr.getName()))
			return m_convoyPlace.get(terr.getName());
		return getCenter(terr);
	}
	
	public Point getKamikazeMarkerLocation(final Territory terr)
	{
		if (m_kamikazePlace.containsKey(terr.getName()))
			return m_kamikazePlace.get(terr.getName());
		return getCenter(terr);
	}
	
	public Point getVCPlacementPoint(final Territory terr)
	{
		if (m_vcPlace.containsKey(terr.getName()))
			return m_vcPlace.get(terr.getName());
		return getCenter(terr);
	}
	
	public Point getBlockadePlacementPoint(final Territory terr)
	{
		if (m_blockadePlace.containsKey(terr.getName()))
			return m_blockadePlace.get(terr.getName());
		return getCenter(terr);
	}
	
	public Point getPUPlacementPoint(final Territory terr)
	{
		if (m_PUPlace.containsKey(terr.getName()))
			return m_PUPlace.get(terr.getName());
		return null;
	}
	
	public Point getNamePlacementPoint(final Territory terr)
	{
		if (m_namePlace.containsKey(terr.getName()))
			return m_namePlace.get(terr.getName());
		return null;
	}
	
	/**
	 * Get the territory at the x,y co-ordinates could be null.
	 */
	public String getTerritoryAt(final double x, final double y)
	{
		String seaName = null;
		// try to find a land territory.
		// sea zones often surround a land territory
		final Iterator<String> keyIter = m_polys.keySet().iterator();
		while (keyIter.hasNext())
		{
			final String name = keyIter.next();
			final Collection<Polygon> polygons = m_polys.get(name);
			final Iterator<Polygon> polyIter = polygons.iterator();
			while (polyIter.hasNext())
			{
				final Polygon poly = polyIter.next();
				if (poly.contains(x, y))
				{
					if (name.endsWith("Sea Zone") || name.startsWith("Sea Zone"))
					{
						seaName = name;
					}
					else
						return name;
				}
			}
		}
		return seaName;
	}
	
	public Dimension getMapDimensions()
	{
		final String widthProperty = m_mapProperties.getProperty("map.width");
		final String heightProperty = m_mapProperties.getProperty("map.height");
		if (widthProperty == null || heightProperty == null)
			throw new IllegalStateException("Missing map.width or map.height in " + MAP_PROPERTIES);
		final int width = Integer.parseInt(widthProperty.trim());
		final int height = Integer.parseInt(heightProperty.trim());
		return new Dimension(width, height);
	}
	
	public Rectangle getBoundingRect(final Territory terr)
	{
		final String name = terr.getName();
		return getBoundingRect(name);
	}
	
	public Rectangle getBoundingRect(final String name)
	{
		final List<Polygon> polys = getPolygons(name);
		if (polys == null)
			throw new IllegalStateException("No polygons found for:" + name + " All territories:" + m_polys.keySet());
		Rectangle bounds = null;
		for (int i = 0; i < polys.size(); i++)
		{
			final Polygon item = polys.get(i);
			if (bounds == null)
				bounds = item.getBounds();
			else
				bounds.add(item.getBounds());
		}
		return bounds;
	}
	
	/**
	 * Get the territories that intersect or are bound by this shapes bounding
	 * rect.
	 * 
	 * @return List of territory names as Strings
	 */
	public List<String> territoriesThatOverlap(final Rectangle2D bounds)
	{
		List<String> rVal = null;
		final Iterator<String> terrIter = getTerritories().iterator();
		while (terrIter.hasNext())
		{
			final String terr = terrIter.next();
			final List<Polygon> polygons = getPolygons(terr);
			for (int i = 0; i < polygons.size(); i++)
			{
				final Polygon item = polygons.get(i);
				if (item.intersects(bounds) || item.contains(bounds) || bounds.contains(item.getBounds2D()))
				{
					if (rVal == null)
						rVal = new ArrayList<String>(4);
					rVal.add(terr);
					// only add it once
					break;
				}
			}
		}
		if (rVal == null)
			return Collections.emptyList();
		return rVal;
	}
	
	public Image getVCImage()
	{
		if (m_vcImage != null)
			return m_vcImage;
		m_vcImage = loadImage("misc/vc.png");
		return m_vcImage;
	}
	
	public Image getBlockadeImage()
	{
		if (m_blockadeImage != null)
			return m_blockadeImage;
		m_blockadeImage = loadImage("misc/blockade.png");
		return m_blockadeImage;
	}
	
	public Image getErrorImage()
	{
		if (m_errorImage != null)
			return m_errorImage;
		m_errorImage = loadImage("misc/error.gif");
		return m_errorImage;
	}
	
	public Image getWarningImage()
	{
		if (m_warningImage != null)
			return m_warningImage;
		m_warningImage = loadImage("misc/warning.gif");
		return m_warningImage;
	}
	
	public Image getInfoImage()
	{
		if (m_infoImage != null)
			return m_infoImage;
		m_infoImage = loadImage("misc/information.gif");
		return m_infoImage;
	}
	
	public Image getHelpImage()
	{
		if (m_helpImage != null)
			return m_helpImage;
		m_helpImage = loadImage("misc/help.gif");
		return m_helpImage;
	}
	
	private BufferedImage loadImage(final String imageName)
	{
		final URL url = m_resourceLoader.getResource(imageName);
		if (url == null)
			throw new IllegalStateException("Could not load " + imageName);
		try
		{
			return ImageIO.read(url);
		} catch (final IOException e)
		{
			e.printStackTrace();
			throw new IllegalStateException(e.getMessage());
		}
	}
	
	public Map<Image, List<Point>> getDecorations()
	{
		return Collections.unmodifiableMap(m_decorations);
	}
	
	public List<Point> getTerritoryEffectPoints(final Territory territory)
	{
		if (m_territoryEffects.get(territory.getName()) == null)
			return Arrays.asList(getCenter(territory));
		return m_territoryEffects.get(territory.getName());
	}
	
	public Image getTerritoryEffectImage(final String m_effectName)
	{
		if (m_effectImages.get(m_effectName) != null)
			return m_effectImages.get(m_effectName);
		final Image effectImage = loadImage("territoryEffects/" + m_effectName + ".png");
		m_effectImages.put(m_effectName, effectImage);
		return effectImage;
	}
}
