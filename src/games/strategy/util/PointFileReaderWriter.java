/*
 * PointFileReader.java
 *
 * Created on January 11, 2002, 11:57 AM
 */

package games.strategy.util;

import java.io.*;
import java.util.*;
import java.awt.Point;

/**
 *
 *
 * Utiltity to read and write files in the form of 
 * String -> a list of points
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
	public Map readOneToOne(InputStream stream) throws IOException
	{
		HashMap mapping = new HashMap();
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
	
	private void readSingle(String aLine, HashMap mapping) throws IOException
	{
		StringTokenizer tokens = new StringTokenizer(aLine, "", false);
		String name = tokens.nextToken("(").trim();
		
		if(mapping.containsKey(name))
			throw new IOException("name found twice:" + name);
		
		int x = Integer.parseInt(tokens.nextToken("(,"));
		int y = Integer.parseInt(tokens.nextToken(",)"));		
		Point p = new Point(x,y);
		mapping.put(name,p);
	}

	/**
	 * Returns a map of the form String -> Collection of points.
	 */
	public Map readOneToMany(InputStream stream) throws IOException
	{
		HashMap mapping = new HashMap();
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
	
	private void readMultiple(String line, HashMap mapping) throws IOException
	{
		StringTokenizer tokens = new StringTokenizer(line, "");
		String name = tokens.nextToken("(").trim();

		if(mapping.containsKey(name))
			throw new IOException("name found twice:" + name);
		
		Collection points = new ArrayList();
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
