/*
 * PolygonTerritory.java
 *
 * Created on November 6, 2001, 3:59 PM
 */

package games.strategy.triplea.ui;

import java.awt.Polygon;
import java.util.*;

import games.strategy.engine.data.*;
import games.strategy.ui.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */

class PolygonTerritory implements Comparable
{
	private Polygon m_polygon;
	private Territory m_territory;
	
	public static PolygonTerritory read(String line, GameData data)
	{
		StringTokenizer st = new StringTokenizer(line, "<>(),", false);
		
		String name = st.nextToken("<>(),").trim();
		Territory territory = data.getMap().getTerritory(name);
		//we must find the territory
		if(territory == null)
			throw new IllegalStateException("Error parsing PolygonTerritory.  No territory found named <" + name + ">");
		
		Polygon poly = new Polygon();
		
		while(st.hasMoreTokens())
		{
			int x = Integer.parseInt(st.nextToken(" <>(),"));
			int y = Integer.parseInt(st.nextToken(" <>(),"));
			poly.addPoint(x,y);
		}
		
		return new PolygonTerritory(poly, territory);
	}
	
	PolygonTerritory(Polygon polygon, Territory territory)
	{
		m_polygon = polygon;
		m_territory = territory;
	}
	
	public Polygon getPolygon()
	{
		return m_polygon;
	}
	
	public Territory getTerritory()
	{
		return m_territory;
	}
	
	private boolean isWater()
	{
		return m_territory.isWater();
	}
	
	public String toString()
	{
		return m_territory.getName() + " " + m_polygon;
	}
	
	public int compareTo(Object obj) 
	{
		
		//most probably a mistake
		if(! (obj instanceof PolygonTerritory))
			throw new IllegalArgumentException("Comparing a PolygonTerritory to a non polygon territory");

		if(this.equals(obj))
			return 0;
		
		PolygonTerritory other = (PolygonTerritory) obj;
		
		if((this.isWater() && other.isWater()) || (!this.isWater() && !other.isWater())) 
			return this.m_territory.getName().compareTo(other.m_territory.getName());
		if(isWater())
			return 1;
		return -1;
	}	
}