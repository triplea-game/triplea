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


package games.strategy.triplea.ui;

import java.util.*;
import java.text.*;

import games.strategy.util.*;
import java.io.*;
import games.strategy.engine.data.*;
import java.awt.*;
import java.util.List;
import java.awt.geom.Rectangle2D;
import java.awt.geom.*;
/**
 * contains data about the territories useful for drawing
 */


public class TerritoryData
{
    private static final String CENTERS_FILE = "centers.txt";
    private static final String POLYGON_FILE = "polygons.txt";
    private static final String PLACEMENT_FILE = "place.txt";

    //maps String -> List of points
    private Map m_place;
    //maps String -> Collection of Polygons
    private Map m_polys;
    //maps String -> Point
    private Map m_centers;
    //maps String -> List of String
    private Map m_contains;

    private static TerritoryData s_instance = new TerritoryData();

    public static TerritoryData getInstance()
    {
        return s_instance;
    }

    private TerritoryData()
    {
        try
        {
            m_place = PointFileReaderWriter.readOneToMany(this.getClass().getResourceAsStream(PLACEMENT_FILE));
            m_polys = PointFileReaderWriter.readOneToManyPolygons(this.getClass().getResourceAsStream(POLYGON_FILE));
            m_centers = PointFileReaderWriter.readOneToOne(this.getClass().getResourceAsStream(CENTERS_FILE));

            initializeContains();

        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    private void initializeContains()
    {
        m_contains = new HashMap();

        Iterator seaIter = getTerritories().iterator();
        while (seaIter.hasNext())
        {
            List contained = new ArrayList();
            String seaTerritory = (String) seaIter.next();
            if (!seaTerritory.endsWith("Sea Zone"))
                continue;

            Iterator landIter = getTerritories().iterator();
            while (landIter.hasNext())
            {
                String landTerritory = (String)landIter.next();
                if(landTerritory.endsWith("Sea Zone"))
                    continue;
                Polygon landPoly = (Polygon) getPolygons(landTerritory).iterator().next();
                Polygon seaPoly = (Polygon) getPolygons(seaTerritory).iterator().next();
                if(seaPoly.contains(landPoly.getBounds()))
                {
                    contained.add(landTerritory);
                }
            }
            if(!contained.isEmpty())
                m_contains.put(seaTerritory, contained);
        }
    }

    /**
     *
     * @return a Set of territory names as Strings.
     * generally this shouldnt be used, instead you should use aGameData.getMap().getTerritories()
     */
    public Set getTerritories()
    {
        return m_polys.keySet();
    }

    /**
     * Does this territory have any territories contained within it
     */
    public boolean hasContainedTerritory(String territoryName)
    {
        return m_contains.containsKey(territoryName);
    }

    /**
     * returns the name of the territory contained in the given territory.
     * This applies to islands within sea zones.
     * @return possiblly null
     */
    public List getContainedTerritory(String territoryName)
    {
        return (List) m_contains.get(territoryName);
    }


    public void verify(GameData data)
    {
        verifyKeys(data, m_centers, "centers");
        verifyKeys(data, m_polys , "polygons");
        verifyKeys(data, m_place, "place");
    }

    private void verifyKeys(GameData data, Map aMap, String dataTypeForErrorMessage) throws IllegalStateException
    {
        StringBuffer errors = new StringBuffer();
        Iterator iter = aMap.keySet().iterator();
        while (iter.hasNext())
        {
            String name = (String) iter.next();
            Territory terr = data.getMap().getTerritory(name);
            if (terr == null)
                errors.append("Territory in file could not be found in game data for " + dataTypeForErrorMessage + ". Territory name <" + name + ">\n");
        }

        Iterator territories = data.getMap().getTerritories().iterator();

        Set keySet = aMap.keySet();
        while(territories.hasNext())
        {
            Territory terr = (Territory) territories.next();
            if(!keySet.contains(terr.getName()))
            {
                errors.append("No data of type " + dataTypeForErrorMessage + " for territory:" + terr.getName() + "\n");
            }
        }

        if(errors.length() > 0)
            throw new IllegalStateException(errors.toString());

    }

    public List getPlacementPoints(Territory terr)
    {
        return (List) m_place.get(terr.getName());
    }

    public List getPolygons(String terr)
    {
        return (List)  m_polys.get(terr);
    }


    public List getPolygons(Territory terr)
    {
        return getPolygons(terr.getName());
    }

    public Point getCenter(String terr)
    {
        return (Point) m_centers.get(terr);
    }


    public Point getCenter(Territory terr)
    {
        return getCenter(terr.getName());
    }

    /**
     * Get the territory at the x,y co-ordinates
     * could be null.
     */
    public String getTerritoryAt(int x, int y)
    {
        String seaName = null;

        //try to find a land territory.
        //sea zones often surround a land territory
        Iterator keyIter = m_polys.keySet().iterator();
        while (keyIter.hasNext())
        {
            String name = (String) keyIter.next();
            Collection polygons = (Collection) m_polys.get(name);
            Iterator polyIter = polygons.iterator();
            while (polyIter.hasNext())
            {
                Polygon poly = (Polygon) polyIter.next();
                if (poly.contains(x,y))
                {
                    if (name.endsWith("Sea Zone"))
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



    public Rectangle getBoundingRect(Territory terr)
    {
        String name = terr.getName();
        return getBoundingRect(name);
    }

    public Rectangle getBoundingRect(String name)
    {
        List polys = getPolygons(name);
        Rectangle bounds = null;

        Iterator iter = polys.iterator();

        while (iter.hasNext())
        {
            Polygon item = (Polygon) iter.next();
            if(bounds == null)
                bounds = item.getBounds();
            else
                bounds.add(item.getBounds());
        }

        return bounds;
    }

    /**
     * Get the territories that intersect or are bound by this shapes bounding rect.
     *
     * @return List of territory names as Strings
     */
    public List intersectsOrIsContainedIn(Shape s)
    {
        Rectangle2D bounds = s.getBounds();
        List rVal = new ArrayList();

        Iterator terrIter = getTerritories().iterator();
        while (terrIter.hasNext())
        {
            String terr = (String) terrIter.next();

            List polygons = getPolygons(terr);
            for(int i = 0; i < polygons.size(); i++)
            {
                Polygon item = (Polygon) polygons.get(i);
                if(item.intersects(bounds) || item.contains(bounds))
                {
                    rVal.add(terr);
                    //only add it once
                    break;
                }
            }
        }

        return rVal;
    }



}
