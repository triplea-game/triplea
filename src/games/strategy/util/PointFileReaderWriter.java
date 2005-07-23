/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * PointFileReader.java
 *
 * Created on January 11, 2002, 11:57 AM
 */

package games.strategy.util;

import java.io.*;
import java.util.*;
import java.util.List;
import java.awt.*;

/**
 *
 *
 * Utiltity to read and write files in the form of
 * String -> a list of points, or string-> list of polygons
 *
 * @author  Sean Bridges
 */
public class PointFileReaderWriter
{
	/** Creates a new instance of PointFileReader */
    public PointFileReaderWriter()
	{
    }

	/**
	 * Returns a map of the form String -> Point.
	 */
    public static Map<String, Point> readOneToOne(InputStream stream) throws IOException
	{
	    if(stream == null)
	        return Collections.emptyMap();
	    
		HashMap<String, Point> mapping = new HashMap<String, Point>();
		LineNumberReader reader = null;
		try
		{
			reader = new LineNumberReader(new InputStreamReader(stream));

			String current = reader.readLine();
			while(current != null)
			{
				if(current.trim().length() != 0)
				{
					readSingle(current, mapping);
				}
				current = reader.readLine();
			}
		} finally
		{
			if(reader != null)
				reader.close();
		}
		return mapping;
	}


	private static void readSingle(String aLine, HashMap<String, Point> mapping) throws IOException
	{
		StringTokenizer tokens = new StringTokenizer(aLine, "", false);
		String name = tokens.nextToken("(").trim();

		if(mapping.containsKey(name))
			throw new IOException("name found twice:" + name);

		int x = Integer.parseInt(tokens.nextToken("(, "));
		int y = Integer.parseInt(tokens.nextToken(",) "));
		Point p = new Point(x,y);
		mapping.put(name,p);
	}

    public static void writeOneToOne(OutputStream sink, Map mapping) throws Exception
    {
        StringBuilder out = new StringBuilder();
        Iterator keyIter = mapping.keySet().iterator();
        while (keyIter.hasNext())
        {
            String name = (String) keyIter.next();
            out.append(name).append(" ");

            Point point = (Point) mapping.get(name);
            out.append(" (").append(point.x).append(",").append(point.y).append(")");
            if(keyIter.hasNext())
            {
                out.append("\n");
            }


        }
        write(out, sink);
    }

    public static void writeOneToManyPolygons(OutputStream sink, Map<String,List<Polygon>> mapping) throws Exception
    {
        StringBuilder out = new StringBuilder();
        Iterator keyIter = mapping.keySet().iterator();
        while (keyIter.hasNext())
        {
            String name = (String) keyIter.next();
            out.append(name).append(" ");
            List<Polygon> points = mapping.get(name);
            Iterator polygonIter = points.iterator();
            while (polygonIter.hasNext())
            {
                out.append(" < ");
                Polygon polygon = (Polygon)polygonIter.next();
                for (int i = 0; i < polygon.npoints; i++)
                {
                    out.append(" (").append(polygon.xpoints[i]).append(",").append(polygon.ypoints[i]).append(")");
                }
                out.append(" > ");
            }
            if(keyIter.hasNext())
            {
                out.append("\n");
            }
        }
        write(out, sink);
    }

    private static void write(StringBuilder buf, OutputStream sink) throws IOException
    {
        OutputStreamWriter out = new OutputStreamWriter(sink);
        out.write(buf.toString());
        out.flush();
    }

    public static void writeOneToMany(OutputStream sink, Map mapping) throws Exception
    {
        StringBuilder out = new StringBuilder();
        Iterator keyIter = mapping.keySet().iterator();
        while (keyIter.hasNext())
        {
            String name = (String) keyIter.next();
            out.append(name).append(" ");
            Collection points = (Collection) mapping.get(name);
            Iterator pointIter = points.iterator();
            while (pointIter.hasNext())
            {
                Point point = (Point)pointIter.next();
                out.append(" (").append(point.x).append(",").append(point.y).append(")");
                if(pointIter.hasNext())
                    out.append(" ");
            }
            if (keyIter.hasNext())
            {
                out.append("\n");
            }

        }
        write(out, sink);
    }

	/**
	 * Returns a map of the form String -> Collection of points.
	 */
	public static Map<String, List<Point>> readOneToMany(InputStream stream) throws IOException
	{
		HashMap<String, List<Point>> mapping = new HashMap<String, List<Point>>();
		LineNumberReader reader = null;
		try
		{
			reader = new LineNumberReader(new InputStreamReader(stream));

			String current = reader.readLine();
			while(current != null)
			{
				if(current.trim().length() != 0)
				{
					readMultiple(current, mapping);
				}
				current = reader.readLine();
			}
		} catch(IOException ioe)
		{
			ioe.printStackTrace();
			System.exit(0);
		}
		finally
		{
			try
			{
				if(reader != null)
					reader.close();
			} catch(IOException e) {}
		}
		return mapping;
	}

    /**
     * Returns a map of the form String -> Collection of points.
     */
    public static Map<String, List<Polygon>> readOneToManyPolygons(InputStream stream) throws IOException
    {
        HashMap<String, List<Polygon>> mapping = new HashMap<String, List<Polygon>>();
        LineNumberReader reader = null;
        try
        {
            reader = new LineNumberReader(new InputStreamReader(stream));

            String current = reader.readLine();
            while(current != null)
            {
                if(current.trim().length() != 0)
                {
                    readMultiplePolygons(current, mapping);
                }
                current = reader.readLine();
            }
        } catch(IOException ioe)
        {
            ioe.printStackTrace();
            System.exit(0);
        }
        finally
        {
            try
            {
                if(reader != null)
                    reader.close();
            } catch(IOException e) {}
        }
        return mapping;
    }


    private static void readMultiplePolygons(String line, HashMap<String, List<Polygon>> mapping) throws IOException
    {
        StringTokenizer tokens = new StringTokenizer(line, "");
        String name = tokens.nextToken("<").trim();
        List<Polygon> polygons = new ArrayList<Polygon>();

        if(mapping.containsKey(name))
            throw new IOException("name found twice:" + name);

        ArrayList<Point> points = new ArrayList<Point>();
        while(tokens.hasMoreTokens())
        {
            String xString = tokens.nextToken(",<(), ");

            //end of a polygon
            if(xString.trim().equals(">"))
            {
                createPolygonFromPoints(polygons, points);
                continue;
            }


            if(!tokens.hasMoreTokens())
                continue;


            String yString = tokens.nextToken(",() ");

            int x = Integer.parseInt(xString);
            int y = Integer.parseInt(yString);
            points.add(new Point(x,y));
        }

        mapping.put(name, polygons);
    }

    private static void createPolygonFromPoints(Collection<Polygon> polygons, ArrayList<Point> points)
    {
        int[] xPoints = new int[points.size()];
        int[] yPoints = new int[points.size()];
        for (int i =0; i < points.size() ;i++ )
        {
            Point p = points.get(i);
            xPoints[i] = p.x;
            yPoints[i] = p.y;
        }
        polygons.add(new Polygon(xPoints, yPoints, xPoints.length));
        points.clear();
    }



	private static void readMultiple(String line, HashMap<String, List<Point>> mapping) throws IOException
	{
		StringTokenizer tokens = new StringTokenizer(line, "");
		String name = tokens.nextToken("(").trim();

		if(mapping.containsKey(name))
			throw new IOException("name found twice:" + name);

		List<Point> points = new ArrayList<Point>();
		while(tokens.hasMoreTokens())
		{
			String xString = tokens.nextToken(",(), ");

			if(!tokens.hasMoreTokens())
				continue;
			String yString = tokens.nextToken(",() ");

			int x = Integer.parseInt(xString);
			int y = Integer.parseInt(yString);
			points.add(new Point(x,y));
		}
		mapping.put(name, points);
	}

	//TODO add write methods
}
