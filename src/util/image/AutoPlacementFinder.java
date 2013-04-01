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
package util.image;

import games.strategy.engine.framework.GameRunner2;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.ui.MapData;
import games.strategy.util.PointFileReaderWriter;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class AutoPlacementFinder
{
	private static int PLACEWIDTH = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
	private static int PLACEHEIGHT = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
	private static MapData s_mapData;
	private static boolean placeDimensionsSet = false;
	private static double unit_zoom_percent = 1;
	private static int unit_width = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
	private static int unit_height = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
	private static File s_mapFolderLocation = null;
	private static final String TRIPLEA_MAP_FOLDER = "triplea.map.folder";
	private static final String TRIPLEA_UNIT_ZOOM = "triplea.unit.zoom";
	private static final String TRIPLEA_UNIT_WIDTH = "triplea.unit.width";
	private static final String TRIPLEA_UNIT_HEIGHT = "triplea.unit.height";
	
	public static String[] getProperties()
	{
		return new String[] { TRIPLEA_MAP_FOLDER, TRIPLEA_UNIT_ZOOM, TRIPLEA_UNIT_WIDTH, TRIPLEA_UNIT_HEIGHT };
	}
	
	public static void main(final String[] args)
	{
		handleCommandLineArgs(args);
		JOptionPane.showMessageDialog(null, new JLabel("<html>"
					+ "This is the AutoPlacementFinder, it will create a place.txt file for you. "
					+ "<br>In order to run this, you must already have created a centers.txt file and a polygons.txt file, "
					+ "<br>and you must have already created the map directory structure in its final place."
					+ "<br>Example: the map folder should have a name, with the 2 text files already in that folder, and "
					+ "<br>the folder should be located in your users\\yourname\\triplea\\maps\\ directory."
					+ "<br><br>The program will ask for the folder name (just the name, not the full path)."
					+ "<br>Then it will ask for unit scale (unit zoom) level [normally between 0.5 and 1.0]"
					+ "<br>Then it will ask for the unit image size when not zoomed [normally 48x48]."
					+ "<br><br>If you want to have less, or more, room around the edges of your units, you can change the unit size."
					+ "<br><br>When done, the program will attempt to make placements for all territories on your map."
					+ "<br>However, it doesn't do a good job with thin or small territories, or islands, so it is a very good idea"
					+ "<br>to use the PlacementPicker to check all placements and redo some of them by hand."
					+ "</html>"));
		calculate();
	}
	
	/**
	 * calculate()
	 * 
	 * Will calculate the placements on the map automatically.
	 */
	static void calculate()
	{
		final Map<String, Collection<Point>> m_placements = new HashMap<String, Collection<Point>>(); // create hash map of placements
		final String mapDir = s_mapFolderLocation == null ? getMapDirectory() : s_mapFolderLocation.getName(); // ask user where the map is
		if (mapDir == null)
		{
			System.out.println("You need to specify a map name for this to work");
			System.out.println("Shutting down");
			System.exit(0);
		}
		File file = new File(GameRunner2.getUserMapsFolder() + File.separator + mapDir + File.separator + "map.properties");
		if (!file.exists())
			file = new File(GameRunner2.getRootFolder() + File.separator + "maps" + File.separator + mapDir + File.separator + "map.properties");
		if (file.exists() && s_mapFolderLocation == null)
			s_mapFolderLocation = file.getParentFile();
		if (!placeDimensionsSet)
		{
			try
			{
				if (file.exists())
				{
					double scale = unit_zoom_percent;
					int width = unit_width;
					int height = unit_height;
					boolean found = false;
					final String scaleProperty = MapData.PROPERTY_UNITS_SCALE + "=";
					final String widthProperty = MapData.PROPERTY_UNITS_WIDTH + "=";
					final String heightProperty = MapData.PROPERTY_UNITS_HEIGHT + "=";
					
					final FileReader reader = new FileReader(file);
					final LineNumberReader reader2 = new LineNumberReader(reader);
					int i = 0;
					while (true)
					{
						reader2.setLineNumber(i);
						final String line = reader2.readLine();
						if (line == null)
							break;
						if (line.contains(scaleProperty))
						{
							try
							{
								scale = Double.parseDouble(line.substring(line.indexOf(scaleProperty) + scaleProperty.length()).trim());
								found = true;
							} catch (final NumberFormatException ex)
							{
							}
						}
						if (line.contains(widthProperty))
						{
							try
							{
								width = Integer.parseInt(line.substring(line.indexOf(widthProperty) + widthProperty.length()).trim());
								found = true;
							} catch (final NumberFormatException ex)
							{
							}
						}
						if (line.contains(heightProperty))
						{
							try
							{
								height = Integer.parseInt(line.substring(line.indexOf(heightProperty) + heightProperty.length()).trim());
								found = true;
							} catch (final NumberFormatException ex)
							{
							}
						}
					}
					i++;
					if (found)
					{
						final int result = JOptionPane.showConfirmDialog(new JPanel(),
										"A map.properties file was found in the map's folder, " +
													"\r\n do you want to use the file to supply the info for the placement box size? " +
													"\r\n Zoom = " + scale + ",  Width = " + width + ",  Height = " + height +
													",    Result = (" + ((int) (scale * width)) + "x" + ((int) (scale * height)) + ")", "File Suggestion", 1);
						// if (result == 2)
						// return;
						if (result == 0)
						{
							unit_zoom_percent = scale;
							PLACEWIDTH = (int) (unit_zoom_percent * width);
							PLACEHEIGHT = (int) (unit_zoom_percent * height);
							placeDimensionsSet = true;
						}
					}
				}
			} catch (final Exception ex)
			{
			}
		}
		if (!placeDimensionsSet
					|| JOptionPane.showConfirmDialog(new JPanel(), "Placement Box Size already set (" + PLACEWIDTH + "x" + PLACEHEIGHT + "), " +
								"do you wish to continue with this?\r\nSelect Yes to continue, Select No to override and change the size.", "Placement Box Size", JOptionPane.YES_NO_OPTION) == 1)
		{
			try
			{
				final String result = getUnitsScale();
				try
				{
					unit_zoom_percent = Double.parseDouble(result.toLowerCase());
				} catch (final NumberFormatException ex)
				{
				}
				final String width = JOptionPane.showInputDialog(null, "Enter the unit's image width in pixels (unscaled / without zoom).\r\n(e.g. 48)");
				if (width != null)
				{
					try
					{
						PLACEWIDTH = (int) (unit_zoom_percent * Integer.parseInt(width));
					} catch (final NumberFormatException ex)
					{
					}
				}
				final String height = JOptionPane.showInputDialog(null, "Enter the unit's image height in pixels (unscaled / without zoom).\r\n(e.g. 48)");
				if (height != null)
				{
					try
					{
						PLACEHEIGHT = (int) (unit_zoom_percent * Integer.parseInt(height));
					} catch (final NumberFormatException ex)
					{
					}
				}
				placeDimensionsSet = true;
			} catch (final Exception ex)
			{
			}
		}
		try
		{
			s_mapData = new MapData(mapDir); // makes TripleA read all the text data files for the map.
		} catch (final Exception ex)
		{
			JOptionPane.showMessageDialog(null, new JLabel("Could not find map. Make sure it is in finalized location and contains centers.txt and polygons.txt"));
			System.out.println("Caught Exception.");
			System.out.println("Could be due to some missing text files.");
			System.out.println("Or due to the map folder not being in the right location.");
			ex.printStackTrace();
			System.exit(0);
		}
		System.out.println("Place Dimensions in pixels, being used: " + PLACEWIDTH + "x" + PLACEHEIGHT);
		final Iterator<String> terrIter = s_mapData.getTerritories().iterator();
		System.out.println("Calculating, this may take a while...");
		while (terrIter.hasNext())
		{
			final String name = terrIter.next();
			List<Point> points;
			if (s_mapData.hasContainedTerritory(name))
			{
				final Set<Polygon> containedPolygons = new HashSet<Polygon>();
				for (final String containedName : s_mapData.getContainedTerritory(name))
				{
					containedPolygons.addAll(s_mapData.getPolygons(containedName));
				}
				points = getPlacementsStartingAtTopLeft(s_mapData.getPolygons(name), s_mapData.getBoundingRect(name), s_mapData.getCenter(name), containedPolygons);
				m_placements.put(name, points);
			}
			else
			{
				points = getPlacementsStartingAtMiddle(s_mapData.getPolygons(name), s_mapData.getBoundingRect(name), s_mapData.getCenter(name));
				m_placements.put(name, points);
			}
			System.out.println(name + ": " + points.size());
		}// while
		try
		{
			final String fileName = new FileSave("Where To Save place.txt ?", "place.txt", s_mapFolderLocation).getPathString();
			if (fileName == null)
			{
				System.out.println("You chose not to save, Shutting down");
				System.exit(0);
			}
			PointFileReaderWriter.writeOneToMany(new FileOutputStream(fileName), m_placements);
			System.out.println("Data written to :" + new File(fileName).getCanonicalPath());
		} catch (final Exception ex)
		{
			ex.printStackTrace();
			System.exit(0);
		}
		System.exit(0); // shut down program when done.
	}
	
	/**
	 * we need the exact map name as indicated in the XML game file
	 * ie. "revised" "classic" "pact_of_steel"
	 * of course, without the quotes.
	 */
	private static String getMapDirectory()
	{
		final String mapDir = JOptionPane.showInputDialog(null, "Enter the map name (ie. folder name)");
		if (mapDir != null)
		{
			return mapDir;
		}
		else
		{
			return null;
		}
	}
	
	private static String getUnitsScale()
	{
		final String unitsScale = JOptionPane.showInputDialog(null, "Enter the unit's scale (zoom).\r\n(e.g. 1.25, 1, 0.875, 0.8333, 0.75, 0.6666, 0.5625, 0.5)");
		if (unitsScale != null)
		{
			return unitsScale;
		}
		else
		{
			return "1";
		}
	}
	
	/**
	 * java.util.List getPlacementsStartingAtMiddle(java.util.Collection, java.awt.Rectangle, java.awt.Point)
	 * 
	 * @param java
	 *            .util.Collection
	 * @param java
	 *            .awt.Rectangle
	 * @param java
	 *            .awt.Point
	 * @return java.util.List
	 */
	static List<Point> getPlacementsStartingAtMiddle(final Collection<Polygon> countryPolygons, final Rectangle bounding, final Point center)
	{
		final List<Rectangle2D> placementRects = new ArrayList<Rectangle2D>();
		final List<Point> placementPoints = new ArrayList<Point>();
		final Rectangle2D place = new Rectangle2D.Double(center.x, center.y, PLACEHEIGHT, PLACEWIDTH);
		int x = center.x - (PLACEHEIGHT / 2);
		int y = center.y - (PLACEWIDTH / 2);
		int step = 1;
		for (int i = 0; i < 2 * Math.max(bounding.width, bounding.height); i++)
		{
			for (int j = 0; j < Math.abs(step); j++)
			{
				if (step > 0)
				{
					x++;
				}
				else
				{
					x--;
				}
				isPlacement(countryPolygons, Collections.<Polygon> emptySet(), placementRects, placementPoints, place, x, y);
			}
			for (int j = 0; j < Math.abs(step); j++)
			{
				if (step > 0)
				{
					y++;
				}
				else
				{
					y--;
				}
				isPlacement(countryPolygons, Collections.<Polygon> emptySet(), placementRects, placementPoints, place, x, y);
			}
			step = -step;
			if (step > 0)
			{
				step++;
			}
			else
			{
				step--;
			}
			// System.out.println("x:" + x + " y:" + y); // For Debugging
		}
		if (placementPoints.isEmpty())
		{
			final int defaultx = center.x - (PLACEHEIGHT / 2);
			final int defaulty = center.y - (PLACEWIDTH / 2);
			placementPoints.add(new Point(defaultx, defaulty));
		}
		return placementPoints;
	}
	
	/**
	 * java.util.List getPlacementsStartingAtTopLeft(java.util.Collection, java.awt.Rectangle, java.awt.Point, java.util.Collection)
	 * 
	 * @param java
	 *            .util.Collection
	 * @param java
	 *            .awt.Rectangle
	 * @param java
	 *            .awt.Point
	 * @param java
	 *            .util.Collection
	 * @return java.util.List
	 */
	static List<Point> getPlacementsStartingAtTopLeft(final Collection<Polygon> countryPolygons, final Rectangle bounding, final Point center, final Collection<Polygon> containedCountryPolygons)
	{
		final List<Rectangle2D> placementRects = new ArrayList<Rectangle2D>();
		final List<Point> placementPoints = new ArrayList<Point>();
		final Rectangle2D place = new Rectangle2D.Double(center.x, center.y, PLACEHEIGHT, PLACEWIDTH);
		for (int x = bounding.x + 1; x < bounding.width + bounding.x; x++)
		{
			for (int y = bounding.y + 1; y < bounding.height + bounding.y; y++)
			{
				isPlacement(countryPolygons, containedCountryPolygons, placementRects, placementPoints, place, x, y);
			}
		}
		if (placementPoints.isEmpty())
		{
			final int defaultx = center.x - (PLACEHEIGHT / 2);
			final int defaulty = center.y - (PLACEWIDTH / 2);
			placementPoints.add(new Point(defaultx, defaulty));
		}
		return placementPoints;
	}
	
	/**
	 * isPlacement(java.util.Collection, java.util.Collection, java.util.List, java.util.List, java.awt.geom.Rectangle2D, java.lang.int, java.lang.int)
	 * 
	 * @param java
	 *            .util.Collection countryPolygons
	 * @param java
	 *            .util.Collection containedCountryPolygons polygons of countries contained with ourselves
	 * @param java
	 *            .util.List placementRects
	 * @param java
	 *            .util.List placementPoints
	 * @param java
	 *            .awt.geom.Rectangle2D place
	 * @param java
	 *            .lang.int x
	 * @param java
	 *            .lang.int y
	 */
	private static void isPlacement(final Collection<Polygon> countryPolygons, final Collection<Polygon> containedCountryPolygons, final List<Rectangle2D> placementRects,
				final List<Point> placementPoints, final Rectangle2D place, final int x, final int y)
	{
		place.setFrame(x, y, PLACEWIDTH, PLACEHEIGHT);
		if (containedIn(place, countryPolygons) && !intersectsOneOf(place, placementRects) &&
					// make sure it is not in or intersects the contained country
					(!containedIn(place, containedCountryPolygons) && !intersectsOneOf(place, containedCountryPolygons)))
		{
			placementPoints.add(new Point((int) place.getX(), (int) place.getY()));
			final Rectangle2D newRect = new Rectangle2D.Double();
			newRect.setFrame(place);
			placementRects.add(newRect);
		}// if
	}
	
	/**
	 * java.lang.boolean containedIn(java.awt.geom.Rectangle2D, java.util.Collection)
	 * 
	 * Function to test if the given 2D rectangle
	 * is contained in any of the given shapes
	 * in the collection.
	 * 
	 * @param java
	 *            .awt.geom.Rectangle2D r
	 * @param java
	 *            .util.Collection shapes
	 */
	public static boolean containedIn(final Rectangle2D r, final Collection<Polygon> shapes)
	{
		for (final Shape item : shapes)
		{
			if (item.contains(r))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * java.lang.boolean intersectsOneOf(java.awt.geom.Rectangle2D, java.util.Collection)
	 * 
	 * Function to test if the given 2D rectangle
	 * intersects any of the shapes given in the
	 * collection.
	 * 
	 * @param java
	 *            .awt.geom.Rectangle2D r
	 * @param java
	 *            .util.Collection shapes
	 */
	public static boolean intersectsOneOf(final Rectangle2D r, final Collection<? extends Shape> shapes)
	{
		if (shapes.isEmpty())
		{
			return false;
		}
		for (final Shape item : shapes)
		{
			if (item.intersects(r))
			{
				return true;
			}
		}
		return false;
	}
	
	private static String getValue(final String arg)
	{
		final int index = arg.indexOf('=');
		if (index == -1)
			return "";
		return arg.substring(index + 1);
	}
	
	private static void handleCommandLineArgs(final String[] args)
	{
		final String[] properties = getProperties();
		if (args.length == 1)
		{
			String value;
			if (args[0].startsWith(TRIPLEA_UNIT_ZOOM))
			{
				value = getValue(args[0]);
			}
			else
			{
				value = args[0];
			}
			try
			{
				Double.parseDouble(value);
				System.setProperty(TRIPLEA_UNIT_ZOOM, value);
			} catch (final Exception ex)
			{
			}
		}
		else if (args.length == 2)
		{
			String value0;
			if (args[0].startsWith(TRIPLEA_UNIT_WIDTH))
			{
				value0 = getValue(args[0]);
			}
			else
			{
				value0 = args[0];
			}
			try
			{
				Integer.parseInt(value0);
				System.setProperty(TRIPLEA_UNIT_WIDTH, value0);
			} catch (final Exception ex)
			{
			}
			
			String value1;
			if (args[0].startsWith(TRIPLEA_UNIT_HEIGHT))
			{
				value1 = getValue(args[1]);
			}
			else
			{
				value1 = args[1];
			}
			try
			{
				Integer.parseInt(value1);
				System.setProperty(TRIPLEA_UNIT_HEIGHT, value1);
			} catch (final Exception ex)
			{
			}
		}
		
		boolean usagePrinted = false;
		for (int argIndex = 0; argIndex < args.length; argIndex++)
		{
			boolean found = false;
			String arg = args[argIndex];
			final int indexOf = arg.indexOf('=');
			if (indexOf > 0)
			{
				arg = arg.substring(0, indexOf);
				for (int propIndex = 0; propIndex < properties.length; propIndex++)
				{
					if (arg.equals(properties[propIndex]))
					{
						final String value = getValue(args[argIndex]);
						System.getProperties().setProperty(properties[propIndex], value);
						System.out.println(properties[propIndex] + ":" + value);
						found = true;
						break;
					}
				}
			}
			if (!found)
			{
				System.out.println("Unrecogized:" + args[argIndex]);
				if (!usagePrinted)
				{
					usagePrinted = true;
					System.out.println("Arguments\r\n"
									+ "   " + TRIPLEA_MAP_FOLDER + "=<FILE_PATH>\r\n"
									+ "   " + TRIPLEA_UNIT_ZOOM + "=<UNIT_ZOOM_LEVEL>\r\n"
									+ "   " + TRIPLEA_UNIT_WIDTH + "=<UNIT_WIDTH>\r\n"
									+ "   " + TRIPLEA_UNIT_HEIGHT + "=<UNIT_HEIGHT>\r\n");
				}
			}
		}
		String folderString = System.getProperty(TRIPLEA_MAP_FOLDER);
		if (folderString != null && folderString.length() > 0)
		{
			folderString = folderString.replaceAll("\\(", " ");
			final File mapFolder = new File(folderString);
			if (mapFolder.exists())
				s_mapFolderLocation = mapFolder;
			else
				System.out.println("Could not find directory: " + folderString);
		}
		final String zoomString = System.getProperty(TRIPLEA_UNIT_ZOOM);
		if (zoomString != null && zoomString.length() > 0)
		{
			try
			{
				unit_zoom_percent = Double.parseDouble(zoomString);
				System.out.println("Unit Zoom Percent to use: " + unit_zoom_percent);
				placeDimensionsSet = true;
			} catch (final Exception ex)
			{
				System.err.println("Not a decimal percentage: " + zoomString);
			}
		}
		final String widthString = System.getProperty(TRIPLEA_UNIT_WIDTH);
		if (widthString != null && widthString.length() > 0)
		{
			try
			{
				unit_width = Integer.parseInt(widthString);
				System.out.println("Unit Width to use: " + unit_width);
				placeDimensionsSet = true;
			} catch (final Exception ex)
			{
				System.err.println("Not an integer: " + widthString);
			}
		}
		final String heightString = System.getProperty(TRIPLEA_UNIT_HEIGHT);
		if (heightString != null && heightString.length() > 0)
		{
			try
			{
				unit_height = Integer.parseInt(heightString);
				System.out.println("Unit Height to use: " + unit_height);
				placeDimensionsSet = true;
			} catch (final Exception ex)
			{
				System.err.println("Not an integer: " + heightString);
			}
		}
		if (placeDimensionsSet)
		{
			PLACEWIDTH = (int) (unit_zoom_percent * unit_width);
			PLACEHEIGHT = (int) (unit_zoom_percent * unit_height);
			System.out.println("Place Dimensions to use: " + PLACEWIDTH + "x" + PLACEHEIGHT);
		}
	}
}// end class AutoPlacementFinder
