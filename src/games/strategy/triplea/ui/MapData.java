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

import games.strategy.engine.data.*;
import games.strategy.triplea.Constants;
import games.strategy.util.PointFileReaderWriter;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;

/**
 * contains data about the territories useful for drawing
 */

public class MapData
{
    private final String DEFAULT_UNIT_SCALE_PROPERTY = "units.scale";
    
    private static final String CENTERS_FILE = "centers.txt";
    private static final String POLYGON_FILE = "polygons.txt";
    private static final String PLACEMENT_FILE = "place.txt";
    private static final String MAP_PROPERTIES = "map.properties";

    //default colour if none is defined.
    private final List m_defaultColours = new ArrayList(Arrays.asList(new Color[]
    { Color.RED, Color.MAGENTA, Color.YELLOW, Color.ORANGE, Color.CYAN, Color.GREEN, Color.PINK, Color.GRAY }));

    //maps PlayerName as String to Color
    private Map m_playerColors = new HashMap();
    {
        //default for null player
        m_playerColors.put(PlayerID.NULL_PLAYERID.getName(), new Color(204, 153, 51));        
    }
    
    //maps String -> List of points
    private Map m_place;

    //maps String -> Collection of Polygons
    private Map m_polys;

    //maps String -> Point
    private Map m_centers;

    //maps String -> List of String
    private Map m_contains;

    private static MapData s_instance;

    private Properties m_mapProperties;

    /**
     * setMapDir(java.lang.String)
     * 
     * sets the map dir from outside
     * 
     * @param java.lang.String
     *            mapDir the given map directory
     */
    public static void setMapDir(String mapDir)
    {
        s_instance = new MapData(mapDir);
    }

    public static MapData getInstance()
    {
        return s_instance;
    }

    /**
     * Constructor TerritoryData(java.lang.String)
     * 
     * Sets the map directory for this instance of TerritoryData
     * 
     * @param java.lang.String
     *            mapNameDir the given map directory
     *  
     */
    private MapData(String mapNameDir)
    {
        try
        {
            String prefix = Constants.MAP_DIR + mapNameDir + java.io.File.separator;
	
            m_place = PointFileReaderWriter.readOneToMany(this.getClass().getResourceAsStream(prefix + PLACEMENT_FILE));
            m_polys = PointFileReaderWriter.readOneToManyPolygons(this.getClass().getResourceAsStream(prefix + POLYGON_FILE));
            m_centers = PointFileReaderWriter.readOneToOne(this.getClass().getResourceAsStream(prefix + CENTERS_FILE));
            m_mapProperties = new Properties();
            
            try
            {
                URL url = this.getClass().getResource(prefix + MAP_PROPERTIES);
                if(url == null)
                    throw new IllegalStateException("No map.properties file defined");
                m_mapProperties.load(url.openStream());
            }
            catch(Exception e)
            {
                System.out.println("Error reading map.properties:" + e);
            }

            initializeContains();
        } catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    public double getDefaultUnitScale()
    {
        
        if(m_mapProperties.getProperty(DEFAULT_UNIT_SCALE_PROPERTY) == null)
            return 1.0;
        
        try
        {
            return Double.parseDouble(m_mapProperties.getProperty(DEFAULT_UNIT_SCALE_PROPERTY));
        } catch (NumberFormatException e)
        {
            e.printStackTrace();
            return 1.0;
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
                String landTerritory = (String) landIter.next();
                if (landTerritory.endsWith("Sea Zone"))
                    continue;

                Polygon landPoly = (Polygon) getPolygons(landTerritory).iterator().next();
                Polygon seaPoly = (Polygon) getPolygons(seaTerritory).iterator().next();
                if (seaPoly.contains(landPoly.getBounds()))
                {
                    contained.add(landTerritory);
                }
            }

            if (!contained.isEmpty())
                m_contains.put(seaTerritory, contained);
        }
    }

    
    public Color getPlayerColor(String playerName)
    {
        //already loaded, just return
        if(m_playerColors.containsKey(playerName))
            return (Color) m_playerColors.get(playerName);
        
        //look in map.properties
        String propertiesKey = "color." + playerName;
        if(m_mapProperties.getProperty(propertiesKey) != null)
        {
            String colorString = m_mapProperties.getProperty(propertiesKey);
            if(colorString.length() != 6)
            {
                throw new IllegalStateException("Player colors must be a 6 digit hex number, eg FF0011, not:" + colorString);
            }
            try
            {
                Integer colorInt = Integer.decode("0x" +colorString);
                Color color = new Color(colorInt.intValue());
                m_playerColors.put(playerName, color);
                return color;
            }
            catch(NumberFormatException nfe)
            {
                throw new IllegalStateException("Player colors must be a 6 digit hex number, eg FF0011");
            }
        }
        
        //dont crash, use one of our default colors
        //its ugly, but usable
        System.out.println("No color defined for " + playerName + ".  Edit map.properties in the map folder to set it");
        Color color = (Color) m_defaultColours.remove(0);
        m_playerColors.put(playerName, color);
        return color;

            
    }
    
    /**
     * 
     * @return a Set of territory names as Strings. generally this shouldnt be
     *         used, instead you should use aGameData.getMap().getTerritories()
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
     * returns the name of the territory contained in the given territory. This
     * applies to islands within sea zones.
     * 
     * @return possiblly null
     */
    public List getContainedTerritory(String territoryName)
    {
        return (List) m_contains.get(territoryName);
    }

    public void verify(GameData data)
    {
        verifyKeys(data, m_centers, "centers");
        verifyKeys(data, m_polys, "polygons");
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
                errors.append("Territory in file could not be found in game data for "
		               + dataTypeForErrorMessage + ". Territory name <" + name + ">\n");
        }

        Iterator territories = data.getMap().getTerritories().iterator();

        Set keySet = aMap.keySet();
        while (territories.hasNext())
        {
            Territory terr = (Territory) territories.next();
            if (!keySet.contains(terr.getName()))
            {
                errors.append("No data of type " + dataTypeForErrorMessage + " for territory:" + terr.getName() + "\n");
            }
        }

        if (errors.length() > 0)
            throw new IllegalStateException(errors.toString());

    }

    public List getPlacementPoints(Territory terr)
    {
        return (List) m_place.get(terr.getName());
    }

    public List getPolygons(String terr)
    {
        return (List) m_polys.get(terr);
    }

    public List getPolygons(Territory terr)
    {
        return getPolygons(terr.getName());
    }

    public Point getCenter(String terr)
    {
        return new Point((Point) m_centers.get(terr));
    }

    public Point getCenter(Territory terr)
    {
        return getCenter(terr.getName());
    }

    /**
     * Get the territory at the x,y co-ordinates could be null.
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
                if (poly.contains(x, y))
                {
                    if (name.endsWith("Sea Zone"))
                    {
                        seaName = name;
                    } else
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
        if (polys == null)
            throw new IllegalStateException("No polygons found for:" + name);

        Rectangle bounds = null;

        for (int i = 0; i < polys.size(); i++)
        {
            Polygon item = (Polygon) polys.get(i);
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
    public List territoriesThatOverlap(Rectangle2D bounds)
    {
        List rVal = null;

        Iterator terrIter = getTerritories().iterator();
        while (terrIter.hasNext())
        {
            String terr = (String) terrIter.next();

            List polygons = getPolygons(terr);
            for (int i = 0; i < polygons.size(); i++)
            {
                Polygon item = (Polygon) polygons.get(i);
                if (item.intersects(bounds) || item.contains(bounds) || bounds.contains(item.getBounds2D()))
                {
                    if (rVal == null)
                        rVal = new ArrayList(4);

                    rVal.add(terr);
                    //only add it once
                    break;
                }
            }
        }
        if (rVal == null)
            return Collections.EMPTY_LIST;

        return rVal;

    }

}
