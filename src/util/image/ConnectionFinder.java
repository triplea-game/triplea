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

/*
 * using algorithms from by eu.hansolo.steelseries.tools.Scaler.java from Gerrit Grunwald
 * Twitter: @hansolo_
 * URL:http://harmoniccode.blogspot.nl/
 * email: han.solo at muenster.de
 * 
 * @author Edwin van der Wal
 */

package util.image;

import games.strategy.util.AlphanumComparator;
import games.strategy.util.PointFileReaderWriter;

import java.awt.Dimension;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

/**
 * Utility to find connections between polygons
 * Not pretty, meant only for one time use.
 * 
 * Inputs - a polygons.txt file
 * Outputs - a list of connections between the Polygons
 * 
 * @author edwin
 * 
 */
public class ConnectionFinder
{
	private static File s_mapFolderLocation = null;
	private static final String TRIPLEA_MAP_FOLDER = "triplea.map.folder";
	private static final String LINE_THICKNESS = "triplea.map.lineThickness";
	private static final String SCALE_PIXELS = "triplea.map.scalePixels";
	private static final String MIN_OVERLAP = "triplea.map.minOverlap";
	private static boolean dimensionsSet = false;
	private static StringBuffer territoryDefinitions = null;
	
	// how many pixels should each area become bigger in both x and y axis to see which area it overlaps?
	public static int scalePixels = 8; // default 8, or if LINE_THICKNESS if given 4x linethickness
	
	// how many pixels should the boundingbox of the overlapping area have for it to be considered a valid connection?
	public static double minOverlap = 32.0; // default 32, or if LINE_THICKNESS is given 16 x linethickness
	
	public static void main(final String[] args)
	{
		handleCommandLineArgs(args);
		JOptionPane.showMessageDialog(null, new JLabel("<html>"
					+ "This is the ConnectionFinder. "
					+ "<br>It will create a file containing the connections between territories, and optionally the territory definitions as well. "
					+ "<br>Copy and paste everything from this file into your game xml file (the 'map' section). "
					+ "<br>The connections file can and Should Be Deleted when finished, because it is Not Needed and not read by the engine. "
					+ "</html>"));
		System.out.println("Select polygons.txt");
		File polyFile = null;
		if (s_mapFolderLocation != null && s_mapFolderLocation.exists())
			polyFile = new File(s_mapFolderLocation, "polygons.txt");
		if (polyFile != null && polyFile.exists()
					&& JOptionPane.showConfirmDialog(null, "A polygons.txt file was found in the map's folder, do you want to use it?", "File Suggestion", 1) == 0)
		{
			// yay
		}
		else
			polyFile = new FileOpen("Select The polygons.txt file", s_mapFolderLocation, ".txt").getFile();
		if (polyFile == null || !polyFile.exists())
		{
			System.out.println("No polygons.txt Selected. Shutting down.");
			System.exit(0);
		}
		if (s_mapFolderLocation == null && polyFile != null)
			s_mapFolderLocation = polyFile.getParentFile();
		
		final Map<String, List<Area>> territoryAreas = new HashMap<String, List<Area>>();
		Map<String, List<Polygon>> mapOfPolygons = null;
		try
		{
			final FileInputStream in = new FileInputStream(polyFile);
			mapOfPolygons = PointFileReaderWriter.readOneToManyPolygons(in);
			for (final String territoryName : mapOfPolygons.keySet())
			{
				final List<Polygon> listOfPolygons = mapOfPolygons.get(territoryName);
				final List<Area> listOfAreas = new ArrayList<Area>();
				for (final Polygon p : listOfPolygons)
				{
					listOfAreas.add(new Area(p));
				}
				territoryAreas.put(territoryName, listOfAreas);
				
			}
		} catch (final FileNotFoundException ex)
		{
			ex.printStackTrace();
		} catch (final IOException ex)
		{
			ex.printStackTrace();
		}
		if (!dimensionsSet)
		{
			final String lineWidth = JOptionPane.showInputDialog(null, "Enter the width of territory border lines on your map? \r\n(eg: 1, or 2, etc.)");
			try
			{
				final int lineThickness = Integer.parseInt(lineWidth);
				scalePixels = lineThickness * 4;
				minOverlap = scalePixels * 4;
				dimensionsSet = true;
			} catch (final NumberFormatException ex)
			{
			}
		}
		if (JOptionPane.showConfirmDialog(null, "Scale set to " + scalePixels + " pixels larger, and minimum overlap set to " + minOverlap + " pixels. \r\n" +
					"Do you wish to continue with this? \r\nSelect Yes to continue, Select No to override and change the size.", "Scale and Overlap Size", JOptionPane.YES_NO_OPTION) == 1)
		{
			final String scale = JOptionPane.showInputDialog(null,
						"Enter the number of pixels larger each territory should become? \r\n(Normally 4x bigger than the border line width. eg: 4, or 8, etc)");
			try
			{
				scalePixels = Integer.parseInt(scale);
			} catch (final NumberFormatException ex)
			{
			}
			final String overlap = JOptionPane.showInputDialog(null,
						"Enter the minimum number of overlapping pixels for a connection? \r\n(Normally 16x bigger than the border line width. eg: 16, or 32, etc.)");
			try
			{
				minOverlap = Integer.parseInt(overlap);
			} catch (final NumberFormatException ex)
			{
			}
		}
		
		final Map<String, Collection<String>> connections = new HashMap<String, Collection<String>>();
		System.out.println("Now Scanning for Connections");
		// sort so that they are in alphabetic order (makes xml's prettier and easier to update in future)
		final List<String> allTerritories = mapOfPolygons == null ? new ArrayList<String>() : new ArrayList<String>(mapOfPolygons.keySet());
		Collections.sort(allTerritories, new AlphanumComparator());
		final List<String> allAreas = new ArrayList<String>(territoryAreas.keySet());
		Collections.sort(allAreas, new AlphanumComparator());
		for (final String territory : allTerritories)
		{
			final Set<String> thisTerritoryConnections = new LinkedHashSet<String>();
			final List<Polygon> currentPolygons = mapOfPolygons.get(territory);
			for (final Polygon currentPolygon : currentPolygons)
			{
				
				final Shape scaledShape = scale(currentPolygon, scalePixels);
				
				for (final String otherTerritory : allAreas)
				{
					if (otherTerritory.equals(territory))
						continue;
					if (thisTerritoryConnections.contains(otherTerritory))
						continue;
					if (connections.get(otherTerritory) != null && connections.get(otherTerritory).contains(territory))
						continue;
					for (final Area otherArea : territoryAreas.get(otherTerritory))
					{
						final Area testArea = new Area(scaledShape);
						testArea.intersect(otherArea);
						if (!testArea.isEmpty() && sizeOfArea(testArea) > minOverlap)
						{
							thisTerritoryConnections.add(otherTerritory);
						}
						else if (!testArea.isEmpty())
						{
							
						}
					}
				}
				connections.put(territory, thisTerritoryConnections);
			}
		}
		if (JOptionPane.showConfirmDialog(null, "Do you also want to create the Territory Definitions?", "Territory Definitions", 1) == 0)
		{
			final String waterString = JOptionPane.showInputDialog(null, "Enter a string or regex that determines if the territory is Water? \r\n(eg: Sea Zone)", "Sea Zone");
			territoryDefinitions = doTerritoryDefinitions(allTerritories, waterString);
		}
		try
		{
			final String fileName = new FileSave("Where To Save connections.txt ? (cancel to print to console)", "connections.txt", s_mapFolderLocation).getPathString();
			final StringBuffer connectionsString = convertToXML(connections);
			if (fileName == null)
			{
				System.out.println();
				if (territoryDefinitions != null)
				{
					System.out.println(territoryDefinitions.toString());
				}
				System.out.println(connectionsString.toString());
			}
			else
			{
				final FileOutputStream out = new FileOutputStream(fileName);
				if (territoryDefinitions != null)
				{
					out.write(String.valueOf(territoryDefinitions).getBytes());
				}
				out.write(String.valueOf(connectionsString).getBytes());
				out.flush();
				out.close();
				System.out.println("Data written to :" + new File(fileName).getCanonicalPath());
			}
		} catch (final Exception ex)
		{
			ex.printStackTrace();
		}
	}// end main
	
	/**
	 * Creates the xml territory definitions.
	 * 
	 * @param allTerritoryNames
	 * @param waterString
	 *            a substring contained in a TerritoryName to define a Sea Zone or a regex expression that indicates that a territory is water
	 * @return StringBuffer containing XML representing these connections
	 */
	private static StringBuffer doTerritoryDefinitions(final List<String> allTerritoryNames, final String waterString)
	{
		// sort for pretty xml's
		Collections.sort(allTerritoryNames, new AlphanumComparator());
		final StringBuffer output = new StringBuffer();
		output.append("<!-- Territory Definitions -->\r\n");
		final Pattern waterPattern = Pattern.compile(waterString);
		for (final String t : allTerritoryNames)
		{
			final Matcher matcher = waterPattern.matcher(t);
			if (matcher.find())
			{
				// <territory name="sea zone 1" water="true"/>
				output.append("<territory name=\"" + t + "\" water=\"true\"/>\r\n");
			}
			else
			{
				// <territory name="neutral territory 2"/>
				output.append("<territory name=\"" + t + "\"/>\r\n");
			}
		}
		output.append("\r\n");
		return output;
	}
	
	/**
	 * Converts a map of connections to XML formatted text with the connections
	 * 
	 * @param connections
	 *            a map of connections between Territories
	 * @return a StringBuffer containing XML representing these connections
	 */
	private static StringBuffer convertToXML(final Map<String, Collection<String>> connections)
	{
		final StringBuffer output = new StringBuffer();
		output.append("<!-- Territory Connections -->\r\n");
		// sort for pretty xml's
		final List<String> allTerritories = new ArrayList<String>(connections.keySet());
		Collections.sort(allTerritories, new AlphanumComparator());
		for (final String t1 : allTerritories)
		{
			for (final String t2 : connections.get(t1))
			{
				output.append("<connection t1=\"" + t1 + "\" t2=\"" + t2 + "\"/>\r\n");
			}
		}
		return output;
	}
	
	/**
	 * Returns the size of the area of the boundingbox of the polygon
	 * 
	 * @param area
	 *            the area of which the boundingbox size is measured
	 * @return the size of the area of the boundingbox of this area
	 */
	
	private static double sizeOfArea(final Area area)
	{
		final Dimension d = area.getBounds().getSize();
		return d.getHeight() * d.getWidth();
	}
	
	/**
	 * from: eu.hansolo.steelseries.tools.Scaler.java
	 * Returns a double that represents the area of the given point array of a polygon
	 * 
	 * @param pointArray
	 * @param N
	 * @return a double that represents the area of the given point array of a polygon
	 */
	private static double calcSignedPolygonArea(final Point2D[] pointArray)
	{
		final int N = pointArray.length;
		int i;
		int j;
		double area = 0;
		
		for (i = 0; i < N; i++)
		{
			j = (i + 1) % N;
			area += pointArray[i].getX() * pointArray[j].getY();
			area -= pointArray[i].getY() * pointArray[j].getX();
		}
		area /= 2.0;
		
		return (area);
	}
	
	/**
	 * from: eu.hansolo.steelseries.tools.Scaler.java
	 * Returns a Point2D object that represents the center of mass of the given point array which represents a
	 * polygon.
	 * 
	 * @param pointArray
	 * @return a Point2D object that represents the center of mass of the given point array
	 */
	private static Point2D calcCenterOfMass(final Point2D[] pointArray)
	{
		final int N = pointArray.length;
		double cx = 0;
		double cy = 0;
		double area = calcSignedPolygonArea(pointArray);
		final Point2D centroid = new Point2D.Double();
		int i;
		int j;
		
		double factor = 0;
		for (i = 0; i < N; i++)
		{
			j = (i + 1) % N;
			factor = (pointArray[i].getX() * pointArray[j].getY() - pointArray[j].getX() * pointArray[i].getY());
			cx += (pointArray[i].getX() + pointArray[j].getX()) * factor;
			cy += (pointArray[i].getY() + pointArray[j].getY()) * factor;
		}
		area *= 6.0f;
		factor = 1 / area;
		cx *= factor;
		cy *= factor;
		
		centroid.setLocation(cx, cy);
		return centroid;
	}
	
	/**
	 * from: eu.hansolo.steelseries.tools.Scaler.java
	 * Returns a Point2D object that represents the center of mass of the given shape.
	 * 
	 * @param currentShape
	 * @return a Point2D object that represents the center of mass of the given shape
	 */
	private static Point2D getCentroid(final Shape currentShape)
	{
		final ArrayList<Point2D> pointList = new ArrayList<Point2D>(32);
		final PathIterator pathIterator = currentShape.getPathIterator(null);
		int lastMoveToIndex = -1;
		while (!pathIterator.isDone())
		{
			final double[] coordinates = new double[6];
			switch (pathIterator.currentSegment(coordinates))
			{
				case PathIterator.SEG_MOVETO:
					pointList.add(new Point2D.Double(coordinates[0], coordinates[1]));
					lastMoveToIndex++;
					break;
				case PathIterator.SEG_LINETO:
					pointList.add(new Point2D.Double(coordinates[0], coordinates[1]));
					break;
				case PathIterator.SEG_QUADTO:
					pointList.add(new Point2D.Double(coordinates[0], coordinates[1]));
					pointList.add(new Point2D.Double(coordinates[2], coordinates[3]));
					break;
				case PathIterator.SEG_CUBICTO:
					pointList.add(new Point2D.Double(coordinates[0], coordinates[1]));
					pointList.add(new Point2D.Double(coordinates[2], coordinates[3]));
					pointList.add(new Point2D.Double(coordinates[4], coordinates[5]));
					break;
				case PathIterator.SEG_CLOSE:
					if (lastMoveToIndex >= 0)
					{
						pointList.add(pointList.get(lastMoveToIndex));
					}
					break;
			}
			pathIterator.next();
		}
		final Point2D[] pointArray = new Point2D[pointList.size()];
		pointList.toArray(pointArray);
		return (calcCenterOfMass(pointArray));
	}
	
	private static Shape scale(final Shape currentShape, final int pixels)
	{
		final Dimension d = currentShape.getBounds().getSize();
		final double scalefactorX = 1.0 + (1 / ((double) d.width)) * pixels;
		final double scalefactorY = 1.0 + (1 / ((double) d.height)) * pixels;
		return scale(currentShape, scalefactorX, scalefactorY);
	}
	
	/**
	 * from: eu.hansolo.steelseries.tools.Scaler.java
	 * Returns a scaled version of the given shape, calculated by the given scale factor.
	 * The scaling will be calculated around the centroid of the shape.
	 * 
	 * @param currentPolygon
	 * @param xScaleFactor
	 *            how much to scale on the x-axis
	 * @param yScaleFactor
	 *            how much to scale on the y-axis
	 *            * @return a scaled version of the given shape, calculated around the centroid by the given scale factors.
	 */
	
	private static Shape scale(final Shape currentPolygon, final double xScaleFactor, final double yScaleFactor)
	{
		final Point2D centroid = getCentroid(currentPolygon);
		final AffineTransform transform = AffineTransform.getTranslateInstance((1.0 - xScaleFactor) * centroid.getX(), (1.0 - yScaleFactor) * centroid.getY());
		transform.scale(xScaleFactor, yScaleFactor);
		final Shape shape = transform.createTransformedShape(currentPolygon);
		return shape;
		
	}
	
	private static void handleCommandLineArgs(final String[] args)
	{
		for (final String arg : args)
		{
			final String value = getValue(arg);
			if (arg.startsWith(TRIPLEA_MAP_FOLDER))
			{
				final File mapFolder = new File(value);
				if (mapFolder.exists())
					s_mapFolderLocation = mapFolder;
				else
					System.out.println("Could not find directory: " + value);
			}
			if (arg.startsWith(LINE_THICKNESS))
			{
				final int lineThickness = new Integer(value).intValue();
				scalePixels = lineThickness * 4;
				minOverlap = scalePixels * 4;
				dimensionsSet = true;
			}
			if (arg.startsWith(MIN_OVERLAP))
			{
				minOverlap = new Integer(value).intValue();
			}
			if (arg.startsWith(SCALE_PIXELS))
			{
				scalePixels = new Integer(value).intValue();
			}
			
		}
		
		// might be set by -D
		if (s_mapFolderLocation == null || s_mapFolderLocation.length() < 1)
		{
			final String value = System.getProperty(TRIPLEA_MAP_FOLDER);
			if (value != null && value.length() > 0)
			{
				final File mapFolder = new File(value);
				if (mapFolder.exists())
					s_mapFolderLocation = mapFolder;
				else
					System.out.println("Could not find directory: " + value);
			}
		}
		String value = System.getProperty(LINE_THICKNESS);
		if (value != null && value.length() > 0)
		{
			final int lineThickness = new Integer(value).intValue();
			scalePixels = lineThickness * 4;
			minOverlap = scalePixels * 4;
			dimensionsSet = true;
		}
		value = System.getProperty(MIN_OVERLAP);
		if (value != null && value.length() > 0)
		{
			minOverlap = new Integer(value).intValue();
		}
		value = System.getProperty(SCALE_PIXELS);
		if (value != null && value.length() > 0)
		{
			scalePixels = new Integer(value).intValue();
		}
	}
	
	private static String getValue(final String arg)
	{
		final int index = arg.indexOf('=');
		if (index == -1)
			return "";
		return arg.substring(index + 1);
	}
}
